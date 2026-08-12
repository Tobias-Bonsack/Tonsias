package de.tonsias.basis.osgi.intf.non.service;

import de.tonsias.basis.model.enums.IValueType;

/**
 * What the payloads of both value families answer alike.
 * <p>
 * {@code IDeltaService} folds a key and {@code InstanzView} asks whether an event
 * concerns the instanz it shows - neither cares which of the two families it came
 * from, and both would otherwise need the same code twice.
 * </p>
 */
public interface ValueEventConstants {

	interface ValueEvent {

		String getKey();

		/** narrowed to its own enum by the two families */
		IValueType getType();
	}

	/**
	 * Which direction a link moved. Lives here rather than nested in one event
	 * record, so the multi side does not bring a third copy along.
	 * {@link InstanzEventConstants#ChangeType} is still its own - see
	 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/84">#84</a>.
	 */
	enum ChangeType {
		ADD, REMOVE;
	}
}
