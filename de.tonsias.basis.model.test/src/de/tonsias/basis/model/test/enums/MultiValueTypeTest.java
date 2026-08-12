package de.tonsias.basis.model.test.enums;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiBooleanValue;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.interfaces.IMultiValue;

public class MultiValueTypeTest {

	@Test
	void testGetByClass_stringValue() {
		assertThat(MultiValueType.getByClass(MultiStringValue.class), is(Optional.of(MultiValueType.MULTI_STRING)));
	}

	@Test
	void testGetByClass_integerValue() {
		assertThat(MultiValueType.getByClass(MultiIntegerValue.class), is(Optional.of(MultiValueType.MULTI_INTEGER)));
	}

	@Test
	void testGetByClass_booleanValue() {
		assertThat(MultiValueType.getByClass(MultiBooleanValue.class), is(Optional.of(MultiValueType.MULTI_BOOLEAN)));
	}

	@Test
	void testGetByClass_floatValue() {
		assertThat(MultiValueType.getByClass(MultiFloatValue.class), is(Optional.of(MultiValueType.MULTI_FLOAT)));
	}

	@Test
	void testGetByClass_nullIsEmpty() {
		assertThat(MultiValueType.getByClass(null), is(Optional.empty()));
	}

	@Test
	void testGetByClass_unknownImplementationIsEmpty() {
		assertThat(MultiValueType.getByClass(IMultiValue.class), is(Optional.empty()));
	}

	@ParameterizedTest
	@EnumSource(MultiValueType.class)
	void testRoundTrip_classToTypeToClass(MultiValueType type) {
		assertThat(MultiValueType.getByClass(type.getClazz()), is(Optional.of(type)));
	}

	@ParameterizedTest
	@EnumSource(MultiValueType.class)
	void testGetPath_isARelativeFolderOfItsOwn(MultiValueType type) {
		// the save path is concatenated with the key, so it has to end in a separator
		assertThat(type.getPath(), endsWith("/"));
		assertThat(type.getPath(), startsWith("multi_value/"));
	}

	@Test
	void testGetPath_isUniquePerType() {
		var paths = Arrays.stream(MultiValueType.values()).map(MultiValueType::getPath)
				.collect(Collectors.toUnmodifiableSet());

		assertThat(paths.size(), is(MultiValueType.values().length));
	}

	@ParameterizedTest
	@EnumSource(MultiValueType.class)
	void testIsMulti_saysWhatKindItIs(MultiValueType type) {
		assertThat(type.isMulti(), is(true));
	}

	/**
	 * The type a value class answers has to be the one that names it, or a value
	 * would be written into a folder nothing looks for it in.
	 */
	@ParameterizedTest
	@EnumSource(MultiValueType.class)
	void testAFreshValueAnswersItsOwnType(MultiValueType type) throws Exception {
		IMultiValue<?> value = type.getClazz().getConstructor(String.class).newInstance("k");

		assertThat(value.getType(), is(type));
		assertThat(value.getPath(), is(type.getPath()));
	}
}
