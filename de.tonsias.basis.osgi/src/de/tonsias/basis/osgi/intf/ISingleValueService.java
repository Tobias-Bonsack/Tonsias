package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;

public interface ISingleValueService {

	/**
	 * Try to resolve a key into a {@link ISingleValue}
	 * 
	 * @param <E>   expected class
	 * @param path  to save location
	 * @param key   of .json file
	 * @param clazz return class identification
	 * @return {@link Optional} of {@link ISingleValue} implementation
	 */
	<E extends ISingleValue<?>> Optional<E> resolveKey(String path, String key, Class<E> clazz);

	/**
	 * Try to resolve keys into a {@link ISingleValue}
	 * 
	 * @param <E>   expected class
	 * @param clazz return class identification
	 * @param path  to save location
	 * @param keys  of .json file
	 * @return {@link Collection} of {@link ISingleValue} implementation
	 */
	<E extends ISingleValue<?>> Collection<E> resolveKeys(Class<E> clazz, String path, Collection<String> keys);

	/**
	 * Creates a new {@link ISingleValue}, but it will not be saved and the parent
	 * will not be informed
	 * 
	 * @param <E>       created class
	 * @param clazz     identification
	 * @param parentKey Key of the parent {@link IInstanz}
	 * @param parameterName name of the parameter that holds the {@link IInstanz}
	 * @param value     of the new created {@link ISingleValue}
	 * @return new instance of {@link ISingleValue} or null
	 */
	<E extends ISingleValue<?>> E createNew(Class<E> clazz, String parentKey, String parameterName, Object value, IEventBrokerBridge.Type eventType);

	/**
	 * saves all {@link ISingleValue}, that are currently in the cache.
	 */
	void saveAll();

	/**
	 * Remove SingleValues from the cache
	 * 
	 * @param keys to remove from the cache
	 * @return if all keys are removed
	 */
	boolean removeFromCache(String... keys);

	/**
	 * Try to change the value of a single value
	 * 
	 * @param ownKey   Key of the single value
	 * @param newValue Possible new value
	 * @return boolean if the change was successful or not
	 */
	boolean changeValue(String ownKey, Object newValue, IEventBrokerBridge.Type eventType);

	/**
	 * Delete SingleValue from cache. It´s still available as file! Deletion will be
	 * handled through {@link IDeltaService}
	 * 
	 * @param valueToDelete {@link ISingleValue} to remove
	 * @return true if remove
	 */
	boolean removeValue(ISingleValue<?> valueToDelete, IEventBrokerBridge.Type eventType);
	
	/**
	 * 
	 * @param singleValueKeyToMark
	 */
	void markSingleValueAsDelete(String singleValueKeyToMark, IEventBrokerBridge.Type eventType);

	/**
	 * saves all {@link ISingleValue} from the collection, if possible. Can only
	 * save Objects in the cache, everything else will be ignored or it will crash
	 * 
	 * @param singlevalueKeysToSave list of keys to resolve and save
	 * @return true, if all possible saved, else false
	 */
	boolean saveAll(Set<String> singlevalueKeysToSave);

	/**
	 * Delete Files, should only called in any save environment!
	 * 
	 * @param singlevalueKeysToDelete Keys of {@link ISingleValue} to delete
	 * @throws {@link CompletionException} with all suppressed exceptions. Look into
	 *                {@link DeleteService} for more info
	 * @return <code>true</code> if all are deleted, if possible, else
	 *         <code>false</code>
	 */
	boolean deleteAll(Set<String> singlevalueKeysToDelete) throws CompletionException;
	
	/**
	 * Adds {@link ISingleValue} to {@link IInstanz} if not already happened
	 * @param type of SV
	 * @param valueKey of SV
	 * @param parentKey of Instanz
	 * @return true if added, else false
	 */
	boolean addToParent(SingleValueType type, String valueKey, String parentKey, IEventBrokerBridge.Type eventType);

}
