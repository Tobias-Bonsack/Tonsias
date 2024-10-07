package de.tonsias.basis.logic.dialog;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.IPreferences;
import de.tonsias.basis.osgi.util.OsgiUtil;

public class PreferencesDialogLogic {

	IKeyService _keyPrefService = OsgiUtil.getService(IKeyService.class);

	IBasicPreferenceService _basicPrefService = OsgiUtil.getService(IBasicPreferenceService.class);

	Map<String, IPreferences> _map;

	public PreferencesDialogLogic() {
		if (_keyPrefService == null || _basicPrefService == null) {
			return;
		}

		_map = Map.of(//
				_keyPrefService.getNode().toString(), _keyPrefService, //
				_basicPrefService.getNode().toString(), _basicPrefService);
	}

	public Collection<String> getPreferenceNames() {
		return _map.keySet();
	}

	public Map<String, String> getPreferences(String name) {
		String[] keys;
		try {
			keys = _map.get(name).getNode().keys();
			Map<String, String> result = Arrays.stream(keys)
					.collect(Collectors.toMap(key -> key, key -> _map.get(name).getNode().get(key, "")));
			return result;
		} catch (BackingStoreException e) {
			e.printStackTrace();
			return Collections.emptyMap();
		}
	}

	public void savePreference(Map<String, String> texts) {
		texts.entrySet().stream().forEach(pair -> {
			try {
				_basicPrefService.saveAsToString(pair.getKey(), pair.getValue());
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}
		});
	}

}
