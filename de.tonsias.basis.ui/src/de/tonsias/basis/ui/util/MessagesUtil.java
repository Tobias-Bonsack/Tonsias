package de.tonsias.basis.ui.util;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * Maps model and preference constants onto their translated label. Both are
 * technical identifiers that must not be translated themselves, so the mapping
 * lives here instead of on the constants.
 */
public class MessagesUtil {

	private MessagesUtil() {
	}

	/**
	 * The label of a value type, from either enum. The fallback is the constant's
	 * own name, which is what {@code MessagesUtilTest} fails on for a type nobody
	 * translated.
	 */
	public static String getValueTypeLabel(Messages messages, IValueType type) {
		if (type instanceof SingleValueType single) {
			switch (single) {
			case SINGLE_STRING:
				return messages.constant_type_string;
			case SINGLE_INTEGER:
				return messages.constant_type_integer;
			case SINGLE_BOOLEAN:
				return messages.constant_type_boolean;
			case SINGLE_FLOAT:
				return messages.constant_type_float;
			case SINGLE_INSTANZ:
				return messages.constant_type_instanz;
			default:
				return single.name();
			}
		}
		if (type instanceof MultiValueType multi) {
			switch (multi) {
			case MULTI_STRING:
				return messages.constant_type_multi_string;
			case MULTI_INTEGER:
				return messages.constant_type_multi_integer;
			case MULTI_BOOLEAN:
				return messages.constant_type_multi_boolean;
			case MULTI_FLOAT:
				return messages.constant_type_multi_float;
			case MULTI_INSTANZ:
				return messages.constant_type_multi_instanz;
			default:
				return multi.name();
			}
		}
		return type.name();
	}

	/**
	 * @param key the stored preference key
	 * @return the translated label, or the key itself if it is unknown
	 */
	public static String getPreferenceLabel(Messages messages, String key) {
		if (IKeyService.Key.CURRENT_KEY.getKey().equals(key)) {
			return messages.pref_currentKey;
		}
		if (IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey().equals(key)) {
			return messages.pref_modelViewText;
		}
		if (IBasicPreferenceService.Key.SHOW_VALUES.getKey().equals(key)) {
			return messages.pref_enableValues;
		}
		if (IBasicPreferenceService.Key.SAVE_PATH.getKey().equals(key)) {
			return messages.pref_modelRootPath;
		}
		return key;
	}
}
