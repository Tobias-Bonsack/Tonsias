package de.tonsias.basis.osgi.intf;

import de.tonsias.basis.osgi.intf.non.service.IPreferences;

public interface IKeyService extends IPreferences {

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
