package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.util.ChangePropagationListener;
import jakarta.annotation.Nullable;

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
	 * Follows a relation: a {@link SingleInstanzValue} carries the key of the
	 * instanz it points at, and this is the one place that turns it back into the
	 * object. The value itself cannot - {@code de.tonsias.basis.model} has no OSGi
	 * dependency to reach a service with.
	 *
	 * @param value the reference to follow, may be {@code null}
	 * @return the target, or empty when the value points nowhere - the state a
	 *         fresh reference is in, and the one it is put back into when its
	 *         target is deleted
	 */
	Optional<IInstanz> resolveInstanzValue(SingleInstanzValue value);

	/**
	 * Records that a {@code SingleInstanzValue} points at this instanz - the
	 * backward direction of the relation, which the value itself does not carry.
	 * The forward direction is the value's own content and stays
	 * {@code ISingleValueService.changeValue}'s;
	 * {@link ChangePropagationListener} is what calls both.
	 *
	 * @param instanzKey of the target being pointed at
	 * @param valueKey   of the relation doing the pointing
	 * @return true if it was newly recorded - false, and no event, when it was
	 *         already there, so the propagation cannot re-enter itself
	 */
	boolean putReferencingValue(String instanzKey, String valueKey, IEventBrokerBridge.Type eventType);

	/**
	 * Takes a relation back out of the target's set, for a relation that was moved
	 * elsewhere or deleted.
	 *
	 * @param instanzKey of the target no longer pointed at
	 * @param valueKey   of the relation that pointed at it
	 * @return true if it was recorded before
	 * @see #putReferencingValue(String, String, IEventBrokerBridge.Type)
	 */
	boolean removeReferencingValue(String instanzKey, String valueKey, IEventBrokerBridge.Type eventType);

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
	 * Remove new child if not already present. If called remove child is same as
	 * delete child {@link IInstanz}
	 * 
	 * @param parentKey of new instanz parent
	 * @param childKey  of new instanz child
	 * @param eventType decide if events should be POST or SEND
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
	void putSingleValue(String instanzKey, SingleValueType type, String key, @Nullable  String name,
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
	 * Mark the {@link IInstanz} as delete and remove from parent if needed
	 * 
	 * @param instanzKey to remove
	 * @return
	 */
	boolean removeSubtreeInstanz(String instanzKey, IEventBrokerBridge.Type eventType);

	/**
	 * Mark the {@link IInstanz} as delete, nothing else. Should only be called from
	 * {@link ChangePropagationListener}
	 * 
	 * @param instanzKey to fire delete event
	 */
	void markInstanzAsDelete(String instanzKey, IEventBrokerBridge.Type eventType);

	/**
	 * saves all {@link IInstanz} from the collection, if possible
	 * 
	 * @param instanzKeysToSave list of keys to save
	 * @return true, if all possible saved, else false
	 */
	boolean saveAll(Set<String> instanzKeysToSave);

	/**
	 * Delete Files!
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
