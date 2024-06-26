package de.tonsias.basis.osgi.intf;

public interface IKeyService {

	/**
	 * Creates a new key and saves state
	 * 
	 * @return unique key
	 */
	String generateKey();

	/**
	 * Creates prefs for keygeneration
	 * 
	 * @return first key ever
	 */
	String initKey();

}
