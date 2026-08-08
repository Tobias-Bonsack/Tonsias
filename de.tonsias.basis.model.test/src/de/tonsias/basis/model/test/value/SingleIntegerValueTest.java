package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;

public class SingleIntegerValueTest {

	@Test
	void testNewValue_startsAtZero() {
		assertThat(new SingleIntegerValue("k").getValue(), is(0));
	}

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new SingleIntegerValue("k").getPath(), is(SingleValueType.SINGLE_INTEGER.getPath()));
	}

	@Test
	void testTryToSetValue_integer() {
		SingleIntegerValue value = new SingleIntegerValue("k");

		assertThat(value.tryToSetValue(42), is(true));
		assertThat(value.getValue(), is(42));
	}

	@Test
	void testTryToSetValue_sameIntegerIsNoChange() {
		SingleIntegerValue value = new SingleIntegerValue("k");
		value.tryToSetValue(42);

		assertThat(value.tryToSetValue(42), is(false));
	}

	@ParameterizedTest
	@CsvSource({ "7, 7", "-3, -3", "+5, 5", "0042, 42" })
	void testTryToSetValue_parsableString(String input, int expected) {
		SingleIntegerValue value = new SingleIntegerValue("k");

		assertThat(value.tryToSetValue(input), is(true));
		assertThat(value.getValue(), is(expected));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "  ", "abc", "1.5", "9999999999" })
	void testTryToSetValue_unparsableInputIsRejected(Object input) {
		SingleIntegerValue value = new SingleIntegerValue("k");
		value.tryToSetValue(11);

		assertThat(value.tryToSetValue(input), is(false));
		assertThat(value.getValue(), is(11));
	}

	/**
	 * {@code accepts} is what the dialog asks before it offers its OK button, so it
	 * has to answer for the same input the same way {@code tryToSetValue} does - a
	 * leading plus and a number past {@code Integer.MAX_VALUE} are where the dialog
	 * used to have a rule of its own.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>
	 */
	@ParameterizedTest
	@ValueSource(strings = { "7", "-3", "+5", "0042", "2147483647", "-2147483648" })
	void testAccepts_agreesWithTryToSetValue_onWhatItTakes(String input) {
		assertThat(SingleIntegerValue.accepts(input), is(true));
		// seeded away from every input, because setValue answers false for "already
		// that value" as well
		assertThat(new SingleIntegerValue("k", Integer.MIN_VALUE + 1, Set.of()).tryToSetValue(input), is(true));
	}

	/** @see #testAccepts_agreesWithTryToSetValue_onWhatItTakes(String) */
	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "abc", "1.5", "2147483648", "-2147483649", "99999999999", "1,000", "4 2" })
	void testAccepts_agreesWithTryToSetValue_onWhatItRejects(String input) {
		assertThat(SingleIntegerValue.accepts(input), is(false));
		assertThat(new SingleIntegerValue("k").tryToSetValue(input), is(false));
	}

	@Test
	void testAccepts_null() {
		assertThat(SingleIntegerValue.accepts(null), is(false));
	}

	@Test
	void testToString_containsKeyValueAndSimpleClassName() {
		SingleIntegerValue value = new SingleIntegerValue("k");
		value.tryToSetValue(5);

		assertThat(value.toString(), is("k 5 : SingleIntegerValue"));
	}
}
