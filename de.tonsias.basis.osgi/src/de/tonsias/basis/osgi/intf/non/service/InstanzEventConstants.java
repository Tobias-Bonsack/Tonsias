package de.tonsias.basis.osgi.intf.non.service;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;

public interface InstanzEventConstants {
	// topic identifier for all topics
	String INSTANZ = "instanz";

	String SELECTED = INSTANZ + "/selected";

	// this key can only be used for event registration, you cannot
	// send out generic events
	String ALL_DELTA_TOPIC = INSTANZ + "/delta/*";

	/**
	 * {@link IEventBroker#DATA} maps to {@link PureInstanzData}
	 */
	String NEW = INSTANZ + "/delta/new";

	/**
	 * {@link IEventBroker#DATA} maps to {@link AttributeChangeData}
	 */
	String CHANGE = INSTANZ + "/delta/change";

	// data and the keys

	static record PureInstanzData(IInstanz _newInstanz) {
	}

	static record AttributeChangeData(String _key, SingleValueType _type, String _oldKey, String _oldValue,
			String _newKey, String _newValue) {
	}
}
