package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * Both ends of a relation that points at several instanzen at once.
 * <p>
 * A relation is stored on the value - it carries the keys of its targets - and
 * every target records the value's key back, so that deleting a target can find
 * the relations pointing at it. Keeping those two in step is
 * {@code ChangePropagationListener}'s, and every assertion here is on the end the
 * call did <em>not</em> touch.
 * </p>
 */
public class MultiRelationPropagationSystemTest {

	IInstanzService _inse;

	IMultiValueService _mvs;

	ISingleValueService _svs;

	IInstanz _owner;

	IInstanz _first;

	IInstanz _second;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_mvs = ProductRuntime.multiValueService();
		_svs = ProductRuntime.singleValueService();

		_owner = _inse.createInstanz(ROOT, Type.SEND);
		_first = _inse.createInstanz(ROOT, Type.SEND);
		_second = _inse.createInstanz(ROOT, Type.SEND);

		_recorder = EventRecorder.subscribeToAllDeltas(ProductRuntime.broker());
	}

	@AfterEach
	void afterEach() {
		_recorder.unsubscribe();
		ProductRuntime.flushDeltas();
	}

	private MultiInstanzValue relationTo(IInstanz... targets) {
		return _mvs.createNew(MultiInstanzValue.class, _owner.getOwnKey(), "points at",
				List.of(targets).stream().map(IInstanz::getOwnKey).toList(), Type.SEND);
	}

	// ---------- creating ----------

	/**
	 * A list of relations is created pointing somewhere already, so every target has
	 * to learn about it here rather than from a change that never comes.
	 */
	@Test
	void testNewRelation_isRecordedOnEveryInstanzItPointsAt() {
		MultiInstanzValue relation = relationTo(_first, _second);

		assertThat(_first.getReferencingValueKeys(), contains(relation.getOwnKey()));
		assertThat(_second.getReferencingValueKeys(), contains(relation.getOwnKey()));
		assertThat("the owner is not a target", _owner.getReferencingValueKeys(), is(empty()));
	}

	@Test
	void testNewRelation_pointingNowhereRecordsNobody() {
		MultiInstanzValue relation = relationTo();

		assertThat(relation.getValues(), is(empty()));
		assertThat(_first.getReferencingValueKeys(), is(empty()));
	}

	/** a list of anything else has no target, so nothing is recorded anywhere */
	@Test
	void testNewStringList_recordsNobody() {
		_mvs.createNew(MultiStringValue.class, _owner.getOwnKey(), "words", List.of(_first.getOwnKey()), Type.SEND);

		assertThat(_first.getReferencingValueKeys(), is(empty()));
	}

	// ---------- changing ----------

	@Test
	void testAddedElement_isRecordedOnTheNewTargetOnly() {
		MultiInstanzValue relation = relationTo(_first);

		_mvs.addElement(relation.getOwnKey(), _second.getOwnKey(), Type.SEND);

		assertThat(_second.getReferencingValueKeys(), contains(relation.getOwnKey()));
		assertThat("the one it already pointed at is untouched", _first.getReferencingValueKeys(),
				contains(relation.getOwnKey()));
	}

	@Test
	void testRemovedElement_isTakenOffThatTargetOnly() {
		MultiInstanzValue relation = relationTo(_first, _second);

		_mvs.removeElement(relation.getOwnKey(), _first.getOwnKey(), Type.SEND);

		assertThat(_first.getReferencingValueKeys(), is(empty()));
		assertThat("the other target still is one", _second.getReferencingValueKeys(),
				contains(relation.getOwnKey()));
	}

	@Test
	void testChangedElements_movesTheRecordInOneStep() {
		MultiInstanzValue relation = relationTo(_first);

		_mvs.changeElements(relation.getOwnKey(), List.of(_second.getOwnKey()), Type.SEND);

		assertThat(_first.getReferencingValueKeys(), is(empty()));
		assertThat(_second.getReferencingValueKeys(), contains(relation.getOwnKey()));
	}

	/**
	 * Nothing started or stopped pointing anywhere, so both ends stay as they were -
	 * the chain runs into services that answer false without firing.
	 */
	@Test
	void testReorderedElements_leaveBothTargetsAsTheyWere() {
		MultiInstanzValue relation = relationTo(_first, _second);

		_mvs.changeElements(relation.getOwnKey(), List.of(_second.getOwnKey(), _first.getOwnKey()), Type.SEND);

		assertThat(_first.getReferencingValueKeys(), contains(relation.getOwnKey()));
		assertThat(_second.getReferencingValueKeys(), contains(relation.getOwnKey()));
	}

	// ---------- deleting ----------

	@Test
	void testDeletedRelation_isTakenOffEveryTarget() {
		MultiInstanzValue relation = relationTo(_first, _second);

		_mvs.markValueAsDelete(relation.getOwnKey(), Type.SEND);

		assertThat(_first.getReferencingValueKeys(), is(empty()));
		assertThat(_second.getReferencingValueKeys(), is(empty()));
		assertThat(_owner.getValues(MultiValueType.MULTI_INSTANZ).containsKey(relation.getOwnKey()), is(false));
	}

	/**
	 * The heart of the multi relation: deleting one target takes <em>its</em> element
	 * out and leaves the rest of the list pointing where it pointed. Emptying the
	 * value would take relations off instanzen nobody asked to change - the same
	 * reason the value itself is left standing.
	 */
	@Test
	void testDeletedTarget_losesItsElementAndNothingElse() {
		MultiInstanzValue relation = relationTo(_first, _second);

		_inse.markInstanzAsDelete(_first.getOwnKey(), Type.SEND);

		assertThat(relation.getValues(), contains(_second.getOwnKey()));
		assertThat(_second.getReferencingValueKeys(), contains(relation.getOwnKey()));
		assertThat(_first.getReferencingValueKeys(), is(empty()));
	}

	/** and the attribute stays on its owner, name and all */
	@Test
	void testDeletedTarget_leavesTheAttributeOnItsOwner() {
		MultiInstanzValue relation = relationTo(_first, _second);

		_inse.markInstanzAsDelete(_first.getOwnKey(), Type.SEND);

		assertThat(_owner.getValues(MultiValueType.MULTI_INSTANZ).get(relation.getOwnKey()), is("points at"));
	}

	@Test
	void testDeletedLastTarget_leavesAnEmptyList() {
		MultiInstanzValue relation = relationTo(_first);

		_inse.markInstanzAsDelete(_first.getOwnKey(), Type.SEND);

		assertThat(relation.getValues(), is(empty()));
	}

	/**
	 * The referencing set of a target holds bare keys, so a target pointed at by
	 * both kinds at once is what says the two are told apart by asking the services
	 * rather than by the event.
	 */
	@Test
	void testDeletedTarget_emptiesASingleRelationAndTrimsAListInOneGo() {
		MultiInstanzValue list = relationTo(_first, _second);
		SingleInstanzValue single = _svs.createNew(SingleInstanzValue.class, _owner.getOwnKey(), "points at too",
				_first.getOwnKey(), Type.SEND);

		assertThat(_first.getReferencingValueKeys(),
				containsInAnyOrder(list.getOwnKey(), single.getOwnKey()));

		_inse.markInstanzAsDelete(_first.getOwnKey(), Type.SEND);

		assertThat("the list keeps its other target", list.getValues(), contains(_second.getOwnKey()));
		assertThat("the single relation points nowhere", single.getValue(), is(""));
		assertThat(_first.getReferencingValueKeys(), is(empty()));
	}

	// ---------- persistence ----------

	@Test
	void testTheWholeRelationSurvivesASave() {
		MultiInstanzValue relation = relationTo(_first, _second);

		ProductRuntime.flushDeltas();

		MultiInstanzValue reloaded = ProductRuntime.reloadValue(MultiValueType.MULTI_INSTANZ, relation.getOwnKey(),
				MultiInstanzValue.class);
		assertThat(reloaded.getValues(), contains(_first.getOwnKey(), _second.getOwnKey()));
		assertThat(ProductRuntime.reloadInstanz(_first.getOwnKey()).getReferencingValueKeys(),
				contains(relation.getOwnKey()));
		assertThat(ProductRuntime.reloadInstanz(_owner.getOwnKey()).getValues(MultiValueType.MULTI_INSTANZ),
				org.hamcrest.Matchers.hasEntry(relation.getOwnKey(), "points at"));
	}
}
