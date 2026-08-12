package de.tonsias.basis.model.interfaces;

import java.util.Collection;

import de.tonsias.basis.model.enums.IValueType;

/**
 * Top interface for a variable of an {@link IInstanz}, whether it holds one
 * content ({@link ISingleValue}) or a list of them ({@link ICollectionValue}).
 * <p>
 * Everything here is what both kinds answer alike: which type they are, and which
 * instanzes hold them.
 * </p>
 */
public interface IValue extends IObject, ISavePathOwner {

	/**
	 * Which type this value is - and with it, where it is saved and what it holds.
	 * Narrowed by the two sub-interfaces, so a caller that already knows the family
	 * gets the enum back rather than the interface.
	 */
	IValueType getType();

	/**
	 * The instanzes holding this value. One value can hang on several of them, which
	 * is why a delete has to tell every one of them.
	 */
	Collection<String> getConnectedInstanzKeys();

	/**
	 * @param key of the {@link IInstanz} now holding this value
	 * @return true if it was not recorded before - false, and no event from the
	 *         service, when it already was, so the propagation cannot re-enter
	 *         itself
	 */
	boolean addConnectedInstanzKey(String key);

	boolean removeConnection(Collection<String> connectedInstanzKeys);
}
