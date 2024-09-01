package de.tonsias.basis.osgi.intf;

public interface IKeyService {

	/**
	 * Creates a new key and saves state
	 * 
	 * @return unique key
	 */
	String generateKey();

	/**
	 * Calculate the next key, without saving it
	 * 
	 * @return unique key
	 */
	String previewNextKey();

	/**
	 * Creates prefs for keygeneration
	 * 
	 * @return first key ever
	 */
	String initKey();

}
