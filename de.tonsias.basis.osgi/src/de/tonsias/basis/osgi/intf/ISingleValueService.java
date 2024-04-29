package de.tonsias.basis.osgi.intf;

import java.util.Optional;

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
	 * Creates a new {@link ISingleValue}, but it will not be saved
	 * 
	 * @param <E>   created class
	 * @param clazz identification
	 * @return new instance of {@link ISingleValue}
	 */
	<E extends ISingleValue<?>> E createNew(Class<E> clazz);

	/**
	 * saves all {@link ISingleValue}, that are currently in the cache.
	 */
	void saveAll();

}
