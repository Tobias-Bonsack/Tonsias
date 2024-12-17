package de.tonsias.basis.osgi.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
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

	protected Collection<Event> _notSavedEvents = new ArrayList<Event>();

	private final Collection<String> _notSaveableEvents = List.of(EventConstants.OPEN_OPERATION,
			EventConstants.CLOSE_OPERATION);

	public void postConstruct() {
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
			if (_notSaveableEvents.contains(event.getTopic())) {
				continue;
			}
			handleInstanzEvents(event, instanzKeysToSave, instanzKeysToDelete);
			handleSingleValueEvents(event, singlevalueKeysToSave, singlevalueKeysToDelete);
		}

		_instanzService.saveAll(instanzKeysToSave);
		_instanzService.deleteAll(instanzKeysToDelete);
		_singleValueService.saveAll(singlevalueKeysToSave);
		_singleValueService.deleteAll(singlevalueKeysToDelete);

		_notSavedEvents.clear();
	}

	private void handleSingleValueEvents(Event event, Set<String> singlevalueKeysToSave,
			Set<String> singlevalueKeysToDelete) {
		switch (event.getTopic()) {
		case SingleValueEventConstants.NEW:
			var instanzData = SingleValueEventConstants.PureSingleValueData.class.cast(IEventBroker.DATA);
			singlevalueKeysToSave.add(instanzData._newSingleValue().getOwnKey());
			break;
		case SingleValueEventConstants.CHANGE:
			var change = SingleValueEventConstants.AttributeChangeData.class.cast(IEventBroker.DATA);
			singlevalueKeysToSave.add(change._key());
			break;
		case SingleValueEventConstants.DELETE:
			instanzData = SingleValueEventConstants.PureSingleValueData.class.cast(IEventBroker.DATA);
			singlevalueKeysToDelete.add(instanzData._newSingleValue().getOwnKey());
			break;
		}
	}

	private void handleInstanzEvents(Event event, Set<String> instanzKeysToSave, Set<String> instanzKeysToDelete) {
		switch (event.getTopic()) {
		case InstanzEventConstants.NEW:
			var instanzData = InstanzEventConstants.PureInstanzData.class.cast(IEventBroker.DATA);
			instanzKeysToSave.add(instanzData._newInstanz().getOwnKey());
			break;
		case InstanzEventConstants.CHANGE:
			var change = InstanzEventConstants.AttributeChangeData.class.cast(IEventBroker.DATA);
			instanzKeysToSave.add(change._key());
			break;
		case InstanzEventConstants.DELETE:
			instanzData = InstanzEventConstants.PureInstanzData.class.cast(IEventBroker.DATA);
			instanzKeysToDelete.add(instanzData._newInstanz().getOwnKey());
			break;
		}
	}
}
