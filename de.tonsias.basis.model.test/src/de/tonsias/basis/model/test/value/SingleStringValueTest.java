package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;

public class SingleStringValueTest {

	@Test
	void testNewValue_startsEmpty() {
		assertThat(new SingleStringValue("k").getValue(), is(""));
	}

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new SingleStringValue("k").getPath(), is(SingleValueType.SINGLE_STRING.getPath()));
	}

	@Test
	void testTryToSetValue_string() {
		SingleStringValue value = new SingleStringValue("k");

		assertThat(value.tryToSetValue("text"), is(true));
		assertThat(value.getValue(), is("text"));
	}

	@Test
	void testTryToSetValue_sameStringIsNoChange() {
		SingleStringValue value = new SingleStringValue("k");
		value.tryToSetValue("text");

		assertThat(value.tryToSetValue("text"), is(false));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = { 42 })
	void testTryToSetValue_nonStringIsRejected(Object input) {
		SingleStringValue value = new SingleStringValue("k");
		value.tryToSetValue("keepMe");

		assertThat(value.tryToSetValue(input), is(false));
		assertThat(value.getValue(), is("keepMe"));
	}

	@Test
	void testToString_containsKeyValueAndSimpleClassName() {
		SingleStringValue value = new SingleStringValue("k");
		value.tryToSetValue("text");

		assertThat(value.toString(), is("k text : SingleStringValue"));
	}
}
