package de.tonsias.basis.ui.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueTypes;
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
		messages.constant_type_float = "Float";
		messages.constant_type_instanz = "Instanz reference";
		messages.constant_type_multi_string = "String list";
		messages.constant_type_multi_integer = "Integer list";
		messages.constant_type_multi_boolean = "Boolean list";
		messages.constant_type_multi_float = "Float list";
		messages.constant_type_multi_instanz = "Instanz reference list";
		messages.pref_currentKey = "Current key";
		messages.pref_modelViewText = "Model view text";
		messages.pref_enableValues = "Enable values";
		messages.pref_modelRootPath = "Model root path";
		return messages;
	}

	static java.util.List<IValueType> everyType() {
		return ValueTypes.valuesList();
	}

	@Test
	void getValueTypeLabel_returnsTheMessageOfTheType() {
		Messages messages = messages();

		assertEquals("String", MessagesUtil.getValueTypeLabel(messages, SingleValueType.SINGLE_STRING));
		assertEquals("Integer", MessagesUtil.getValueTypeLabel(messages, SingleValueType.SINGLE_INTEGER));
		assertEquals("Boolean", MessagesUtil.getValueTypeLabel(messages, SingleValueType.SINGLE_BOOLEAN));
		assertEquals("Float", MessagesUtil.getValueTypeLabel(messages, SingleValueType.SINGLE_FLOAT));
		assertEquals("Instanz reference", MessagesUtil.getValueTypeLabel(messages, SingleValueType.SINGLE_INSTANZ));
	}

	@Test
	void getValueTypeLabel_returnsTheMessageOfEveryListType() {
		Messages messages = messages();

		assertEquals("String list", MessagesUtil.getValueTypeLabel(messages, MultiValueType.MULTI_STRING));
		assertEquals("Integer list", MessagesUtil.getValueTypeLabel(messages, MultiValueType.MULTI_INTEGER));
		assertEquals("Boolean list", MessagesUtil.getValueTypeLabel(messages, MultiValueType.MULTI_BOOLEAN));
		assertEquals("Float list", MessagesUtil.getValueTypeLabel(messages, MultiValueType.MULTI_FLOAT));
		assertEquals("Instanz reference list",
				MessagesUtil.getValueTypeLabel(messages, MultiValueType.MULTI_INSTANZ));
	}

	/**
	 * Guards the switch in {@link MessagesUtil}: a new value type in either enum
	 * must not fall through to the untranslated enum name.
	 */
	@ParameterizedTest
	@MethodSource("everyType")
	void getValueTypeLabel_translatesEveryType(IValueType type) {
		String label = MessagesUtil.getValueTypeLabel(messages(), type);

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
