package de.tonsias.basis.logic.test.dialog;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.logic.dialog.PreferencesDialogLogic;
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
	void testGetPreferences() {
		_logic.getPreferences("");
	}
}
