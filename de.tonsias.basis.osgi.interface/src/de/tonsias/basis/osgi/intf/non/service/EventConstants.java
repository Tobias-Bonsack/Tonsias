package de.tonsias.basis.osgi.intf.non.service;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.osgi.intf.IDeltaService;

public interface EventConstants {
	enum EventType {
		NEW, DELETE, CHANGE;
	}

	String EVENT_TYPE = "eventType";
	String OLD_VALUE = "oldValue";
	String NEW_VAlUE = "newvalue";

	// Operations and others
	/**
	 * No {@link IEventBroker#DATA} needed
	 */
	String OPEN_OPERATION = "open_operation";
	/**
	 * No {@link IEventBroker#DATA} needed
	 */
	String CLOSE_OPERATION = "close_operation";

	/**
	 * Marker-Event to tell {@link IDeltaService} to save all changes and clear its
	 * history
	 */
	String SAVE_ALL = "save_all";
}
