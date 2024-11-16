package de.tonsias.basis.osgi.intf.non.service;

import de.tonsias.basis.model.interfaces.IInstanz;

public interface InstanzEventConstants {
	// topic identifier for all topics
	String INSTANZ = "instanz";

	// this key can only be used for event registration, you cannot
	// send out generic events
	String ALL_DELTA_TOPIC = INSTANZ + "/delta/*";

	String NEW = INSTANZ + "/delta/new";

	String SELECTED = INSTANZ + "/selected";

	// data keys
	String DATA_INSTANZ = INSTANZ;

	static record PureInstanzData(IInstanz newInstanz) {
	}
}
