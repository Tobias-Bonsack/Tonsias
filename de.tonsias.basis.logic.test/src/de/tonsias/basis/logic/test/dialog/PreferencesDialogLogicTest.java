package de.tonsias.basis.logic.test.dialog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.prefs.BackingStoreException;
import de.tonsias.basis.logic.dialog.PreferencesDialogLogic;
import de.tonsias.basis.logic.dialog.PreferencesDialogLogic.PreferenceFeature;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService.Key;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.IPreferences;
import de.tonsias.basis.osgi.intf.non.service.IPreferences.PreferenceKeyEnum;

@ExtendWith(MockitoExtension.class)
public class PreferencesDialogLogicTest {

	@Mock
	IKeyService _keyPrefService;

	@Mock
	IBasicPreferenceService _basicPrefService;

	@Mock
	Map<String, IPreferences> _map;

	@InjectMocks
	PreferencesDialogLogic _logic;

	/**
	 * Every key of the preference yields one feature. The stored node has no value
	 * for them, so each feature falls back to the key's init value.
	 */
	@Test
	void testGetPreferences_validRequest() throws BackingStoreException {
		doReturn(_basicPrefService).when(_map).get(anyString());
		doReturn(IBasicPreferenceService.Key.values()).when(_basicPrefService).getKeys();
		doReturn(mock(IEclipsePreferences.class)).when(_basicPrefService).getNode();

		Collection<PreferenceFeature> preferences = _logic.getPreferences("");

		assertThat(preferences.size(), is(3));
		assertThat(preferences.stream().map(PreferenceFeature::value).toList(),
				is(Arrays.stream(IBasicPreferenceService.Key.values()).map(Key::getInitValue).toList()));
	}

	/** A preference that declares no keys contributes no features. */
	@Test
	void testGetPreferences_noKeys_emptyList() throws BackingStoreException {
		doReturn(_basicPrefService).when(_map).get(anyString());
		doReturn(new PreferenceKeyEnum[0]).when(_basicPrefService).getKeys();

		Collection<PreferenceFeature> preferences = _logic.getPreferences("");

		assertThat(preferences.size(), is(0));
	}

	@Test
	void testSavePreference() throws BackingStoreException {
		doNothing().when(_basicPrefService).saveAsToString(anyString(), any());
		_logic.savePreference(Map.of("a", "aa", "b", "bb"));

		verify(_basicPrefService, times(2)).saveAsToString(anyString(), any());
	}
}
