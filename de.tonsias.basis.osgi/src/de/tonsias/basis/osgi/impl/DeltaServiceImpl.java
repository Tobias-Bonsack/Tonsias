package de.tonsias.basis.osgi.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.*;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.*;
import jakarta.inject.Inject;

/**
 * contains all emerging model-changed events, in order to have a bracket, the
 * operation is still listened to.
 * 
 * 11/23/2024: operations are not summarized, that doesn't seem to be relevant
 * to me at the moment, since even with undo/redo you can find out the brackets
 * here
 */
public class DeltaServiceImpl implements IDeltaService {

	@Inject
	protected IEventBrokerBridge _eventBridge;

	@Inject
	protected IInstanzService _instanzService;

	@Inject
	protected ISingleValueService _singleValueService;

	protected Collection<Event> _notSavedEvents = new LinkedList<Event>();

	public void postConstruct() {
		_notSavedEvents.add(START_EVENT);

		_eventBridge.subscribe(InstanzEventConstants.ALL_DELTA_TOPIC, this, true);
		_eventBridge.subscribe(SingleValueEventConstants.ALL_DELTA_TOPIC, this, true);
		_eventBridge.subscribe(EventConstants.OPEN_OPERATION, this, true);
		_eventBridge.subscribe(EventConstants.CLOSE_OPERATION, this, true);
		_eventBridge.subscribe(EventConstants.SAVE_ALL, event -> saveDeltas(), true);
	}

	@Override
	public void handleEvent(Event event) {
		_notSavedEvents.add(event);
	}

	@Override
	public void saveDeltas() {
		Set<String> instanzKeysToSave = new HashSet<String>();
		Set<String> singlevalueKeysToSave = new HashSet<String>();
		Set<String> instanzKeysToDelete = new HashSet<String>();
		Set<String> singlevalueKeysToDelete = new HashSet<String>();

		for (Event event : _notSavedEvents) {
			if (InstanzEventConstants.KNOWN_DELTA.contains(event.getTopic())) {
				handleInstanzEvents(event, instanzKeysToSave, instanzKeysToDelete);
			}
			if (SingleValueEventConstants.KNOWN_DELTA.contains(event.getTopic())) {
				handleSingleValueEvents(event, singlevalueKeysToSave, singlevalueKeysToDelete);
			}
		}

		_instanzService.saveAll(instanzKeysToSave);
		_instanzService.deleteAll(instanzKeysToDelete);
		_singleValueService.saveAll(singlevalueKeysToSave);
		_singleValueService.deleteAll(singlevalueKeysToDelete);

		_notSavedEvents.clear();
		_notSavedEvents.add(START_EVENT);
	}

	private void handleSingleValueEvents(Event event, Set<String> singlevalueKeysToSave,
			Set<String> singlevalueKeysToDelete) {
		switch (event.getTopic()) {
		case SingleValueEventConstants.NEW:
			var value = SingleValueNewEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToSave.add(value._key());
			break;
		case SingleValueEventConstants.VALUE_CHANGE:
			var value2 = ValueChangeEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToSave.add(value2._key());
			break;
		case SingleValueEventConstants.INSTANZ_LIST_CHANGE:
			var value3 = LinkedInstanzChangeEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToSave.add(value3._key());
			break;
		case SingleValueEventConstants.DELETE:
			var value4 = SingleValueDeleteEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToDelete.add(value4._key());
			break;
		default:
			throw new IllegalArgumentException("Enum value unknown: " + event.getTopic());
		}
	}

	private void handleInstanzEvents(Event event, Set<String> instanzKeysToSave, Set<String> instanzKeysToDelete) {
		Object property = event.getProperty(IEventBroker.DATA);
		switch (event.getTopic()) {
		case InstanzEventConstants.NEW:
			var value = InstanzEvent.class.cast(property);
			instanzKeysToSave.add(value._key());
			break;
		case InstanzEventConstants.PARENT_CHANGE:
			var value6 = ParentChange.class.cast(property);
			instanzKeysToSave.add(value6._key());
			break;
		case InstanzEventConstants.CHILD_LIST_CHANGE:
			var value5 = LinkedChildChangeEvent.class.cast(property);
			instanzKeysToSave.add(value5._key());
			break;
		case InstanzEventConstants.NAME_CHANGE:
			var value2 = ValueRenameEvent.class.cast(property);
			instanzKeysToSave.add(value2._key());
			break;
		case InstanzEventConstants.VALUE_LIST_CHANGE:
			var value3 = LinkedValueChangeEvent.class.cast(property);
			instanzKeysToSave.add(value3._key());
			break;
		case InstanzEventConstants.DELETE:
			var value4 = InstanzEvent.class.cast(property);
			instanzKeysToDelete.add(value4._key());
			_instanzService.resolveKey(value4._key()).ifPresent(i -> instanzKeysToSave.add(i.getParentKey()));
			break;
		default:
			throw new IllegalArgumentException("Enum value unknown: " + event.getTopic());
		}
	}

	@Override
	public Collection<Event> getDeltas() {
		return _notSavedEvents;
	}
}
