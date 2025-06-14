package de.tonsias.basis.logic.dialog;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

	public Collection<PreferenceFeature> getPreferences(String name) {
		IPreferences iPreferences = _map.get(name);
		return Arrays.stream(iPreferences.getKeys())//
				.map(pref -> new PreferenceFeature(//
						pref.getKey(), //
						pref.getInitValue() == null ? iPreferences.getNode().get(pref.getKey(), "")
								: pref.getInitValue(), //
						pref.isEnabled()))//
				.collect(Collectors.toUnmodifiableList());
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

	public record PreferenceFeature(String name, String value, boolean editable) {
		//
	}

}
