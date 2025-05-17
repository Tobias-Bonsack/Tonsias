package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
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
	 * @param keys: keys to resolve
	 * @return collection of resolvable {@link IInstanz}
	 */
	Collection<IInstanz> resolveKeys(Collection<String> keys);

	/**
	 * Creates a new {@link IInstanz}, but does not save it
	 * 
	 * @param parentKey of the new instance
	 * @return a new {@link IInstanz}
	 */
	IInstanz createInstanz(String parentKey, IEventBrokerBridge.Type eventType);

	/**
	 * Add new child if not already present
	 * 
	 * @param parentKey of new instanz parent
	 * @param childKey  of new instanz child
	 * @return true, if newly added, else false
	 */
	boolean putChild(String parentKey, String childKey, IEventBrokerBridge.Type eventType);

	/**
	 * Add new child if not already present
	 * 
	 * @param parentKey of new instanz parent
	 * @param childKey  of new instanz child
	 * @return true, if newly added, else false
	 */
	boolean removeChild(String parentKey, String childKey, IEventBrokerBridge.Type eventType);

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
	void changeSingleValueName(String instanzKey, SingleValueType type, String key, String newName,
			IEventBrokerBridge.Type eventType);

	/**
	 * Add attribute to instanz
	 * 
	 * @param instanzKey of the instance to change a attribute
	 * @param type       of the attribute to change {@link SingleValueType}
	 * @param key        of the attribute to change
	 * @param name       of the attribute
	 */
	void putSingleValue(String instanzKey, SingleValueType type, String key, String name,
			IEventBrokerBridge.Type eventType);

	/**
	 * Removes the given key from the given {@link IInstanz} keys in the
	 * {@link SingleValueType}
	 * 
	 * @param instanzKeys      where to remove the key
	 * @param type             of the key
	 * @param valueKeyToRemove {@link ISingleValue} key to remove
	 * @return true if no given {@link IInstanz} has the key anymore
	 */
	boolean removeValueKey(Collection<String> instanzKeys, SingleValueType type, String valueKeyToRemove,
			IEventBrokerBridge.Type eventType);

	/**
	 * Mark the {@link IInstanz} as delete, remove from cache, nothing else
	 * 
	 * @param instanzKeys
	 * @return
	 */
	boolean deleteInstanz(String instanzKey, IEventBrokerBridge.Type eventType);

	/**
	 * saves all {@link IInstanz} from the collection, if possible
	 * 
	 * @param instanzKeysToSave list of keys to save
	 * @return true, if all possible saved, else false
	 */
	boolean saveAll(Set<String> instanzKeysToSave);

	/**
	 * Delete Files
	 * 
	 * @param instanzKeysToDelete Key of {@link IInstanz} to delete
	 * @throws {@link CompletionException} with all suppressed exceptions. Look into
	 *                {@link DeleteService} for more info
	 * @return <code>true</code> if all are deleted, if possible, else
	 *         <code>false</code>
	 */
	boolean deleteAll(Set<String> instanzKeysToDelete) throws CompletionException;

	/**
	 * Change its parent to the new one if not already is the new parent
	 * 
	 * @param childKey  of the child
	 * @param parentKey of new parent
	 * @return true, if set, else false
	 */
	boolean changeParent(String childKey, String parentKey, IEventBrokerBridge.Type eventType);
}
