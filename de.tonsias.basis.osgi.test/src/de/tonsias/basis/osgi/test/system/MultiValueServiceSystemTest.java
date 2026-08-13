package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.ElementsChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueNewEvent;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The registered {@code MultiValueServiceImpl} against the real key service, the
 * real persistence services and the real instanz service on the other side of
 * every link - the mirror of {@code SingleValueServiceSystemTest}.
 * <p>
 * Its own registration is under test here as well: a component whose descriptor,
 * manifest entry or {@code build.properties} line is missing never activates, and
 * {@code ProductRuntime.multiValueService()} is what says so.
 * </p>
 */
public class MultiValueServiceSystemTest {

	private static final String STRING_PATH = MultiValueType.MULTI_STRING.getPath();

	IInstanzService _inse;

	IMultiValueService _mvs;

	IInstanz _owner;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_mvs = ProductRuntime.multiValueService();
		_owner = _inse.createInstanz(ROOT, Type.SEND);

		_recorder = EventRecorder.subscribeToAllDeltas(ProductRuntime.broker());
	}

	@AfterEach
	void afterEach() {
		_recorder.unsubscribe();
		ProductRuntime.flushDeltas();
	}

	private MultiStringValue newStringList(String name, String... elements) {
		return _mvs.createNew(MultiStringValue.class, _owner.getOwnKey(), name, List.of(elements), Type.SEND);
	}

	// ---------- createNew ----------

	@Test
	void testCreateNew_linksTheOwnerBothWaysAndFiresNew() {
		_recorder.clear();

		MultiStringValue created = newStringList("parameter", "a", "b");

		assertThat(created.getValues(), contains("a", "b"));
		assertThat(created.getConnectedInstanzKeys(), contains(_owner.getOwnKey()));
		assertThat(_owner.getValues(MultiValueType.MULTI_STRING).get(created.getOwnKey()), is("parameter"));

		MultiValueNewEvent data = _recorder.onlyDataOf(MultiValueEventConstants.NEW, MultiValueNewEvent.class);
		assertThat(data._key(), is(created.getOwnKey()));
		assertThat(data._type(), is(MultiValueType.MULTI_STRING));
		assertThat(data._name(), is("parameter"));
		assertThat(data._ownerKeys(), contains(_owner.getOwnKey()));
		assertThat(data._elements(), contains("a", "b"));
	}

	/**
	 * The list of one content is a different attribute from the single value of the
	 * same content - the pair that would go unnoticed if the maps leaked.
	 */
	@Test
	void testCreateNew_landsInItsOwnMapAndNotTheSingleOne() {
		MultiIntegerValue created = _mvs.createNew(MultiIntegerValue.class, _owner.getOwnKey(), "numbers",
				List.of("1", 2), Type.SEND);

		assertThat(created.getValues(), contains(1, 2));
		assertThat(_owner.getValues(MultiValueType.MULTI_INTEGER).get(created.getOwnKey()), is("numbers"));
		assertThat(_owner.getValues(de.tonsias.basis.model.enums.SingleValueType.SINGLE_INTEGER)
				.containsKey(created.getOwnKey()), is(false));
		assertThat(_owner.getValues(MultiValueType.MULTI_STRING).containsKey(created.getOwnKey()), is(false));
	}

	/** an empty list is what a list says instead of a default value */
	@Test
	void testCreateNew_anEmptyListIsAValue() {
		MultiStringValue created = newStringList("empty");

		assertThat(created.getValues(), is(empty()));
		assertThat(_owner.getValues(MultiValueType.MULTI_STRING).get(created.getOwnKey()), is("empty"));
	}

	@Test
	void testCreateNew_aClassNoTypeMapsToIsNull() {
		assertThat(_mvs.createNew(de.tonsias.basis.model.interfaces.IMultiValue.class, _owner.getOwnKey(), "n",
				List.of(), Type.SEND), is(nullValue()));
	}

	// ---------- the elements ----------

	@Test
	void testAddElement_appendsAndFiresWhatWasAdded() {
		MultiStringValue value = newStringList("list", "a");
		_recorder.clear();

		assertThat(_mvs.addElement(value.getOwnKey(), "b", Type.SEND), is(true));

		assertThat(value.getValues(), contains("a", "b"));
		ElementsChangeEvent data = _recorder.onlyDataOf(MultiValueEventConstants.VALUES_CHANGE,
				ElementsChangeEvent.class);
		assertThat(data._addedElements(), contains("b"));
		assertThat(data._removedElements(), is(empty()));
	}

	/**
	 * The guard that ends a propagation chain: nothing changed, so nothing is fired
	 * and nobody comes back around.
	 */
	@Test
	void testAddElement_aDuplicateChangesNothingAndFiresNothing() {
		MultiStringValue value = newStringList("list", "a");
		_recorder.clear();

		assertThat(_mvs.addElement(value.getOwnKey(), "a", Type.SEND), is(false));

		assertThat(value.getValues(), contains("a"));
		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testAddElement_whatTheTypeWillNotReadIsRejected() {
		MultiIntegerValue value = _mvs.createNew(MultiIntegerValue.class, _owner.getOwnKey(), "numbers", List.of(1),
				Type.SEND);
		_recorder.clear();

		assertThat(_mvs.addElement(value.getOwnKey(), "abc", Type.SEND), is(false));

		assertThat(value.getValues(), contains(1));
		assertThat(_recorder.events(), is(empty()));
	}

	/**
	 * The event says what the list now holds, not what the caller typed: the widgets
	 * hand their input on as text, and an event carrying "2" for a list holding
	 * {@code 2} would send a listener looking for something that is not there.
	 */
	@Test
	void testAddElement_theEventCarriesTheConvertedElement() {
		MultiIntegerValue value = _mvs.createNew(MultiIntegerValue.class, _owner.getOwnKey(), "numbers", List.of(1),
				Type.SEND);
		_recorder.clear();

		_mvs.addElement(value.getOwnKey(), "2", Type.SEND);

		ElementsChangeEvent data = _recorder.onlyDataOf(MultiValueEventConstants.VALUES_CHANGE,
				ElementsChangeEvent.class);
		assertThat(data._addedElements(), contains(2));
	}

	@Test
	void testRemoveElement_takesItOutAndFiresWhatWent() {
		MultiStringValue value = newStringList("list", "a", "b");
		_recorder.clear();

		assertThat(_mvs.removeElement(value.getOwnKey(), "a", Type.SEND), is(true));

		assertThat(value.getValues(), contains("b"));
		ElementsChangeEvent data = _recorder.onlyDataOf(MultiValueEventConstants.VALUES_CHANGE,
				ElementsChangeEvent.class);
		assertThat(data._removedElements(), contains("a"));
		assertThat(data._addedElements(), is(empty()));
	}

	@Test
	void testRemoveElement_whatIsNotThereChangesNothing() {
		MultiStringValue value = newStringList("list", "a");
		_recorder.clear();

		assertThat(_mvs.removeElement(value.getOwnKey(), "b", Type.SEND), is(false));
		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testChangeElements_firesBothDirectionsAtOnce() {
		MultiStringValue value = newStringList("list", "a", "b");
		_recorder.clear();

		assertThat(_mvs.changeElements(value.getOwnKey(), List.of("b", "c"), Type.SEND), is(true));

		assertThat(value.getValues(), contains("b", "c"));
		ElementsChangeEvent data = _recorder.onlyDataOf(MultiValueEventConstants.VALUES_CHANGE,
				ElementsChangeEvent.class);
		assertThat(data._addedElements(), contains("c"));
		assertThat(data._removedElements(), contains("a"));
	}

	/**
	 * The order is part of the value, so it has to be written out - and the event is
	 * what tells the delta service to. Nothing started or stopped pointing anywhere,
	 * so both collections are empty, which is a different thing from firing nothing.
	 */
	@Test
	void testChangeElements_aReorderIsAChangeWithNothingAddedOrRemoved() {
		MultiStringValue value = newStringList("list", "a", "b");
		_recorder.clear();

		assertThat(_mvs.changeElements(value.getOwnKey(), List.of("b", "a"), Type.SEND), is(true));

		assertThat(value.getValues(), contains("b", "a"));
		ElementsChangeEvent data = _recorder.onlyDataOf(MultiValueEventConstants.VALUES_CHANGE,
				ElementsChangeEvent.class);
		assertThat(data._addedElements(), is(empty()));
		assertThat(data._removedElements(), is(empty()));
	}

	@Test
	void testChangeElements_theSameListChangesNothingAndFiresNothing() {
		MultiStringValue value = newStringList("list", "a", "b");
		_recorder.clear();

		assertThat(_mvs.changeElements(value.getOwnKey(), List.of("a", "b"), Type.SEND), is(false));
		assertThat(_recorder.events(), is(empty()));
	}

	/** one bad element fails the whole call rather than landing half a list */
	@Test
	void testChangeElements_oneBadElementLeavesTheListAsItWas() {
		MultiFloatValue value = _mvs.createNew(MultiFloatValue.class, _owner.getOwnKey(), "ratios", List.of("1.5"),
				Type.SEND);
		_recorder.clear();

		assertThat(_mvs.changeElements(value.getOwnKey(), List.of("2.5", "NaN"), Type.SEND), is(false));

		assertThat(value.getValues(), contains(1.5f));
		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testChangeElements_anUnknownKeyIsNoChange() {
		assertThat(_mvs.changeElements("no-such-key", List.of("a"), Type.SEND), is(false));
	}

	// ---------- resolving ----------

	@Test
	void testResolveKey_theSameObjectComesBackFromTheCache() {
		MultiStringValue created = newStringList("list", "a");

		assertThat(_mvs.resolveKey(STRING_PATH, created.getOwnKey(), MultiStringValue.class).get(),
				is(org.hamcrest.Matchers.sameInstance(created)));
	}

	@Test
	void testResolveKey_blankKeyIsEmpty() {
		assertThat(_mvs.resolveKey(STRING_PATH, "", MultiStringValue.class).isEmpty(), is(true));
		assertThat(_mvs.resolveKey(STRING_PATH, null, MultiStringValue.class).isEmpty(), is(true));
	}

	/**
	 * A miss in the wrong folder must not be remembered, or the key would never
	 * resolve again in this session - not even out of the right folder.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/79">#79</a>
	 */
	@Test
	void testResolveKey_aMissInTheWrongFolderIsNotRemembered() {
		MultiStringValue created = newStringList("list", "a");
		_mvs.saveAll(Set.of(created.getOwnKey()));
		_mvs.removeFromCache(created.getOwnKey());

		assertThat(_mvs.resolveKey(MultiValueType.MULTI_FLOAT.getPath(), created.getOwnKey(), MultiFloatValue.class)
				.isEmpty(), is(true));

		assertThat(_mvs.resolveKey(STRING_PATH, created.getOwnKey(), MultiStringValue.class).isPresent(), is(true));
	}

	/**
	 * A key alone does not say which type it is, so a value that is only on disk is
	 * found by trying one folder after the other.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/78">#78</a>
	 */
	@Test
	void testChangeElements_reachesAValueThatIsOnDiskOnly() {
		MultiStringValue created = newStringList("list", "a");
		String key = created.getOwnKey();
		_mvs.saveAll(Set.of(key));
		_mvs.removeFromCache(key);

		assertThat(_mvs.addElement(key, "b", Type.SEND), is(true));

		assertThat(_mvs.resolveAnyKey(key).get().getValues(), contains("a", "b"));
	}

	@Test
	void testResolveAnyKey_anUnknownKeyIsEmpty() {
		assertThat(_mvs.resolveAnyKey("no-such-key").isEmpty(), is(true));
		assertThat(_mvs.resolveAnyKey("").isEmpty(), is(true));
	}

	// ---------- saving and deleting ----------

	@Test
	void testSaveAll_aListSurvivesTheRoundTripThroughItsOwnFolder() {
		MultiFloatValue created = _mvs.createNew(MultiFloatValue.class, _owner.getOwnKey(), "ratios",
				List.of("1.5", "-0.5"), Type.SEND);

		assertThat(_mvs.saveAll(Set.of(created.getOwnKey())), is(true));

		assertThat(ProductRuntime.valueFileExists(MultiValueType.MULTI_FLOAT, created.getOwnKey()), is(true));
		MultiFloatValue reloaded = ProductRuntime.reloadValue(MultiValueType.MULTI_FLOAT, created.getOwnKey(),
				MultiFloatValue.class);
		assertThat(reloaded.getValues(), contains(1.5f, -0.5f));
		assertThat(reloaded.getConnectedInstanzKeys(), contains(_owner.getOwnKey()));
	}

	@Test
	void testMarkValueAsDelete_cutsEveryOwnerAndFiresTheElementsAlong() {
		MultiStringValue value = newStringList("list", "a", "b");
		_recorder.clear();

		_mvs.markValueAsDelete(value.getOwnKey(), Type.SEND);

		MultiValueDeleteEvent data = _recorder.onlyDataOf(MultiValueEventConstants.DELETE,
				MultiValueDeleteEvent.class);
		assertThat(data._ownerKeys(), contains(_owner.getOwnKey()));
		assertThat(data._elements(), contains("a", "b"));
		assertThat(value.getConnectedInstanzKeys(), is(empty()));
		assertThat(_owner.getValues(MultiValueType.MULTI_STRING).containsKey(value.getOwnKey()), is(false));
	}

	@Test
	void testMarkValueAsDelete_anUnknownKeyIsANoOp() {
		_recorder.clear();

		_mvs.markValueAsDelete("no-such-key", Type.SEND);

		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testDeleteAll_findsAnUncachedValueByTryingEveryFolder() {
		MultiStringValue created = newStringList("list", "a");
		String key = created.getOwnKey();
		_mvs.saveAll(Set.of(key));
		_mvs.removeFromCache(key);

		assertThat(_mvs.deleteAll(Set.of(key)), is(true));

		assertThat(ProductRuntime.valueFileExists(MultiValueType.MULTI_STRING, key), is(false));
	}

	/**
	 * A value created and dropped again before any save never got written, and no
	 * file is what the delete was after.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/53">#53</a>
	 */
	@Test
	void testDeleteAll_aValueWithoutAFileInAnyFolderIsNoFailure() {
		assertThat(_mvs.deleteAll(Set.of("never-written")), is(true));
		assertThat(_mvs.deleteAll(Set.of()), is(true));
	}

	@Test
	void testDeleteAll_collectsEveryFailureInOneException() {
		Path firstBlocked = ProductRuntime.valueFile(MultiValueType.MULTI_STRING, "mv-blocked-1");
		Path secondBlocked = ProductRuntime.valueFile(MultiValueType.MULTI_STRING, "mv-blocked-2");
		ProductRuntime.block(firstBlocked);
		ProductRuntime.block(secondBlocked);

		try {
			CompletionException thrown = assertThrows(CompletionException.class,
					() -> _mvs.deleteAll(Set.of("mv-blocked-1", "mv-blocked-2")));

			assertThat(List.of(thrown.getSuppressed()), hasSize(2));
		} finally {
			ProductRuntime.unblock(firstBlocked);
			ProductRuntime.unblock(secondBlocked);
		}
	}

	// ---------- the owner link ----------

	@Test
	void testAddToParent_recordsTheOwnerOnBothEnds() {
		MultiStringValue value = newStringList("list", "a");
		IInstanz second = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_mvs.addToParent(MultiValueType.MULTI_STRING, value.getOwnKey(), second.getOwnKey(), Type.SEND),
				is(true));

		assertThat(value.getConnectedInstanzKeys(), containsInAnyOrder(_owner.getOwnKey(), second.getOwnKey()));
		assertThat(second.getValues(MultiValueType.MULTI_STRING).containsKey(value.getOwnKey()), is(true));
	}

	/** already an owner - false without an event, so the chain ends here */
	@Test
	void testAddToParent_theSameOwnerTwiceChangesNothing() {
		MultiStringValue value = newStringList("list", "a");
		_recorder.clear();

		assertThat(_mvs.addToParent(MultiValueType.MULTI_STRING, value.getOwnKey(), _owner.getOwnKey(), Type.SEND),
				is(false));
		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testAddToParent_anUnknownValueIsFalse() {
		assertThat(_mvs.addToParent(MultiValueType.MULTI_STRING, "no-such-key", _owner.getOwnKey(), Type.SEND),
				is(false));
	}

	// ---------- the relation ----------

	@Test
	void testCreateNew_aRelationStoresEveryTargetKey() {
		IInstanz first = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz second = _inse.createInstanz(ROOT, Type.SEND);

		MultiInstanzValue created = _mvs.createNew(MultiInstanzValue.class, _owner.getOwnKey(), "points at",
				List.of(first.getOwnKey(), second.getOwnKey()), Type.SEND);

		assertThat(created.getValues(), contains(first.getOwnKey(), second.getOwnKey()));
		assertThat(_owner.getValues(MultiValueType.MULTI_INSTANZ).get(created.getOwnKey()), is("points at"));
	}
}
