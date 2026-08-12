package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;

public class MultiInstanzValueTest {

	@Test
	void testGetPath_matchesTheEnum() {
		assertThat(new MultiInstanzValue("k").getPath(), is(MultiValueType.MULTI_INSTANZ.getPath()));
	}

	/**
	 * The folder sits below {@code multi_value/}, so it meets neither the
	 * {@code instanz/} folder the instanzen themselves are written into nor the
	 * single relation's.
	 */
	@Test
	void testGetPath_standsOnItsOwn() {
		assertThat(MultiValueType.MULTI_INSTANZ.getPath(), is("multi_value/instanz/"));
	}

	@Test
	void testTryToAddValue_takesKeys() {
		MultiInstanzValue value = new MultiInstanzValue("k");

		assertThat(value.tryToAddValue("3"), is(true));
		assertThat(value.tryToAddValue("1a"), is(true));

		assertThat(value.getValues(), contains("3", "1a"));
	}

	/**
	 * A fresh list points nowhere by being empty, so there is no "points nowhere"
	 * element to add - unlike {@link SingleInstanzValue}, which stores the empty
	 * string for exactly that state. An empty element would be a second spelling of
	 * something the list already says.
	 */
	@Test
	void testTryToAddValue_theEmptyKeyIsNoElement() {
		MultiInstanzValue value = new MultiInstanzValue("k");

		assertThat("but a single relation does take it",
				new SingleInstanzValue("s", "seed", java.util.Set.of()).tryToSetValue(""), is(true));

		assertThat(value.tryToAddValue(""), is(false));
		assertThat(value.getValues(), is(java.util.List.of()));
	}

	/** and setting the whole list cannot get past the rule adding one obeys */
	@Test
	void testTryToSetValues_theEmptyKeyIsNoElementThereEither() {
		MultiInstanzValue value = new MultiInstanzValue("k");
		value.tryToSetValues(List.of("3"));

		assertThat(value.tryToSetValues(List.of("1a", "")), is(false));
		assertThat(value.getValues(), contains("3"));
	}

	/**
	 * The same target at most once. That is what lets the target keep a plain set of
	 * value keys rather than counting how often it is pointed at - see
	 * {@code IInstanz.getReferencingValueKeys}.
	 */
	@Test
	void testTryToAddValue_theSameTargetOnlyOnce() {
		MultiInstanzValue value = new MultiInstanzValue("k");
		value.tryToAddValue("3");

		assertThat(value.tryToAddValue("3"), is(false));
		assertThat(value.getValues(), contains("3"));
	}

	/** upper case is out: the files are named after keys, and Windows folds case */
	@ParameterizedTest
	@ValueSource(strings = { " ", "A", "1A", "-1", "1 2", "1.2", "a b", " 3" })
	void testTryToAddValue_whatIsNoKeyIsRejected(String input) {
		MultiInstanzValue value = new MultiInstanzValue("k");

		assertThat(value.tryToAddValue(input), is(false));
		assertThat(value.size(), is(0));
	}

	@Test
	void testTryToRemoveValue_takesTheTargetOut() {
		MultiInstanzValue value = new MultiInstanzValue("k");
		value.tryToSetValues(List.of("3", "1a"));

		assertThat(value.tryToRemoveValue("3"), is(true));
		assertThat("the other target is left alone", value.getValues(), contains("1a"));
	}

	/** the same rule the single relation asks, out of the same place */
	@ParameterizedTest
	@ValueSource(strings = { "0", "3", "z", "1a", "zzz", "", " ", "A", "-1" })
	void testAccepts_agreesWithTheSingleValue(String input) {
		assertThat(MultiInstanzValue.accepts(input), is(SingleInstanzValue.accepts(input)));
	}
}
