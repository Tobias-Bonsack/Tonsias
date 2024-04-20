package de.tonsias.basis.data.access.osgi.interfaces;

public interface IKeyService {

	/**
	 * Creates a new key and saves state
	 * 
	 * @return unique key
	 */
	String generateKey();

}
