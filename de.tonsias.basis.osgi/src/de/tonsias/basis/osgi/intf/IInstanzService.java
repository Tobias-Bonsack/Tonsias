package de.tonsias.basis.osgi.intf;

import java.util.Collection;
import java.util.Optional;

import de.tonsias.basis.model.interfaces.IInstanz;

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
	 * @return a new {@link IInstanz}
	 */
	IInstanz createInstanz();

	/**
	 * saves all {@link IInstanz}, that are currently in the cache.
	 */
	void saveAll();

}
