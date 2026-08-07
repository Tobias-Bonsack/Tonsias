package de.tonsias.basis.ui.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.util.MessagesUtil;

class MessagesUtilTest {

	private Messages messages() {
		Messages messages = new Messages();
		messages.constant_type_string = "String";
		messages.constant_type_integer = "Integer";
		messages.constant_type_boolean = "Boolean";
		messages.pref_currentKey = "Current key";
		messages.pref_modelViewText = "Model view text";
		messages.pref_enableValues = "Enable values";
		messages.pref_modelRootPath = "Model root path";
		return messages;
	}

	@Test
	void getSingleValueTypeLabel_returnsTheMessageOfTheType() {
		Messages messages = messages();

		assertEquals("String", MessagesUtil.getSingleValueTypeLabel(messages, SingleValueType.SINGLE_STRING));
		assertEquals("Integer", MessagesUtil.getSingleValueTypeLabel(messages, SingleValueType.SINGLE_INTEGER));
		assertEquals("Boolean", MessagesUtil.getSingleValueTypeLabel(messages, SingleValueType.SINGLE_BOOLEAN));
	}

	/**
	 * Guards the switch in {@link MessagesUtil}: a new {@link SingleValueType} must
	 * not fall through to the untranslated enum name.
	 */
	@ParameterizedTest
	@EnumSource(SingleValueType.class)
	void getSingleValueTypeLabel_translatesEveryType(SingleValueType type) {
		String label = MessagesUtil.getSingleValueTypeLabel(messages(), type);

		assertNotNull(label);
		assertEquals(false, type.name().equals(label), "no translation for " + type.name());
	}

	@Test
	void getPreferenceLabel_returnsTheMessageOfTheKey() {
		Messages messages = messages();

		assertEquals("Current key",
				MessagesUtil.getPreferenceLabel(messages, IKeyService.Key.CURRENT_KEY.getKey()));
		assertEquals("Model view text",
				MessagesUtil.getPreferenceLabel(messages, IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey()));
		assertEquals("Enable values",
				MessagesUtil.getPreferenceLabel(messages, IBasicPreferenceService.Key.SHOW_VALUES.getKey()));
		assertEquals("Model root path",
				MessagesUtil.getPreferenceLabel(messages, IBasicPreferenceService.Key.SAVE_PATH.getKey()));
	}

	@ParameterizedTest
	@EnumSource(IBasicPreferenceService.Key.class)
	void getPreferenceLabel_translatesEveryBasicPreferenceKey(IBasicPreferenceService.Key key) {
		String label = MessagesUtil.getPreferenceLabel(messages(), key.getKey());

		assertEquals(false, key.getKey().equals(label), "no translation for " + key.getKey());
	}

	@ParameterizedTest
	@EnumSource(IKeyService.Key.class)
	void getPreferenceLabel_translatesEveryKeyServiceKey(IKeyService.Key key) {
		String label = MessagesUtil.getPreferenceLabel(messages(), key.getKey());

		assertEquals(false, key.getKey().equals(label), "no translation for " + key.getKey());
	}

	@Test
	void getPreferenceLabel_fallsBackToTheKeyOfAnUnknownPreference() {
		assertEquals("SomeUnknownPreference", MessagesUtil.getPreferenceLabel(messages(), "SomeUnknownPreference"));
	}
}
