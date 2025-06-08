package de.tonsias.basis.osgi.intf;

import java.util.List;
import java.util.Optional;

import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.non.service.IPreferences;
import de.tonsias.basis.osgi.intf.non.service.PreferenceEventConstants;

public interface IBasicPreferenceService extends IPreferences {

	/**
	 * WARNING: if you change a key, change in {@link PreferenceEventConstants} too
	 */
	enum Key {
		MODEL_VIEW_TEXT("ModelViewText", "Name"), //
		SHOW_VALUES("EnableValues", "true");

		private final String _key;
		private String _initValue;

		Key(String key, String initValue) {
			_key = key;
			_initValue = initValue;
		}

		public final String getKey() {
			return _key;
		}
		
		public final String getInitValue() {
			return _initValue;
		}
	}

	String REGEX = "-_-";

	/**
	 * Get value of preference as an list of type T. Can crash if it can´t cast to T
	 * 
	 * 
	 * @param <T>  type of List
	 * @param key  for the value
	 * @param type class to cast to
	 * @return List of type, or empty if none is there
	 */
	<T> List<T> getAsList(String key, Class<T> type);

	/**
	 * Stores list in basicpreference
	 * 
	 * @param key  for the value
	 * @param list to store
	 * @throws BackingStoreException if flush does not work
	 */
	void saveAsList(String key, List<?> list) throws BackingStoreException;

	/**
	 * Get Value as T. Exception is cast does not work
	 * 
	 * @param <T>  result type
	 * @param key  for value
	 * @param type to cast to
	 * @return {@link Optional} with value if there was one
	 */
	<T> Optional<T> getValue(String key, Class<T> type);

	/**
	 * Saves any object as {@link Object.#toString()}
	 * 
	 * @param key    for value
	 * @param toSave object to save
	 * @throws BackingStoreException if flush does not work
	 */
	void saveAsToString(String key, Object toSave) throws BackingStoreException;
}
