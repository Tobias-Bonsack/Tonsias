package de.tonsias.basis.osgi.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import jakarta.inject.Inject;

/**
 * contains all emerging model-changed events, in order to have a bracket, the
 * operation is still listened to.
 * 
 * 11/23/2024: operations are not summarized, that doesn't seem to be relevant
 * to me at the moment, since even with undo/redo you can find out the brackets
 * here
 */
public class DeltaServiceImpl implements IDeltaService, EventHandler {

	@Inject
	IEventBrokerBridge _eventBride;

	@Inject
	IInstanzService _instanzService;

	@Inject
	ISingleValueService _singleValueService;

	Collection<Event> _notSavedEvents = new ArrayList<Event>();

	private final Collection<String> _notSaveableEvents = List.of(EventConstants.OPEN_OPERATION,
			EventConstants.CLOSE_OPERATION);

	public void postConstruct() {
		_eventBride.subscribe(InstanzEventConstants.ALL_DELTA_TOPIC, this, true);
		_eventBride.subscribe(EventConstants.OPEN_OPERATION, this, true);
		_eventBride.subscribe(EventConstants.CLOSE_OPERATION, this, true);
		_eventBride.subscribe(EventConstants.SAVE_ALL, event -> saveDeltas(), true);
	}

	@Override
	public void handleEvent(Event event) {
		System.out.printf("%s :: Delta Event - %s", this.getClass().toString(), event.toString());
		_notSavedEvents.add(event);
	}

	@Override
	public void saveDeltas() {
		Set<String> instanzKeysToSave = new HashSet<String>();
		Set<String> singlevalueKeysToSave = new HashSet<String>();

		for (Event event : _notSavedEvents) {
			if (_notSaveableEvents.contains(event.getTopic())) {
				continue;
			}
			handleInstanzEvents(event, instanzKeysToSave);
			handleSingleValueEvents(event, singlevalueKeysToSave);
		}

		_instanzService.saveAll(instanzKeysToSave);
		_singleValueService.saveAll(singlevalueKeysToSave);

		_notSavedEvents.clear();
	}

	private void handleSingleValueEvents(Event event, Set<String> singlevalueKeysToSave) {

	}

	private void handleInstanzEvents(Event event, Set<String> instanzKeysToSave) {
		String[] propertyNames = event.getPropertyNames();
		for (String string : propertyNames) {
			if (InstanzEventConstants.PureInstanzData.class.getName().equals(string)) {
				var instanzData = InstanzEventConstants.PureInstanzData.class.cast(event.getProperty(string));
				instanzKeysToSave.add(instanzData._newInstanz().getOwnKey());
			}
		}
	}
}
