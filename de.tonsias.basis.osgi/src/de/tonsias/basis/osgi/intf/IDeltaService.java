package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Collections;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public interface IDeltaService extends EventHandler {

	static final String START_TOPIC = "START";

	static final Event START_EVENT = new Event(START_TOPIC, Collections.emptyMap());

	/**
	 * Save all changes on the model.
	 * <p>
	 * Every part of the save is attempted, and the log is emptied either way - it
	 * describes the difference against the disk, and a set that was offered once is
	 * not offered again. What failed is carried out as suppressed exceptions of a
	 * {@link java.util.concurrent.CompletionException}, so a caller can report it,
	 * but the next save starts from what happened after this one.
	 * </p>
	 */
	void saveDeltas();

	Collection<Event> getDeltas();

}
