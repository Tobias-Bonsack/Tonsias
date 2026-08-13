package de.tonsias.basis.osgi.intf.non.service;

import java.util.Collection;
import java.util.List;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.osgi.intf.non.service.ValueEventConstants.ValueEvent;

/**
 * The single value's four topics, once more for the lists.
 * <p>
 * A topic added here has to get a {@code case} in
 * {@code DeltaServiceImpl.handleMultiValueEvents} in the same edit: the delta
 * service throws over a topic in {@link #KNOWN_DELTA} it has no branch for, and
 * it clears the log in a {@code finally} - so the whole batch would be lost
 * without a trace.
 * </p>
 */
public interface MultiValueEventConstants {

	// topic identifier
	String MULTI_VALUE = "multiValue";

	String ALL_DELTA_TOPIC = MULTI_VALUE + "/delta/*";

	/**
	 * {@link IEventBroker#DATA} maps to {@link MultiValueNewEvent}
	 */
	String NEW = MULTI_VALUE + "/delta/new";

	/**
	 * {@link IEventBroker#DATA} maps to {@link ElementsChangeEvent}
	 */
	String VALUES_CHANGE = MULTI_VALUE + "/delta/valuesChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link LinkedInstanzChangeEvent}
	 */
	String INSTANZ_LIST_CHANGE = MULTI_VALUE + "/delta/linkedInstanzChange";

	/**
	 * {@link IEventBroker#DATA} maps to {@link MultiValueDeleteEvent}
	 */
	String DELETE = MULTI_VALUE + "/delta/delete";

	final List<String> KNOWN_DELTA = List.of(NEW, VALUES_CHANGE, INSTANZ_LIST_CHANGE, DELETE);

	// data and the keys
	interface MultiValueEvent extends ValueEvent {

		@Override
		String getKey();

		@Override
		MultiValueType getType();
	}

	/**
	 * @param _elements the list it was created holding - a relation created pointing
	 *                  somewhere already, so the targets learn about it here rather
	 *                  than from a change that never comes
	 */
	static record MultiValueNewEvent(MultiValueType _type, String _key, String _name, Collection<String> _ownerKeys,
			Collection<?> _elements) implements MultiValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public MultiValueType getType() {
			return _type;
		}

	}

	/**
	 * @param _elements the list it still held - carried along rather than looked up,
	 *                  because by the time a listener runs the value may be out of
	 *                  the cache and its file already scheduled for deletion
	 */
	static record MultiValueDeleteEvent(MultiValueType _type, String _key, Collection<String> _ownerKeys,
			Collection<?> _elements) implements MultiValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public MultiValueType getType() {
			return _type;
		}

	}

	/**
	 * What "the value changed" is for a list: which elements came and which went.
	 * <p>
	 * {@code IDeltaService} only folds the key - the file is rewritten either way -
	 * but {@code ChangePropagationListener} has to know which target stopped being
	 * pointed at, and the key alone does not say. Carried rather than diffed in the
	 * listener, which would be a second copy of the rule that computed it.
	 * </p>
	 * <p>
	 * Both sides are empty when the list was only reordered: the order is part of
	 * the value, so the file has to be rewritten, but nothing started or stopped
	 * pointing anywhere. What ends a propagation chain is the service answering
	 * {@code false} and firing nothing at all - not an event with nothing in it.
	 * </p>
	 */
	static record ElementsChangeEvent(String _key, MultiValueType _type, Collection<?> _addedElements,
			Collection<?> _removedElements) implements MultiValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public MultiValueType getType() {
			return _type;
		}
	}

	static record LinkedInstanzChangeEvent(String _key, MultiValueType _multiValuetype, ChangeType _changeType,
			Collection<String> _instanzKeys) implements MultiValueEvent {

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public MultiValueType getType() {
			return _multiValuetype;
		}
	}
}
