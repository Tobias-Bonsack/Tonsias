package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;

public class MultiIntegerValueTest {

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new MultiIntegerValue("k").getPath(), is(MultiValueType.MULTI_INTEGER.getPath()));
	}

	@Test
	void testTryToAddValue_takesTheNumberAndTheText() {
		MultiIntegerValue value = new MultiIntegerValue("k");

		assertThat(value.tryToAddValue(42), is(true));
		assertThat(value.tryToAddValue("-7"), is(true));

		assertThat(value.getValues(), contains(42, -7));
	}

	/**
	 * "42" and 42 are the same element, so the second one is a duplicate - the
	 * widgets hand their input on as text and the model already holds numbers.
	 */
	@Test
	void testTryToAddValue_textAndNumberAreTheSameElement() {
		MultiIntegerValue value = new MultiIntegerValue("k");
		value.tryToAddValue(42);

		assertThat(value.tryToAddValue("42"), is(false));
		assertThat(value.getValues(), contains(42));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "abc", "3.14", "99999999999" })
	void testTryToAddValue_whatIsNoNumberIsRejected(String input) {
		MultiIntegerValue value = new MultiIntegerValue("k");

		assertThat(value.tryToAddValue(input), is(false));
		assertThat(value.size(), is(0));
	}

	@Test
	void testTryToSetValues_convertsEveryElement() {
		MultiIntegerValue value = new MultiIntegerValue("k");

		assertThat(value.tryToSetValues(List.of("1", 2, "3")), is(true));
		assertThat(value.getValues(), contains(1, 2, 3));
	}

	/** the same rule the single value asks, out of the same place */
	@ParameterizedTest
	@ValueSource(strings = { "0", "42", "-7", "", "abc", "99999999999" })
	void testAccepts_agreesWithTheSingleValue(String input) {
		assertThat(MultiIntegerValue.accepts(input), is(SingleIntegerValue.accepts(input)));
	}
}
