package de.tonsias.basis.osgi.intf;

import java.util.Collection;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public interface IDeltaService extends EventHandler {

	void saveDeltas();

	Collection<Event> getDeltas();

}
