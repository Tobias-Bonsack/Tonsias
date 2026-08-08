package de.tonsias.basis.osgi.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.impl.KeyServiceImpl;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The key service the whole model hangs off: every instanz and every single
 * value gets its key from here, and the keys become file names as they are.
 * <p>
 * The registered service is a singleton whose counter the other tests in this
 * bundle keep advancing, so the tests that need a <em>particular</em> starting
 * key build a second {@link KeyServiceImpl} on a preference node of their own.
 * Nothing is substituted there either - it is the real class writing real
 * Eclipse instance preferences, only into a node no other test shares.
 * </p>
 */
public class KeyServiceSystemTest {

	private static final String CURRENT_KEY = IKeyService.Key.CURRENT_KEY.getKey();

	IKeyService _registered;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_registered = ProductRuntime.keyService();
	}

	// ---------- the registered service ----------

	@Test
	void testInitKey_isTheKeyTheServiceIsStandingOn() {
		String key = _registered.initKey();

		assertThat(key, is(notNullValue()));
		assertThat(_registered.initKey(), is(key));
	}

	/**
	 * The create dialog shows the key an instanz is about to get, so asking must
	 * not consume it.
	 */
	@Test
	void testPreviewNextKey_announcesWithoutAdvancing() {
		String before = _registered.initKey();

		String preview = _registered.previewNextKey();

		assertThat(_registered.previewNextKey(), is(preview));
		assertThat(_registered.initKey(), is(before));
	}

	@Test
	void testGenerateKey_handsOutExactlyWhatWasAnnouncedAndThenMovesOn() {
		String announced = _registered.previewNextKey();

		String generated = _registered.generateKey();

		assertThat(generated, is(announced));
		assertThat(_registered.initKey(), is(generated));
		assertThat(_registered.previewNextKey(), is(not(generated)));
	}

	/**
	 * Two objects sharing a key would share a file, so the sequence may never
	 * repeat itself.
	 */
	@Test
	void testGenerateKey_neverRepeatsItself() {
		Set<String> keys = new LinkedHashSet<>();

		for (int i = 0; i < 50; i++) {
			keys.add(_registered.generateKey());
		}

		assertThat(keys, hasSize(50));
	}

	/**
	 * Keys become file names as they are, so an alphabet holding both cases would
	 * make 'a' and 'A' the same file on Windows and macOS.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/35">#35</a>
	 */
	@Test
	void testGenerateKey_staysLowerCase() {
		for (int i = 0; i < 40; i++) {
			String key = _registered.generateKey();
			assertThat(key, is(key.toLowerCase(Locale.ROOT)));
		}
	}

	/** The counter has to survive a restart, so it lives in the preferences. */
	@Test
	void testGenerateKey_isWrittenThroughToTheInstancePreferences() {
		String generated = _registered.generateKey();

		assertThat(_registered.getNode().get(CURRENT_KEY, ""), is(generated));
	}

	@Test
	void testGetKeys_namesTheOnePreferenceTheServiceOwns() {
		assertThat(Arrays.stream(_registered.getKeys()).map(key -> key.getKey()).toList(), is(List.of(CURRENT_KEY)));
	}

	// ---------- counting up, from a known starting key ----------

	@ParameterizedTest
	@CsvSource({ //
			"a, b", //
			"9, a", // the digits run into the letters without a gap
			"z, 00", //
			"a1, b1", //
			"z0, 01", //
			"zz, 000", //
			"2z0, 3z0", //
			"zzt, 00u", //
			"zzzzz, 000000" //
	})
	void testGenerateKey_countsTheLeastSignificantCharacterUpFirst(String stored, String expected)
			throws BackingStoreException {
		IKeyService service = serviceStartingAt(stored);

		assertThat(service.previewNextKey(), is(expected));
		assertThat(service.generateKey(), is(expected));
	}

	@Test
	void testInitKey_anEmptyNodeStartsTheSequenceAtZero() throws BackingStoreException {
		IKeyService service = serviceStartingAt("");

		assertThat(service.initKey(), is("0"));
	}

	/**
	 * A workspace written before issue #35 can hold a key of the old base 62
	 * alphabet, which {@code countKeyUp} can not count up any more. It is folded
	 * down to lower case and written back, so the migration happens once.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/35">#35</a>
	 */
	@Test
	void testInitKey_anOldUpperCaseKeyIsMigratedAndPersisted() throws BackingStoreException {
		KeyServiceImpl service = serviceStartingAt("1A");

		assertThat(service.initKey(), is("1a"));

		assertThat(service.getNode().get(CURRENT_KEY, ""), is("1a"));
		assertThat(service.generateKey(), is("2a"));
	}

	// ---------- the alphabet ----------

	@Test
	void testKeychars_areBase36AndLowerCaseOnly() {
		assertThat(String.valueOf(KeyServiceImpl.KEYCHARS), is("0123456789abcdefghijklmnopqrstuvwxyz"));
	}

	/** {@link Arrays#binarySearch} only works on a sorted alphabet. */
	@Test
	void testKeychars_areSortedAscending() {
		char[] sorted = KeyServiceImpl.KEYCHARS.clone();
		Arrays.sort(sorted);

		assertThat(String.valueOf(KeyServiceImpl.KEYCHARS), is(String.valueOf(sorted)));
	}

	/**
	 * The real service on a real instance preference node of its own, seeded the
	 * way a workspace seeds it - by holding a value for {@code CurrentKey}.
	 */
	private static KeyServiceImpl serviceStartingAt(String storedKey) throws BackingStoreException {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode("KeyServiceSystemTest");
		node.put(CURRENT_KEY, storedKey);
		node.flush();

		return new KeyServiceImpl() {
			@Override
			public IEclipsePreferences getNode() {
				return node;
			}
		};
	}
}
