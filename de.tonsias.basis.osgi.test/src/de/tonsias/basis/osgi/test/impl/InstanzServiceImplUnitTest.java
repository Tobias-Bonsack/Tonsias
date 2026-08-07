package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.BeforeEach;
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
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.impl.InstanzServiceImpl;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ChangeType;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedChildChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedValueChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ParentChange;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ValueRenameEvent;

/**
 * Isolated tests for {@code InstanzServiceImpl}: every collaborator is a mock,
 * so the cache, the persistence hand-off and the fired events can be checked
 * without a running workbench. The event-flow tests that need real services
 * live in {@link InstanzServiceImplTest}.
 */
@ExtendWith(MockitoExtension.class)
public class InstanzServiceImplUnitTest {

	@Mock
	IKeyService _keyService;

	@Mock
	LoadService _loadService;

	@Mock
	SaveService _saveService;

	@Mock
	DeleteService _deleteService;

	@Mock
	IEventBrokerBridge _broker;

	@InjectMocks
	InstanzServiceImpl _service;

	private Instanz _parent;

	@BeforeEach
	void beforeEach() {
		_parent = new Instanz("parent");
	}

	private void givenOnDisk(String key, Instanz instanz) {
		when(_loadService.loadFromGson("instanz/" + key, Instanz.class)).thenReturn(instanz);
	}

	// ---------- getRoot ----------

	@Test
	void testGetRoot_loadedFromDisk() {
		Instanz root = new Instanz("0");
		givenOnDisk("0", root);

		assertThat(_service.getRoot(), is(sameInstance(root)));
		verify(_saveService, never()).safeAsGson(any(IInstanz.class), any());
	}

	@Test
	void testGetRoot_secondCallComesFromTheCache() {
		givenOnDisk("0", new Instanz("0"));

		IInstanz first = _service.getRoot();
		IInstanz second = _service.getRoot();

		assertThat(second, is(sameInstance(first)));
		verify(_loadService).loadFromGson("instanz/0", Instanz.class);
	}

	@Test
	void testGetRoot_createdAndSavedWhenNothingIsOnDisk() {
		givenOnDisk("0", null);
		when(_keyService.initKey()).thenReturn("0");

		IInstanz root = _service.getRoot();

		assertThat(root.getOwnKey(), is("0"));
		verify(_saveService).safeAsGson(root, Instanz.class);
	}

	// ---------- resolveKey ----------

	@Test
	void testResolveKey_nullKeyIsEmpty() {
		assertThat(_service.resolveKey(null), is(Optional.empty()));
		verifyNoInteractions(_loadService);
	}

	@Test
	void testResolveKey_unknownKeyIsEmpty() {
		givenOnDisk("nope", null);

		assertThat(_service.resolveKey("nope"), is(Optional.empty()));
	}

	@Test
	void testResolveKeys_skipsWhatCannotBeResolved() {
		givenOnDisk("known", _parent);
		givenOnDisk("unknown", null);

		var resolved = _service.resolveKeys(List.of("known", "unknown"));

		assertThat(resolved, contains(_parent));
	}

	// ---------- createInstanz ----------

	@Test
	void testCreateInstanz_blankParentKeyIsRejected() {
		assertThat(_service.createInstanz(null, Type.SEND), is(nullValue()));
		assertThat(_service.createInstanz("  ", Type.SEND), is(nullValue()));
		verifyNoInteractions(_broker, _keyService);
	}

	@Test
	void testCreateInstanz_firesNewEventAndCaches() {
		when(_keyService.generateKey()).thenReturn("newKey");

		IInstanz created = _service.createInstanz("parent", Type.SEND);

		assertThat(created.getParentKey(), is("parent"));
		verify(_broker).send(InstanzEventConstants.NEW,
				Map.of(IEventBroker.DATA, new InstanzEvent("newKey", "parent")));
		// cached, so no disk access is needed to resolve it again
		assertThat(_service.resolveKey("newKey").get(), is(sameInstance(created)));
	}

	@Test
	void testCreateInstanz_postTypeUsesPost() {
		when(_keyService.generateKey()).thenReturn("newKey");

		_service.createInstanz("parent", Type.POST);

		verify(_broker).post(eq(InstanzEventConstants.NEW), any());
		verify(_broker, never()).send(anyString(), any());
	}

	// ---------- child handling ----------

	@Test
	void testPutChild_firesChildListChange() {
		givenOnDisk("parent", _parent);

		assertThat(_service.putChild("parent", "child", Type.SEND), is(true));

		verify(_broker).send(InstanzEventConstants.CHILD_LIST_CHANGE,
				Map.of(IEventBroker.DATA, new LinkedChildChangeEvent("parent", ChangeType.ADD, List.of("child"))));
	}

	@Test
	void testPutChild_knownChildIsSilent() {
		_parent.addChildKeys("child");
		givenOnDisk("parent", _parent);

		assertThat(_service.putChild("parent", "child", Type.SEND), is(false));

		verifyNoInteractions(_broker);
	}

	@Test
	void testRemoveChild_firesChildListChange() {
		_parent.addChildKeys("child");
		givenOnDisk("parent", _parent);

		assertThat(_service.removeChild("parent", "child", Type.SEND), is(true));

		assertThat(_parent.getChildren(), hasSize(0));
		verify(_broker).send(InstanzEventConstants.CHILD_LIST_CHANGE,
				Map.of(IEventBroker.DATA, new LinkedChildChangeEvent("parent", ChangeType.REMOVE, List.of("child"))));
	}

	// ---------- parent handling ----------

	@Test
	void testChangeParent_firesParentChange() {
		Instanz child = new Instanz("child");
		child.setParentKey("oldParent");
		givenOnDisk("child", child);
		givenOnDisk("parent", _parent);

		assertThat(_service.changeParent("child", "parent", Type.SEND), is(true));

		assertThat(child.getParentKey(), is("parent"));
		verify(_broker).send(InstanzEventConstants.PARENT_CHANGE,
				Map.of(IEventBroker.DATA, new ParentChange("child", "parent", "oldParent")));
	}

	@Test
	void testChangeParent_sameParentIsSilent() {
		Instanz child = new Instanz("child");
		child.setParentKey("parent");
		givenOnDisk("child", child);
		givenOnDisk("parent", _parent);

		assertThat(_service.changeParent("child", "parent", Type.SEND), is(false));

		verifyNoInteractions(_broker);
	}

	/**
	 * An instanz without a parent is not exotic: the root has none, and
	 * {@code removeSubtreeInstanz} clears it. Giving such an instanz a parent has
	 * to work.
	 */
	@Test
	void testChangeParent_instanzWithoutParentGetsOne() {
		Instanz orphan = new Instanz("orphan");
		givenOnDisk("orphan", orphan);
		givenOnDisk("parent", _parent);

		assertThat(_service.changeParent("orphan", "parent", Type.SEND), is(true));

		assertThat(orphan.getParentKey(), is("parent"));
	}

	@Test
	void testChangeParent_unresolvableKeysAreRejected() {
		givenOnDisk("child", null);

		assertThat(_service.changeParent("child", "parent", Type.SEND), is(false));
	}

	// ---------- single value links ----------

	@Test
	void testPutSingleValue_firesValueListChange() {
		givenOnDisk("parent", _parent);

		_service.putSingleValue("parent", SingleValueType.SINGLE_STRING, "vKey", "vName", Type.SEND);

		assertThat(_parent.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("vName"));
		verify(_broker).send(InstanzEventConstants.VALUE_LIST_CHANGE, Map.of(IEventBroker.DATA,
				new LinkedValueChangeEvent("parent", SingleValueType.SINGLE_STRING, ChangeType.ADD, List.of("vKey"))));
	}

	@Test
	void testPutSingleValue_blankNameFallsBackToTheKey() {
		givenOnDisk("parent", _parent);

		_service.putSingleValue("parent", SingleValueType.SINGLE_STRING, "vKey", "  ", Type.SEND);

		assertThat(_parent.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("vKey"));
	}

	@Test
	void testPutSingleValue_alreadyLinkedIsSilent() {
		_parent.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("vKey", "vName"));
		givenOnDisk("parent", _parent);

		_service.putSingleValue("parent", SingleValueType.SINGLE_STRING, "vKey", "otherName", Type.SEND);

		assertThat(_parent.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("vName"));
		verifyNoInteractions(_broker);
	}

	@Test
	void testChangeSingleValueName_firesRenameWithOldAndNewName() {
		_parent.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("vKey", "oldName"));
		givenOnDisk("parent", _parent);

		_service.changeSingleValueName("parent", SingleValueType.SINGLE_STRING, "vKey", "newName", Type.SEND);

		assertThat(_parent.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("newName"));
		verify(_broker).send(InstanzEventConstants.NAME_CHANGE, Map.of(IEventBroker.DATA,
				new ValueRenameEvent("parent", SingleValueType.SINGLE_STRING, "vKey", "oldName", "newName")));
	}

	@Test
	void testRemoveValueKey_unlinksFromEveryInstanz() {
		Instanz other = new Instanz("other");
		_parent.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("vKey", "n1"));
		other.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("vKey", "n2"));
		givenOnDisk("parent", _parent);
		givenOnDisk("other", other);

		_service.removeValueKey(List.of("parent", "other"), SingleValueType.SINGLE_STRING, "vKey", Type.SEND);

		assertThat(_parent.getSingleValues(SingleValueType.SINGLE_STRING).size(), is(0));
		assertThat(other.getSingleValues(SingleValueType.SINGLE_STRING).size(), is(0));
		verify(_broker, times(2)).send(eq(InstanzEventConstants.VALUE_LIST_CHANGE), any());
	}

	// ---------- delete ----------

	@Test
	void testRemoveSubtreeInstanz_clearsTheParentLinkOnBothSides() {
		Instanz child = new Instanz("child");
		child.setParentKey("parent");
		_parent.addChildKeys("child");
		givenOnDisk("child", child);
		givenOnDisk("parent", _parent);

		assertThat(_service.removeSubtreeInstanz("child", Type.SEND), is(true));

		assertThat(child.getParentKey(), is(nullValue()));
		assertThat(_parent.getChildren(), hasSize(0));
		verify(_broker).send(InstanzEventConstants.DELETE,
				Map.of(IEventBroker.DATA, new InstanzEvent("child", null)));
	}

	@Test
	void testRemoveSubtreeInstanz_alreadyDetachedIsRejected() {
		Instanz orphan = new Instanz("orphan");
		givenOnDisk("orphan", orphan);

		assertThat(_service.removeSubtreeInstanz("orphan", Type.SEND), is(false));

		verifyNoInteractions(_broker);
	}

	@Test
	void testMarkInstanzAsDelete_firesDelete() {
		_service.markInstanzAsDelete("key", Type.SEND);

		verify(_broker).send(InstanzEventConstants.DELETE, Map.of(IEventBroker.DATA, new InstanzEvent("key", null)));
	}

	// ---------- persistence hand-off ----------

	@Test
	void testSaveAll_savesEveryResolvableKey() {
		givenOnDisk("known", _parent);
		givenOnDisk("unknown", null);

		assertThat(_service.saveAll(Set.of("known", "unknown")), is(true));

		verify(_saveService).safeAsGson(_parent, Instanz.class);
	}

	/**
	 * {@code DeleteService.deleteFile(..)} expects the path of a file, while the
	 * delta bookkeeping only ever knows keys. Whatever the service passes on has to
	 * point at the instanz json, otherwise nothing is ever deleted.
	 */
	@Test
	void testDeleteAll_passesAFilePathNotABareKey() throws IOException {
		_service.deleteAll(Set.of("k1"));

		ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
		verify(_deleteService).deleteFile(path.capture());
		assertThat(path.getValue(), endsWith("instanz/k1.json"));
	}

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

	@Test
	void testSaveAll_writesTheWholeCache() {
		givenOnDisk("a", _parent);
		Instanz other = new Instanz("b");
		givenOnDisk("b", other);
		_service.resolveKey("a");
		_service.resolveKey("b");

		_service.saveAll();

		ArgumentCaptor<IInstanz> saved = ArgumentCaptor.forClass(IInstanz.class);
		verify(_saveService, times(2)).safeAsGson(saved.capture(), any());
		assertThat(saved.getAllValues(), containsInAnyOrder(_parent, other));
	}
}
