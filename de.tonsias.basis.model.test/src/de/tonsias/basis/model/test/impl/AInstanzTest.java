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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.enums.ValueTypes;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IValue;

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

	static List<IValueType> everyType() {
		return ValueTypes.valuesList();
	}

	@ParameterizedTest
	@MethodSource("everyType")
	void testAddValuekeys_storedUnderItsType(IValueType type) {
		_instanz.addValuekeys(type, Map.entry("valueKey", "name"));

		assertThat(_instanz.getValues(type), hasEntry("valueKey", "name"));
		// the maps are strictly per type, nothing leaks into any other one - single
		// and multi of the same content least of all, they are the pair that would
		// go unnoticed
		for (IValueType other : ValueTypes.valuesList()) {
			if (other != type) {
				assertThat("leaked into " + other, _instanz.getValues(other).size(), is(0));
			}
		}
	}

	@ParameterizedTest
	@MethodSource("everyType")
	void testGetValues_sameMapPerType(IValueType type) {
		assertThat(_instanz.getValues(type), is(sameInstance(_instanz.getValues(type))));
	}

	/**
	 * The maps are created on demand rather than by a field initializer, because
	 * Gson skips those. Every type has to answer with a usable map from the start -
	 * including the five an instanz written before they existed names nowhere.
	 */
	@ParameterizedTest
	@MethodSource("everyType")
	void testGetValues_isEmptyNotNullOnANewInstanz(IValueType type) {
		assertThat(_instanz.getValues(type), is(anEmptyMap()));
	}

	@Test
	void testGetValues_nullTypeRejected() {
		assertThrows(IllegalArgumentException.class, () -> _instanz.getValues(null));
	}

	/** a type from neither enum belongs to no map either */
	@Test
	void testGetValues_foreignTypeRejected() {
		IValueType foreign = new IValueType() {

			@Override
			public String name() {
				return "FOREIGN";
			}

			@Override
			public String getPath() {
				return "foreign/";
			}

			@Override
			public Class<? extends IValue> getClazz() {
				return IValue.class;
			}

			@Override
			public ValueContentType getContentType() {
				return ValueContentType.STRING;
			}

			@Override
			public boolean isMulti() {
				return false;
			}
		};

		assertThrows(IllegalArgumentException.class, () -> _instanz.getValues(foreign));
	}

	/**
	 * A list attribute and a single attribute of the same content are two different
	 * attributes, and the name side is unique per map rather than across them.
	 */
	@Test
	void testAddValuekeys_singleAndMultiOfOneContentAreSeparate() {
		_instanz.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("k1", "name"));
		_instanz.addValuekeys(MultiValueType.MULTI_STRING, Map.entry("k2", "name"));

		assertThat(_instanz.getValues(SingleValueType.SINGLE_STRING), hasEntry("k1", "name"));
		assertThat(_instanz.getValues(MultiValueType.MULTI_STRING), hasEntry("k2", "name"));
	}

	@Test
	void testDeleteKeys_removesByKey() {
		_instanz.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("k1", "n1"));
		_instanz.addValuekeys(SingleValueType.SINGLE_STRING, Map.entry("k2", "n2"));

		_instanz.deleteKeys(SingleValueType.SINGLE_STRING, "k1", "unknown");

		assertThat(_instanz.getValues(SingleValueType.SINGLE_STRING), hasEntry("k2", "n2"));
		assertThat(_instanz.getValues(SingleValueType.SINGLE_STRING).size(), is(1));
	}

	@Test
	void testDeleteParam_removesByName() {
		_instanz.addValuekeys(SingleValueType.SINGLE_INTEGER, Map.entry("k1", "n1"));
		_instanz.addValuekeys(SingleValueType.SINGLE_INTEGER, Map.entry("k2", "n2"));

		_instanz.deleteParam(SingleValueType.SINGLE_INTEGER, "n1", "unknown");

		assertThat(_instanz.getValues(SingleValueType.SINGLE_INTEGER), hasEntry("k2", "n2"));
		assertThat(_instanz.getValues(SingleValueType.SINGLE_INTEGER).size(), is(1));
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

	// ---------- the backward end of a relation ----------

	/**
	 * Created on demand like the value maps, and for the same reason: Gson skips
	 * field initializers, and an instanz stored before this field existed names it
	 * nowhere at all.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/74">#74</a>
	 */
	@Test
	void testGetReferencingValueKeys_isEmptyNotNullOnANewInstanz() {
		assertThat(_instanz.getReferencingValueKeys(), is(empty()));
		assertThat(_instanz.getReferencingValueKeys(), is(sameInstance(_instanz.getReferencingValueKeys())));
	}

	@Test
	void testAddReferencingValueKey_reportsWhetherItWasNew() {
		assertThat(_instanz.addReferencingValueKey("v1"), is(true));
		assertThat(_instanz.addReferencingValueKey("v1"), is(false));
		assertThat(_instanz.addReferencingValueKey("v2"), is(true));

		assertThat(_instanz.getReferencingValueKeys(), containsInAnyOrder("v1", "v2"));
	}

	@Test
	void testRemoveReferencingValueKey_reportsWhetherItWasThere() {
		_instanz.addReferencingValueKey("v1");

		assertThat(_instanz.removeReferencingValueKey("v1"), is(true));
		assertThat(_instanz.removeReferencingValueKey("v1"), is(false));
		assertThat(_instanz.getReferencingValueKeys(), is(empty()));
	}

	/** nothing is no value key - a relation pointing nowhere records nobody */
	@Test
	void testAddReferencingValueKey_blankIsRejected() {
		assertThat(_instanz.addReferencingValueKey(null), is(false));
		assertThat(_instanz.addReferencingValueKey(""), is(false));
		assertThat(_instanz.addReferencingValueKey(" "), is(false));

		assertThat(_instanz.getReferencingValueKeys(), is(empty()));
	}
}
