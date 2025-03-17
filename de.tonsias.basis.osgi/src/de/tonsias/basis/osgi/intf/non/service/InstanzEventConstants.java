package de.tonsias.basis.osgi.intf.non.service;

import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.model.enums.SingleValueType;

public interface InstanzEventConstants {
	// topic identifier for all topics
	final String INSTANZ = "instanz";

	/**
	 * {@link IEventBroker#DATA} maps to {@link InstanzEvent}
	 */
	final String SELECTED = INSTANZ + "/selected";

	// this key can only be used for event registration, you cannot
	// send out generic events
	final String ALL_DELTA_TOPIC = INSTANZ + "/delta/*";

	/**
	 * {@link IEventBroker#DATA} maps to {@link InstanzEvent}
	 */
	final String NEW = INSTANZ + "/delta/new";

	/**
	 * {@link IEventBroker#DATA} maps to {@link ValueRenameEvent}
	 */
	final String NAME_CHANGE = INSTANZ + "/delta/nameChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link LinkedChildChangeEvent}
	 */
	final String CHILD_LIST_CHANGE = INSTANZ + "/delta/childChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link LinkedValueChangeEvent}
	 */
	final String VALUE_LIST_CHANGE = INSTANZ + "/delta/valueChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link InstanzEvent}
	 */
	final String DELETE = INSTANZ + "/delta/delete";

	final List<String> KNOWN_DELTA = List.of(NEW, NAME_CHANGE, VALUE_LIST_CHANGE, CHILD_LIST_CHANGE, DELETE);

	// data and the keys

	static record InstanzEvent(String _key) {

	}

	static record ValueRenameEvent(String _key, SingleValueType _type, String _attrKey, String _oldName,
			String _newName) {

	}

	public static enum ChangeType {
		ADD, REMOVE;
	}

	static record LinkedChildChangeEvent(String _key, ChangeType _changeType, List<String> _valueKeys) {
	}

	static record LinkedValueChangeEvent(String _key, SingleValueType _singleValuetype, ChangeType _changeType,
			List<String> _valueKeys) {
	}
}
