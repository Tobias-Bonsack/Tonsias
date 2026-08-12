package de.tonsias.basis.model.test.value;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.impl.value.MultiStringValue;

/**
 * Exercises the {@code AMultiValue} behaviour through {@link MultiStringValue},
 * the type-specific parts live in the subclass tests.
 */
public class AMultiValueTest {

	private MultiStringValue _value;

	@BeforeEach
	void beforeEach() {
		_value = new MultiStringValue("key");
	}

	/**
	 * A fresh list is empty, the way a fresh string is "". There is no first element
	 * that could be meant.
	 */
	@Test
	void testNewValue_startsEmpty() {
		assertThat(_value.getValues(), is(empty()));
		assertThat(_value.size(), is(0));
	}

	@Test
	void testAddValue_keepsTheOrderTheyWentIn() {
		_value.addValue("c");
		_value.addValue("a");
		_value.addValue("b");

		assertThat(_value.getValues(), contains("c", "a", "b"));
	}

	/**
	 * The guard that ends a propagation chain: a second add of the same element
	 * changes nothing, so the service fires nothing and nobody comes back around.
	 */
	@Test
	void testAddValue_theSameElementOnlyOnce() {
		assertThat(_value.addValue("a"), is(true));
		assertThat(_value.addValue("a"), is(false));

		assertThat(_value.getValues(), contains("a"));
	}

	@Test
	void testAddValue_nullIsNoElement() {
		assertThat(_value.addValue(null), is(false));
		assertThat(_value.getValues(), is(empty()));
	}

	@Test
	void testRemoveValue_takesItOutAndSaysSo() {
		_value.addValue("a");
		_value.addValue("b");

		assertThat(_value.removeValue("a"), is(true));
		assertThat(_value.getValues(), contains("b"));
	}

	@Test
	void testRemoveValue_whatIsNotThereIsNoChange() {
		_value.addValue("a");

		assertThat(_value.removeValue("b"), is(false));
		assertThat(_value.getValues(), contains("a"));
	}

	@Test
	void testGetValues_isUnmodifiable() {
		_value.addValue("a");

		assertThrows(UnsupportedOperationException.class, () -> _value.getValues().add("b"));
	}

	@Test
	void testSetValues_replacesTheWholeList() {
		_value.addValue("a");

		assertThat(_value.setValues(List.of("x", "y")), is(true));
		assertThat(_value.getValues(), contains("x", "y"));
	}

	@Test
	void testSetValues_theSameListIsNoChange() {
		_value.setValues(List.of("x", "y"));

		assertThat(_value.setValues(List.of("x", "y")), is(false));
	}

	/** the order is part of the list, so moving elements around is a change */
	@Test
	void testSetValues_reorderedIsAChange() {
		_value.setValues(List.of("x", "y"));

		assertThat(_value.setValues(List.of("y", "x")), is(true));
		assertThat(_value.getValues(), contains("y", "x"));
	}

	@Test
	void testSetValues_dropsDuplicatesTheWayAddValueWould() {
		assertThat(_value.setValues(List.of("x", "y", "x")), is(true));

		assertThat(_value.getValues(), contains("x", "y"));
	}

	@Test
	void testSetValues_emptyClearsIt() {
		_value.addValue("a");

		assertThat(_value.setValues(List.of()), is(true));
		assertThat(_value.getValues(), is(empty()));
	}

	@Test
	void testContainsAndSize() {
		_value.setValues(List.of("a", "b"));

		assertThat(_value.contains("a"), is(true));
		assertThat(_value.contains("c"), is(false));
		assertThat(_value.size(), is(2));
	}

	@Test
	void testTryToAddValue_takesWhatTheTypeReads() {
		assertThat(_value.tryToAddValue("a"), is(true));
		assertThat(_value.tryToAddValue(3), is(false));

		assertThat(_value.getValues(), contains("a"));
	}

	@Test
	void testTryToRemoveValue_takesWhatTheTypeReads() {
		_value.addValue("a");

		assertThat(_value.tryToRemoveValue(3), is(false));
		assertThat(_value.tryToRemoveValue("a"), is(true));
		assertThat(_value.getValues(), is(empty()));
	}

	/**
	 * One element the type will not read fails the whole call. Half a new list is a
	 * state nobody asked for, and the caller handed over one list.
	 */
	@Test
	void testTryToSetValues_oneBadElementLeavesTheListAsItWas() {
		_value.setValues(List.of("a"));

		assertThat(_value.tryToSetValues(List.of("x", 3, "y")), is(false));
		assertThat(_value.getValues(), contains("a"));
	}

	// ---------- what AValue brings along ----------

	@Test
	void testAddConnectedInstanzKey_onlyOnce() {
		assertThat(_value.addConnectedInstanzKey("a"), is(true));
		assertThat(_value.addConnectedInstanzKey("a"), is(false));
		assertThat(_value.getConnectedInstanzKeys(), contains("a"));
	}

	@Test
	void testFullConstructor_keepsEverything() {
		var full = new MultiStringValue("k", List.of("v1", "v2"), Set.of("i1", "i2"));

		assertThat(full.getOwnKey(), is("k"));
		assertThat(full.getValues(), contains("v1", "v2"));
		assertThat(full.getConnectedInstanzKeys(), containsInAnyOrder("i1", "i2"));
	}

	@Test
	void testFullConstructor_dropsDuplicates() {
		var full = new MultiStringValue("k", List.of("v1", "v1", "v2"), Set.of());

		assertThat(full.getValues(), contains("v1", "v2"));
	}

	@Test
	void testToString_containsKeyValuesAndSimpleClassName() {
		_value.setValues(List.of("a", "b"));

		assertThat(_value.toString(), is("key [a, b] : MultiStringValue"));
	}
}
