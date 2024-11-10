package de.tonsias.basis.osgi.intf.non.service;

public interface InstanzEventConstants {
	// topic identifier for all topics
	String INSTANZ = "instanz";

	// this key can only be used for event registration, you cannot
	// send out generic events
	String ALL_DELTA_TOPIC = INSTANZ + "/delta/*";

	String NEW = INSTANZ + "/delta/new";

	String SELECTED = INSTANZ + "/selected";
}
