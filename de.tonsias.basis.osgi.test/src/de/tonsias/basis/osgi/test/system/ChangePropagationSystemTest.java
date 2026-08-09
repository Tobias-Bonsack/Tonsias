package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
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
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.LinkedInstanzChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueNewEvent;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.basis.osgi.util.ChangePropagationListener;

/**
 * {@code ChangePropagationListener} as the product runs it: contributed as an
 * addon, subscribed through its {@code @EventTopic} methods, holding the
 * registered services.
 * <p>
 * Every test here puts one event on the real bus - the way a service would -
 * and then looks at the model. That is the listener's whole job: it never
 * returns anything, it only turns an event about one end of a relation into
 * calls on the other end. Those calls fire events again, so what is asserted is
 * where the whole chain came to rest.
 * </p>
 * <p>
 * {@link EventChainSystemTest} approaches the same listener from the front, by
 * calling the services; this one approaches it from the bus, which is the only
 * way to reach payloads no service would ever produce.
 * </p>
 */
public class ChangePropagationSystemTest {

	IInstanzService _inse;

	ISingleValueService _svs;

	IEventBrokerBridge _broker;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_broker = ProductRuntime.broker();
	}

	@AfterEach
	void afterEach() {
		ProductRuntime.flushDeltas();
	}

	/** puts an event on the bus exactly as the services do */
	private void publish(String topic, Object data) {
		_broker.send(topic, Map.of(IEventBroker.DATA, data));
	}

	private IInstanz newInstanz() {
		return _inse.createInstanz(ROOT, Type.SEND);
	}

	/**
	 * An instanz that is in the model but attached to nobody - what
	 * {@code removeSubtreeInstanz} leaves behind.
	 */
	private IInstanz newDetachedInstanz() {
		IInstanz detached = newInstanz();
		_inse.removeSubtreeInstanz(detached.getOwnKey(), Type.SEND);
		return detached;
	}

	/**
	 * Without the listener nothing keeps two ends of a relation in sync, so this is
	 * the precondition of every other test in the bundle.
	 */
	@Test
	void testListener_isSubscribedInTheRunningRuntime() {
		IInstanz parent = newInstanz();
		IInstanz orphan = newDetachedInstanz();

		publish(InstanzEventConstants.NEW, new InstanzEvent(orphan.getOwnKey(), parent.getOwnKey()));

		assertThat(parent.getChildren(), hasItem(orphan.getOwnKey()));
	}

	// ---------- instanz to instanz ----------

	@Test
	void testNewInstanz_addsTheKeyToItsParentsChildList() {
		IInstanz parent = newInstanz();
		IInstanz orphan = newDetachedInstanz();

		publish(InstanzEventConstants.NEW, new InstanzEvent(orphan.getOwnKey(), parent.getOwnKey()));

		assertThat(parent.getChildren(), contains(orphan.getOwnKey()));
		assertThat(orphan.getParentKey(), is(parent.getOwnKey()));
	}

	@Test
	void testChildAdded_setsTheParentOnEveryChildInThePayload() {
		IInstanz parent = newInstanz();
		IInstanz first = newDetachedInstanz();
		IInstanz second = newDetachedInstanz();

		publish(InstanzEventConstants.CHILD_LIST_CHANGE, new LinkedChildChangeEvent(parent.getOwnKey(), ChangeType.ADD,
				List.of(first.getOwnKey(), second.getOwnKey())));

		assertThat(first.getParentKey(), is(parent.getOwnKey()));
		assertThat(second.getParentKey(), is(parent.getOwnKey()));
		assertThat(parent.getChildren(), containsInAnyOrder(first.getOwnKey(), second.getOwnKey()));
	}

	/**
	 * A removal that names the child's actual parent is a delete: the child is
	 * detached and marked, and so is everything below it.
	 */
	@Test
	void testChildRemoved_detachesAndMarksTheSubtree() {
		IInstanz parent = newInstanz();
		IInstanz child = _inse.createInstanz(parent.getOwnKey(), Type.SEND);

		publish(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent(parent.getOwnKey(), ChangeType.REMOVE, List.of(child.getOwnKey())));

		assertThat(child.getParentKey(), is(nullValue()));
		assertThat(parent.getChildren(), not(hasItem(child.getOwnKey())));
	}

	/**
	 * The child already belongs to somebody else, so this removal is the other half
	 * of a move and must not delete anything - that guard is what keeps a move from
	 * throwing away the moved subtree.
	 */
	@Test
	void testChildRemoved_aChildOfAnotherParentIsLeftAlone() {
		IInstanz oldParent = newInstanz();
		IInstanz newParent = newInstanz();
		IInstanz child = _inse.createInstanz(newParent.getOwnKey(), Type.SEND);

		publish(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent(oldParent.getOwnKey(), ChangeType.REMOVE, List.of(child.getOwnKey())));

		assertThat(child.getParentKey(), is(newParent.getOwnKey()));
		assertThat(newParent.getChildren(), contains(child.getOwnKey()));
	}

	@Test
	void testChildRemoved_anUnresolvableChildIsLeftAlone() {
		IInstanz parent = newInstanz();

		publish(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent(parent.getOwnKey(), ChangeType.REMOVE, List.of("no-such-key")));

		assertThat(parent.getChildren(), hasSize(0));
	}

	@Test
	void testParentChanged_movesTheKeyBetweenBothChildLists() {
		IInstanz oldParent = newInstanz();
		IInstanz newParent = newInstanz();
		IInstanz child = _inse.createInstanz(oldParent.getOwnKey(), Type.SEND);

		publish(InstanzEventConstants.PARENT_CHANGE,
				new ParentChange(child.getOwnKey(), newParent.getOwnKey(), oldParent.getOwnKey()));

		assertThat(newParent.getChildren(), contains(child.getOwnKey()));
		assertThat(oldParent.getChildren(), not(hasItem(child.getOwnKey())));
	}

	@Test
	void testInstanzDeleted_marksEveryChildBelowIt() {
		IInstanz branch = newInstanz();
		IInstanz first = _inse.createInstanz(branch.getOwnKey(), Type.SEND);
		IInstanz second = _inse.createInstanz(branch.getOwnKey(), Type.SEND);
		ProductRuntime.flushDeltas();

		publish(InstanzEventConstants.DELETE, new InstanzEvent(branch.getOwnKey(), null));

		// the delete of a child is what puts it into the delta service's delete set
		assertThat(deltaKeysOn(InstanzEventConstants.DELETE),
				containsInAnyOrder(branch.getOwnKey(), first.getOwnKey(), second.getOwnKey()));
	}

	@Test
	void testInstanzDeleted_anUnresolvableKeyIsANoOp() {
		ProductRuntime.flushDeltas();

		publish(InstanzEventConstants.DELETE, new InstanzEvent("no-such-key", null));

		assertThat(deltaKeysOn(InstanzEventConstants.DELETE), contains("no-such-key"));
	}

	// ---------- instanz to single value ----------

	@Test
	void testValueListAdded_linksTheValueBackToTheInstanz() {
		IInstanz owner = newInstanz();
		IInstanz secondOwner = newInstanz();
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);

		publish(InstanzEventConstants.VALUE_LIST_CHANGE, new LinkedValueChangeEvent(secondOwner.getOwnKey(),
				SingleValueType.SINGLE_STRING, ChangeType.ADD, List.of(value.getOwnKey())));

		assertThat(value.getConnectedInstanzKeys(),
				containsInAnyOrder(owner.getOwnKey(), secondOwner.getOwnKey()));
	}

	/**
	 * The other direction has no logic yet - a value list removal is answered by
	 * the single value service, not by the listener.
	 */
	@Test
	void testValueListRemoved_isNotPropagatedYet() {
		IInstanz owner = newInstanz();
		SingleStringValue value = _svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", "content",
				Type.SEND);

		publish(InstanzEventConstants.VALUE_LIST_CHANGE, new LinkedValueChangeEvent(owner.getOwnKey(),
				SingleValueType.SINGLE_STRING, ChangeType.REMOVE, List.of(value.getOwnKey())));

		assertThat(value.getConnectedInstanzKeys(), contains(owner.getOwnKey()));
	}

	// ---------- single value to instanz ----------

	@Test
	void testNewSingleValue_isPutOnEveryOwnerItNames() {
		IInstanz first = newInstanz();
		IInstanz second = newInstanz();

		publish(SingleValueEventConstants.NEW, new SingleValueNewEvent(SingleValueType.SINGLE_STRING, "fabricated-new", "vName",
				List.of(first.getOwnKey(), second.getOwnKey())));

		assertThat(first.getSingleValues(SingleValueType.SINGLE_STRING).get("fabricated-new"), is("vName"));
		assertThat(second.getSingleValues(SingleValueType.SINGLE_STRING).get("fabricated-new"), is("vName"));
	}

	@Test
	void testDeletedSingleValue_isUnlinkedFromEveryOwner() {
		IInstanz first = newInstanz();
		IInstanz second = newInstanz();
		publish(SingleValueEventConstants.NEW, new SingleValueNewEvent(SingleValueType.SINGLE_STRING, "fabricated-delete", "vName",
				List.of(first.getOwnKey(), second.getOwnKey())));

		publish(SingleValueEventConstants.DELETE, new SingleValueDeleteEvent(SingleValueType.SINGLE_STRING, "fabricated-delete",
				List.of(first.getOwnKey(), second.getOwnKey())));

		assertThat(first.getSingleValues(SingleValueType.SINGLE_STRING).containsKey("fabricated-delete"), is(false));
		assertThat(second.getSingleValues(SingleValueType.SINGLE_STRING).containsKey("fabricated-delete"), is(false));
	}

	/** The chain carries no name here, so the key has to stand in for one. */
	@Test
	void testInstanzListAdded_putsTheValueOnTheNewOwnerUnderItsKey() {
		IInstanz owner = newInstanz();

		publish(SingleValueEventConstants.INSTANZ_LIST_CHANGE, new LinkedInstanzChangeEvent("fabricated-add",
				SingleValueType.SINGLE_STRING, LinkedInstanzChangeEvent.ChangeType.ADD, List.of(owner.getOwnKey())));

		assertThat(owner.getSingleValues(SingleValueType.SINGLE_STRING).get("fabricated-add"), is("fabricated-add"));
	}

	@Test
	void testInstanzListRemoved_isNotPropagatedYet() {
		IInstanz owner = newInstanz();
		publish(SingleValueEventConstants.INSTANZ_LIST_CHANGE, new LinkedInstanzChangeEvent("fabricated-remove",
				SingleValueType.SINGLE_STRING, LinkedInstanzChangeEvent.ChangeType.ADD, List.of(owner.getOwnKey())));

		publish(SingleValueEventConstants.INSTANZ_LIST_CHANGE, new LinkedInstanzChangeEvent("fabricated-remove",
				SingleValueType.SINGLE_STRING, LinkedInstanzChangeEvent.ChangeType.REMOVE, List.of(owner.getOwnKey())));

		assertThat(owner.getSingleValues(SingleValueType.SINGLE_STRING).containsKey("fabricated-remove"), is(true));
	}

	// ---------- the relation, which is held at both ends too ----------

	/**
	 * A relation is created pointing somewhere already, so there is no later change
	 * for the target to learn from - the new event is where it is told.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/74">#74</a>
	 */
	@Test
	void testNewRelation_isRecordedOnTheInstanzItPointsAt() {
		IInstanz owner = newInstanz();
		IInstanz target = newInstanz();

		SingleInstanzValue relation = _svs.createNew(SingleInstanzValue.class, owner.getOwnKey(), "points at",
				target.getOwnKey(), Type.SEND);

		assertThat(target.getReferencingValueKeys(), contains(relation.getOwnKey()));
	}

	/** and a value of any other type leaves every reference set alone */
	@Test
	void testNewStringValue_isNoRelationAndRecordsNothing() {
		IInstanz owner = newInstanz();
		IInstanz other = newInstanz();

		_svs.createNew(SingleStringValue.class, owner.getOwnKey(), "parameter", other.getOwnKey(), Type.SEND);

		assertThat(other.getReferencingValueKeys(), hasSize(0));
	}

	/** repointing a relation is two changes, and both ends have to follow */
	@Test
	void testChangedRelation_movesTheRecordToTheNewTarget() {
		IInstanz owner = newInstanz();
		IInstanz first = newInstanz();
		IInstanz second = newInstanz();
		SingleInstanzValue relation = _svs.createNew(SingleInstanzValue.class, owner.getOwnKey(), "points at",
				first.getOwnKey(), Type.SEND);

		_svs.changeValue(relation.getOwnKey(), second.getOwnKey(), Type.SEND);

		assertThat(first.getReferencingValueKeys(), hasSize(0));
		assertThat(second.getReferencingValueKeys(), contains(relation.getOwnKey()));
	}

	@Test
	void testDeletedRelation_isTakenOffTheTarget() {
		IInstanz owner = newInstanz();
		IInstanz target = newInstanz();
		SingleInstanzValue relation = _svs.createNew(SingleInstanzValue.class, owner.getOwnKey(), "points at",
				target.getOwnKey(), Type.SEND);

		_svs.removeValue(relation, Type.SEND);

		assertThat(target.getReferencingValueKeys(), hasSize(0));
	}

	/**
	 * The whole point of the backward end: the target is gone, so every relation
	 * that named it is put back to pointing nowhere. The value itself stays - it is
	 * an attribute of its owner, under a name that owner gave it, and deleting a
	 * target is no reason to take an attribute off somebody else.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/74">#74</a>
	 */
	@Test
	void testDeletedInstanz_emptiesEveryRelationPointingAtIt() {
		IInstanz owner = newInstanz();
		IInstanz target = newInstanz();
		SingleInstanzValue relation = _svs.createNew(SingleInstanzValue.class, owner.getOwnKey(), "points at",
				target.getOwnKey(), Type.SEND);

		_inse.removeSubtreeInstanz(target.getOwnKey(), Type.SEND);

		assertThat(relation.getValue(), is(""));
		assertThat(_inse.resolveInstanzValue(relation), is(java.util.Optional.empty()));
		assertThat("the attribute itself is untouched",
				owner.getSingleValues(SingleValueType.SINGLE_INSTANZ).get(relation.getOwnKey()), is("points at"));
		assertThat(target.getReferencingValueKeys(), hasSize(0));
	}

	/** a whole branch: every instanz below the deleted one is a target too */
	@Test
	void testDeletedInstanz_reachesTheRelationsPointingIntoItsSubtree() {
		IInstanz owner = newInstanz();
		IInstanz branch = newInstanz();
		IInstanz leaf = _inse.createInstanz(branch.getOwnKey(), Type.SEND);
		SingleInstanzValue relation = _svs.createNew(SingleInstanzValue.class, owner.getOwnKey(), "points deep",
				leaf.getOwnKey(), Type.SEND);

		_inse.removeSubtreeInstanz(branch.getOwnKey(), Type.SEND);

		assertThat(relation.getValue(), is(""));
	}

	/**
	 * Two relations on the same target, and the second one must not be skipped: the
	 * set is walked over a copy, because emptying a value comes straight back round
	 * and takes its key out of exactly that set.
	 */
	@Test
	void testDeletedInstanz_emptiesEveryOneOfThem() {
		IInstanz owner = newInstanz();
		IInstanz target = newInstanz();
		SingleInstanzValue first = _svs.createNew(SingleInstanzValue.class, owner.getOwnKey(), "first", target.getOwnKey(),
				Type.SEND);
		SingleInstanzValue second = _svs.createNew(SingleInstanzValue.class, owner.getOwnKey(), "second",
				target.getOwnKey(), Type.SEND);

		_inse.removeSubtreeInstanz(target.getOwnKey(), Type.SEND);

		assertThat(first.getValue(), is(""));
		assertThat(second.getValue(), is(""));
	}

	/** an instanz pointing at itself is allowed, and deleting it has to terminate */
	@Test
	void testDeletedInstanz_aRelationOntoItselfIsEmptiedToo() {
		IInstanz self = newInstanz();
		SingleInstanzValue relation = _svs.createNew(SingleInstanzValue.class, self.getOwnKey(), "points at itself",
				self.getOwnKey(), Type.SEND);

		_inse.removeSubtreeInstanz(self.getOwnKey(), Type.SEND);

		assertThat(relation.getValue(), is(""));
	}

	// ---------- malformed payloads ----------

	/**
	 * {@link ChangePropagationListener} rejects a change type it does not know, and
	 * the event admin keeps that failure inside the handler. Both halves matter: a
	 * payload no service would build changes nothing, and it does not take the
	 * sender down with it either.
	 */
	@Test
	void testChildListChange_withoutAChangeType_changesNothingAndDoesNotEscape() {
		IInstanz parent = newInstanz();
		IInstanz child = _inse.createInstanz(parent.getOwnKey(), Type.SEND);

		publish(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent(parent.getOwnKey(), null, List.of(child.getOwnKey())));

		assertThat(child.getParentKey(), is(parent.getOwnKey()));
		assertThat(parent.getChildren(), contains(child.getOwnKey()));
	}

	/** @return the keys of every delete event the delta service has taken since the last flush */
	private List<String> deltaKeysOn(String topic) {
		return ProductRuntime.deltaService().getDeltas().stream()//
				.filter(event -> topic.equals(event.getTopic()))//
				.map(event -> InstanzEvent.class.cast(event.getProperty(IEventBroker.DATA))._key())//
				.toList();
	}
}
