package de.tonsias.basis.logic.test.part;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueNewEvent;

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

	/**
	 * While nothing is selected there is no key to compare the delta against, so the
	 * single value is not even resolved.
	 */
	@Test
	void testAffectsShownInstanz_noInstanzShown() {
		var svService = mock(ISingleValueService.class);
		var logic = new InstanzViewLogic(null, svService);

		assertThat(logic.affectsShownInstanz(null, DELTA), is(false));
		verifyNoInteractions(svService);
	}

	/** A delta of a value the shown instanz owns has to refresh the view. */
	@Test
	void testAffectsShownInstanz_connectedValue() {
		var value = mock(SingleStringValue.class);
		when(value.getConnectedInstanzKeys()).thenReturn(List.of("shown"));

		assertThat(logicResolving(value).affectsShownInstanz(shownInstanz(), DELTA), is(true));
	}

	/** A delta of a value belonging to another instanz must not refresh the view. */
	@Test
	void testAffectsShownInstanz_foreignValue() {
		var value = mock(SingleStringValue.class);
		when(value.getConnectedInstanzKeys()).thenReturn(List.of("other"));

		assertThat(logicResolving(value).affectsShownInstanz(shownInstanz(), DELTA), is(false));
	}

	/** A deleted value no longer resolves — nothing to refresh for. */
	@Test
	void testAffectsShownInstanz_unresolvableValue() {
		assertThat(logicResolving(null).affectsShownInstanz(mock(Instanz.class), DELTA), is(false));
	}

	private static final SingleValueEvent DELTA = new SingleValueNewEvent(SingleValueType.SINGLE_STRING, "value",
			"name", List.of("shown"));

	private Instanz shownInstanz() {
		var instanz = mock(Instanz.class);
		when(instanz.getOwnKey()).thenReturn("shown");
		return instanz;
	}

	private InstanzViewLogic logicResolving(SingleStringValue value) {
		var svService = mock(ISingleValueService.class);
		when(svService.<SingleStringValue>resolveKey(eq(SingleValueType.SINGLE_STRING.getPath()), eq("value"), any()))
				.thenReturn(Optional.ofNullable(value));
		return new InstanzViewLogic(null, svService);
	}
}
