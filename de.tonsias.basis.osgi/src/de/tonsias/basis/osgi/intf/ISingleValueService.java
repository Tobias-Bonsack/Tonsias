package de.tonsias.basis.osgi.intf;

import java.util.Collection;
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
	 * Try to resolve keys into a {@link ISingleValue}
	 * 
	 * @param <E>   expected class
	 * @param clazz return class identification
	 * @param path  to save location
	 * @param keys  of .json file
	 * @return {@link Collection} of {@link ISingleValue} implementation
	 */
	<E extends ISingleValue<?>> Collection<E> resolveKeys(Class<E> clazz, String path, String... keys);

	/**
	 * Creates a new {@link ISingleValue}, but it will not be saved
	 * 
	 * @param <E>    created class
	 * @param clazz  identification
	 * @param parent of the new singlevalue
	 * @param name   of the new variables
	 * @return new instance of {@link ISingleValue}
	 */
	<E extends ISingleValue<?>> E createNew(Class<E> clazz, IInstanz parent, String name);

	/**
	 * saves all {@link ISingleValue}, that are currently in the cache.
	 */
	void saveAll();

}
