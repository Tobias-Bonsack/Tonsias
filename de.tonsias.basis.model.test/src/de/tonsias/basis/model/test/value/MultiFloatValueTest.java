package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.SingleFloatValue;

public class MultiFloatValueTest {

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new MultiFloatValue("k").getPath(), is(MultiValueType.MULTI_FLOAT.getPath()));
	}

	@Test
	void testTryToAddValue_takesTheNumberAndTheText() {
		MultiFloatValue value = new MultiFloatValue("k");

		assertThat(value.tryToAddValue(3.14f), is(true));
		assertThat(value.tryToAddValue("-0.5"), is(true));

		assertThat(value.getValues(), contains(3.14f, -0.5f));
	}

	/**
	 * The list holds {@code Float}s and nothing else - a bare {@link Integer} is
	 * neither text nor a float and must not slip past as one, or the list would
	 * hold two things that print alike and compare unequal.
	 */
	@Test
	void testTryToAddValue_anIntegerIsNoFloat() {
		MultiFloatValue value = new MultiFloatValue("k");

		assertThat(value.tryToAddValue(Integer.valueOf(7)), is(false));
		assertThat(value.tryToAddValue("7"), is(true));
		assertThat(value.getValues(), contains(7.0f));
	}

	/**
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>
	 */
	@ParameterizedTest
	@ValueSource(strings = { "", "abc", "3,14", "NaN", "Infinity", "1e5", "3f",
			"10000000000000000000000000000000000000000" })
	void testTryToAddValue_whatIsNoDecimalNumberIsRejected(String input) {
		MultiFloatValue value = new MultiFloatValue("k");

		assertThat(value.tryToAddValue(input), is(false));
		assertThat(value.size(), is(0));
	}

	@Test
	void testTryToAddValue_nonFiniteFloatIsRejected() {
		MultiFloatValue value = new MultiFloatValue("k");

		assertThat(value.tryToAddValue(Float.POSITIVE_INFINITY), is(false));
		assertThat(value.tryToAddValue(Float.NaN), is(false));
		assertThat(value.size(), is(0));
	}

	@Test
	void testTryToSetValues_oneBadElementLeavesTheListAsItWas() {
		MultiFloatValue value = new MultiFloatValue("k");
		value.tryToSetValues(List.of("1.5"));

		assertThat(value.tryToSetValues(List.of("2.5", "NaN")), is(false));
		assertThat(value.getValues(), contains(1.5f));
	}

	/** the same rule the single value asks, out of the same place */
	@ParameterizedTest
	@ValueSource(strings = { "3.14", "-0.5", "7", "  1.25  ", "", "3,14", "NaN", "1e5" })
	void testAccepts_agreesWithTheSingleValue(String input) {
		assertThat(MultiFloatValue.accepts(input), is(SingleFloatValue.accepts(input)));
	}
}
