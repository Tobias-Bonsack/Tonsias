package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.impl.value.ValueContentRules;

/**
 * The rules themselves, asked directly rather than through one of the ten value
 * classes that delegate to them. What the single values do with them is checked
 * in the {@code Single*ValueTest}s, which is also where these cases used to live
 * - one copy per class, and the copies are what this class exists to prevent.
 */
public class ValueContentRulesTest {

	// ---------- STRING ----------

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "abc", "3,14", "a b", "äöü" })
	void testAccepts_stringTakesEveryText(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.STRING, input), is(true));
	}

	@Test
	void testConvert_stringTakesTextAndNothingElse() {
		assertThat(ValueContentRules.convert(ValueContentType.STRING, "abc"), is(Optional.of("abc")));
		assertThat(ValueContentRules.convert(ValueContentType.STRING, 3), is(Optional.empty()));
	}

	// ---------- INTEGER ----------

	@ParameterizedTest
	@ValueSource(strings = { "0", "42", "-7", "0042", "2147483647", "-2147483648" })
	void testAccepts_integerTakesWhatValueOfTakes(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.INTEGER, input), is(true));
	}

	/**
	 * "99999999999" is the input that must not be offered and then silently land as
	 * 0.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>
	 */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "abc", "3.14", "99999999999", "2147483648", "1 2", " 7 " })
	void testAccepts_integerRejectsTheRest(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.INTEGER, input), is(false));
	}

	@Test
	void testConvert_integerTakesTheNumberItself() {
		assertThat(ValueContentRules.convert(ValueContentType.INTEGER, 42), is(Optional.of(42)));
		assertThat(ValueContentRules.convert(ValueContentType.INTEGER, "42"), is(Optional.of(42)));
		assertThat(ValueContentRules.convert(ValueContentType.INTEGER, 42.0f), is(Optional.empty()));
	}

	// ---------- BOOLEAN ----------

	@ParameterizedTest
	@ValueSource(strings = { "true", "false", "TRUE", "False", " true " })
	void testAccepts_booleanTakesTheTwoLiterals(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.BOOLEAN, input), is(true));
	}

	/** a typo is rejected rather than folded into false */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "yes", "no", "1", "0", "ture", "wahr" })
	void testAccepts_booleanRejectsTheRest(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.BOOLEAN, input), is(false));
	}

	@Test
	void testConvert_booleanTakesTheFlagItself() {
		assertThat(ValueContentRules.convert(ValueContentType.BOOLEAN, Boolean.TRUE), is(Optional.of(true)));
		assertThat(ValueContentRules.convert(ValueContentType.BOOLEAN, "FALSE"), is(Optional.of(false)));
		assertThat(ValueContentRules.convert(ValueContentType.BOOLEAN, "yes"), is(Optional.empty()));
	}

	// ---------- FLOAT ----------

	@ParameterizedTest
	@ValueSource(strings = { "3.14", "-0.5", "7", "0042", "  1.25  " })
	void testAccepts_floatTakesDecimalNotation(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.FLOAT, input), is(true));
	}

	/**
	 * Everything {@link Float#parseFloat} would read beyond decimal notation, plus
	 * the number that passes the notation and folds to {@code Infinity}.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>
	 */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "  ", "abc", "3,14", "NaN", "Infinity", "-Infinity", "1e5", "3f", "3d", "0x1p3", "3.",
			".5", "1_000", "10000000000000000000000000000000000000000" })
	void testAccepts_floatRejectsTheRest(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.FLOAT, input), is(false));
	}

	@Test
	void testConvert_floatRejectsWhatIsNotFiniteEvenAsAFloat() {
		assertThat(ValueContentRules.convert(ValueContentType.FLOAT, 1.5f), is(Optional.of(1.5f)));
		assertThat(ValueContentRules.convert(ValueContentType.FLOAT, Float.POSITIVE_INFINITY), is(Optional.empty()));
		assertThat(ValueContentRules.convert(ValueContentType.FLOAT, Float.NaN), is(Optional.empty()));
		// a bare Integer is neither text nor a float, and must not slip past as one
		assertThat(ValueContentRules.convert(ValueContentType.FLOAT, 7), is(Optional.empty()));
	}

	// ---------- INSTANZ ----------

	@ParameterizedTest
	@ValueSource(strings = { "0", "3", "z", "1a", "10", "zzz" })
	void testAccepts_instanzTakesTheShapeOfAKey(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.INSTANZ, input), is(true));
	}

	/** upper case is out: the files are named after keys, and Windows folds case */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "A", "1A", "-1", "1 2", "1.2", "a b", " 3" })
	void testAccepts_instanzRejectsTheRest(String input) {
		assertThat(ValueContentRules.accepts(ValueContentType.INSTANZ, input), is(false));
	}

	/**
	 * The one input the two questions answer differently. Pointing nowhere is a
	 * state a single relation has, so it has to be storable; offering it in a dialog
	 * is what must not happen.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/74">#74</a>
	 */
	@Test
	void testTheEmptyKeyIsStorableButNeverOnOffer() {
		assertThat(ValueContentRules.convert(ValueContentType.INSTANZ, ""), is(Optional.of("")));
		assertThat(ValueContentRules.accepts(ValueContentType.INSTANZ, ""), is(false));
	}

	// ---------- across the five ----------

	@ParameterizedTest
	@EnumSource(ValueContentType.class)
	void testNothingIsSomethingNoTypeReads(ValueContentType content) {
		assertThat(ValueContentRules.accepts(content, null), is(false));
		assertThat(ValueContentRules.convert(content, null), is(Optional.empty()));
	}

	/**
	 * Whatever {@code accepts} says yes to has to be storable, or a dialog would
	 * offer its OK button for something the model then refuses. The other direction
	 * is deliberately not claimed - see
	 * {@link #testTheEmptyKeyIsStorableButNeverOnOffer()}.
	 */
	@ParameterizedTest
	@EnumSource(ValueContentType.class)
	void testWhatIsAcceptedIsAlsoStored(ValueContentType content) {
		for (String candidate : new String[] { "", " ", "abc", "0", "42", "-7", "3.14", "true", "false", "1a", "3,14",
				"NaN", "99999999999" }) {
			if (ValueContentRules.accepts(content, candidate)) {
				assertThat("accepted but not stored: '" + candidate + "' as " + content,
						ValueContentRules.convert(content, candidate).isPresent(), is(true));
			}
		}
	}
}
