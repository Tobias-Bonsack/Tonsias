package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.impl.SingleValueServiceImpl;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.LinkedInstanzChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueNewEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.ValueChangeEvent;

/**
 * Isolated tests for {@code SingleValueServiceImpl} - the cache, the events it
 * fires and the persistence hand-off, all against mocked collaborators.
 */
@ExtendWith(MockitoExtension.class)
public class SingleValueServiceImplUnitTest {

	private static final String STRING_PATH = SingleValueType.SINGLE_STRING.getPath();

	@Mock
	SaveService _saveService;

	@Mock
	LoadService _loadService;

	@Mock
	DeleteService _deleteService;

	@Mock
	IKeyService _keyService;

	@Mock
	IEventBrokerBridge _broker;

	@InjectMocks
	SingleValueServiceImpl _service;

	private SingleStringValue givenCreated(String key, String owner, Object value) {
		when(_keyService.generateKey()).thenReturn(key);
		return _service.createNew(SingleStringValue.class, owner, "paramName", value, Type.SEND);
	}

	// ---------- resolveKey ----------

	@Test
	void testResolveKey_blankKeyIsEmpty() {
		assertThat(_service.resolveKey(STRING_PATH, null, SingleStringValue.class), is(Optional.empty()));
		assertThat(_service.resolveKey(STRING_PATH, "  ", SingleStringValue.class), is(Optional.empty()));
		verifyNoInteractions(_loadService);
	}

	@Test
	void testResolveKey_blankPathAndNotCachedIsEmpty() {
		// saveAll(Set) relies on this: it asks with a null path and expects the cache
		assertThat(_service.resolveKey(null, "key", SingleStringValue.class), is(Optional.empty()));
		verifyNoInteractions(_loadService);
	}

	@Test
	void testResolveKey_loadsOnceAndThenCaches() {
		SingleStringValue onDisk = new SingleStringValue("key");
		when(_loadService.loadFromGson(STRING_PATH + "key", SingleStringValue.class)).thenReturn(onDisk);

		assertThat(_service.resolveKey(STRING_PATH, "key", SingleStringValue.class).get(), is(sameInstance(onDisk)));
		assertThat(_service.resolveKey(STRING_PATH, "key", SingleStringValue.class).get(), is(sameInstance(onDisk)));

		verify(_loadService).loadFromGson(STRING_PATH + "key", SingleStringValue.class);
	}

	@Test
	void testResolveKey_cachedValueOfAnotherTypeIsEmpty() {
		givenCreated("key", "owner", "value");

		assertThat(_service.resolveKey(SingleValueType.SINGLE_INTEGER.getPath(), "key", SingleIntegerValue.class),
				is(Optional.empty()));
	}

	@Test
	void testResolveKeys_skipsWhatCannotBeResolved() {
		SingleStringValue onDisk = new SingleStringValue("known");
		when(_loadService.loadFromGson(STRING_PATH + "known", SingleStringValue.class)).thenReturn(onDisk);
		when(_loadService.loadFromGson(STRING_PATH + "unknown", SingleStringValue.class)).thenReturn(null);

		var resolved = _service.resolveKeys(SingleStringValue.class, STRING_PATH, List.of("known", "unknown"));

		assertThat(resolved, contains(onDisk));
	}

	// ---------- createNew ----------

	@Test
	void testCreateNew_linksTheOwnerAndFiresNew() {
		SingleStringValue created = givenCreated("key", "owner", "value");

		assertThat(created.getOwnKey(), is("key"));
		assertThat(created.getValue(), is("value"));
		assertThat(created.getConnectedInstanzKeys(), contains("owner"));
		verify(_broker).send(SingleValueEventConstants.NEW, Map.of(IEventBroker.DATA, new SingleValueNewEvent(
				SingleValueType.SINGLE_STRING, "key", "paramName", List.of("owner"))));
	}

	@Test
	void testCreateNew_integerValue() {
		when(_keyService.generateKey()).thenReturn("key");

		SingleIntegerValue created = _service.createNew(SingleIntegerValue.class, "owner", "paramName", 42, Type.SEND);

		assertThat(created, is(instanceOf(SingleIntegerValue.class)));
		assertThat(created.getValue(), is(42));
	}

	@Test
	void testCreateNew_nullArgumentsAreRejected() {
		assertThrows(NullPointerException.class,
				() -> _service.createNew(SingleStringValue.class, null, "paramName", "value", Type.SEND));
		assertThrows(NullPointerException.class,
				() -> _service.createNew(SingleStringValue.class, "owner", "paramName", null, Type.SEND));
	}

	@Test
	void testCreateNew_unknownValueClassIsNull() {
		// no SingleValueType maps to it, so there is nothing the service could build
		assertThat(_service.createNew(UnknownValue.class, "owner", "paramName", "value", Type.SEND), is(nullValue()));
		verifyNoInteractions(_broker, _keyService);
	}

	// ---------- changeValue ----------

	@Test
	void testChangeValue_firesValueChangeWithOldAndNewValue() {
		givenCreated("key", "owner", "old");

		assertThat(_service.changeValue("key", "new", Type.SEND), is(true));

		verify(_broker).send(SingleValueEventConstants.VALUE_CHANGE, Map.of(IEventBroker.DATA,
				new ValueChangeEvent("key", SingleValueType.SINGLE_STRING, "old", "new")));
	}

	@Test
	void testChangeValue_sameValueIsSilent() {
		givenCreated("key", "owner", "same");

		assertThat(_service.changeValue("key", "same", Type.SEND), is(false));

		verify(_broker, never()).send(eq(SingleValueEventConstants.VALUE_CHANGE), any());
	}

	@Test
	void testChangeValue_unusableInputIsSilent() {
		givenCreated("key", "owner", "text");

		assertThat(_service.changeValue("key", 42, Type.SEND), is(false));

		verify(_broker, never()).send(eq(SingleValueEventConstants.VALUE_CHANGE), any());
	}

	// ---------- cache ----------

	@Test
	void testRemoveFromCache_knownKeys() {
		givenCreated("key", "owner", "value");

		assertThat(_service.removeFromCache("key"), is(true));
		assertThat(_service.resolveKey(null, "key", SingleStringValue.class), is(Optional.empty()));
	}

	@Test
	void testRemoveFromCache_unknownKey() {
		assertThat(_service.removeFromCache("nope"), is(false));
	}

	// ---------- addToParent ----------

	@Test
	void testAddToParent_firesInstanzListChange() {
		givenCreated("key", "owner", "value");

		assertThat(_service.addToParent(SingleValueType.SINGLE_STRING, "key", "second", Type.SEND), is(true));

		verify(_broker).send(SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				Map.of(IEventBroker.DATA, new LinkedInstanzChangeEvent("key", SingleValueType.SINGLE_STRING,
						LinkedInstanzChangeEvent.ChangeType.ADD, Set.of("second"))));
	}

	@Test
	void testAddToParent_knownOwnerIsSilent() {
		givenCreated("key", "owner", "value");

		assertThat(_service.addToParent(SingleValueType.SINGLE_STRING, "key", "owner", Type.SEND), is(false));

		verify(_broker, never()).send(eq(SingleValueEventConstants.INSTANZ_LIST_CHANGE), any());
	}

	@Test
	void testAddToParent_unresolvableValueIsRejected() {
		when(_loadService.loadFromGson(STRING_PATH + "key", SingleStringValue.class)).thenReturn(null);

		assertThat(_service.addToParent(SingleValueType.SINGLE_STRING, "key", "owner", Type.SEND), is(false));
	}

	// ---------- delete ----------

	/**
	 * The delete event is what tells every owning instanz to drop the value key, so
	 * it has to still carry those owners - clearing the connections first must not
	 * empty the payload.
	 */
	@Test
	void testMarkSingleValueAsDelete_eventKeepsTheOwners() {
		givenCreated("key", "owner", "value");

		_service.markSingleValueAsDelete("key", Type.SEND);

		ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
		verify(_broker).send(eq(SingleValueEventConstants.DELETE), data.capture());

		@SuppressWarnings("unchecked")
		var payload = (SingleValueDeleteEvent) ((Map<String, Object>) data.getValue()).get(IEventBroker.DATA);
		assertThat(payload._key(), is("key"));
		assertThat(payload._type(), is(SingleValueType.SINGLE_STRING));
		assertThat(payload._ownerKeys(), contains("owner"));
	}

	@Test
	void testMarkSingleValueAsDelete_connectionsAreCleared() {
		SingleStringValue created = givenCreated("key", "owner", "value");

		_service.markSingleValueAsDelete("key", Type.SEND);

		assertThat(created.getConnectedInstanzKeys(), hasSize(0));
	}

	@Test
	void testRemoveValue_firesDelete() {
		SingleStringValue created = givenCreated("key", "owner", "value");

		assertThat(_service.removeValue(created, Type.SEND), is(true));

		verify(_broker).send(eq(SingleValueEventConstants.DELETE), any());
	}

	// ---------- persistence hand-off ----------

	@Test
	void testSaveAll_savesTheCachedValuesOfTheGivenKeys() {
		SingleStringValue created = givenCreated("key", "owner", "value");

		assertThat(_service.saveAll(Set.of("key", "notCached")), is(true));

		verify(_saveService).safeAsGson(created, SingleStringValue.class);
	}

	/**
	 * {@code DeleteService.deleteFile(..)} expects the path of a file, the delta
	 * bookkeeping only knows keys.
	 */
	@Test
	void testDeleteAll_passesAFilePathNotABareKey() throws IOException {
		_service.deleteAll(Set.of("key"));

		ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
		verify(_deleteService).deleteFile(path.capture());
		assertThat(path.getValue(), endsWith("key.json"));
	}

	/**
	 * A failed delete has to surface as the declared {@link CompletionException}
	 * carrying the {@link IOException}s.
	 */
	@Test
	void testDeleteAll_collectsEveryFailure() throws IOException {
		when(_deleteService.deleteFile(anyString())).thenThrow(new IOException("boom"));

		CompletionException thrown = assertThrows(CompletionException.class,
				() -> _service.deleteAll(Set.of("k1", "k2")));

		assertThat(List.of(thrown.getSuppressed()), hasSize(2));
	}

	@Test
	void testDeleteAll_emptySetIsANoOp() {
		assertThat(_service.deleteAll(Set.of()), is(true));

		verifyNoInteractions(_deleteService);
	}

	/** Which folder a value lives in depends on its type. */
	@Test
	void testDeleteAll_cachedValueIsDeletedInItsOwnFolder() throws IOException {
		when(_keyService.generateKey()).thenReturn("key");
		_service.createNew(SingleIntegerValue.class, "owner", "paramName", 42, Type.SEND);

		_service.deleteAll(Set.of("key"));

		verify(_deleteService).deleteFile(SingleValueType.SINGLE_INTEGER.getPath() + "key.json");
	}

	/**
	 * A value that is no longer cached no longer tells its type, so the folders
	 * have to be tried one after the other.
	 */
	@Test
	void testDeleteAll_uncachedValueIsLookedUpInEveryFolder() throws IOException {
		when(_deleteService.deleteFile(STRING_PATH + "key.json")).thenThrow(new NoSuchFileException("nope"));

		assertThat(_service.deleteAll(Set.of("key")), is(true));

		verify(_deleteService).deleteFile(SingleValueType.SINGLE_INTEGER.getPath() + "key.json");
	}

	@Test
	void testDeleteAll_valueInNoFolderAtAllFails() throws IOException {
		when(_deleteService.deleteFile(anyString())).thenThrow(new NoSuchFileException("nope"));

		CompletionException thrown = assertThrows(CompletionException.class, () -> _service.deleteAll(Set.of("key")));

		assertThat(List.of(thrown.getSuppressed()), hasSize(1));
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
		public java.util.Collection<String> getConnectedInstanzKeys() {
			return List.of();
		}

		@Override
		public boolean addConnectedInstanzKey(String key) {
			return false;
		}

		@Override
		public boolean removeConnection(java.util.Collection<String> connectedInstanzKeys) {
			return false;
		}
	}
}
