package de.tonsias.basis.osgi.intf;

public interface InstanzEventConstants {
	// topic identifier for all topics
	String INSTANZ = "instanz";

	// this key can only be used for event registration, you cannot
	// send out generic events
	String INSTANZ_ALL_TOPIC = INSTANZ + "/*";

	String INSTANZ_NEW = INSTANZ + "/new";
}
