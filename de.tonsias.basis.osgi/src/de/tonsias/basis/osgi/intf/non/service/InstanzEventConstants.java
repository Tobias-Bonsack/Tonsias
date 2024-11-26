package de.tonsias.basis.osgi.intf.non.service;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;

public interface InstanzEventConstants {
	// topic identifier for all topics
	String INSTANZ = "instanz";

	String SELECTED = INSTANZ + "/selected";

	// this key can only be used for event registration, you cannot
	// send out generic events
	String ALL_DELTA_TOPIC = INSTANZ + "/delta/*";

	String NEW = INSTANZ + "/delta/new";

	String CHANGE = INSTANZ + "delta/change";

	// data and the keys

	static record PureInstanzData(IInstanz _newInstanz) {
	}

	static record AttributeChangeData(String _key, SingleValueType _type, String _oldKey, String _oldValue,
			String _newKey, String _newValue) {
	}
}
