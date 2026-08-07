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

import de.tonsias.basis.model.impl.value.SingleStringValue;

/**
 * Exercises the {@code ASingleValue} behaviour through {@link SingleStringValue},
 * the type-specific parts live in the subclass tests.
 */
public class ASingleValueTest {

	private SingleStringValue _value;

	@BeforeEach
	void beforeEach() {
		_value = new SingleStringValue("key");
	}

	@Test
	void testSetValue_changedValueReturnsTrue() {
		assertThat(_value.setValue("new"), is(true));
		assertThat(_value.getValue(), is("new"));
	}

	@Test
	void testSetValue_sameValueReturnsFalse() {
		_value.setValue("same");

		assertThat(_value.setValue("same"), is(false));
		assertThat(_value.getValue(), is("same"));
	}

	@Test
	void testSetValue_nullIsRejectedLoudly() {
		// documents the current contract: the null check is on the incoming value
		assertThrows(NullPointerException.class, () -> _value.setValue(null));
	}

	@Test
	void testAddConnectedInstanzKey_onlyOnce() {
		assertThat(_value.addConnectedInstanzKey("a"), is(true));
		assertThat(_value.addConnectedInstanzKey("a"), is(false));
		assertThat(_value.getConnectedInstanzKeys(), contains("a"));
	}

	@Test
	void testGetConnectedInstanzKeys_isUnmodifiable() {
		_value.addConnectedInstanzKey("a");

		assertThrows(UnsupportedOperationException.class, () -> _value.getConnectedInstanzKeys().add("b"));
	}

	/**
	 * {@code getConnectedInstanzKeys()} hands out a live view, not a copy. Callers
	 * that want to keep the keys around while mutating the connections have to copy
	 * first - see the delete path in {@code SingleValueServiceImpl}.
	 */
	@Test
	void testGetConnectedInstanzKeys_isALiveView() {
		_value.addConnectedInstanzKey("a");
		var view = _value.getConnectedInstanzKeys();

		_value.addConnectedInstanzKey("b");

		assertThat(view, containsInAnyOrder("a", "b"));
	}

	@Test
	void testRemoveConnection_removesTheGivenKeys() {
		_value.addConnectedInstanzKey("a");
		_value.addConnectedInstanzKey("b");

		assertThat(_value.removeConnection(List.of("a")), is(true));

		assertThat(_value.getConnectedInstanzKeys(), contains("b"));
	}

	@Test
	void testRemoveConnection_unknownKeyChangesNothing() {
		_value.addConnectedInstanzKey("a");

		assertThat(_value.removeConnection(List.of("unknown")), is(false));

		assertThat(_value.getConnectedInstanzKeys(), contains("a"));
	}

	@Test
	void testRemoveConnection_withOwnKeySetClearsEverything() {
		_value.addConnectedInstanzKey("a");
		_value.addConnectedInstanzKey("b");

		assertThat(_value.removeConnection(_value.getConnectedInstanzKeys()), is(true));

		assertThat(_value.getConnectedInstanzKeys(), is(empty()));
	}

	@Test
	void testFullConstructor_keepsEverything() {
		var full = new SingleStringValue("k", "v", Set.of("i1", "i2"));

		assertThat(full.getOwnKey(), is("k"));
		assertThat(full.getValue(), is("v"));
		assertThat(full.getConnectedInstanzKeys(), containsInAnyOrder("i1", "i2"));
	}
}
