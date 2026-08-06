package de.tonsias.basis.logic.test.part;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.logic.part.InstanzViewLogic;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class InstanzViewLogicTest {

	private InstanzViewLogic _logic = new InstanzViewLogic(null, null);

//	@Test
//	void testCreateBiFunctionJob_validExecution() throws OperationCanceledException, InterruptedException {
//		Job job = _logic.createBiFunctionJob((a, b) -> true, "a", "b");
//		job.schedule();
//		assertThat(Job.getJobManager().find(_logic), arrayWithSize(1));
//		Job.getJobManager().join(_logic, new NullProgressMonitor());
//	}
//
//	@Test
//	void testCreateQuadConsumerJob_validExecution() throws OperationCanceledException, InterruptedException {
//		Job job = _logic.createPentaConsumerJob((a, b, c, d, e) -> {
//		}, "a", SingleValueType.SINGLE_STRING, "c", "d");
//		job.schedule();
//		assertThat(Job.getJobManager().find(_logic), arrayWithSize(1));
//		Job.getJobManager().join(_logic, new NullProgressMonitor());
//	}
//
//	@Test
//	void testCreateOneAndTriFunctionJob() throws OperationCanceledException, InterruptedException {
//		var value = mock(SingleStringValue.class);
//		when(value.getConnectedInstanzKeys()).thenReturn(Collections.emptyList());
//
//		Job job = _logic.createBiAndQuadFunctionJob((a, b) -> true, value, (a, b, c, d) -> true);
//		job.schedule();
//		assertThat(Job.getJobManager().find(_logic), arrayWithSize(1));
//		Job.getJobManager().join(_logic, new NullProgressMonitor());
//	}

	/**
	 * Case 0 (apply) brackets the pending change jobs with an open and a close
	 * operation event. Both are fired from scheduled {@link Job}s, so the test has
	 * to wait for the job family before verifying.
	 */
	@Test
	void testExecuteChanges_Case0() throws OperationCanceledException, InterruptedException {
		var broker = mock(IEventBrokerBridge.class);

		_logic.executeChanges(0, broker, null);
		Job.getJobManager().join(_logic, new NullProgressMonitor());

		verify(broker).send(eq(EventConstants.OPEN_OPERATION), any());
		verify(broker).send(eq(EventConstants.CLOSE_OPERATION), any());
	}

	/** Case 1 (cancel) discards the pending changes without notifying anyone. */
	@Test
	void testExecuteChanges_Case1() {
		var broker = mock(IEventBrokerBridge.class);

		_logic.executeChanges(1, broker, null);

		verifyNoInteractions(broker);
	}

	/** Case 2 (apply and reselect) re-selects the shown instanz synchronously. */
	@Test
	void testExecuteChanges_Case2() {
		var broker = mock(IEventBrokerBridge.class);
		var instanz = mock(Instanz.class);
		when(instanz.getOwnKey()).thenReturn("key");

		_logic.executeChanges(2, broker, instanz);

		verify(broker).send(InstanzEventConstants.SELECTED,
				Map.of(IEventBroker.DATA, new InstanzEvent("key", null)));
	}
}
