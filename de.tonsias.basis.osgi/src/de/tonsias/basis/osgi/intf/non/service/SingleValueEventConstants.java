package de.tonsias.basis.osgi.intf.non.service;

import java.util.Collection;
import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.model.enums.SingleValueType;

public interface SingleValueEventConstants {

	// topic identifier
	String SINGLE_VALUE = "singleValue";

	String ALL_DELTA_TOPIC = SINGLE_VALUE + "/delta/*";

	/**
	 * {@link IEventBroker#DATA} maps to {@link SingleValueEvent}
	 */
	String NEW = SINGLE_VALUE + "/delta/new";

	/**
	 * {@link IEventBroker#DATA} maps to {@link ValueChangeEvent}
	 */
	String VALUE_CHANGE = SINGLE_VALUE + "/delta/valueChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link LinkedInstanzChangeEvent}
	 */
	String INSTANZ_LIST_CHANGE = SINGLE_VALUE + "/delta/linkedInstanzChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link SingleValueDeleteEvent}
	 */
	String DELETE = SINGLE_VALUE + "/delta/delete";

	final List<String> KNOWN_DELTA = List.of(NEW, VALUE_CHANGE, INSTANZ_LIST_CHANGE, DELETE);

	// data and the keys
	interface SingleValueEvent {
		String getKey();

		SingleValueType getType();
	}

	static record SingleValueNewEvent(SingleValueType _type, String _key, String _name, Collection<String> _ownerKeys)
			implements SingleValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public SingleValueType getType() {
			return _type;
		}

	}

	static record SingleValueDeleteEvent(SingleValueType _type, String _key, Collection<String> _ownerKeys)
			implements SingleValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public SingleValueType getType() {
			return _type;
		}

	}

	static record ValueChangeEvent(String _key, SingleValueType _type, Object _oldValue, Object _newValue)
			implements SingleValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public SingleValueType getType() {
			return _type;
		}
	}

	static record LinkedInstanzChangeEvent(String _key, SingleValueType _singleValuetype, ChangeType _changeType,
			Collection<String> _instanzKeys) implements SingleValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public SingleValueType getType() {
			return _singleValuetype;
		}

		public static enum ChangeType {
			ADD, REMOVE;
		}
	}
}
