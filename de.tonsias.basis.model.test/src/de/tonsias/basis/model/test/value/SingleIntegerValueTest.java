package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
	@CsvSource({ "7, 7", "-3, -3", "0042, 42" })
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

	@Test
	void testToString_containsKeyValueAndSimpleClassName() {
		SingleIntegerValue value = new SingleIntegerValue("k");
		value.tryToSetValue(5);

		assertThat(value.toString(), is("k 5 : SingleIntegerValue"));
	}
}
