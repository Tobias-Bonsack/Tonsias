package de.tonsias.basis.osgi.intf.non.service;

/**
 * {@link EventConstants#OLD_VALUE} to be a String <br>
 * {@link EventConstants#NEW_VAlUE} to be a Primitive || List of Primitives
 */
public interface PreferenceEventConstants extends EventConstants {
	// topic identifier for all topics
	String PREFERENCE = "preference/";

	// this key can only be used for event registration, you cannot
	// send out generic events
	String ALL_TOPIC = PREFERENCE + "*";

	final String SHOW_VALUE_TOPIC = PREFERENCE + "EnableValues";
	final String MODEL_VIEW_TEXT_TOPIC = PREFERENCE + "ModelViewText";

	static String getForKey(String key) {
		return PREFERENCE + key;
	}
}
