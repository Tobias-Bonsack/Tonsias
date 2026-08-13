package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiBooleanValue;
import de.tonsias.basis.model.impl.value.SingleBooleanValue;

public class MultiBooleanValueTest {

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new MultiBooleanValue("k").getPath(), is(MultiValueType.MULTI_BOOLEAN.getPath()));
	}

	@Test
	void testTryToAddValue_takesTheFlagAndTheLiteral() {
		MultiBooleanValue value = new MultiBooleanValue("k");

		assertThat(value.tryToAddValue(true), is(true));
		assertThat(value.tryToAddValue("false"), is(true));

		assertThat(value.getValues(), contains(true, false));
	}

	/**
	 * Holding no duplicates, this list is at most two elements long - which is the
	 * honest consequence of offering the five contents alike, not a defect.
	 */
	@Test
	void testTheListIsAtMostTwoElementsLong() {
		MultiBooleanValue value = new MultiBooleanValue("k");

		assertThat(value.tryToSetValues(List.of(true, false)), is(true));
		assertThat(value.tryToAddValue("TRUE"), is(false));
		assertThat(value.size(), is(2));
	}

	/** a typo is rejected rather than folded into false */
	@ParameterizedTest
	@ValueSource(strings = { "", " ", "yes", "1", "ture" })
	void testTryToAddValue_whatIsNoLiteralIsRejected(String input) {
		MultiBooleanValue value = new MultiBooleanValue("k");

		assertThat(value.tryToAddValue(input), is(false));
		assertThat(value.size(), is(0));
	}

	/** the same rule the single value asks, out of the same place */
	@ParameterizedTest
	@ValueSource(strings = { "true", "false", "TRUE", " true ", "", "yes", "1" })
	void testAccepts_agreesWithTheSingleValue(String input) {
		assertThat(MultiBooleanValue.accepts(input), is(SingleBooleanValue.accepts(input)));
	}
}
