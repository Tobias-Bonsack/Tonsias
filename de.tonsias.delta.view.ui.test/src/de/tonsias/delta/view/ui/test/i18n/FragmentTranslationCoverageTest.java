package de.tonsias.delta.view.ui.test.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.tonsias.delta.view.ui.DeltaView;

/**
 * Every {@code %key} of {@code fragment.e4xmi} must be translated in every
 * locale, and neither locale may carry keys the fragment never asks for.
 */
class FragmentTranslationCoverageTest {

	private static final Pattern MODEL_KEY = Pattern.compile("\"%([A-Za-z0-9_.]+)\"");

	private static Bundle bundle() {
		Bundle bundle = FrameworkUtil.getBundle(DeltaView.class);
		assertNotNull(bundle, "not running inside OSGi, no bundle for " + DeltaView.class.getName());
		return bundle;
	}

	private static URL entry(String path) {
		URL entry = bundle().getEntry(path);
		assertNotNull(entry, "missing " + path + " in " + bundle().getSymbolicName());
		return entry;
	}

	private static Properties load(String locale) throws IOException {
		Properties properties = new Properties();
		try (InputStream stream = entry("OSGI-INF/l10n/bundle" + locale + ".properties").openStream()) {
			properties.load(stream);
		}
		return properties;
	}

	private static Set<String> keysOf(Properties properties) {
		return new TreeSet<>(properties.stringPropertyNames());
	}

	private static Set<String> fragmentKeys() throws IOException {
		String model;
		try (InputStream stream = entry("fragment.e4xmi").openStream()) {
			model = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}

		Set<String> keys = new TreeSet<>();
		Matcher matcher = MODEL_KEY.matcher(model);
		while (matcher.find()) {
			keys.add(matcher.group(1));
		}
		return keys;
	}

	@ParameterizedTest(name = "bundle{0}.properties")
	@ValueSource(strings = { "", "_de" })
	void everyFragmentKeyIsTranslated(String locale) throws IOException {
		Set<String> fragmentKeys = fragmentKeys();

		assertTrue(!fragmentKeys.isEmpty(), "no %keys found in fragment.e4xmi, the check would pass on anything");
		assertEquals(fragmentKeys, keysOf(load(locale)),
				"fragment.e4xmi keys and bundle" + locale + ".properties keys differ");
	}

	@ParameterizedTest(name = "bundle{0}.properties")
	@ValueSource(strings = { "", "_de" })
	void noEntryIsBlank(String locale) throws IOException {
		Properties translations = load(locale);

		for (String key : keysOf(translations)) {
			assertTrue(!translations.getProperty(key).isBlank(), key + " is blank in bundle" + locale + ".properties");
		}
	}

	@Test
	void bothLocalesCarryTheSameKeys() throws IOException {
		assertEquals(keysOf(load("")), keysOf(load("_de")));
	}
}
