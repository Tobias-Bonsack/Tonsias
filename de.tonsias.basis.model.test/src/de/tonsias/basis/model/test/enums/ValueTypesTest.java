package de.tonsias.basis.model.test.enums;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueTypes;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.SingleFloatValue;
import de.tonsias.basis.model.interfaces.IValue;

public class ValueTypesTest {

	static List<IValueType> everyType() {
		return ValueTypes.valuesList();
	}

	@Test
	void testValuesList_holdsBothEnumsSingleFirst() {
		List<IValueType> all = ValueTypes.valuesList();

		assertThat(all, hasSize(SingleValueType.values().length + MultiValueType.values().length));
		assertThat(all.subList(0, SingleValueType.values().length), contains(SingleValueType.values()));
		assertThat(all.subList(SingleValueType.values().length, all.size()), contains(MultiValueType.values()));
	}

	/**
	 * The create dialog's combo indexes into this list, so a caller must not be able
	 * to reorder what the next caller reads.
	 */
	@Test
	void testValuesList_isUnmodifiable() {
		assertThrows(UnsupportedOperationException.class, () -> ValueTypes.valuesList().add(SingleValueType.SINGLE_STRING));
	}

	@Test
	void testOf_handsOutTheOneKind() {
		assertThat(ValueTypes.of(false), arrayContaining((IValueType[]) SingleValueType.values()));
		assertThat(ValueTypes.of(true), arrayContaining((IValueType[]) MultiValueType.values()));
	}

	@Test
	void testByClass_findsBothFamilies() {
		assertThat(ValueTypes.byClass(SingleFloatValue.class), is(Optional.of(SingleValueType.SINGLE_FLOAT)));
		assertThat(ValueTypes.byClass(MultiFloatValue.class), is(Optional.of(MultiValueType.MULTI_FLOAT)));
	}

	@Test
	void testByClass_unknownIsEmpty() {
		assertThat(ValueTypes.byClass(null), is(Optional.empty()));
		assertThat(ValueTypes.byClass(IValue.class), is(Optional.empty()));
	}

	@Test
	void testByName_findsBothFamilies() {
		assertThat(ValueTypes.byName("SINGLE_STRING"), is(Optional.of(SingleValueType.SINGLE_STRING)));
		assertThat(ValueTypes.byName("MULTI_STRING"), is(Optional.of(MultiValueType.MULTI_STRING)));
		assertThat(ValueTypes.byName("NOPE"), is(Optional.empty()));
		assertThat(ValueTypes.byName(null), is(Optional.empty()));
	}

	/**
	 * A value that is no longer cached does not say which type it is; the services
	 * find its file by trying one folder after the other. That only works while no
	 * two types share a folder - across <em>both</em> enums, which is the check
	 * neither {@code SingleValueTypeTest} nor {@code MultiValueTypeTest} can make on
	 * its own.
	 */
	@Test
	void testGetPath_isUniqueAcrossBothEnums() {
		var paths = ValueTypes.valuesList().stream().map(IValueType::getPath).collect(Collectors.toUnmodifiableSet());

		assertThat(paths, hasSize(ValueTypes.valuesList().size()));
	}

	/**
	 * The instanz files live in {@code instanz/}, and a value folder that met it
	 * would put two objects of the same key into one file.
	 */
	@ParameterizedTest
	@MethodSource("everyType")
	void testGetPath_doesNotCollideWithTheInstanzFolder(IValueType type) {
		assertThat(type.getPath().startsWith("instanz/"), is(false));
	}

	@ParameterizedTest
	@MethodSource("everyType")
	void testRoundTrip_everyTypeIsFoundByItsOwnClass(IValueType type) {
		assertThat(ValueTypes.byClass(type.getClazz()), is(Optional.of(type)));
	}

	/**
	 * Ten types are five contents times two kinds - if that ever stopped holding,
	 * every switch that only asks the content would quietly miss a case.
	 */
	@Test
	void testEveryContentExistsInBothKinds() {
		for (SingleValueType single : SingleValueType.values()) {
			assertThat("no multi type holds " + single.getContentType(),
					ValueTypes.valuesList().stream()
							.anyMatch(type -> type.isMulti() && type.getContentType() == single.getContentType()),
					is(true));
		}
		assertThat(MultiValueType.values().length, is(SingleValueType.values().length));
	}
}
