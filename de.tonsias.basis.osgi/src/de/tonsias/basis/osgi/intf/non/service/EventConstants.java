package de.tonsias.basis.osgi.intf.non.service;

import de.tonsias.basis.osgi.intf.IDeltaService;

public interface EventConstants {
	enum EventType {
		NEW, DELETE, CHANGE;
	}

	String EVENT_TYPE = "eventType";
	String OLD_VALUE = "oldValue";
	String NEW_VAlUE = "newvalue";

	// Operations and others
	String OPEN_OPERATION = "open_operation";
	String CLOSE_OPERATION = "close_operation";

	/**
	 * Marker-Event to tell {@link IDeltaService} to save all changes and clear its
	 * history
	 */
	String SAVE_ALL = "save_all";
}
