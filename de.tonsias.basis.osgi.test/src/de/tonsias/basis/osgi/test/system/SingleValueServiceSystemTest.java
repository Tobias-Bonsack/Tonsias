package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleBooleanValue;
import de.tonsias.basis.model.impl.value.SingleFloatValue;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.LinkedInstanzChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueNewEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.ValueChangeEvent;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The registered {@code SingleValueServiceImpl} against the real key service,
 * the real persistence services and the real instanz service on the other side
 * of every link.
 * <p>
 * An attribute is split over two files - the value in its own, the name in the
 * owning instanz - so nearly every call here has to be checked on both ends.
 * The folder an attribute lives in comes from its {@link SingleValueType}, and
 * because the delta bookkeeping only ever knows keys, the delete path has to
 * find that folder again on its own.
 * </p>
 */
public class SingleValueServiceSystemTest {

	private static final String STRING_PATH = SingleValueType.SINGLE_STRING.getPath();

	IInstanzService _inse;

	ISingleValueService _svs;

	IInstanz _owner;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_owner = _inse.createInstanz(ROOT, Type.SEND);

		_recorder = EventRecorder.subscribeToAllDeltas(ProductRuntime.broker());
	}

	@AfterEach
	void afterEach() {
		_recorder.unsubscribe();
		ProductRuntime.flushDeltas();
	}

	private SingleStringValue newStringValue(String name, String value) {
		return _svs.createNew(SingleStringValue.class, _owner.getOwnKey(), name, value, Type.SEND);
	}

	// ---------- createNew ----------

	@Test
	void testCreateNew_linksTheOwnerBothWaysAndFiresNew() {
		_recorder.clear();

		SingleStringValue created = newStringValue("parameter", "content");

		assertThat(created.getValue(), is("content"));
		assertThat(created.getConnectedInstanzKeys(), contains(_owner.getOwnKey()));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_STRING).get(created.getOwnKey()), is("parameter"));

		SingleValueNewEvent data = _recorder.onlyDataOf(SingleValueEventConstants.NEW, SingleValueNewEvent.class);
		assertThat(data._key(), is(created.getOwnKey()));
		assertThat(data._type(), is(SingleValueType.SINGLE_STRING));
		assertThat(data._name(), is("parameter"));
		assertThat(data._ownerKeys(), contains(_owner.getOwnKey()));
	}

	@Test
	void testCreateNew_integerValueLandsInTheIntegerMap() {
		SingleIntegerValue created = _svs.createNew(SingleIntegerValue.class, _owner.getOwnKey(), "number", 42,
				Type.SEND);

		assertThat(created, is(instanceOf(SingleIntegerValue.class)));
		assertThat(created.getValue(), is(42));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_INTEGER).get(created.getOwnKey()), is("number"));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_STRING).containsKey(created.getOwnKey()), is(false));
	}

	@Test
	void testCreateNew_booleanValueLandsInTheBooleanMap() {
		_recorder.clear();

		SingleBooleanValue created = _svs.createNew(SingleBooleanValue.class, _owner.getOwnKey(), "flag", true,
				Type.SEND);

		assertThat(created.getValue(), is(true));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_BOOLEAN).get(created.getOwnKey()), is("flag"));
		assertThat(_recorder.onlyDataOf(SingleValueEventConstants.NEW, SingleValueNewEvent.class)._type(),
				is(SingleValueType.SINGLE_BOOLEAN));
	}

	@Test
	void testCreateNew_floatValueLandsInTheFloatMap() {
		_recorder.clear();

		// the dialog and InstanzView hand the value on as text
		SingleFloatValue created = _svs.createNew(SingleFloatValue.class, _owner.getOwnKey(), "ratio", "3.14",
				Type.SEND);

		assertThat(created.getValue(), is(3.14f));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_FLOAT).get(created.getOwnKey()), is("ratio"));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_INTEGER).containsKey(created.getOwnKey()), is(false));
		assertThat(_recorder.onlyDataOf(SingleValueEventConstants.NEW, SingleValueNewEvent.class)._type(),
				is(SingleValueType.SINGLE_FLOAT));
	}

	/**
	 * The whole way out and back in: the value goes to its own folder and comes off
	 * disk as the same number, through the type variable {@code ASingleValue}
	 * declares its value with.
	 */
	@Test
	void testSaveAll_aFloatValueSurvivesTheRoundTripThroughItsOwnFolder() {
		SingleFloatValue created = _svs.createNew(SingleFloatValue.class, _owner.getOwnKey(), "ratio", "-0.5",
				Type.SEND);

		assertThat(_svs.saveAll(Set.of(created.getOwnKey())), is(true));

		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_FLOAT, created.getOwnKey()), is(true));
		SingleFloatValue reloaded = ProductRuntime.reloadValue(SingleValueType.SINGLE_FLOAT, created.getOwnKey(),
				SingleFloatValue.class);
		assertThat(reloaded.getValue(), is(-0.5f));
		assertThat(reloaded.getConnectedInstanzKeys(), contains(_owner.getOwnKey()));
	}

	@Test
	void testCreateNew_nullArgumentsAreRejected() {
		assertThrows(NullPointerException.class,
				() -> _svs.createNew(SingleStringValue.class, null, "parameter", "content", Type.SEND));
		assertThrows(NullPointerException.class,
				() -> _svs.createNew(SingleStringValue.class, _owner.getOwnKey(), "parameter", null, Type.SEND));
	}

	/** No {@code SingleValueType} maps to it, so there is nothing to build. */
	@Test
	void testCreateNew_anUnknownValueClassIsRejectedAndSilent() {
		_recorder.clear();

		assertThat(_svs.createNew(UnknownValue.class, _owner.getOwnKey(), "parameter", "content", Type.SEND),
				is(nullValue()));

		assertThat(_recorder.events(), hasSize(0));
	}

	// ---------- resolveKey ----------

	@Test
	void testResolveKey_blankKeyIsEmpty() {
		assertThat(_svs.resolveKey(STRING_PATH, null, SingleStringValue.class), is(Optional.empty()));
		assertThat(_svs.resolveKey(STRING_PATH, "  ", SingleStringValue.class), is(Optional.empty()));
	}

	/**
	 * {@code saveAll(Set)} asks with a null path and expects the cache to answer -
	 * a value it does not hold can then not be found.
	 */
	@Test
	void testResolveKey_withoutAPathOnlyTheCacheAnswers() {
		SingleStringValue cached = newStringValue("parameter", "content");

		assertThat(_svs.resolveKey(null, cached.getOwnKey(), SingleStringValue.class).get(), is(sameInstance(cached)));
		assertThat(_svs.resolveKey(null, "no-such-key", SingleStringValue.class), is(Optional.empty()));
	}

	@Test
	void testResolveKey_readsBackWhatWasWrittenAndThenCachesIt() {
		SingleStringValue created = newStringValue("parameter", "content");
		String key = created.getOwnKey();
		ProductRuntime.flushDeltas();
		_svs.removeFromCache(key);

		Optional<SingleStringValue> reloaded = _svs.resolveKey(STRING_PATH, key, SingleStringValue.class);

		assertThat(reloaded.get().getValue(), is("content"));
		assertThat(reloaded.get().getConnectedInstanzKeys(), contains(_owner.getOwnKey()));
		// second call is the cached one, not another read
		assertThat(_svs.resolveKey(null, key, SingleStringValue.class).get(), is(sameInstance(reloaded.get())));
	}

	/** Asking for the wrong type must not hand out a value of another one. */
	@Test
	void testResolveKey_aCachedValueOfAnotherTypeIsEmpty() {
		SingleStringValue created = newStringValue("parameter", "content");

		assertThat(_svs.resolveKey(SingleValueType.SINGLE_INTEGER.getPath(), created.getOwnKey(),
				SingleIntegerValue.class), is(Optional.empty()));
	}

	@Test
	void testResolveKeys_skipsWhatCannotBeResolved() {
		SingleStringValue known = newStringValue("parameter", "content");

		Collection<SingleStringValue> resolved = _svs.resolveKeys(SingleStringValue.class, STRING_PATH,
				List.of(known.getOwnKey(), "no-such-key"));

		assertThat(resolved, contains(known));
	}

	// ---------- changeValue ----------

	@Test
	void testChangeValue_firesValueChangeWithOldAndNewValue() {
		SingleStringValue value = newStringValue("parameter", "old");
		_recorder.clear();

		assertThat(_svs.changeValue(value.getOwnKey(), "new", Type.SEND), is(true));

		ValueChangeEvent data = _recorder.onlyDataOf(SingleValueEventConstants.VALUE_CHANGE, ValueChangeEvent.class);
		assertThat(data._key(), is(value.getOwnKey()));
		assertThat(data._type(), is(SingleValueType.SINGLE_STRING));
		assertThat(data._oldValue(), is("old"));
		assertThat(data._newValue(), is("new"));

		assertThat(value.getValue(), is("new"));
	}

	@Test
	void testChangeValue_toTheValueItAlreadyHasIsSilent() {
		SingleStringValue value = newStringValue("parameter", "same");
		_recorder.clear();

		assertThat(_svs.changeValue(value.getOwnKey(), "same", Type.SEND), is(false));

		assertThat(_recorder.events(), hasSize(0));
	}

	/** A string value can not take a number, and rejecting it is not a change. */
	@Test
	void testChangeValue_anUnusableInputIsSilent() {
		SingleStringValue value = newStringValue("parameter", "text");
		_recorder.clear();

		assertThat(_svs.changeValue(value.getOwnKey(), 42, Type.SEND), is(false));

		assertThat(value.getValue(), is("text"));
		assertThat(_recorder.events(), hasSize(0));
	}

	// ---------- cache ----------

	@Test
	void testRemoveFromCache_forgetsTheValueUntilItIsLoadedAgain() {
		SingleStringValue value = newStringValue("parameter", "content");

		assertThat(_svs.removeFromCache(value.getOwnKey()), is(true));

		assertThat(_svs.resolveKey(null, value.getOwnKey(), SingleStringValue.class), is(Optional.empty()));
	}

	@Test
	void testRemoveFromCache_anUnknownKeyReportsFailure() {
		assertThat(_svs.removeFromCache("no-such-key"), is(false));
	}

	// ---------- addToParent ----------

	@Test
	void testAddToParent_linksTheSecondOwnerBothWays() {
		SingleStringValue value = newStringValue("parameter", "content");
		IInstanz secondOwner = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), secondOwner.getOwnKey(),
				Type.SEND), is(true));

		LinkedInstanzChangeEvent data = _recorder.onlyDataOf(SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				LinkedInstanzChangeEvent.class);
		assertThat(data._key(), is(value.getOwnKey()));
		assertThat(data._changeType(), is(LinkedInstanzChangeEvent.ChangeType.ADD));
		assertThat(data._instanzKeys(), contains(secondOwner.getOwnKey()));

		assertThat(value.getConnectedInstanzKeys(),
				containsInAnyOrder(_owner.getOwnKey(), secondOwner.getOwnKey()));
		// the chain carries no name for the new owner, so the key stands in for it
		assertThat(secondOwner.getSingleValues(SingleValueType.SINGLE_STRING).get(value.getOwnKey()),
				is(value.getOwnKey()));
	}

	@Test
	void testAddToParent_anOwnerItAlreadyHasIsSilent() {
		SingleStringValue value = newStringValue("parameter", "content");
		_recorder.clear();

		assertThat(_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), _owner.getOwnKey(), Type.SEND),
				is(false));

		assertThat(_recorder.events(), hasSize(0));
	}

	@Test
	void testAddToParent_anUnresolvableValueIsRejected() {
		assertThat(_svs.addToParent(SingleValueType.SINGLE_STRING, "no-such-key", _owner.getOwnKey(), Type.SEND),
				is(false));
	}

	// ---------- delete ----------

	/**
	 * The connections are cut before the event goes out, so the payload has to
	 * carry its own copy of the owners - they could not be found afterwards, and
	 * they are exactly who has to drop the value key.
	 */
	@Test
	void testMarkSingleValueAsDelete_theEventStillNamesEveryOwner() {
		SingleStringValue value = newStringValue("parameter", "content");
		IInstanz secondOwner = _inse.createInstanz(ROOT, Type.SEND);
		_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), secondOwner.getOwnKey(), Type.SEND);
		_recorder.clear();

		_svs.markSingleValueAsDelete(value.getOwnKey(), Type.SEND);

		SingleValueDeleteEvent data = _recorder.onlyDataOf(SingleValueEventConstants.DELETE,
				SingleValueDeleteEvent.class);
		assertThat(data._key(), is(value.getOwnKey()));
		assertThat(data._type(), is(SingleValueType.SINGLE_STRING));
		assertThat(data._ownerKeys(), containsInAnyOrder(_owner.getOwnKey(), secondOwner.getOwnKey()));

		assertThat(value.getConnectedInstanzKeys(), hasSize(0));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat(secondOwner.getSingleValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()),
				is(false));
	}

	@Test
	void testRemoveValue_endsUpWhereMarkAsDeleteDoes() {
		SingleStringValue value = newStringValue("parameter", "content");
		_recorder.clear();

		assertThat(_svs.removeValue(value, Type.SEND), is(true));

		assertThat(_recorder.onlyDataOf(SingleValueEventConstants.DELETE, SingleValueDeleteEvent.class)._key(),
				is(value.getOwnKey()));
		assertThat(_owner.getSingleValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
	}

	// ---------- persistence hand-off ----------

	@Test
	void testSaveAll_writesTheCachedValuesOfTheGivenKeys() {
		SingleStringValue value = newStringValue("parameter", "content");

		assertThat(_svs.saveAll(Set.of(value.getOwnKey(), "not-cached")), is(true));

		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_STRING, value.getOwnKey()), is(true));
		assertThat(ProductRuntime
				.reloadValue(SingleValueType.SINGLE_STRING, value.getOwnKey(), SingleStringValue.class).getValue(),
				is("content"));
	}

	/**
	 * Which folder a value lives in depends on its type, and while it is cached the
	 * value itself says which one that is.
	 */
	@Test
	void testDeleteAll_removesACachedValueFromItsOwnFolder() {
		SingleIntegerValue value = _svs.createNew(SingleIntegerValue.class, _owner.getOwnKey(), "number", 42,
				Type.SEND);
		_svs.saveAll(Set.of(value.getOwnKey()));
		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_INTEGER, value.getOwnKey()), is(true));

		assertThat(_svs.deleteAll(Set.of(value.getOwnKey())), is(true));

		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_INTEGER, value.getOwnKey()), is(false));
	}

	/**
	 * A value that is no longer cached no longer tells its type, so the folders
	 * have to be tried one after the other - the string folder comes first and does
	 * not hold it.
	 */
	@Test
	void testDeleteAll_findsAnUncachedValueByTryingEveryFolder() {
		SingleIntegerValue value = _svs.createNew(SingleIntegerValue.class, _owner.getOwnKey(), "number", 42,
				Type.SEND);
		String key = value.getOwnKey();
		_svs.saveAll(Set.of(key));
		_svs.removeFromCache(key);

		assertThat(_svs.deleteAll(Set.of(key)), is(true));

		assertThat(ProductRuntime.valueFileExists(SingleValueType.SINGLE_INTEGER, key), is(false));
	}

	/**
	 * Coming up empty in every folder is no failure: no file is the state the
	 * delete is after, and a value created and dropped again before any save never
	 * had one.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/53">#53</a>
	 */
	@Test
	void testDeleteAll_aValueInNoFolderAtAllIsNoFailure() {
		assertThat(_svs.deleteAll(Set.of("no-such-key")), is(true));
	}

	/**
	 * What does fail still has to surface, and one failure must not hide the others
	 * - {@code saveDeltas} hands over a whole set at once.
	 */
	@Test
	void testDeleteAll_collectsEveryFailureInOneException() {
		Path firstBlocked = ProductRuntime.valueFile(SingleValueType.SINGLE_STRING, "blocked-1");
		Path secondBlocked = ProductRuntime.valueFile(SingleValueType.SINGLE_STRING, "blocked-2");
		ProductRuntime.block(firstBlocked);
		ProductRuntime.block(secondBlocked);

		try {
			CompletionException thrown = assertThrows(CompletionException.class,
					() -> _svs.deleteAll(Set.of("blocked-1", "blocked-2")));

			assertThat(List.of(thrown.getSuppressed()), hasSize(2));
		} finally {
			ProductRuntime.unblock(firstBlocked);
			ProductRuntime.unblock(secondBlocked);
		}
	}

	@Test
	void testDeleteAll_anEmptySetIsANoOp() {
		assertThat(_svs.deleteAll(Set.of()), is(true));
	}

	/** An {@link ISingleValue} that no {@code SingleValueType} knows about. */
	private static final class UnknownValue implements ISingleValue<String> {

		@Override
		public String getOwnKey() {
			return "unknown";
		}

		@Override
		public String getPath() {
			return "unknown/";
		}

		@Override
		public String getValue() {
			return null;
		}

		@Override
		public boolean setValue(String value) {
			return false;
		}

		@Override
		public boolean tryToSetValue(Object value) {
			return false;
		}

		@Override
		public Collection<String> getConnectedInstanzKeys() {
			return List.of();
		}

		@Override
		public boolean addConnectedInstanzKey(String key) {
			return false;
		}

		@Override
		public boolean removeConnection(Collection<String> connectedInstanzKeys) {
			return false;
		}
	}
}
