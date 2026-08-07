package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleBooleanValue;

public class SingleBooleanValueTest {

	@Test
	void testNewValue_startsAtFalse() {
		assertThat(new SingleBooleanValue("k").getValue(), is(false));
	}

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new SingleBooleanValue("k").getPath(), is(SingleValueType.SINGLE_BOOLEAN.getPath()));
	}

	@Test
	void testTryToSetValue_boolean() {
		SingleBooleanValue value = new SingleBooleanValue("k");

		assertThat(value.tryToSetValue(true), is(true));
		assertThat(value.getValue(), is(true));
	}

	@Test
	void testTryToSetValue_sameBooleanIsNoChange() {
		SingleBooleanValue value = new SingleBooleanValue("k");
		value.tryToSetValue(true);

		assertThat(value.tryToSetValue(true), is(false));
	}

	@ParameterizedTest
	@CsvSource({ "true, true", "false, false", "TRUE, true", "False, false", "'  true  ', true" })
	void testTryToSetValue_parsableString(String input, boolean expected) {
		SingleBooleanValue value = new SingleBooleanValue("k");
		// false is the default, so the false cases need a different starting point to
		// tell "was set" from "was already there"
		value.tryToSetValue(!expected);

		assertThat(value.tryToSetValue(input), is(true));
		assertThat(value.getValue(), is(expected));
	}

	/**
	 * Anything that is not one of the two literals is rejected rather than folded
	 * into {@code false} - a typo must not silently clear the value.
	 */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "  ", "ja", "yes", "1", "0" })
	void testTryToSetValue_unparsableInputIsRejected(Object input) {
		SingleBooleanValue value = new SingleBooleanValue("k");
		value.tryToSetValue(true);

		assertThat(value.tryToSetValue(input), is(false));
		assertThat(value.getValue(), is(true));
	}

	@Test
	void testTryToSetValue_numberIsRejected() {
		SingleBooleanValue value = new SingleBooleanValue("k");

		assertThat(value.tryToSetValue(1), is(false));
		assertThat(value.getValue(), is(false));
	}

	@Test
	void testToString_containsKeyValueAndSimpleClassName() {
		SingleBooleanValue value = new SingleBooleanValue("k");
		value.tryToSetValue(true);

		assertThat(value.toString(), is("k true : SingleBooleanValue"));
	}
}
