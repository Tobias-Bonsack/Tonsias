package de.tonsias.basis.osgi.impl;

import java.util.Arrays;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.IKeyService;

@Component
public class KeyServiceImpl implements IKeyService {

	/**
	 * Base 36 - lower case only, and deliberately so: keys become file names as
	 * they are, and on a case insensitive file system (Windows, macOS) 'a' and 'A'
	 * would be one and the same file. Must stay sorted ascending for the
	 * {@link Arrays#binarySearch(char[], char)} in {@link #countKeyUp(char[])}.
	 */
	public static final char[] KEYCHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	private static final String KEY_KEY = "CurrentKey";

	protected String _key;

	@Override
	public String initKey() {
		return getCurrentKey();
	}

	@Override
	public String generateKey() {
		String key = getCurrentKey();
		char[] keyArray = key.toCharArray();

		String result = countKeyUp(keyArray);
		saveKey(result);

		return getCurrentKey();
	}

	private String countKeyUp(char[] keyArray) {
		boolean isExcess = false;
		for (int i = 0; i < keyArray.length; i++) {
			char c = keyArray[i];
			if (c == KEYCHARS[KEYCHARS.length - 1]) {
				isExcess = true;
				keyArray[i] = KEYCHARS[0];
				continue;
			}

			isExcess = false;
			keyArray[i] = KEYCHARS[Arrays.binarySearch(KEYCHARS, c) + 1];
			break;
		}

		String result = String.valueOf(keyArray);
		if (isExcess) {
			result += KEYCHARS[0];
		}
		return result;
	}

	private String getCurrentKey() {
		if (_key != null) {
			return _key;
		}

		IEclipsePreferences node = getNode();
		String key = node.get(KEY_KEY, "");
		if (key.isBlank()) {
			key = String.valueOf(KEYCHARS[0]);
			node.put(KEY_KEY, key);
			flush(node);
		}

		String lowerCase = key.toLowerCase(Locale.ROOT);
		if (!lowerCase.equals(key)) {
			key = lowerCase;
			node.put(KEY_KEY, key);
			flush(node);
		}
		return _key = key;
	}

	private void saveKey(String newKey) {
		IEclipsePreferences node = getNode();
		node.put(KEY_KEY, newKey);
		flush(node);

		_key = newKey;
	}

	protected void flush(IEclipsePreferences node) {
		try {
			node.flush();
		} catch (BackingStoreException e) {
			Platform.getLog(getClass()).error("Can't flush Key: ", e);
		}
	}

	@Override
	public IEclipsePreferences getNode() {
		return InstanceScope.INSTANCE.getNode(KEY_KEY);
	}

	@Override
	public String previewNextKey() {
		return countKeyUp(getCurrentKey().toCharArray());
	}

	@Override
	public PreferenceKeyEnum[] getKeys() {
		return Key.values();
	}
}
