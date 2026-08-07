package de.tonsias.basis.ui.test.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.util.MessagesUtil;

/**
 * Keeps the translations and the things that reference them in sync: every
 * {@link Messages} field needs a key in every locale, every {@code %key} in the
 * e4 model needs one too, and neither file may carry keys nobody asks for.
 */
class TranslationCoverageTest {

	/** {@code %key} as it is written in the e4 model files. */
	private static final Pattern MODEL_KEY = Pattern.compile("\"%([A-Za-z0-9_.]+)\"");

	private static final String L10N_ROOT = "OSGI-INF/l10n/";

	private static Properties load(Class<?> classOfBundle, String locale) throws IOException {
		Bundle bundle = FrameworkUtil.getBundle(classOfBundle);
		assertNotNull(bundle, "not running inside OSGi, no bundle for " + classOfBundle.getName());

		URL entry = bundle.getEntry(L10N_ROOT + "bundle" + locale + ".properties");
		assertNotNull(entry, "missing " + L10N_ROOT + "bundle" + locale + ".properties in " + bundle.getSymbolicName());

		Properties properties = new Properties();
		try (InputStream stream = entry.openStream()) {
			properties.load(stream);
		}
		return properties;
	}

	private static Set<String> keysOf(Properties properties) {
		return new TreeSet<>(properties.stringPropertyNames());
	}

	private static Set<String> messagesFields() {
		return Arrays.stream(Messages.class.getFields())//
				.filter(field -> !Modifier.isStatic(field.getModifiers()))//
				.map(Field::getName)//
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private static Set<String> modelKeysOf(Class<?> classOfBundle, String modelFile) throws IOException {
		Bundle bundle = FrameworkUtil.getBundle(classOfBundle);
		URL entry = bundle.getEntry(modelFile);
		assertNotNull(entry, "missing " + modelFile + " in " + bundle.getSymbolicName());

		String model;
		try (InputStream stream = entry.openStream()) {
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
	void everyMessagesFieldHasExactlyOneEntry(String locale) throws IOException {
		Properties translations = load(Messages.class, locale);
		Set<String> fields = messagesFields();

		assertTrue(!fields.isEmpty(), "no fields found on Messages, the check would pass on anything");
		assertEquals(fields, keysOf(translations),
				"Messages fields and bundle" + locale + ".properties keys differ");
	}

	@ParameterizedTest(name = "bundle{0}.properties")
	@ValueSource(strings = { "", "_de" })
	void noMessagesEntryIsBlank(String locale) throws IOException {
		Properties translations = load(Messages.class, locale);

		for (String key : keysOf(translations)) {
			assertTrue(!translations.getProperty(key).isBlank(), key + " is blank in bundle" + locale + ".properties");
		}
	}

	@ParameterizedTest(name = "bundle{0}.properties")
	@ValueSource(strings = { "", "_de" })
	void everyModelKeyOfTheApplicationIsTranslated(String locale) throws IOException {
		Properties translations = load(MessagesUtil.class, locale);
		Set<String> modelKeys = modelKeysOf(MessagesUtil.class, "Application.e4xmi");

		assertTrue(!modelKeys.isEmpty(), "no %keys found in Application.e4xmi, the check would pass on anything");
		assertEquals(modelKeys, keysOf(translations),
				"Application.e4xmi keys and bundle" + locale + ".properties keys differ");
	}

	@Test
	void bothLocalesOfTheApplicationCarryTheSameKeys() throws IOException {
		assertEquals(keysOf(load(MessagesUtil.class, "")), keysOf(load(MessagesUtil.class, "_de")));
	}
}
