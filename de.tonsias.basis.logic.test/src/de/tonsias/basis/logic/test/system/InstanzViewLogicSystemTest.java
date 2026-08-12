package de.tonsias.basis.logic.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.logic.part.InstanzViewLogic;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueNewEvent;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The logic behind the Instanz view, on the registered services and the real
 * job manager.
 * <p>
 * The view does not write while the user types: every edit becomes a
 * {@link Job} in a serial group, and nothing reaches the model until the dialog
 * is applied. So each test here queues edits, applies, waits for the job family
 * and then looks at the model itself - which is also the only place the "a
 * pending delete beats every edit of the same value" rule can be seen.
 * </p>
 */
public class InstanzViewLogicSystemTest {

	IInstanzService _inse;

	ISingleValueService _svs;

	IEventBrokerBridge _broker;

	IInstanz _owner;

	InstanzViewLogic _logic;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_svs = ProductRuntime.singleValueService();
		_broker = ProductRuntime.broker();
		_owner = _inse.createInstanz(ROOT, Type.SEND);

		_logic = new InstanzViewLogic(_inse, _svs);
		_recorder = EventRecorder.subscribeToAllDeltasAndOperations(_broker);
	}

	@AfterEach
	void afterEach() throws InterruptedException {
		Job.getJobManager().join(_logic, new NullProgressMonitor());
		_recorder.unsubscribe();
		ProductRuntime.flushDeltas();
	}

	private SingleStringValue newValue(String name, String content) {
		return _svs.createNew(SingleStringValue.class, _owner.getOwnKey(), name, content, Type.SEND);
	}

	/** applies the dialog and waits until every scheduled job has run */
	private void apply() throws OperationCanceledException, InterruptedException {
		_logic.executeChanges(0, _broker, null);
		Job.getJobManager().join(_logic, new NullProgressMonitor());
	}

	private String nameOf(SingleStringValue value) {
		return _owner.getValues(SingleValueType.SINGLE_STRING).get(value.getOwnKey());
	}

	// ---------- the pending jobs ----------

	@Test
	void testModifyJob_reachesTheValueOnlyOnApply() throws Exception {
		SingleStringValue value = newValue("parameter", "old");

		_logic.createModifySvJob(value.getOwnKey(), "new");
		assertThat(value.getValue(), is("old"));

		apply();

		assertThat(value.getValue(), is("new"));
	}

	@Test
	void testModifyJob_theLastEditOfAValueWins() throws Exception {
		SingleStringValue value = newValue("parameter", "old");

		_logic.createModifySvJob(value.getOwnKey(), "first");
		_logic.createModifySvJob(value.getOwnKey(), "second");
		apply();

		assertThat(value.getValue(), is("second"));
	}

	@Test
	void testNameJob_renamesTheAttributeOnItsOwner() throws Exception {
		SingleStringValue value = newValue("oldName", "content");

		_logic.createSvNameModifyJob(_owner.getOwnKey(), "newName", value);
		apply();

		assertThat(nameOf(value), is("newName"));
	}

	@Test
	void testDeleteJob_unlinksTheValueFromItsOwner() throws Exception {
		SingleStringValue value = newValue("parameter", "content");

		_logic.createDeleteSvJob(value);
		apply();

		assertThat(_owner.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat(value.getConnectedInstanzKeys(), hasSize(0));
	}

	/**
	 * Editing a value and then deleting it has to end in a delete, not in a save of
	 * something that is on its way out.
	 */
	@Test
	void testDeleteJob_dropsTheEditsAlreadyQueuedForTheSameValue() throws Exception {
		SingleStringValue value = newValue("oldName", "old");

		_logic.createModifySvJob(value.getOwnKey(), "new");
		_logic.createSvNameModifyJob(_owner.getOwnKey(), "newName", value);
		_logic.createDeleteSvJob(value);
		apply();

		assertThat(_owner.getValues(SingleValueType.SINGLE_STRING).containsKey(value.getOwnKey()), is(false));
		assertThat(value.getValue(), is("old"));
	}

	@Test
	void testEditsAfterADelete_areIgnored() throws Exception {
		SingleStringValue value = newValue("oldName", "old");

		_logic.createDeleteSvJob(value);
		_logic.createModifySvJob(value.getOwnKey(), "new");
		_logic.createSvNameModifyJob(_owner.getOwnKey(), "newName", value);
		apply();

		assertThat(value.getValue(), is("old"));
	}

	/** The view greys out a row whose value is on its way out. */
	@Test
	void testIsInDelete_onlyForValuesWithAPendingDelete() {
		SingleStringValue deleted = newValue("deleted", "content");
		SingleStringValue kept = newValue("kept", "content");

		_logic.createDeleteSvJob(deleted);

		assertThat(_logic.isInDelete(deleted), is(true));
		assertThat(_logic.isInDelete(kept), is(false));
	}

	@Test
	void testApply_clearsThePendingJobsSoASecondApplyDoesNothing() throws Exception {
		SingleStringValue value = newValue("parameter", "old");
		_logic.createModifySvJob(value.getOwnKey(), "new");
		apply();
		_svs.changeValue(value.getOwnKey(), "changed elsewhere", Type.SEND);

		apply();

		assertThat(value.getValue(), is("changed elsewhere"));
	}

	// ---------- the three dialog returns ----------

	/**
	 * Case 0 brackets the change jobs with an open and a close operation, which is
	 * what lets the Delta view group them into one row.
	 */
	@Test
	void testExecuteChanges_apply_bracketsTheChangesWithOneOperation() throws Exception {
		SingleStringValue value = newValue("parameter", "old");
		_logic.createModifySvJob(value.getOwnKey(), "new");
		_recorder.clear();

		apply();

		List<String> topics = _recorder.topics();
		assertThat(topics.get(0), is(EventConstants.OPEN_OPERATION));
		assertThat(topics.get(topics.size() - 1), is(EventConstants.CLOSE_OPERATION));
		assertThat(topics, hasItem(SingleValueEventConstants.VALUE_CHANGE));
	}

	@Test
	void testExecuteChanges_apply_withoutAnyEditStillBracketsTheOperation() throws Exception {
		_recorder.clear();

		apply();

		assertThat(_recorder.topics(),
				contains(EventConstants.OPEN_OPERATION, EventConstants.CLOSE_OPERATION));
	}

	/** Case 1 discards the pending changes without telling anyone. */
	@Test
	void testExecuteChanges_cancel_dropsEveryPendingJobSilently() throws Exception {
		SingleStringValue value = newValue("parameter", "old");
		_logic.createModifySvJob(value.getOwnKey(), "new");
		_logic.createDeleteSvJob(newValue("other", "content"));
		_recorder.clear();

		_logic.executeChanges(1, _broker, null);
		apply();

		assertThat(value.getValue(), is("old"));
		assertThat(_recorder.topics(), not(hasItem(SingleValueEventConstants.VALUE_CHANGE)));
	}

	/**
	 * Case 2 re-selects the shown instanz, so the view redraws from the model. The
	 * selection is not a delta - it sits outside the delta topics and therefore
	 * needs a recorder of its own.
	 */
	@Test
	void testExecuteChanges_applyAndReselect_announcesTheShownInstanz() {
		EventRecorder selections = EventRecorder.subscribeTo(_broker, InstanzEventConstants.SELECTED);
		try {
			_logic.executeChanges(2, _broker, _owner);

			assertThat(selections.onlyDataOf(InstanzEventConstants.SELECTED, InstanzEvent.class),
					is(new InstanzEvent(_owner.getOwnKey(), null)));
		} finally {
			selections.unsubscribe();
		}
	}

	// ---------- deciding whether a delta concerns the shown instanz ----------

	@Test
	void testAffectsShownInstanz_whileNothingIsShownNoDeltaConcernsTheView() {
		SingleStringValue value = newValue("parameter", "content");

		assertThat(_logic.affectsShownInstanz(null, deltaOf(value)), is(false));
	}

	@Test
	void testAffectsShownInstanz_aValueTheShownInstanzOwns() {
		SingleStringValue value = newValue("parameter", "content");

		assertThat(_logic.affectsShownInstanz(_owner, deltaOf(value)), is(true));
	}

	@Test
	void testAffectsShownInstanz_aValueOfAnotherInstanz() {
		SingleStringValue value = newValue("parameter", "content");
		IInstanz other = _inse.createInstanz(ROOT, Type.SEND);

		assertThat(_logic.affectsShownInstanz(other, deltaOf(value)), is(false));
	}

	/** A deleted value no longer resolves - there is nothing left to refresh for. */
	@Test
	void testAffectsShownInstanz_aValueThatNoLongerResolves() {
		SingleValueEvent gone = new SingleValueNewEvent(SingleValueType.SINGLE_STRING, "no-such-key", "name",
				List.of(_owner.getOwnKey()));

		assertThat(_logic.affectsShownInstanz(_owner, gone), is(false));
	}

	private static SingleValueEvent deltaOf(SingleStringValue value) {
		return new SingleValueNewEvent(SingleValueType.SINGLE_STRING, value.getOwnKey(), "parameter",
				List.of("irrelevant"));
	}
}
