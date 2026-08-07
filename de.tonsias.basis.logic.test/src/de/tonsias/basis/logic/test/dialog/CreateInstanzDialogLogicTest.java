package de.tonsias.basis.logic.test.dialog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic;
import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic.TableRecord;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;

@ExtendWith(MockitoExtension.class)
public class CreateInstanzDialogLogicTest {

	/** {@code okPressed} works in a {@link org.eclipse.core.runtime.jobs.Job}. */
	private static final long JOB_TIMEOUT = 10_000;

	@Mock
	IInstanzService _instanzService;

	@Mock
	ISingleValueService _singleValueService;

	@Mock
	IBasicPreferenceService _prefService;

	private CreateInstanzDialogLogic newLogic(Optional<String> modelViewText) {
		lenient().when(_prefService.getValue(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), String.class))
				.thenReturn(modelViewText);
		return new CreateInstanzDialogLogic(_instanzService, _singleValueService, _prefService);
	}

	@Test
	void testNewLogic_seedsARowForTheConfiguredNameParameter() {
		var logic = newLogic(Optional.of("Name"));

		assertThat(logic.getInput(), hasSize(1));
		TableRecord row = logic.getInput().iterator().next();
		assertThat(row.type, is(SingleValueType.SINGLE_STRING));
		assertThat(row.parameterName, is("Name"));
	}

	@Test
	void testNewLogic_withoutThePreferenceStartsEmpty() {
		assertThat(newLogic(Optional.empty()).getInput(), is(empty()));
	}

	@Test
	void testAddNewEntry_appendsADefaultRow() {
		var logic = newLogic(Optional.empty());

		logic.addNewEntry();

		assertThat(logic.getInput(), hasSize(1));
		assertThat(logic.getInput().iterator().next().type, is(SingleValueType.SINGLE_STRING));
	}

	@Test
	void testRemoveSelectedEntry_dropsTheGivenRow() {
		var logic = newLogic(Optional.empty());
		logic.addNewEntry();
		logic.addNewEntry();
		TableRecord toRemove = logic.getInput().iterator().next();

		logic.removeSelectedEntry(toRemove);

		assertThat(logic.getInput(), hasSize(1));
		assertThat(logic.getInput().contains(toRemove), is(false));
	}

	@Test
	void testRemoveSelectedEntry_unknownRowChangesNothing() {
		var logic = newLogic(Optional.empty());
		logic.addNewEntry();

		logic.removeSelectedEntry("not a row");

		assertThat(logic.getInput(), hasSize(1));
	}

	@Test
	void testOkPressed_createsTheInstanzAndItsValuesInsideOneOperation() {
		var logic = newLogic(Optional.empty());
		logic.setInstanzParent(new Instanz("parent"));
		logic.addNewEntry();
		TableRecord row = logic.getInput().iterator().next();
		row.parameterName = "param";
		row.value = "value";

		IEventBrokerBridge broker = mock(IEventBrokerBridge.class);
		when(_instanzService.createInstanz("parent", Type.SEND)).thenReturn(new Instanz("newKey"));

		logic.okPressed(broker);

		verify(broker, timeout(JOB_TIMEOUT)).send(EventConstants.CLOSE_OPERATION, null);
		verify(_singleValueService).createNew(SingleStringValue.class, "newKey", "param", "value", Type.SEND);

		var order = inOrder(broker, _instanzService, _singleValueService);
		order.verify(broker).send(EventConstants.OPEN_OPERATION, null);
		order.verify(_instanzService).createInstanz("parent", Type.SEND);
		order.verify(_singleValueService).createNew(any(), eq("newKey"), eq("param"), eq("value"), eq(Type.SEND));
		order.verify(broker).send(EventConstants.CLOSE_OPERATION, null);
	}

	@Test
	void testOkPressed_withoutRowsStillBracketsTheOperation() {
		var logic = newLogic(Optional.empty());
		logic.setInstanzParent(new Instanz("parent"));

		IEventBrokerBridge broker = mock(IEventBrokerBridge.class);
		when(_instanzService.createInstanz("parent", Type.SEND)).thenReturn(new Instanz("newKey"));

		logic.okPressed(broker);

		verify(broker, timeout(JOB_TIMEOUT)).send(EventConstants.CLOSE_OPERATION, null);
		verify(_singleValueService, never()).createNew(any(), any(), any(), any(), any());
	}

	@Test
	void testGetInput_reflectsEveryChange() {
		var logic = newLogic(Optional.of("Name"));
		var input = logic.getInput();

		logic.addNewEntry();

		assertThat(input, hasSize(2));
		assertThat(input.stream().map(r -> r.parameterName).toList(), contains("Name", "parameterName"));
	}
}
