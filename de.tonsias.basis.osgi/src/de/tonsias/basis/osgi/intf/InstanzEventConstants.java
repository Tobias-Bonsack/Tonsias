package de.tonsias.basis.osgi.intf;

public interface InstanzEventConstants {
	// topic identifier for all topics
	String INSTANZ = "instanz";

	// this key can only be used for event registration, you cannot
	// send out generic events
	String ALL_TOPIC = INSTANZ + "/*";

	String NEW = INSTANZ + "/new";

	String SELECTED = INSTANZ + "/selected";
}
