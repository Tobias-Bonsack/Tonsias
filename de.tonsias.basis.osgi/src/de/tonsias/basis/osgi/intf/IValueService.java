package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IValue;

/**
 * What holding values of one family means, whatever the family. A cache in front
 * of the files, a way from a key back to an object, and the save and delete the
 * delta service drives.
 * <p>
 * What the two families do <em>not</em> share is how a value is changed - one
 * value is set, a list has elements added and removed - so that stays with
 * {@link ISingleValueService} and {@link IMultiValueService}.
 * </p>
 *
 * @param <V> the values this service holds
 * @param <T> the types those values have
 */
public interface IValueService<V extends IValue, T extends IValueType> {

	/**
	 * Try to resolve a key into a value
	 *
	 * @param <E>   expected class
	 * @param path  to save location
	 * @param key   of .json file
	 * @param clazz return class identification
	 * @return {@link Optional} of the value
	 */
	<E extends V> Optional<E> resolveKey(String path, String key, Class<E> clazz);

	/**
	 * Try to resolve keys into values
	 *
	 * @param <E>   expected class
	 * @param clazz return class identification
	 * @param path  to save location
	 * @param keys  of .json files
	 * @return {@link Collection} of the values that resolved
	 */
	<E extends V> Collection<E> resolveKeys(Class<E> clazz, String path, Collection<String> keys);

	/**
	 * The value behind a key, whatever its type. The cache answers first; a value
	 * that has not been touched in this session is looked for in one type folder
	 * after the other, because a key alone does not say which type it belongs to.
	 * <p>
	 * The one caller outside this service is
	 * {@code ChangePropagationListener.emptyReferencesPointingAt}: the referencing
	 * set of an instanz holds bare keys, and asking which family one belongs to is
	 * the only way to know whether a relation is emptied or has one element taken
	 * out.
	 * </p>
	 *
	 * @return the value, or empty for a key no folder holds a file for
	 */
	Optional<V> resolveAnyKey(String key);

	/**
	 * saves all values that are currently in the cache.
	 */
	void saveAll();

	/**
	 * saves all values from the collection, if possible. Can only save objects in
	 * the cache, everything else will be ignored
	 *
	 * @param keysToSave list of keys to resolve and save
	 * @return true, if all possible saved, else false
	 */
	boolean saveAll(Set<String> keysToSave);

	/**
	 * Remove values from the cache
	 *
	 * @param keys to remove from the cache
	 * @return if all keys are removed
	 */
	boolean removeFromCache(String... keys);

	/**
	 * Delete files, should only be called in a save environment!
	 *
	 * @param keysToDelete keys of the values to delete
	 * @throws CompletionException with all suppressed exceptions. Look into
	 *                             {@link DeleteService} for more info
	 * @return <code>true</code> if all are deleted, if possible, else
	 *         <code>false</code>
	 */
	boolean deleteAll(Set<String> keysToDelete) throws CompletionException;

	/**
	 * Delete a value from the cache. It is still available as a file! Deletion will
	 * be handled through {@link IDeltaService}
	 *
	 * @param valueToDelete value to remove
	 * @return true if removed
	 */
	boolean deleteValue(V valueToDelete, IEventBrokerBridge.Type eventType);

	/**
	 * Cuts every owner loose and fires the delete event the delta service turns into
	 * a removed file.
	 */
	void markValueAsDelete(String keyToMark, IEventBrokerBridge.Type eventType);

	/**
	 * Adds a value to an {@link IInstanz} if not already happened
	 *
	 * @param type      of the value
	 * @param valueKey  of the value
	 * @param parentKey of the instanz
	 * @return true if added, else false - and no event either way, which is what
	 *         keeps the propagation from re-entering itself
	 */
	boolean addToParent(T type, String valueKey, String parentKey, IEventBrokerBridge.Type eventType);
}
