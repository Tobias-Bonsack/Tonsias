package de.tonsias.basis.osgi.intf;

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

}
