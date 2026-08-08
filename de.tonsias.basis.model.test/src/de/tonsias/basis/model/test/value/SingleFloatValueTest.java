package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleFloatValue;

public class SingleFloatValueTest {

	@Test
	void testNewValue_startsAtZero() {
		assertThat(new SingleFloatValue("k").getValue(), is(0.0f));
	}

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new SingleFloatValue("k").getPath(), is(SingleValueType.SINGLE_FLOAT.getPath()));
	}

	@Test
	void testTryToSetValue_float() {
		SingleFloatValue value = new SingleFloatValue("k");

		assertThat(value.tryToSetValue(3.14f), is(true));
		assertThat(value.getValue(), is(3.14f));
	}

	@Test
	void testTryToSetValue_sameFloatIsNoChange() {
		SingleFloatValue value = new SingleFloatValue("k");
		value.tryToSetValue(3.14f);

		assertThat(value.tryToSetValue(3.14f), is(false));
	}

	@ParameterizedTest
	@CsvSource({ "3.14, 3.14", "-0.5, -0.5", "7, 7.0", "0042, 42.0", "  1.25  , 1.25" })
	void testTryToSetValue_parsableString(String input, float expected) {
		SingleFloatValue value = new SingleFloatValue("k");

		assertThat(value.tryToSetValue(input), is(true));
		assertThat(value.getValue(), is(expected));
	}

	/**
	 * "3,14" is the German notation and the rest is what {@link Float#parseFloat}
	 * would take beyond decimal notation - all of it is rejected instead of turning
	 * into a number nobody typed.
	 */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "  ", "abc", "3,14", "NaN", "Infinity", "-Infinity", "1e5", "3f", "3d", "0x1p3", "3.",
			".5", "1_000" })
	void testTryToSetValue_unparsableInputIsRejected(Object input) {
		SingleFloatValue value = new SingleFloatValue("k");
		value.tryToSetValue(1.5f);

		assertThat(value.tryToSetValue(input), is(false));
		assertThat(value.getValue(), is(1.5f));
	}

	@Test
	void testTryToSetValue_integerIsRejected() {
		// tryToSetValue takes an Object and the widgets hand their input on as text -
		// a bare Integer is neither, and must not slip past as a Float
		SingleFloatValue value = new SingleFloatValue("k");

		assertThat(value.tryToSetValue(Integer.valueOf(7)), is(false));
		assertThat(value.getValue(), is(0.0f));
	}

	@Test
	void testToString_containsKeyValueAndSimpleClassName() {
		SingleFloatValue value = new SingleFloatValue("k");
		value.tryToSetValue(5.5f);

		assertThat(value.toString(), is("k 5.5 : SingleFloatValue"));
	}
}
