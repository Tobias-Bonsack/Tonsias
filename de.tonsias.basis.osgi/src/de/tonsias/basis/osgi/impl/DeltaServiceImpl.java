package de.tonsias.basis.osgi.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import jakarta.inject.Inject;

public class DeltaServiceImpl implements IDeltaService, EventHandler {

	@Inject
	IEventBrokerBridge _eventBride;

	@Inject
	SaveService _saveService;

	Collection<Event> _notSavedEvents = new ArrayList<Event>();

	public void postConstruct() {
		_eventBride.subscribe(InstanzEventConstants.ALL_DELTA_TOPIC, this, true);
	}

	@Override
	public void handleEvent(Event event) {
		System.out.printf("%s :: Delta Event - %s", this.getClass().toString(), event.toString());
		_notSavedEvents.add(event);
	}
}
