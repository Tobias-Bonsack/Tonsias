package de.tonsias.basis.logic.test.part;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.logic.part.InstanzViewLogic;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class InstanzViewLogicTest {

	private InstanzViewLogic _logic = new InstanzViewLogic();

	@Test
	void testCreateBiFunctionJob_validExecution() throws OperationCanceledException, InterruptedException {
		Job job = _logic.createBiFunctionJob((a, b) -> true, "a", "b");
		job.schedule();
		assertThat(Job.getJobManager().find(_logic), arrayWithSize(1));
		Job.getJobManager().join(_logic, new NullProgressMonitor());
	}

	@Test
	void testCreateQuadConsumerJob_validExecution() throws OperationCanceledException, InterruptedException {
		Job job = _logic.createPentaConsumerJob((a, b, c, d, e) -> {
		}, "a", SingleValueType.SINGLE_STRING, "c", "d");
		job.schedule();
		assertThat(Job.getJobManager().find(_logic), arrayWithSize(1));
		Job.getJobManager().join(_logic, new NullProgressMonitor());
	}

	@Test
	void testCreateOneAndTriFunctionJob() throws OperationCanceledException, InterruptedException {
		var value = mock(SingleStringValue.class);
		when(value.getConnectedInstanzKeys()).thenReturn(Collections.emptyList());

		Job job = _logic.createOneAndQuadFunctionJob((a) -> true, value, (a, b, c, d) -> true);
		job.schedule();
		assertThat(Job.getJobManager().find(_logic), arrayWithSize(1));
		Job.getJobManager().join(_logic, new NullProgressMonitor());
	}

	@Test
	void testExecuteChanges_Case0() {
		var broker = mock(IEventBrokerBridge.class);
		when(broker.post(anyString(), any())).thenReturn(true);
		_logic.executeChanges(0, broker, null);

		verify(broker, times(2)).post(anyString(), any());
	}

	@Test
	void testExecuteChanges_Case1() {
		_logic.executeChanges(1, null, null);
	}

	@Test
	void testExecuteChanges_Case2() {
		var broker = mock(IEventBrokerBridge.class);
		when(broker.post(anyString(), any())).thenReturn(true);
		var instanz = mock(Instanz.class);

		_logic.executeChanges(2, broker, instanz);

		verify(broker, times(1)).post(anyString(), eq(instanz));
	}
}
