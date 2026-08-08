package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;

import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ChangeType;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedChildChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedValueChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ParentChange;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ValueRenameEvent;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The registered {@code InstanzServiceImpl}, driven through
 * {@link IInstanzService} exactly as the parts and handlers drive it.
 * <p>
 * The service is never alone in the product: the key service hands out its
 * keys, the persistence services back its cache, and
 * {@code ChangePropagationListener} answers every event it fires. All of them
 * are the real ones here, so a call is checked by what the model looks like
 * afterwards and by which events left the bus - not by what the service was
 * seen calling.
 * </p>
 *
 * @see EventChainSystemTest for the propagation chains themselves
 * @see DeltaPersistenceSystemTest for what those chains write to disk
 */
public class InstanzServiceSystemTest {

	IInstanzService _inse;

	ISingleValueService _svs;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();

		_recorder = EventRecorder.subscribeToAllDeltas(ProductRuntime.broker());
	}

	@AfterEach
	void afterEach() {
		_recorder.unsubscribe();
		ProductRuntime.flushDeltas();
	}

	// ---------- getRoot ----------

	/**
	 * Key {@code "0"} is the root by convention, and every part starts from it. It
	 * has to be the same object on every call, or two views would edit two trees.
	 */
	@Test
	void testGetRoot_isKeyZeroAndAlwaysTheSameInstanz() {
		IInstanz root = _inse.getRoot();

		assertThat(root.getOwnKey(), is(ROOT));
		assertThat(root.getParentKey(), is(nullValue()));
		assertThat(_inse.getRoot(), is(sameInstance(root)));
		assertThat(_inse.resolveKey(ROOT).get(), is(sameInstance(root)));
	}

	/**
	 * Nothing had written the root when the first test asked for it, so the service
	 * created it - and saved it right away, without waiting for a delta.
	 */
	@Test
	void testGetRoot_isOnDiskWithoutAnySave() {
		_inse.getRoot();

		assertThat(ProductRuntime.instanzFileExists(ROOT), is(true));
		assertThat(ProductRuntime.reloadInstanz(ROOT).getOwnKey(), is(ROOT));
	}

	// ---------- resolveKey ----------

	@Test
	void testResolveKey_nullOrUnknownKeyIsEmpty() {
		assertThat(_inse.resolveKey(null), is(Optional.empty()));
		assertThat(_inse.resolveKey("no-such-key"), is(Optional.empty()));
	}

	@Test
	void testResolveKeys_skipsWhatCannotBeResolved() {
		IInstanz known = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_inse.resolveKeys(List.of(known.getOwnKey(), "no-such-key")), contains(known));
	}

	/**
	 * The cache is what makes a key as good as a reference: a resolved instanz is
	 * the one the services mutate, not a copy read back from the file.
	 */
	@Test
	void testResolveKey_returnsTheInstanzTheServicesMutate() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);

		_inse.putSingleValue(child.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "vName", Type.SEND);

		assertThat(_inse.resolveKey(child.getOwnKey()).get(), is(sameInstance(child)));
		assertThat(child.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("vName"));
	}

	/** After a save the file is a complete second source for the same instanz. */
	@Test
	void testResolveKey_survivesARestartOfTheService() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		ProductRuntime.flushDeltas();

		assertThat(ProductRuntime.reloadInstanz(child.getOwnKey()).getParentKey(), is(ROOT));
	}

	// ---------- createInstanz ----------

	@Test
	void testCreateInstanz_takesTheKeyTheKeyServiceAnnounced() {
		String announced = ProductRuntime.keyService().previewNextKey();

		IInstanz created = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(created.getOwnKey(), is(announced));
		assertThat(created.getParentKey(), is(ROOT));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), hasItem(announced));
	}

	@Test
	void testCreateInstanz_firesNewWithBothKeys() {
		_recorder.clear();

		IInstanz created = _inse.createInstanz(ROOT, Type.SEND);

		InstanzEvent data = _recorder.onlyDataOf(InstanzEventConstants.NEW, InstanzEvent.class);
		assertThat(data._key(), is(created.getOwnKey()));
		assertThat(data._parentKey(), is(ROOT));
	}

	@Test
	void testCreateInstanz_blankParentKeyIsRejectedAndSilent() {
		_recorder.clear();

		assertThat(_inse.createInstanz(null, Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("", Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("  ", Type.SEND), is(nullValue()));

		assertThat(_recorder.events(), hasSize(0));
	}

	/**
	 * {@code POST} hands the first event over asynchronously, but the instanz
	 * itself is built before that - only the linkage arrives late.
	 */
	@Test
	void testCreateInstanz_postedIsLinkedUpAsWell() {
		_recorder.clear();

		IInstanz created = _inse.createInstanz(ROOT, Type.POST);
		_recorder.awaitCount(2);

		assertThat(created.getParentKey(), is(ROOT));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), hasItem(created.getOwnKey()));
	}

	// ---------- child handling ----------

	@Test
	void testPutChild_linksBothSidesAndFiresTheChildListChange() {
		IInstanz parent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_inse.putChild(parent.getOwnKey(), child.getOwnKey(), Type.SEND), is(true));

		LinkedChildChangeEvent added = _recorder.dataOf(InstanzEventConstants.CHILD_LIST_CHANGE,
				LinkedChildChangeEvent.class).stream()//
				.filter(data -> parent.getOwnKey().equals(data._key()))//
				.findFirst().orElseThrow();
		assertThat(added._changeType(), is(ChangeType.ADD));
		assertThat(added._instanzKeys(), contains(child.getOwnKey()));

		assertThat(parent.getChildren(), contains(child.getOwnKey()));
		assertThat(child.getParentKey(), is(parent.getOwnKey()));
	}

	@Test
	void testPutChild_aChildItAlreadyHasIsSilent() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_inse.putChild(ROOT, child.getOwnKey(), Type.SEND), is(false));

		assertThat(_recorder.events(), hasSize(0));
	}

	@Test
	void testPutChild_unresolvableParentIsRejected() {
		assertThat(_inse.putChild("no-such-key", "test", Type.SEND), is(false));
	}

	@Test
	void testPutChild_blankChildKeyIsRejected() {
		assertThat(_inse.putChild(ROOT, null, Type.SEND), is(false));
		assertThat(_inse.putChild(ROOT, "", Type.SEND), is(false));
		assertThat(_inse.putChild(ROOT, "  ", Type.SEND), is(false));
	}

	@Test
	void testPutChild_movesTheChildOffItsOldParent() {
		IInstanz newParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz toMove = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_inse.putChild(newParent.getOwnKey(), toMove.getOwnKey(), Type.SEND), is(true));

		assertThat(newParent.getChildren(), contains(toMove.getOwnKey()));
		assertThat(toMove.getParentKey(), is(newParent.getOwnKey()));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), not(hasItem(toMove.getOwnKey())));
	}

	@Test
	void testRemoveChild_detachesTheChildFromItsParent() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_inse.removeChild(ROOT, child.getOwnKey(), Type.SEND), is(true));

		assertThat(_inse.resolveKey(ROOT).get().getChildren(), not(hasItem(child.getOwnKey())));
		assertThat(child.getParentKey(), is(nullValue()));
	}

	@Test
	void testRemoveChild_firesExactlyOneDeleteForTheRemovedInstanz() {
		IInstanz toDelete = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz toKeep = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_inse.removeChild(ROOT, toDelete.getOwnKey(), Type.SEND), is(true));

		InstanzEvent deleted = _recorder.onlyDataOf(InstanzEventConstants.DELETE, InstanzEvent.class);
		assertThat(deleted._key(), is(toDelete.getOwnKey()));
		assertThat(deleted._parentKey(), is(nullValue()));

		assertThat(_inse.resolveKey(ROOT).get().getChildren(), hasItem(toKeep.getOwnKey()));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), not(hasItem(toDelete.getOwnKey())));
	}

	@Test
	void testRemoveChild_invalidInputIsRejected() {
		assertThat(_inse.removeChild("no-such-key", "1", Type.SEND), is(false));
		assertThat(_inse.removeChild(ROOT, null, Type.SEND), is(false));
		assertThat(_inse.removeChild(ROOT, "", Type.SEND), is(false));
		assertThat(_inse.removeChild(ROOT, "   ", Type.SEND), is(false));
		assertThat(_inse.removeChild(ROOT, "no-such-key", Type.SEND), is(false));
	}

	@Test
	void testRemoveChild_keyIsNotAChildOfThatParent_changesNothing() {
		IInstanz one = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz other = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_inse.removeChild(one.getOwnKey(), other.getOwnKey(), Type.SEND), is(false));

		assertThat(_inse.resolveKey(ROOT).get().getChildren(), hasItems(one.getOwnKey(), other.getOwnKey()));
		assertThat(one.getParentKey(), is(ROOT));
		assertThat(other.getParentKey(), is(ROOT));
	}

	// ---------- parent handling ----------

	@Test
	void testChangeParent_firesParentChangeCarryingBothEnds() {
		IInstanz oldParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz newParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz child = _inse.createInstanz(oldParent.getOwnKey(), Type.SEND);
		_recorder.clear();

		assertThat(_inse.changeParent(child.getOwnKey(), newParent.getOwnKey(), Type.SEND), is(true));

		ParentChange data = _recorder.onlyDataOf(InstanzEventConstants.PARENT_CHANGE, ParentChange.class);
		assertThat(data._key(), is(child.getOwnKey()));
		assertThat(data._newParentKey(), is(newParent.getOwnKey()));
		assertThat(data._oldParentKey(), is(oldParent.getOwnKey()));

		assertThat(child.getParentKey(), is(newParent.getOwnKey()));
	}

	@Test
	void testChangeParent_toTheParentItAlreadyHasIsSilent() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_inse.changeParent(child.getOwnKey(), ROOT, Type.SEND), is(false));

		assertThat(_recorder.events(), hasSize(0));
	}

	/**
	 * An instanz without a parent is not exotic: the root has none, and
	 * {@code removeSubtreeInstanz} clears it. Giving such an instanz a parent has
	 * to work, and the listeners have to see that there was no old one.
	 */
	@Test
	void testChangeParent_anOrphanGetsItsFirstParentReportedWithoutAnOldOne() {
		IInstanz orphan = _inse.createInstanz(ROOT, Type.SEND);
		_inse.removeSubtreeInstanz(orphan.getOwnKey(), Type.SEND);
		assertThat(orphan.getParentKey(), is(nullValue()));
		_recorder.clear();

		assertThat(_inse.changeParent(orphan.getOwnKey(), ROOT, Type.SEND), is(true));

		ParentChange data = _recorder.onlyDataOf(InstanzEventConstants.PARENT_CHANGE, ParentChange.class);
		assertThat(data._oldParentKey(), is(nullValue()));
		assertThat(orphan.getParentKey(), is(ROOT));
	}

	@Test
	void testChangeParent_unresolvableKeysAreRejected() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_inse.changeParent("no-such-child", ROOT, Type.SEND), is(false));
		assertThat(_inse.changeParent(child.getOwnKey(), "no-such-parent", Type.SEND), is(false));
	}

	// ---------- single value links ----------

	@Test
	void testPutSingleValue_storesTheNameAndFiresTheValueListChange() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		_inse.putSingleValue(owner.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "vName", Type.SEND);

		LinkedValueChangeEvent data = _recorder.onlyDataOf(InstanzEventConstants.VALUE_LIST_CHANGE,
				LinkedValueChangeEvent.class);
		assertThat(data._key(), is(owner.getOwnKey()));
		assertThat(data._singleValuetype(), is(SingleValueType.SINGLE_STRING));
		assertThat(data._changeType(), is(ChangeType.ADD));
		assertThat(data._valueKeys(), contains("vKey"));

		assertThat(owner.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("vName"));
	}

	/** Without a name the key stands in for it, so the tree never shows a blank. */
	@Test
	void testPutSingleValue_blankNameFallsBackToTheKey() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);

		_inse.putSingleValue(owner.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "  ", Type.SEND);

		assertThat(owner.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("vKey"));
	}

	@Test
	void testPutSingleValue_anAlreadyLinkedValueKeepsItsNameAndIsSilent() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_inse.putSingleValue(owner.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "vName", Type.SEND);
		_recorder.clear();

		_inse.putSingleValue(owner.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "otherName", Type.SEND);

		assertThat(owner.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("vName"));
		assertThat(_recorder.events(), hasSize(0));
	}

	@Test
	void testPutSingleValue_anUnresolvableInstanzIsIgnored() {
		_recorder.clear();

		_inse.putSingleValue("no-such-key", SingleValueType.SINGLE_STRING, "vKey", "vName", Type.SEND);

		assertThat(_recorder.events(), hasSize(0));
	}

	@Test
	void testChangeSingleValueName_firesTheRenameWithOldAndNewName() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_inse.putSingleValue(owner.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "oldName", Type.SEND);
		_recorder.clear();

		_inse.changeSingleValueName(owner.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "newName", Type.SEND);

		ValueRenameEvent data = _recorder.onlyDataOf(InstanzEventConstants.NAME_CHANGE, ValueRenameEvent.class);
		assertThat(data._key(), is(owner.getOwnKey()));
		assertThat(data._attrKey(), is("vKey"));
		assertThat(data._oldName(), is("oldName"));
		assertThat(data._newName(), is("newName"));

		assertThat(owner.getSingleValues(SingleValueType.SINGLE_STRING).get("vKey"), is("newName"));
	}

	@Test
	void testRemoveValueKey_unlinksTheValueFromEveryInstanzItNames() {
		IInstanz one = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz other = _inse.createInstanz(ROOT, Type.SEND);
		_inse.putSingleValue(one.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "n1", Type.SEND);
		_inse.putSingleValue(other.getOwnKey(), SingleValueType.SINGLE_STRING, "vKey", "n2", Type.SEND);
		_recorder.clear();

		_inse.removeValueKey(List.of(one.getOwnKey(), other.getOwnKey()), SingleValueType.SINGLE_STRING, "vKey",
				Type.SEND);

		assertThat(one.getSingleValues(SingleValueType.SINGLE_STRING).containsKey("vKey"), is(false));
		assertThat(other.getSingleValues(SingleValueType.SINGLE_STRING).containsKey("vKey"), is(false));

		assertThat(_recorder.dataOf(InstanzEventConstants.VALUE_LIST_CHANGE, LinkedValueChangeEvent.class).stream()
				.map(LinkedValueChangeEvent::_key).toList(),
				containsInAnyOrder(one.getOwnKey(), other.getOwnKey()));
	}

	// ---------- delete ----------

	/**
	 * Only the removed instanz loses its parent; the subtree below it stays linked,
	 * which is what would make the delete undoable in one piece.
	 */
	@Test
	void testRemoveSubtreeInstanz_detachesTheTopAndLeavesTheSubtreeIntact() {
		IInstanz branch = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz leaf = _inse.createInstanz(branch.getOwnKey(), Type.SEND);

		assertThat(_inse.removeSubtreeInstanz(branch.getOwnKey(), Type.SEND), is(true));

		assertThat(branch.getParentKey(), is(emptyOrNullString()));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), not(hasItem(branch.getOwnKey())));
		assertThat(branch.getChildren(), hasItem(leaf.getOwnKey()));
		assertThat(leaf.getParentKey(), is(branch.getOwnKey()));
		assertThat(leaf.getChildren(), hasSize(0));
	}

	/**
	 * The detached instanz is what the listener calls back with after the remove,
	 * so the second run has to be silent - it is the guard that ends the chain.
	 */
	@Test
	void testRemoveSubtreeInstanz_anAlreadyDetachedInstanzIsRejectedAndSilent() {
		IInstanz branch = _inse.createInstanz(ROOT, Type.SEND);
		_inse.removeSubtreeInstanz(branch.getOwnKey(), Type.SEND);
		_recorder.clear();

		assertThat(_inse.removeSubtreeInstanz(branch.getOwnKey(), Type.SEND), is(false));

		assertThat(_recorder.events(), hasSize(0));
	}

	@Test
	void testRemoveSubtreeInstanz_anUnresolvableKeyIsRejected() {
		assertThat(_inse.removeSubtreeInstanz("no-such-key", Type.SEND), is(false));
	}

	@Test
	void testMarkInstanzAsDelete_firesDeleteForThatKeyAlone() {
		IInstanz lonely = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		_inse.markInstanzAsDelete(lonely.getOwnKey(), Type.SEND);

		InstanzEvent data = _recorder.onlyDataOf(InstanzEventConstants.DELETE, InstanzEvent.class);
		assertThat(data._key(), is(lonely.getOwnKey()));
		assertThat(data._parentKey(), is(nullValue()));
	}

	// ---------- persistence hand-off ----------

	@Test
	void testSaveAll_writesEveryResolvableKeyAndSkipsTheRest() {
		IInstanz known = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_inse.saveAll(Set.of(known.getOwnKey(), "no-such-key")), is(true));

		assertThat(ProductRuntime.instanzFileExists(known.getOwnKey()), is(true));
		assertThat(ProductRuntime.reloadInstanz(known.getOwnKey()).getParentKey(), is(ROOT));
	}

	@Test
	void testSaveAll_withoutArgumentsWritesTheWholeCache() {
		IInstanz one = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz other = _inse.createInstanz(ROOT, Type.SEND);

		_inse.saveAll();

		assertThat(ProductRuntime.instanzFileExists(one.getOwnKey()), is(true));
		assertThat(ProductRuntime.instanzFileExists(other.getOwnKey()), is(true));
	}

	/**
	 * {@code DeleteService} works on file paths while the delta bookkeeping only
	 * knows keys. If the service ever passed the bare key on, nothing would be
	 * deleted and this file would survive.
	 */
	@Test
	void testDeleteAll_removesTheInstanzJsonOfEveryKey() {
		IInstanz doomed = _inse.createInstanz(ROOT, Type.SEND);
		_inse.saveAll(Set.of(doomed.getOwnKey()));
		assertThat(ProductRuntime.instanzFileExists(doomed.getOwnKey()), is(true));

		assertThat(_inse.deleteAll(Set.of(doomed.getOwnKey())), is(true));

		assertThat(ProductRuntime.instanzFileExists(doomed.getOwnKey()), is(false));
	}

	@Test
	void testDeleteAll_anEmptySetIsANoOp() {
		assertThat(_inse.deleteAll(Set.of()), is(true));
	}

	/**
	 * No file is the state a delete is after, so finding none is no failure - an
	 * instanz created and dropped again before any save never had one, and
	 * {@code saveDeltas} would otherwise fail on it again with every save from then
	 * on.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/53">#53</a>
	 */
	@Test
	void testDeleteAll_keysWithoutAFileAreNoFailure() {
		assertThat(_inse.deleteAll(Set.of("no-such-key-1", "no-such-key-2")), is(true));
	}

	/**
	 * What does fail still has to surface rather than pass silently, and one
	 * failure must not hide the others - {@code saveDeltas} hands over a whole set
	 * at once.
	 */
	@Test
	void testDeleteAll_collectsEveryFailureInOneException() {
		Path firstBlocked = ProductRuntime.instanzFile("blocked-1");
		Path secondBlocked = ProductRuntime.instanzFile("blocked-2");
		ProductRuntime.block(firstBlocked);
		ProductRuntime.block(secondBlocked);

		try {
			CompletionException thrown = assertThrows(CompletionException.class,
					() -> _inse.deleteAll(Set.of("blocked-1", "blocked-2")));

			assertThat(List.of(thrown.getSuppressed()), hasSize(2));
		} finally {
			ProductRuntime.unblock(firstBlocked);
			ProductRuntime.unblock(secondBlocked);
		}
	}

	/**
	 * Keys are lower case only, so a brand new key can not collide with an already
	 * written file on a case insensitive file system.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/35">#35</a>
	 */
	@Test
	void testCreateInstanz_theNewKeyHasNoFileYet() {
		IInstanz created = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(created.getOwnKey(), is(created.getOwnKey().toLowerCase(java.util.Locale.ROOT)));
		assertThat(ProductRuntime.instanzFileExists(created.getOwnKey()), is(false));
	}

	/**
	 * The instanz side of an attribute is only the name; the value itself lives in
	 * its own file. Both have to agree once the value service created one.
	 */
	@Test
	void testSingleValueLink_agreesWithTheValueService() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);

		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);

		assertThat(owner.getSingleValues(SingleValueType.SINGLE_STRING).get(value.getOwnKey()), is("parameter"));
		assertThat(value.getConnectedInstanzKeys(), contains(owner.getOwnKey()));
	}
}
