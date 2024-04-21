package de.tonsias.basis.osgi.impl;

import java.util.Arrays;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.IKeyService;

@Component
public class KeyServiceImpl implements IKeyService {

	public static final char[] KEYCHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
			'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
			'v', 'w', 'x', 'y', 'z' };

	private static final String KEY_KEY = "CurrentKey";

	private String _key;

	@Override
	public String generateKey() {
		String key = getCurrentKey();
		char[] keyArray = key.toCharArray();

		boolean isExcess = false;
		for (char c : keyArray) {
			if (c == KEYCHARS[KEYCHARS.length - 1]) {
				isExcess = true;
				c = KEYCHARS[0];
				continue;
			}

			if (isExcess) {
				isExcess = false;
				if (c == KEYCHARS[KEYCHARS.length - 1]) {
					isExcess = true;
					c = KEYCHARS[0];
					continue;
				}
				c = KEYCHARS[Arrays.binarySearch(keyArray, c) + 1];
			}
		}

		String result = String.valueOf(keyArray);
		if (isExcess) {
			result += Character.MIN_VALUE;
		}
		saveKey(result);

		return getCurrentKey();
	}

	private String getCurrentKey() {
		if (_key != null) {
			return _key;
		}

		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(KEY_KEY);
		String key = node.get(KEY_KEY, "");
		if (key.isBlank()) {
			key = String.valueOf(KEYCHARS[0]);
			node.put(KEY_KEY, key);
			flush(node);
		}
		return _key = key;
	}

	private void saveKey(String newKey) {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(KEY_KEY);
		node.put(KEY_KEY, newKey);
		flush(node);
	}

	private void flush(IEclipsePreferences node) {
		try {
			node.flush();
		} catch (BackingStoreException e) {
			Platform.getLog(getClass()).error("Can't flush Key: ", e);
		}
	}

	@Override
	public String initKey() {
		return getCurrentKey();
	}

}
