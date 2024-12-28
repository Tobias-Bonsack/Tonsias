package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Collections;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public interface IDeltaService extends EventHandler {

	static final String START_TOPIC = "START";

	static final Event START_EVENT = new Event(START_TOPIC, Collections.emptyMap());

	void saveDeltas();

	Collection<Event> getDeltas();

}
