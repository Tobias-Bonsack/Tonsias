package de.tonsias.basis.logic.test.part;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.logic.part.InstanzViewLogic;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;

/**
 * The view collects edits as jobs and only runs them when the dialog is
 * applied. What matters is which jobs survive until then - a pending delete
 * wins over any edit of the same value.
 */
@ExtendWith(MockitoExtension.class)
public class InstanzViewLogicJobTest {

	@Mock
	IInstanzService _instanzService;

	@Mock
	ISingleValueService _singleValueService;

	private InstanzViewLogic _logic;

	@BeforeEach
	void beforeEach() {
		_logic = new InstanzViewLogic(_instanzService, _singleValueService);
	}

	private void apply() throws OperationCanceledException, InterruptedException {
		_logic.executeChanges(0, mock(IEventBrokerBridge.class), null);
		Job.getJobManager().join(_logic, new NullProgressMonitor());
	}

	private static SingleStringValue value(String key) {
		return new SingleStringValue(key);
	}

	@Test
	void testModifyJob_runsOnApply() throws Exception {
		_logic.createModifySvJob("vKey", "newValue");

		apply();

		verify(_singleValueService).changeValue("vKey", "newValue", Type.SEND);
	}

	@Test
	void testModifyJob_lastEditOfAValueWins() throws Exception {
		_logic.createModifySvJob("vKey", "first");
		_logic.createModifySvJob("vKey", "second");

		apply();

		verify(_singleValueService).changeValue("vKey", "second", Type.SEND);
		verify(_singleValueService, never()).changeValue("vKey", "first", Type.SEND);
	}

	@Test
	void testNameJob_runsOnApply() throws Exception {
		SingleStringValue sv = value("vKey");

		_logic.createSvNameModifyJob("instanz", "newName", sv);
		apply();

		verify(_instanzService).changeSingleValueName("instanz", SingleValueType.SINGLE_STRING, "vKey", "newName",
				Type.SEND);
	}

	@Test
	void testDeleteJob_runsOnApply() throws Exception {
		SingleStringValue sv = value("vKey");

		_logic.createDeleteSvJob(sv);
		apply();

		verify(_singleValueService).removeValue(sv, Type.SEND);
	}

	@Test
	void testDeleteJob_dropsAlreadyQueuedEditsOfTheSameValue() throws Exception {
		SingleStringValue sv = value("vKey");
		_logic.createModifySvJob("vKey", "newValue");
		_logic.createSvNameModifyJob("instanz", "newName", sv);

		_logic.createDeleteSvJob(sv);
		apply();

		verify(_singleValueService).removeValue(sv, Type.SEND);
		verify(_singleValueService, never()).changeValue(anyString(), any(), any());
		verify(_instanzService, never()).changeSingleValueName(anyString(), any(), anyString(), anyString(), any());
	}

	@Test
	void testEditsAfterADelete_areIgnored() throws Exception {
		SingleStringValue sv = value("vKey");
		_logic.createDeleteSvJob(sv);

		_logic.createModifySvJob("vKey", "newValue");
		_logic.createSvNameModifyJob("instanz", "newName", sv);
		apply();

		verify(_singleValueService, never()).changeValue(anyString(), any(), any());
		verify(_instanzService, never()).changeSingleValueName(anyString(), any(), anyString(), anyString(), any());
	}

	@Test
	void testIsInDelete_onlyForValuesWithAPendingDelete() {
		SingleStringValue deleted = value("deleted");
		SingleStringValue kept = value("kept");

		_logic.createDeleteSvJob(deleted);

		assertThat(_logic.isInDelete(deleted), is(true));
		assertThat(_logic.isInDelete(kept), is(false));
	}

	@Test
	void testCancel_dropsEveryPendingJob() throws Exception {
		_logic.createModifySvJob("vKey", "newValue");
		_logic.createDeleteSvJob(value("other"));

		_logic.executeChanges(1, mock(IEventBrokerBridge.class), null);
		apply();

		verifyNoInteractions(_singleValueService, _instanzService);
	}

	@Test
	void testApply_clearsThePendingJobs() throws Exception {
		_logic.createModifySvJob("vKey", "newValue");
		apply();

		apply();

		verify(_singleValueService).changeValue("vKey", "newValue", Type.SEND);
	}
}
