package de.tonsias.basis.osgi.intf.non.service;

import java.util.Collection;
import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.model.enums.IValueType;

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
	 * {@link IEventBroker#DATA} maps to {@link ParentChange}
	 */
	final String PARENT_CHANGE = INSTANZ + "/delta/parentChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link LinkedValueChangeEvent}
	 */
	final String VALUE_LIST_CHANGE = INSTANZ + "/delta/valueChange";

	/**
	 * The instanz is the <em>target</em> of a relation, not its owner: a relation
	 * started or stopped pointing at it.
	 * <p>
	 * {@link IEventBroker#DATA} maps to {@link LinkedReferenceChangeEvent}
	 * </p>
	 */
	final String REFERENCE_LIST_CHANGE = INSTANZ + "/delta/referenceChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link InstanzEvent}
	 */
	final String DELETE = INSTANZ + "/delta/delete";

	final List<String> KNOWN_DELTA = List.of(NEW, NAME_CHANGE, VALUE_LIST_CHANGE, CHILD_LIST_CHANGE, DELETE,
			PARENT_CHANGE, REFERENCE_LIST_CHANGE);

	// data and the keys

	interface KeyEvent {
		String getKey();
	}

	static record InstanzEvent(String _key, String _parentKey) implements KeyEvent {

		@Override
		public String getKey() {
			return _key;
		}

	}

	static record ValueRenameEvent(String _key, IValueType _type, String _attrKey, String _oldName, String _newName)
			implements KeyEvent {

		@Override
		public String getKey() {
			return _key;
		}

	}

	public static enum ChangeType {
		ADD, REMOVE;
	}

	static record ParentChange(String _key, String _newParentKey, String _oldParentKey) implements KeyEvent {

		@Override
		public String getKey() {
			return _key;
		}

	}

	static record LinkedChildChangeEvent(String _key, ChangeType _changeType, Collection<String> _instanzKeys)
			implements KeyEvent {

		@Override
		public String getKey() {
			return _key;
		}

	}

	static record LinkedValueChangeEvent(String _key, IValueType _valueType, ChangeType _changeType,
			Collection<String> _valueKeys) implements KeyEvent {

		@Override
		public String getKey() {
			return _key;
		}

	}

	/**
	 * The counterpart of {@link LinkedValueChangeEvent} for the relation: there the
	 * instanz owns the values, here it is what they point at.
	 * <p>
	 * A relation is a {@code SINGLE_INSTANZ} or a {@code MULTI_INSTANZ} value, and
	 * the type is still not carried: what the target stores is a set of value keys
	 * and nothing else, so the only consumer - {@code DeltaServiceImpl}, which folds
	 * {@link #_key()} - has no use for it. Whoever does have to know which of the
	 * two a key is asks the services, see
	 * {@code ChangePropagationListener.emptyReferencesPointingAt}.
	 * </p>
	 *
	 * @param _key       of the instanz being pointed at
	 * @param _valueKeys of the relations doing the pointing
	 */
	static record LinkedReferenceChangeEvent(String _key, ChangeType _changeType, Collection<String> _valueKeys)
			implements KeyEvent {

		@Override
		public String getKey() {
			return _key;
		}

	}
}
