package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;

public interface IInstanzService {

	/**
	 * try to resolve the key into an {@link IInstanz}
	 * 
	 * @param key to search for
	 * @return of an {@link Optional} for the {@link IInstanz}
	 */
	Optional<IInstanz> resolveKey(String key);

	/**
	 * return of the Root {@link IInstanz}. If there is no root, it creates one.
	 * There must be an root!!!
	 * 
	 * @return {@link IInstanz}
	 */
	IInstanz getRoot();

	/**
	 * try to resolve keys, ignores none resolvable keys
	 * 
	 * @param children: keys to resolve
	 * @return collection of resolvable {@link IInstanz}
	 */
	Collection<IInstanz> getInstanzes(Collection<String> children);

	/**
	 * Creates a new {@link IInstanz}, but does not save it
	 * 
	 * @param parent of the new instance
	 * @return a new {@link IInstanz}
	 */
	IInstanz createInstanz(IInstanz parent);

	/**
	 * saves all {@link IInstanz}, that are currently in the cache.
	 */
	void saveAll();

	/**
	 * Will change a attribute name
	 * 
	 * @param instanzKey of the instance to change a attribute
	 * @param type       of the attribute to change
	 * @param key        of the attribute to change
	 * @param newName    of the attribute
	 */
	void putSingleValue(String instanzKey, SingleValueType type, String key, String newName);

	/**
	 * Removes the given key from the given {@link IInstanz} keys in the
	 * {@link SingleValueType}
	 * 
	 * @param instanzKeys      where to remove the key
	 * @param type             of the key
	 * @param valueKeyToRemove {@link ISingleValue} key to remove
	 * @return true if no given {@link IInstanz} has the key anymore
	 */
	boolean removeValueKey(Collection<String> instanzKeys, SingleValueType type, String valueKeyToRemove);

	/**
	 * saves all {@link IInstanz} from the collection, if possible
	 * 
	 * @param instanzKeysToSave list of keys to save
	 * @return true, if all possible saved, else false
	 */
	boolean saveAll(Set<String> instanzKeysToSave);
}
