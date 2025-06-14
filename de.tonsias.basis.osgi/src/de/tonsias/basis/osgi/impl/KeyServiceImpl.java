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
