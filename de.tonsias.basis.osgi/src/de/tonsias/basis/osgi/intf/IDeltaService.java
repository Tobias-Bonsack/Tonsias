package de.tonsias.basis.osgi.intf;

import org.osgi.service.event.EventHandler;

public interface IDeltaService extends EventHandler {

	void saveDeltas();

}
