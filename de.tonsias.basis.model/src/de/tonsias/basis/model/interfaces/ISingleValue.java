package de.tonsias.basis.model.interfaces;

import de.tonsias.basis.model.enums.SingleValueType;

/**
 * A variable of an {@link IInstanz}, available here as single object. The same
 * content as a list is {@link IMultiValue}.
 */
public interface ISingleValue<T> extends IValue {

	@Override
	SingleValueType getType();

	T getValue();

	boolean setValue(T value);

	/**
	 * Stores what the type will read out of this object - an already typed value as
	 * well as the text a widget hands over - and nothing else.
	 *
	 * @return true if the value changed; false when the type will not read it, and
	 *         false when it already read that way, which is what ends a propagation
	 *         chain
	 */
	boolean tryToSetValue(Object value);
}
