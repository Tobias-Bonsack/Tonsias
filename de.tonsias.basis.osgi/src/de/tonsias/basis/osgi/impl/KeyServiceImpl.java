package de.tonsias.basis.osgi.impl;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.IKeyService;

@Component
public class KeyServiceImpl implements IKeyService {

	private static final String KEY_KEY = "CurrentKey";

	private String _key;

	@Override
	public String generateKey() {
		String key = getCurrentKey();
		char[] keyArray = key.toCharArray();

		boolean isExcess = false;
		for (char c : keyArray) {
			if (c == Character.MAX_VALUE) {
				isExcess = true;
				c = Character.MIN_VALUE;
				continue;
			}

			if (isExcess) {
				isExcess = false;
				if (c == Character.MAX_VALUE) {
					isExcess = true;
					c = Character.MIN_VALUE;
					continue;
				}
				c++;
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
		String minKey = String.valueOf(Character.MIN_VALUE);
		String key = node.get(KEY_KEY, minKey);
		if (key.equals(minKey)) {
			key = String.valueOf(Character.MIN_VALUE + 1);
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

}
