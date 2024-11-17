package de.tonsias.basis.osgi.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import jakarta.inject.Inject;

public class DeltaServiceImpl implements IDeltaService, EventHandler {

	@Inject
	IEventBrokerBridge _eventBride;

	@Inject
	IInstanzService _instanzService;

	@Inject
	ISingleValueService _singleValueService;

	Collection<Event> _notSavedEvents = new ArrayList<Event>();

	public void postConstruct() {
		_eventBride.subscribe(InstanzEventConstants.ALL_DELTA_TOPIC, this, true);
	}

	@Override
	public void handleEvent(Event event) {
		System.out.printf("%s :: Delta Event - %s", this.getClass().toString(), event.toString());
		_notSavedEvents.add(event);
	}

	@Override
	public void saveDeltas() {
		for (Event event : _notSavedEvents) {
			handleInstanzEvents(event);
			handleSingleValueEvents(event);
		}

		_instanzService.saveAll();
		_singleValueService.saveAll();
	}

	private void handleSingleValueEvents(Event event) {
		// TODO Auto-generated method stub

	}

	private void handleInstanzEvents(Event event) {
		if (event.getTopic().equals(InstanzEventConstants.DELETE)) {

		}
	}
}
