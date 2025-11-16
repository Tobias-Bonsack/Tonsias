package de.tonsias.basis.osgi.intf;

import de.tonsias.basis.osgi.intf.non.service.IPreferences;
import de.tonsias.basis.osgi.intf.non.service.PreferenceEventConstants;

public interface IKeyService extends IPreferences {

	/**
	 * WARNING: if you change a key, change in {@link PreferenceEventConstants} too
	 */
	enum Key implements PreferenceKeyEnum {
		CURRENT_KEY("CurrentKey", null, false);

		private final String _key;
		private final String _initValue;
		private final boolean _enabled;

		Key(String key, String initValue, boolean isEnabled) {
			_key = key;
			_initValue = initValue;
			_enabled = isEnabled;
		}

		@Override
		public String getKey() {
			return _key;
		}

		@Override
		public String getInitValue() {
			return _initValue;
		}

		@Override
		public boolean isEnabled() {
			return _enabled;
		}
	}

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
