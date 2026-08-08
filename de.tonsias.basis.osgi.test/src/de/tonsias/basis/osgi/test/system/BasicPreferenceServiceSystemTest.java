package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.intf.IBasicPreferenceService.REGEX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService.Key;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.PreferenceEventConstants;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The registered {@code BasicPreferenceServiceImpl} against the real Eclipse
 * instance preferences it stores into.
 * <p>
 * Nothing here writes to a stand-in store: every value goes through
 * {@code InstanceScope}, is flushed, and is read back the way the preferences
 * dialog and the create dialog read it. Saving also announces itself on the
 * bus, which is how the views notice a changed setting without polling, so the
 * event is part of what a save has to do.
 * </p>
 */
public class BasicPreferenceServiceSystemTest {

	/** a key of its own, so the product's settings survive the run untouched */
	private static final String TEST_KEY = "BasicPreferenceServiceSystemTest";

	IBasicPreferenceService _prefs;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_prefs = ProductRuntime.preferenceService();
		_recorder = EventRecorder.subscribeTo(ProductRuntime.broker(),
				PreferenceEventConstants.getForKey(Key.MODEL_VIEW_TEXT.getKey()));
	}

	@AfterEach
	void afterEach() throws BackingStoreException {
		_recorder.unsubscribe();
		_prefs.getNode().remove(TEST_KEY);
		_prefs.getNode().flush();
	}

	// ---------- the keys the product ships ----------

	/**
	 * A node that has never been written still has to answer, or the create dialog
	 * comes up without its name row on a fresh workspace.
	 */
	@Test
	void testGetNode_seedsEveryShippedKeyWithItsInitValue() {
		for (Key key : Key.values()) {
			assertThat("no value for " + key.getKey(), _prefs.getNode().get(key.getKey(), null), is(notNullValue()));
		}
	}

	@Test
	void testGetKeys_namesTheThreeShippedPreferences() {
		assertThat(Arrays.stream(_prefs.getKeys()).map(key -> key.getKey()).toList(),
				is(Arrays.stream(Key.values()).map(Key::getKey).toList()));
	}

	// ---------- single values ----------

	@Test
	void testSaveAsToString_isReadBackAsTheTypeItWasAskedFor() throws BackingStoreException {
		_prefs.saveAsToString(TEST_KEY, 42);

		assertThat(_prefs.getValue(TEST_KEY, String.class), is(Optional.of("42")));
		assertThat(_prefs.getValue(TEST_KEY, Integer.class), is(Optional.of(42)));
	}

	@Test
	void testSaveAsToString_survivesInTheInstancePreferences() throws BackingStoreException {
		_prefs.saveAsToString(TEST_KEY, "stored");

		assertThat(_prefs.getNode().get(TEST_KEY, null), is("stored"));
	}

	@Test
	void testGetValue_aKeyThatWasNeverWrittenIsEmpty() {
		assertThat(_prefs.getValue("no-such-preference", String.class), is(Optional.empty()));
	}

	@Test
	void testGetValue_booleanAndIntegerAreParsed() throws BackingStoreException {
		_prefs.saveAsToString(TEST_KEY, true);
		assertThat(_prefs.getValue(TEST_KEY, Boolean.class), is(Optional.of(true)));

		_prefs.saveAsToString(TEST_KEY, 7);
		assertThat(_prefs.getValue(TEST_KEY, Integer.class), is(Optional.of(7)));
	}

	@Test
	void testGetValue_anUnsupportedTypeIsRejected() throws BackingStoreException {
		_prefs.saveAsToString(TEST_KEY, "stored");

		assertThrows(UnsupportedOperationException.class, () -> _prefs.getValue(TEST_KEY, Double.class));
	}

	// ---------- lists ----------

	@Test
	void testSaveAsList_roundTripsThroughTheSeparator() throws BackingStoreException {
		_prefs.saveAsList(TEST_KEY, List.of("a", "b", "c"));

		assertThat(_prefs.getAsList(TEST_KEY, String.class), contains("a", "b", "c"));
		assertThat(_prefs.getNode().get(TEST_KEY, ""), is("a" + REGEX + "b" + REGEX + "c"));
	}

	@Test
	void testGetAsList_parsesEveryEntryAsTheTypeItWasAskedFor() throws BackingStoreException {
		_prefs.saveAsList(TEST_KEY, List.of(1, 2, 3));
		assertThat(_prefs.getAsList(TEST_KEY, Integer.class), contains(1, 2, 3));

		_prefs.saveAsList(TEST_KEY, List.of(true, false));
		assertThat(_prefs.getAsList(TEST_KEY, Boolean.class), contains(true, false));
	}

	/** A single entry is a list of one, not a list of none. */
	@Test
	void testGetAsList_aValueWithoutASeparatorIsOneEntry() throws BackingStoreException {
		_prefs.saveAsList(TEST_KEY, List.of("only"));

		assertThat(_prefs.getAsList(TEST_KEY, String.class), contains("only"));
	}

	// ---------- announcing the change ----------

	/**
	 * The views listen for this instead of re-reading the node, so a save that
	 * stays quiet leaves them showing the old setting.
	 */
	@Test
	void testSaveAsToString_announcesTheChangeOnTheBus() throws BackingStoreException {
		String original = _prefs.getValue(Key.MODEL_VIEW_TEXT.getKey(), String.class).orElse("Name");
		try {
			_recorder.clear();

			_prefs.saveAsToString(Key.MODEL_VIEW_TEXT.getKey(), original + " changed");
			_recorder.awaitCount(1);

			assertThat(_recorder.events(), hasSize(1));
			Map<?, ?> data = _recorder.onlyDataOf(PreferenceEventConstants.MODEL_VIEW_TEXT_TOPIC, Map.class);
			assertThat(data.get(EventConstants.EVENT_TYPE), is(EventConstants.EventType.CHANGE));
			assertThat(data.get(EventConstants.OLD_VALUE), is(original));
			assertThat(data.get(EventConstants.NEW_VAlUE), is(original + " changed"));
		} finally {
			_prefs.saveAsToString(Key.MODEL_VIEW_TEXT.getKey(), original);
		}
	}

	/** Storing what is already stored is not a change and must stay quiet. */
	@Test
	void testSaveAsToString_writingTheSameValueAgainAnnouncesNothing() throws BackingStoreException {
		String stored = _prefs.getValue(Key.MODEL_VIEW_TEXT.getKey(), String.class).orElse("Name");
		_recorder.clear();

		_prefs.saveAsToString(Key.MODEL_VIEW_TEXT.getKey(), stored);

		assertThat(_recorder.events(), hasSize(0));
	}

	@Test
	void testSaveAsList_writingTheSameListAgainAnnouncesNothing() throws BackingStoreException {
		_prefs.saveAsList(TEST_KEY, List.of("a", "b"));
		EventRecorder recorder = EventRecorder.subscribeTo(ProductRuntime.broker(),
				PreferenceEventConstants.getForKey(TEST_KEY));
		try {
			_prefs.saveAsList(TEST_KEY, List.of("a", "b"));

			assertThat(recorder.events(), hasSize(0));
			assertThat(_prefs.getAsList(TEST_KEY, String.class), hasItem("a"));
		} finally {
			recorder.unsubscribe();
		}
	}

}
