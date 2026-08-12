package de.tonsias.basis.osgi.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

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
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
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
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.LinkedInstanzChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueNewEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.ValueChangeEvent;
import de.tonsias.basis.osgi.test.E4ServiceContext;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.util.ChangePropagationListener;
import de.tonsias.basis.osgi.util.OsgiUtil;

/**
 * End-to-end tests of the event bus: real {@code InstanzServiceImpl}, real
 * {@code SingleValueServiceImpl}, the real broker and the real
 * {@link ChangePropagationListener} in between - nothing is mocked.
 * <p>
 * Every mutating service call is only the first link of a chain: the service
 * fires an event, the listener turns that event into calls on the <em>other</em>
 * side of the relation, and those fire events again. Each test therefore checks
 * three things:
 * </p>
 * <ol>
 * <li>which events the chain produced - no more (a missing loop guard) and no
 * fewer (propagation stopped early) than expected,</li>
 * <li>what the payloads carry, because the delta service derives the keys it
 * saves and deletes from exactly those payloads,</li>
 * <li>that both ends of the relation agree afterwards.</li>
 * </ol>
 * The effect of those events on the files is covered by
 * {@link DeltaPersistenceSystemTest}.
 */
public class EventChainSystemTest {

	private static final String ROOT = "0";

	IInstanzService _inse;

	ISingleValueService _svs;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		E4ServiceContext.prime();
		_inse = OsgiUtil.getService(IInstanzService.class);
		_svs = OsgiUtil.getService(ISingleValueService.class);
		// in the product ModelView creates the root at start-up
		_inse.getRoot();

		_recorder = EventRecorder.subscribeToAllDeltas(OsgiUtil.getService(IEventBrokerBridge.class));
	}

	@AfterEach
	void afterEach() {
		_recorder.unsubscribe();
		try {
			// keeps the shared delta log from carrying this test's events into the next
			OsgiUtil.getService(IDeltaService.class).saveDeltas();
		} catch (CompletionException e) {
		}
	}

	/**
	 * ------------- parent / child chains -------------
	 */

	@Test
	void testCreateInstanz_newAndChildListChange_linkBothSides() {
		_recorder.clear();

		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_recorder.topics(),
				containsInAnyOrder(InstanzEventConstants.NEW, InstanzEventConstants.CHILD_LIST_CHANGE));

		InstanzEvent created = _recorder.onlyDataOf(InstanzEventConstants.NEW, InstanzEvent.class);
		assertThat(created._key(), is(child.getOwnKey()));
		assertThat(created._parentKey(), is(ROOT));

		LinkedChildChangeEvent childList = _recorder.onlyDataOf(InstanzEventConstants.CHILD_LIST_CHANGE,
				LinkedChildChangeEvent.class);
		assertThat(childList._key(), is(ROOT));
		assertThat(childList._changeType(), is(ChangeType.ADD));
		assertThat(childList._instanzKeys(), contains(child.getOwnKey()));

		assertThat(child.getParentKey(), is(ROOT));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), hasItem(child.getOwnKey()));
	}

	/**
	 * The child list change makes the listener call back into
	 * {@code changeParent}, which must recognise that the parent it is asked for
	 * is already set and stay silent - otherwise the two handlers keep answering
	 * each other.
	 */
	@Test
	void testCreateInstanz_parentIsAlreadySet_soNoParentChangeIsFired() {
		_recorder.clear();

		_inse.createInstanz(ROOT, Type.SEND);

		assertThat(_recorder.topics(), not(hasItem(InstanzEventConstants.PARENT_CHANGE)));
	}

	@Test
	void testCreateInstanz_belowAnotherNewInstanz_propagatesOnlyToItsOwnParent() {
		IInstanz parent = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		IInstanz child = _inse.createInstanz(parent.getOwnKey(), Type.SEND);

		assertThat(_recorder.topics(),
				containsInAnyOrder(InstanzEventConstants.NEW, InstanzEventConstants.CHILD_LIST_CHANGE));
		assertThat(_recorder.onlyDataOf(InstanzEventConstants.CHILD_LIST_CHANGE, LinkedChildChangeEvent.class)._key(),
				is(parent.getOwnKey()));

		assertThat(parent.getChildren(), contains(child.getOwnKey()));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), not(hasItem(child.getOwnKey())));
	}

	@Test
	void testCreateInstanz_invalidParentKey_firesNothing() {
		_recorder.clear();

		assertThat(_inse.createInstanz(null, Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("", Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("   ", Type.SEND), is(nullValue()));

		assertThat(_recorder.events(), is(empty()));
	}

	/**
	 * Moving a child is the chain with the most links: add on the new parent ->
	 * parent change on the child -> remove on the old parent. The remove must
	 * <em>not</em> reach the delete branch of the listener, or every move would
	 * throw away the moved subtree.
	 */
	@Test
	void testPutChild_moveToAnotherParent_reparentsWithoutDeleting() {
		IInstanz oldParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz newParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz moved = _inse.createInstanz(oldParent.getOwnKey(), Type.SEND);
		_recorder.clear();

		assertThat(_inse.putChild(newParent.getOwnKey(), moved.getOwnKey(), Type.SEND), is(true));

		assertThat(_recorder.topics(),
				containsInAnyOrder(InstanzEventConstants.CHILD_LIST_CHANGE, InstanzEventConstants.PARENT_CHANGE,
						InstanzEventConstants.CHILD_LIST_CHANGE));

		ParentChange parentChange = _recorder.onlyDataOf(InstanzEventConstants.PARENT_CHANGE, ParentChange.class);
		assertThat(parentChange._key(), is(moved.getOwnKey()));
		assertThat(parentChange._newParentKey(), is(newParent.getOwnKey()));
		assertThat(parentChange._oldParentKey(), is(oldParent.getOwnKey()));

		assertThat(childListChangeOf(newParent)._changeType(), is(ChangeType.ADD));
		assertThat(childListChangeOf(newParent)._instanzKeys(), contains(moved.getOwnKey()));
		assertThat(childListChangeOf(oldParent)._changeType(), is(ChangeType.REMOVE));
		assertThat(childListChangeOf(oldParent)._instanzKeys(), contains(moved.getOwnKey()));

		assertThat(moved.getParentKey(), is(newParent.getOwnKey()));
		assertThat(newParent.getChildren(), contains(moved.getOwnKey()));
		assertThat(oldParent.getChildren(), not(hasItem(moved.getOwnKey())));
	}

	@Test
	void testChangeParent_producesTheSameLinkageAsPutChild() {
		IInstanz oldParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz newParent = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz moved = _inse.createInstanz(oldParent.getOwnKey(), Type.SEND);
		_recorder.clear();

		assertThat(_inse.changeParent(moved.getOwnKey(), newParent.getOwnKey(), Type.SEND), is(true));

		assertThat(_recorder.topics(),
				containsInAnyOrder(InstanzEventConstants.PARENT_CHANGE, InstanzEventConstants.CHILD_LIST_CHANGE,
						InstanzEventConstants.CHILD_LIST_CHANGE));

		assertThat(moved.getParentKey(), is(newParent.getOwnKey()));
		assertThat(newParent.getChildren(), contains(moved.getOwnKey()));
		assertThat(oldParent.getChildren(), not(hasItem(moved.getOwnKey())));
	}

	@Test
	void testChangeParent_toTheParentItAlreadyHas_firesNothing() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_inse.changeParent(child.getOwnKey(), ROOT, Type.SEND), is(false));

		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testPutChild_childItAlreadyHas_firesNothing() {
		IInstanz child = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_inse.putChild(ROOT, child.getOwnKey(), Type.SEND), is(false));

		assertThat(_recorder.events(), is(empty()));
	}

	/**
	 * Removing a child is a delete: the child list change reaches the delete
	 * branch of the listener, which detaches the child and marks it - and from
	 * there every descendant - for deletion.
	 */
	@Test
	void testRemoveChild_marksTheWholeSubtreeAsDeleted() {
		IInstanz branch = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz leaf = _inse.createInstanz(branch.getOwnKey(), Type.SEND);
		IInstanz deepLeaf = _inse.createInstanz(leaf.getOwnKey(), Type.SEND);
		_recorder.clear();

		assertThat(_inse.removeChild(ROOT, branch.getOwnKey(), Type.SEND), is(true));

		assertThat(_recorder.topics(),
				containsInAnyOrder(InstanzEventConstants.CHILD_LIST_CHANGE, InstanzEventConstants.DELETE,
						InstanzEventConstants.DELETE, InstanzEventConstants.DELETE));

		assertThat(_recorder.dataOf(InstanzEventConstants.DELETE, InstanzEvent.class).stream().map(InstanzEvent::_key)
				.toList(), containsInAnyOrder(branch.getOwnKey(), leaf.getOwnKey(), deepLeaf.getOwnKey()));

		LinkedChildChangeEvent childList = _recorder.onlyDataOf(InstanzEventConstants.CHILD_LIST_CHANGE,
				LinkedChildChangeEvent.class);
		assertThat(childList._key(), is(ROOT));
		assertThat(childList._changeType(), is(ChangeType.REMOVE));

		// only the removed instanz loses its parent, the subtree below it stays
		// intact - that is what makes the delete undoable in one piece
		assertThat(branch.getParentKey(), is(nullValue()));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), not(hasItem(branch.getOwnKey())));
		assertThat(branch.getChildren(), contains(leaf.getOwnKey()));
		assertThat(leaf.getParentKey(), is(branch.getOwnKey()));
	}

	@Test
	void testRemoveSubtreeInstanz_calledDirectly_producesTheSameChain() {
		IInstanz branch = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz leaf = _inse.createInstanz(branch.getOwnKey(), Type.SEND);
		_recorder.clear();

		assertThat(_inse.removeSubtreeInstanz(branch.getOwnKey(), Type.SEND), is(true));

		assertThat(_recorder.topics(), containsInAnyOrder(InstanzEventConstants.CHILD_LIST_CHANGE,
				InstanzEventConstants.DELETE, InstanzEventConstants.DELETE));
		assertThat(_recorder.dataOf(InstanzEventConstants.DELETE, InstanzEvent.class).stream().map(InstanzEvent::_key)
				.toList(), containsInAnyOrder(branch.getOwnKey(), leaf.getOwnKey()));
	}

	/**
	 * The detached instanz is what the listener calls back with after the remove,
	 * so the second run has to be silent - it is the guard that ends the chain.
	 */
	@Test
	void testRemoveSubtreeInstanz_alreadyDetached_firesNothing() {
		IInstanz branch = _inse.createInstanz(ROOT, Type.SEND);
		_inse.removeSubtreeInstanz(branch.getOwnKey(), Type.SEND);
		_recorder.clear();

		assertThat(_inse.removeSubtreeInstanz(branch.getOwnKey(), Type.SEND), is(false));

		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testRemoveChild_keyIsNotAChild_firesNothing() {
		IInstanz stranger = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz other = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		assertThat(_inse.removeChild(stranger.getOwnKey(), other.getOwnKey(), Type.SEND), is(false));

		assertThat(_recorder.events(), is(empty()));
		assertThat(other.getParentKey(), is(ROOT));
	}

	/**
	 * ------------- instanz / single value chains -------------
	 */

	@Test
	void testCreateSingleValue_linksValueAndOwnerBothWays() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);

		assertThat(_recorder.topics(),
				containsInAnyOrder(SingleValueEventConstants.NEW, InstanzEventConstants.VALUE_LIST_CHANGE));

		SingleValueNewEvent created = _recorder.onlyDataOf(SingleValueEventConstants.NEW, SingleValueNewEvent.class);
		assertThat(created._key(), is(value.getOwnKey()));
		assertThat(created._type(), is(SingleValueType.SINGLE_STRING));
		assertThat(created._name(), is("parameter"));
		assertThat(created._ownerKeys(), contains(owner.getOwnKey()));

		LinkedValueChangeEvent valueList = _recorder.onlyDataOf(InstanzEventConstants.VALUE_LIST_CHANGE,
				LinkedValueChangeEvent.class);
		assertThat(valueList._key(), is(owner.getOwnKey()));
		assertThat(valueList._singleValuetype(), is(SingleValueType.SINGLE_STRING));
		assertThat(valueList._changeType(), is(ChangeType.ADD));
		assertThat(valueList._valueKeys(), contains(value.getOwnKey()));

		assertThat(value.getConnectedInstanzKeys(), contains(owner.getOwnKey()));
		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).get(value.getOwnKey()), is("parameter"));
	}

	/**
	 * The value already knows its first owner when the event goes out, so the
	 * listener's call back into {@code addToParent} must not answer with another
	 * event.
	 */
	@Test
	void testCreateSingleValue_ownerIsAlreadyConnected_soNoInstanzListChangeIsFired() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		_svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content", Type.SEND);

		assertThat(_recorder.topics(), not(hasItem(SingleValueEventConstants.INSTANZ_LIST_CHANGE)));
	}

	@Test
	void testAddToParent_secondOwner_linksBothWaysAndNamesTheValueAfterItsKey() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz secondOwner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_recorder.clear();

		assertThat(_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), secondOwner.getOwnKey(),
				Type.SEND), is(true));

		assertThat(_recorder.topics(), containsInAnyOrder(SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				InstanzEventConstants.VALUE_LIST_CHANGE));

		LinkedInstanzChangeEvent instanzList = _recorder.onlyDataOf(SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				LinkedInstanzChangeEvent.class);
		assertThat(instanzList._key(), is(value.getOwnKey()));
		assertThat(instanzList._changeType(), is(LinkedInstanzChangeEvent.ChangeType.ADD));
		assertThat(instanzList._instanzKeys(), contains(secondOwner.getOwnKey()));

		assertThat(value.getConnectedInstanzKeys(),
				containsInAnyOrder(owner.getOwnKey(), secondOwner.getOwnKey()));
		// the chain carries no name for the new owner, so the key stands in for it
		assertThat(secondOwner.getValues(SingleValueType.SINGLE_STRING).get(value.getOwnKey()),
				is(value.getOwnKey()));
		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).get(value.getOwnKey()), is("parameter"));
	}

	@Test
	void testAddToParent_ownerIsAlreadyConnected_firesNothing() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_recorder.clear();

		assertThat(_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), owner.getOwnKey(), Type.SEND),
				is(false));

		assertThat(_recorder.events(), is(empty()));
	}

	@Test
	void testChangeValue_firesOnlyOnTheValueItself() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "old",
				Type.SEND);
		_recorder.clear();

		assertThat(_svs.changeValue(value.getOwnKey(), "new", Type.SEND), is(true));

		assertThat(_recorder.topics(), contains(SingleValueEventConstants.VALUE_CHANGE));

		ValueChangeEvent changed = _recorder.onlyDataOf(SingleValueEventConstants.VALUE_CHANGE, ValueChangeEvent.class);
		assertThat(changed._key(), is(value.getOwnKey()));
		assertThat(changed._type(), is(SingleValueType.SINGLE_STRING));
		assertThat(changed._oldValue(), is("old"));
		assertThat(changed._newValue(), is("new"));

		assertThat(value.getValue(), is("new"));
	}

	@Test
	void testChangeValue_toTheValueItAlreadyHas_firesNothing() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_recorder.clear();

		assertThat(_svs.changeValue(value.getOwnKey(), "content", Type.SEND), is(false));

		assertThat(_recorder.events(), is(empty()));
	}

	/**
	 * The name of an attribute belongs to the owning instanz, not to the value, so
	 * the rename must stay on the instanz side of the bus.
	 */
	@Test
	void testChangeSingleValueName_staysOnTheOwner() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "oldName", "content",
				Type.SEND);
		_recorder.clear();

		_inse.changeSingleValueName(owner.getOwnKey(), SingleValueType.SINGLE_STRING, value.getOwnKey(), "newName",
				Type.SEND);

		assertThat(_recorder.topics(), contains(InstanzEventConstants.NAME_CHANGE));

		ValueRenameEvent renamed = _recorder.onlyDataOf(InstanzEventConstants.NAME_CHANGE, ValueRenameEvent.class);
		assertThat(renamed._key(), is(owner.getOwnKey()));
		assertThat(renamed._attrKey(), is(value.getOwnKey()));
		assertThat(renamed._oldName(), is("oldName"));
		assertThat(renamed._newName(), is("newName"));

		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).get(value.getOwnKey()), is("newName"));
		assertThat(value.getConnectedInstanzKeys(), contains(owner.getOwnKey()));
	}

	/**
	 * Deleting a value has to reach every instanz that references it, otherwise an
	 * owner keeps a key whose file is gone.
	 */
	@Test
	void testMarkSingleValueAsDelete_dropsTheKeyFromEveryOwner() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		IInstanz secondOwner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_svs.addToParent(SingleValueType.SINGLE_STRING, value.getOwnKey(), secondOwner.getOwnKey(), Type.SEND);
		_recorder.clear();

		_svs.markSingleValueAsDelete(value.getOwnKey(), Type.SEND);

		assertThat(_recorder.topics(), containsInAnyOrder(SingleValueEventConstants.DELETE,
				InstanzEventConstants.VALUE_LIST_CHANGE, InstanzEventConstants.VALUE_LIST_CHANGE));

		SingleValueDeleteEvent deleted = _recorder.onlyDataOf(SingleValueEventConstants.DELETE,
				SingleValueDeleteEvent.class);
		assertThat(deleted._key(), is(value.getOwnKey()));
		assertThat(deleted._type(), is(SingleValueType.SINGLE_STRING));
		// the connections are cut before the event goes out, so it has to carry its
		// own copy - the owners could not be found afterwards
		assertThat(deleted._ownerKeys(), containsInAnyOrder(owner.getOwnKey(), secondOwner.getOwnKey()));

		assertThat(_recorder.dataOf(InstanzEventConstants.VALUE_LIST_CHANGE, LinkedValueChangeEvent.class).stream()
				.map(LinkedValueChangeEvent::_key).toList(),
				containsInAnyOrder(owner.getOwnKey(), secondOwner.getOwnKey()));
		_recorder.dataOf(InstanzEventConstants.VALUE_LIST_CHANGE, LinkedValueChangeEvent.class)
				.forEach(data -> assertThat(data._changeType(), is(ChangeType.REMOVE)));

		assertThat(value.getConnectedInstanzKeys(), is(empty()));
		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat(secondOwner.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()),
				is(false));
	}

	@Test
	void testRemoveValue_producesTheSameChainAsMarkAsDelete() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);
		_recorder.clear();

		assertThat(_svs.removeValue(value, Type.SEND), is(true));

		assertThat(_recorder.topics(),
				containsInAnyOrder(SingleValueEventConstants.DELETE, InstanzEventConstants.VALUE_LIST_CHANGE));
		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
	}

	/**
	 * An integer value walks the same chain as a string one, but through the other
	 * half of {@code SingleValueType} - the type travels in the payload and picks
	 * the map the owner stores the key in.
	 */
	@Test
	void testCreateSingleValue_integer_usesItsOwnTypeThroughoutTheChain() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		SingleIntegerValue value = _svs.createNew(SingleIntegerValue.class, owner.getOwnKey(), "number", 42, Type.SEND);

		assertThat(_recorder.onlyDataOf(SingleValueEventConstants.NEW, SingleValueNewEvent.class)._type(),
				is(SingleValueType.SINGLE_INTEGER));
		assertThat(_recorder.onlyDataOf(InstanzEventConstants.VALUE_LIST_CHANGE, LinkedValueChangeEvent.class)
				._singleValuetype(), is(SingleValueType.SINGLE_INTEGER));

		assertThat(owner.getValues(SingleValueType.SINGLE_INTEGER).get(value.getOwnKey()), is("number"));
		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).keySet(), is(empty()));
	}

	/** The same for the third type, whose value travels as a {@code Boolean}. */
	@Test
	void testCreateSingleValue_boolean_usesItsOwnTypeThroughoutTheChain() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		SingleBooleanValue value = _svs.createNew(SingleBooleanValue.class, owner.getOwnKey(), "flag", true, Type.SEND);

		assertThat(value.getValue(), is(true));
		assertThat(_recorder.onlyDataOf(SingleValueEventConstants.NEW, SingleValueNewEvent.class)._type(),
				is(SingleValueType.SINGLE_BOOLEAN));
		assertThat(_recorder.onlyDataOf(InstanzEventConstants.VALUE_LIST_CHANGE, LinkedValueChangeEvent.class)
				._singleValuetype(), is(SingleValueType.SINGLE_BOOLEAN));

		assertThat(owner.getValues(SingleValueType.SINGLE_BOOLEAN).get(value.getOwnKey()), is("flag"));
		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).keySet(), is(empty()));
		assertThat(owner.getValues(SingleValueType.SINGLE_INTEGER).keySet(), is(empty()));
	}

	/**
	 * The same for the fourth type. Its value comes from the dialog as text, so
	 * this is also where the chain is checked with a string that has to be parsed
	 * on the way in.
	 */
	@Test
	void testCreateSingleValue_float_usesItsOwnTypeThroughoutTheChain() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		_recorder.clear();

		SingleFloatValue value = _svs.createNew(SingleFloatValue.class, owner.getOwnKey(), "ratio", "3.14", Type.SEND);

		assertThat(value.getValue(), is(3.14f));
		assertThat(_recorder.onlyDataOf(SingleValueEventConstants.NEW, SingleValueNewEvent.class)._type(),
				is(SingleValueType.SINGLE_FLOAT));
		assertThat(_recorder.onlyDataOf(InstanzEventConstants.VALUE_LIST_CHANGE, LinkedValueChangeEvent.class)
				._singleValuetype(), is(SingleValueType.SINGLE_FLOAT));

		assertThat(owner.getValues(SingleValueType.SINGLE_FLOAT).get(value.getOwnKey()), is("ratio"));
		assertThat(owner.getValues(SingleValueType.SINGLE_STRING).keySet(), is(empty()));
		assertThat(owner.getValues(SingleValueType.SINGLE_INTEGER).keySet(), is(empty()));
		assertThat(owner.getValues(SingleValueType.SINGLE_BOOLEAN).keySet(), is(empty()));
	}

	/**
	 * ------------- asynchronous delivery -------------
	 */

	/**
	 * {@code POST} only changes how the first event is handed over; the listener
	 * still re-enters the services synchronously, so the same chain has to arrive
	 * - just later.
	 */
	@Test
	void testCreateInstanz_posted_completesTheSameChainAsynchronously() {
		_recorder.clear();

		IInstanz child = _inse.createInstanz(ROOT, Type.POST);

		_recorder.awaitCount(2);

		assertThat(_recorder.topics(),
				containsInAnyOrder(InstanzEventConstants.NEW, InstanzEventConstants.CHILD_LIST_CHANGE));
		assertThat(child.getParentKey(), is(ROOT));
		assertThat(_inse.resolveKey(ROOT).get().getChildren(), hasItem(child.getOwnKey()));
	}

	@Test
	void testChangeValue_posted_arrivesWithItsPayload() {
		IInstanz owner = _inse.createInstanz(ROOT, Type.SEND);
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "old",
				Type.SEND);
		_recorder.clear();

		assertThat(_svs.changeValue(value.getOwnKey(), "new", Type.POST), is(true));

		_recorder.awaitCount(1);

		assertThat(_recorder.events(), hasSize(1));
		ValueChangeEvent changed = _recorder.onlyDataOf(SingleValueEventConstants.VALUE_CHANGE, ValueChangeEvent.class);
		assertThat(changed._oldValue(), is("old"));
		assertThat(changed._newValue(), is("new"));
	}

	private LinkedChildChangeEvent childListChangeOf(IInstanz instanz) {
		return _recorder.dataOf(InstanzEventConstants.CHILD_LIST_CHANGE, LinkedChildChangeEvent.class).stream()//
				.filter(data -> instanz.getOwnKey().equals(data._key()))//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no child list change for " + instanz.getOwnKey()));
	}
}
