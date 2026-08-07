package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.osgi.impl.DeltaServiceImpl;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
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

/**
 * Covers how {@code DeltaServiceImpl} folds its event log into the four key
 * sets it hands to the services. {@link DeltaServiceImplTest} only checks that
 * the hand-off happens at all.
 */
@ExtendWith(MockitoExtension.class)
public class DeltaServiceImplDeltaTest {

	@Mock
	IInstanzService _instanzService;

	@Mock
	ISingleValueService _singleValueService;

	@Mock
	IEventBrokerBridge _eventBridge;

	private DeltaServiceImplTestee _service;

	@BeforeEach
	void beforeEach() {
		_service = new DeltaServiceImplTestee(_instanzService, _singleValueService, _eventBridge);
	}

	private static Event event(String topic, Object data) {
		return new Event(topic, data == null ? Map.of() : Map.of(IEventBroker.DATA, data));
	}

	private Set<String> capturedInstanzSaves() {
		ArgumentCaptor<Set<String>> keys = ArgumentCaptor.captor();
		verify(_instanzService).saveAll(keys.capture());
		return keys.getValue();
	}

	private Set<String> capturedSingleValueSaves() {
		ArgumentCaptor<Set<String>> keys = ArgumentCaptor.captor();
		verify(_singleValueService).saveAll(keys.capture());
		return keys.getValue();
	}

	@Test
	void testPostConstruct_subscribesToEveryDeltaTopicHeadless() {
		_service.postConstruct();

		verify(_eventBridge).subscribe(InstanzEventConstants.ALL_DELTA_TOPIC, _service, true);
		verify(_eventBridge).subscribe(SingleValueEventConstants.ALL_DELTA_TOPIC, _service, true);
		verify(_eventBridge).subscribe(eq(EventConstants.OPEN_OPERATION), any(), eq(true));
		verify(_eventBridge).subscribe(eq(EventConstants.CLOSE_OPERATION), any(), eq(true));
		verify(_eventBridge).subscribe(eq(EventConstants.SAVE_ALL), any(), eq(true));
	}

	@Test
	void testPostConstruct_logStartsWithTheStartEvent() {
		_service.postConstruct();

		assertThat(_service.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	@Test
	void testHandleEvent_keepsEveryEventInOrder() {
		Event first = event(InstanzEventConstants.NEW, new InstanzEvent("a", "0"));
		Event second = event(EventConstants.OPEN_OPERATION, null);

		_service.handleEvent(first);
		_service.handleEvent(second);

		assertThat(_service.getDeltas(), contains(first, second));
	}

	@Test
	void testSaveDeltas_everyInstanzChangeTopicMarksItsKeyForSaving() {
		_service.handleEvent(event(InstanzEventConstants.NEW, new InstanzEvent("new", "0")));
		_service.handleEvent(event(InstanzEventConstants.PARENT_CHANGE, new ParentChange("parent", "n", "o")));
		_service.handleEvent(event(InstanzEventConstants.CHILD_LIST_CHANGE,
				new LinkedChildChangeEvent("child", ChangeType.ADD, List.of("x"))));
		_service.handleEvent(event(InstanzEventConstants.NAME_CHANGE,
				new ValueRenameEvent("name", SingleValueType.SINGLE_STRING, "vKey", "old", "new")));
		_service.handleEvent(event(InstanzEventConstants.VALUE_LIST_CHANGE,
				new LinkedValueChangeEvent("value", SingleValueType.SINGLE_STRING, ChangeType.ADD, List.of("x"))));

		_service.saveDeltas();

		assertThat(capturedInstanzSaves(), containsInAnyOrder("new", "parent", "child", "name", "value"));
	}

	@Test
	void testSaveDeltas_deleteMarksTheKeyAndReSavesItsParent() {
		Instanz deleted = new Instanz("gone");
		deleted.setParentKey("parent");
		when(_instanzService.resolveKey("gone")).thenReturn(Optional.of(deleted));

		_service.handleEvent(event(InstanzEventConstants.DELETE, new InstanzEvent("gone", null)));

		_service.saveDeltas();

		ArgumentCaptor<Set<String>> deletes = ArgumentCaptor.captor();
		verify(_instanzService).deleteAll(deletes.capture());
		assertThat(deletes.getValue(), contains("gone"));
		assertThat(capturedInstanzSaves(), contains("parent"));
	}

	@Test
	void testSaveDeltas_everySingleValueChangeTopicMarksItsKeyForSaving() {
		_service.handleEvent(event(SingleValueEventConstants.NEW,
				new SingleValueNewEvent(SingleValueType.SINGLE_STRING, "new", "n", List.of("0"))));
		_service.handleEvent(event(SingleValueEventConstants.VALUE_CHANGE,
				new ValueChangeEvent("changed", SingleValueType.SINGLE_STRING, "a", "b")));
		_service.handleEvent(event(SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				new LinkedInstanzChangeEvent("linked", SingleValueType.SINGLE_STRING,
						LinkedInstanzChangeEvent.ChangeType.ADD, List.of("0"))));

		_service.saveDeltas();

		assertThat(capturedSingleValueSaves(), containsInAnyOrder("new", "changed", "linked"));
	}

	@Test
	void testSaveDeltas_singleValueDeleteGoesToTheDeleteSet() {
		_service.handleEvent(event(SingleValueEventConstants.DELETE,
				new SingleValueDeleteEvent(SingleValueType.SINGLE_STRING, "gone", List.of("0"))));

		_service.saveDeltas();

		ArgumentCaptor<Set<String>> deletes = ArgumentCaptor.captor();
		verify(_singleValueService).deleteAll(deletes.capture());
		assertThat(deletes.getValue(), contains("gone"));
		assertThat(capturedSingleValueSaves(), is(empty()));
	}

	@Test
	void testSaveDeltas_operationBracketsAndUnknownTopicsAreIgnored() {
		_service.handleEvent(event(EventConstants.OPEN_OPERATION, null));
		_service.handleEvent(event(InstanzEventConstants.SELECTED, new InstanzEvent("selected", null)));
		_service.handleEvent(event(EventConstants.CLOSE_OPERATION, null));

		_service.saveDeltas();

		assertThat(capturedInstanzSaves(), is(empty()));
	}

	@Test
	void testSaveDeltas_theSameKeyIsSavedOnlyOnce() {
		_service.handleEvent(event(InstanzEventConstants.NEW, new InstanzEvent("a", "0")));
		_service.handleEvent(event(InstanzEventConstants.NAME_CHANGE,
				new ValueRenameEvent("a", SingleValueType.SINGLE_STRING, "vKey", "old", "new")));

		_service.saveDeltas();

		assertThat(capturedInstanzSaves(), contains("a"));
	}

	@Test
	void testSaveDeltas_logIsResetToTheStartEvent() {
		_service.handleEvent(event(InstanzEventConstants.NEW, new InstanzEvent("a", "0")));

		_service.saveDeltas();

		assertThat(_service.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	private static class DeltaServiceImplTestee extends DeltaServiceImpl {

		DeltaServiceImplTestee(IInstanzService instanzService, ISingleValueService singleValueService,
				IEventBrokerBridge eventBridge) {
			_instanzService = instanzService;
			_singleValueService = singleValueService;
			_eventBridge = eventBridge;
		}
	}
}
