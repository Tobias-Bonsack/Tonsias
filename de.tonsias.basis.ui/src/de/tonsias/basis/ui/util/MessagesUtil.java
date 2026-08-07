package de.tonsias.basis.ui.util;

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

	public static String getSingleValueTypeLabel(Messages messages, SingleValueType type) {
		switch (type) {
		case SINGLE_STRING:
			return messages.constant_type_string;
		case SINGLE_INTEGER:
			return messages.constant_type_integer;
		default:
			return type.name();
		}
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
