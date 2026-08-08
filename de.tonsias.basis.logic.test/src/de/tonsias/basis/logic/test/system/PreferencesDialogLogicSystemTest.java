package de.tonsias.basis.logic.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.logic.dialog.PreferencesDialogLogic;
import de.tonsias.basis.logic.dialog.PreferencesDialogLogic.PreferenceFeature;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService.Key;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The logic behind the preferences dialog, which resolves its own services from
 * the OSGi registry in its constructor.
 * <p>
 * That is the whole point of testing it in a running runtime: outside one both
 * lookups return null, the constructor gives up before it builds its map and
 * every method after that would fail. Here it finds the same two preference
 * services the product finds, and every value read or written goes through the
 * real Eclipse instance preferences.
 * </p>
 */
public class PreferencesDialogLogicSystemTest {

	/** keys of its own, so the product's settings survive the run untouched */
	private static final String TEST_KEY = "PreferencesDialogLogicSystemTest";

	private static final String OTHER_TEST_KEY = TEST_KEY + "Other";

	IKeyService _keyService;

	IBasicPreferenceService _basicService;

	PreferencesDialogLogic _logic;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_keyService = ProductRuntime.keyService();
		_basicService = ProductRuntime.preferenceService();

		_logic = new PreferencesDialogLogic();
	}

	@AfterEach
	void afterEach() throws BackingStoreException {
		_basicService.getNode().remove(TEST_KEY);
		_basicService.getNode().remove(OTHER_TEST_KEY);
		_basicService.getNode().flush();
	}

	private String basicNode() {
		return _basicService.getNode().toString();
	}

	private String keyNode() {
		return _keyService.getNode().toString();
	}

	/**
	 * The dialog lists one page per preference node, named after the node itself -
	 * those names are identifiers, not labels, and are shown untranslated.
	 */
	@Test
	void testGetPreferenceNames_areTheTwoRegisteredPreferenceNodes() {
		assertThat(_logic.getPreferenceNames(), containsInAnyOrder(basicNode(), keyNode()));
	}

	@Test
	void testGetPreferences_theBasicNodeYieldsOneFeaturePerShippedKey() {
		Collection<PreferenceFeature> features = _logic.getPreferences(basicNode());

		assertThat(features, hasSize(Key.values().length));
		assertThat(features.stream().map(PreferenceFeature::name).toList(),
				is(Arrays.stream(Key.values()).map(Key::getKey).toList()));
	}

	/**
	 * A stored value wins over the key's init value - the dialog has to show what
	 * the workspace holds, not what the enum defaults to.
	 */
	@Test
	void testGetPreferences_showTheStoredValueRatherThanTheInitValue() throws BackingStoreException {
		String stored = _basicService.getValue(Key.MODEL_VIEW_TEXT.getKey(), String.class).orElseThrow();
		try {
			_basicService.saveAsToString(Key.MODEL_VIEW_TEXT.getKey(), "Bezeichnung");

			assertThat(featureNamed(Key.MODEL_VIEW_TEXT.getKey()).value(), is("Bezeichnung"));
		} finally {
			_basicService.saveAsToString(Key.MODEL_VIEW_TEXT.getKey(), stored);
		}
	}

	/**
	 * Whether a feature may be edited comes from the key, not from the dialog - the
	 * save path is derived, not chosen.
	 */
	@Test
	void testGetPreferences_carryTheEditableFlagOfTheirKey() {
		assertThat(featureNamed(Key.MODEL_VIEW_TEXT.getKey()).editable(), is(true));
		assertThat(featureNamed(Key.SAVE_PATH.getKey()).editable(), is(false));
	}

	/** The key service owns exactly one preference, and it is not editable. */
	@Test
	void testGetPreferences_theKeyNodeYieldsTheCurrentKey() {
		Collection<PreferenceFeature> features = _logic.getPreferences(keyNode());

		assertThat(features.stream().map(PreferenceFeature::name).toList(),
				contains(IKeyService.Key.CURRENT_KEY.getKey()));
		PreferenceFeature currentKey = features.iterator().next();
		assertThat(currentKey.value(), is(_keyService.initKey()));
		assertThat(currentKey.editable(), is(false));
	}

	@Test
	void testSavePreference_writesEveryPairThroughToTheBasicNode() {
		_logic.savePreference(Map.of(TEST_KEY, "one", OTHER_TEST_KEY, "two"));

		assertThat(_basicService.getNode().get(TEST_KEY, null), is("one"));
		assertThat(_basicService.getNode().get(OTHER_TEST_KEY, null), is("two"));
	}

	@Test
	void testSavePreference_isReadBackByTheSameLogic() {
		_logic.savePreference(Map.of(Key.MODEL_VIEW_TEXT.getKey(), "Bezeichnung"));
		try {
			assertThat(featureNamed(Key.MODEL_VIEW_TEXT.getKey()).value(), is("Bezeichnung"));
		} finally {
			_logic.savePreference(Map.of(Key.MODEL_VIEW_TEXT.getKey(), Key.MODEL_VIEW_TEXT.getInitValue()));
		}
	}

	@Test
	void testSavePreference_anEmptyMapChangesNothing() {
		List<String> before = valuesOfBasicNode();

		_logic.savePreference(Map.of());

		assertThat(valuesOfBasicNode(), is(before));
	}

	private PreferenceFeature featureNamed(String name) {
		return _logic.getPreferences(basicNode()).stream()//
				.filter(feature -> name.equals(feature.name()))//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no feature named " + name));
	}

	private List<String> valuesOfBasicNode() {
		return _logic.getPreferences(basicNode()).stream().map(PreferenceFeature::value).toList();
	}
}
