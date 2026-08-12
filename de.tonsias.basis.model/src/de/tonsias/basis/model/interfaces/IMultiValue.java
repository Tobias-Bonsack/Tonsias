package de.tonsias.basis.model.interfaces;

import java.util.Collection;
import java.util.List;

import de.tonsias.basis.model.enums.MultiValueType;

/**
 * A variable of an {@link IInstanz} holding a list of the same content a
 * {@link ISingleValue} holds one of.
 * <p>
 * The list is ordered and holds no duplicates. Ordered, because the order is what
 * the owner put things in and there is no other one to fall back on; without
 * duplicates, because {@link #addValue} answering {@code false} on something
 * already in the list is what ends a propagation chain, the same way
 * {@link IValue#addConnectedInstanzKey} does.
 * </p>
 */
public interface IMultiValue<T> extends ICollectionValue {

	@Override
	MultiValueType getType();

	/** the elements in insertion order, unmodifiable - changes go through the rest */
	List<T> getValues();

	/**
	 * @return false when the list already holds it, so nothing changed and nothing
	 *         is fired
	 */
	boolean addValue(T value);

	/** {@link #addValue} of what the type reads out of this object */
	boolean tryToAddValue(Object value);

	/** @return false when it was not in the list */
	boolean removeValue(T value);

	/** {@link #removeValue} of what the type reads out of this object */
	boolean tryToRemoveValue(Object value);

	/**
	 * Replaces the whole list. Duplicates among the new elements are dropped, the
	 * way {@link #addValue} would have dropped them.
	 *
	 * @return false when the list already read that way
	 */
	boolean setValues(Collection<? extends T> values);

	/**
	 * {@link #setValues} of what the type reads out of these objects.
	 *
	 * @return false when one of them is something the type will not read - the list
	 *         is then left as it was, rather than half taken over
	 */
	boolean tryToSetValues(Collection<?> values);

	boolean contains(T value);

	int size();
}
