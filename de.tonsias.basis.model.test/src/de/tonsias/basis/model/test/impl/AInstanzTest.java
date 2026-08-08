package de.tonsias.basis.model.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;

/**
 * Exercises the {@code AInstanz} behaviour through {@link Instanz}, the only
 * concrete subclass.
 */
public class AInstanzTest {

	private Instanz _instanz;

	@BeforeEach
	void beforeEach() {
		_instanz = new Instanz("key");
	}

	@Test
	void testNewInstanz_hasNoParentAndNoChildren() {
		assertThat(_instanz.getOwnKey(), is("key"));
		assertThat(_instanz.getParentKey(), is(nullValue()));
		assertThat(_instanz.getChildren(), is(empty()));
	}

	@Test
	void testSetParentKey_isKept() {
		_instanz.setParentKey("parent");

		assertThat(_instanz.getParentKey(), is("parent"));
	}

	@Test
	void testAddChildKeys_newKeysReportedAsAdded() {
		Map<Boolean, Collection<String>> result = _instanz.addChildKeys("a", "b");

		assertThat(_instanz.getChildren(), containsInAnyOrder("a", "b"));
		assertThat(result.get(true), containsInAnyOrder("a", "b"));
		assertThat(result.get(false), is(empty()));
	}

	@Test
	void testAddChildKeys_knownKeyReportedAsNotAdded() {
		_instanz.addChildKeys("a");

		Map<Boolean, Collection<String>> result = _instanz.addChildKeys("a", "b");

		assertThat(_instanz.getChildren(), hasSize(2));
		assertThat(result.get(true), contains("b"));
		assertThat(result.get(false), contains("a"));
	}

	@Test
	void testRemoveChildKeys_splitsRemovedAndUnknown() {
		_instanz.addChildKeys("a", "b");

		Map<Boolean, Collection<String>> result = _instanz.removeChildKeys("a", "unknown");

		assertThat(_instanz.getChildren(), contains("b"));
		assertThat(result.get(true), contains("a"));
		assertThat(result.get(false), contains("unknown"));
	}

	@ParameterizedTest
	@EnumSource(SingleValueType.class)
	void testAddValuekeys_storedUnderItsType(SingleValueType type) {
		_instanz.addValuekeys(type, Map.entry("valueKey", "name"));

		assertThat(_instanz.getSingleValues(type), hasEntry("valueKey", "name"));
		// the maps are strictly per type, nothing leaks into the other one
		for (SingleValueType other : SingleValueType.values()) {
			if (other != type) {
				assertThat(_instanz.getSingleValues(other).size(), is(0));
			}
		}
	}

	@ParameterizedTest
	@EnumSource(SingleValueType.class)
	void testGetSingleValues_sameMapPerType(SingleValueType type) {
		assertThat(_instanz.getSingleValues(type), is(sameInstance(_instanz.getSingleValues(type))));
	}

	/**
	 * The maps are created on demand rather than by a field initializer, because
	 * Gson skips those. Every type has to answer with a usable map from the start.
	 */
	@ParameterizedTest
	@EnumSource(SingleValueType.class)
	void testGetSingleValues_isEmptyNotNullOnANewInstanz(SingleValueType type) {
		assertThat(_instanz.getSingleValues(type), is(anEmptyMap()));
	}

	@Test
	void testGetSingleValues_nullTypeRejected() {
		// switch over an enum throws NPE before reaching the default branch
		assertThrows(NullPointerException.class, () -> _instanz.getSingleValues(null));
	}

	@Test
	void testDeleteKeys_removesByKey() {
		_instanz.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("k1", "n1"));
		_instanz.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("k2", "n2"));

		_instanz.deleteKeys(SingleValueType.SINGLE_STRING, "k1", "unknown");

		assertThat(_instanz.getSingleValues(SingleValueType.SINGLE_STRING), hasEntry("k2", "n2"));
		assertThat(_instanz.getSingleValues(SingleValueType.SINGLE_STRING).size(), is(1));
	}

	@Test
	void testDeleteParam_removesByName() {
		_instanz.addValuekeys(SingleValueType.SINGLE_INTEGER, Map.entry("k1", "n1"));
		_instanz.addValuekeys(SingleValueType.SINGLE_INTEGER, Map.entry("k2", "n2"));

		_instanz.deleteParam(SingleValueType.SINGLE_INTEGER, "n1", "unknown");

		assertThat(_instanz.getSingleValues(SingleValueType.SINGLE_INTEGER), hasEntry("k2", "n2"));
		assertThat(_instanz.getSingleValues(SingleValueType.SINGLE_INTEGER).size(), is(1));
	}

	@Test
	void testAddValuekeys_biMapRejectsDuplicateName() {
		_instanz.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("k1", "sameName"));

		// a BiMap keeps the name side unique, so reusing a name is a hard error
		assertThrows(IllegalArgumentException.class,
				() -> _instanz.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("k2", "sameName")));
	}

	@Test
	void testChildrenView_reflectsLaterAdds() {
		var children = _instanz.getChildren();
		_instanz.addChildKeys("a");

		assertThat(children, contains("a"));
		assertThat(children, is(not(empty())));
	}
}
