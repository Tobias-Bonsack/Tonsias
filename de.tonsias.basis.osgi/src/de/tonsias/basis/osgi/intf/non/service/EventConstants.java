package de.tonsias.basis.osgi.intf.non.service;

public interface EventConstants {
	enum EventType {
		NEW, DELETE, CHANGE;
	}

	String EVENT_TYPE = "eventType";
	String OLD_VALUE = "oldValue";
	String NEW_VAlUE = "newvalue";

}
