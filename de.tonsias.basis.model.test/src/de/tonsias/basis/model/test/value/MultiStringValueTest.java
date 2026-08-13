package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.MultiStringValue;

public class MultiStringValueTest {

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new MultiStringValue("k").getPath(), is(MultiValueType.MULTI_STRING.getPath()));
	}

	/**
	 * The two folders of one content must not meet: a key is handed out once for
	 * the whole model, but a value that is no longer cached is looked for folder by
	 * folder, and the first file of that name wins.
	 */
	@Test
	void testGetPath_doesNotCollideWithTheSingleFolder() {
		assertThat(MultiValueType.MULTI_STRING.getPath(), is("multi_value/string/"));
		assertThat(MultiValueType.MULTI_STRING.getPath(), is(not(SingleValueType.SINGLE_STRING.getPath())));
	}

	@Test
	void testTryToAddValue_takesText() {
		MultiStringValue value = new MultiStringValue("k");

		assertThat(value.tryToAddValue("abc"), is(true));
		assertThat(value.getValues(), contains("abc"));
	}

	/**
	 * The empty string is an element here, unlike in a list of relations: a text
	 * that happens to be empty is still a text somebody put in the list.
	 */
	@Test
	void testTryToAddValue_theEmptyStringIsAnElement() {
		MultiStringValue value = new MultiStringValue("k");

		assertThat(value.tryToAddValue(""), is(true));
		assertThat(value.getValues(), contains(""));
	}

	@Test
	void testTryToAddValue_whatIsNoTextIsRejected() {
		MultiStringValue value = new MultiStringValue("k");

		assertThat(value.tryToAddValue(3), is(false));
		assertThat(value.tryToAddValue(null), is(false));
		assertThat(value.size(), is(0));
	}

	@Test
	void testAccepts_takesEveryText() {
		assertThat(MultiStringValue.accepts("abc"), is(true));
		assertThat(MultiStringValue.accepts(""), is(true));
		assertThat(MultiStringValue.accepts(null), is(false));
	}
}
