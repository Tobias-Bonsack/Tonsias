package de.tonsias.basis.logic.test.dialog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.IPreferences;

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

	@Test
	void testGetPreferences_validRequest() throws BackingStoreException {
		doReturn(_basicPrefService).when(_map).get(anyString());
		doReturn(IBasicPreferenceService.Key.values()).when(_basicPrefService).getKeys();

		Collection<PreferenceFeature> preferences = _logic.getPreferences("");

		assertThat(preferences.size(), is(3));
	}

	@Test
	void testGetPreferences_error_emptyList() throws BackingStoreException {
		doReturn(_basicPrefService).when(_map).get(anyString());

		IEclipsePreferences mockedPreferences = mock(IEclipsePreferences.class);
		doThrow(BackingStoreException.class).when(mockedPreferences).keys();
		lenient().doReturn("aa").when(mockedPreferences).get(eq("a"), anyString());
		lenient().doReturn("bb").when(mockedPreferences).get(eq("b"), anyString());

		doReturn(mockedPreferences).when(_basicPrefService).getNode();

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
