package de.tonsias.basis.model.test.enums;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleBooleanValue;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;

public class SingleValueTypeTest {

	@Test
	void testGetByClass_stringValue() {
		assertThat(SingleValueType.getByClass(SingleStringValue.class),
				is(Optional.of(SingleValueType.SINGLE_STRING)));
	}

	@Test
	void testGetByClass_integerValue() {
		assertThat(SingleValueType.getByClass(SingleIntegerValue.class),
				is(Optional.of(SingleValueType.SINGLE_INTEGER)));
	}

	@Test
	void testGetByClass_booleanValue() {
		assertThat(SingleValueType.getByClass(SingleBooleanValue.class),
				is(Optional.of(SingleValueType.SINGLE_BOOLEAN)));
	}

	@Test
	void testGetByClass_nullIsEmpty() {
		assertThat(SingleValueType.getByClass(null), is(Optional.empty()));
	}

	@Test
	void testGetByClass_unknownImplementationIsEmpty() {
		assertThat(SingleValueType.getByClass(ISingleValue.class), is(Optional.empty()));
	}

	@ParameterizedTest
	@EnumSource(SingleValueType.class)
	void testRoundTrip_classToTypeToClass(SingleValueType type) {
		assertThat(SingleValueType.getByClass(type.getClazz()), is(Optional.of(type)));
	}

	@ParameterizedTest
	@EnumSource(SingleValueType.class)
	void testGetPath_isARelativeFolder(SingleValueType type) {
		// the save path is concatenated with the key, so it has to end in a separator
		assertThat(type.getPath(), endsWith("/"));
	}

	@Test
	void testGetPath_isUniquePerType() {
		var paths = Arrays.stream(SingleValueType.values()).map(SingleValueType::getPath)
				.collect(Collectors.toUnmodifiableSet());

		assertThat(paths.size(), is(SingleValueType.values().length));
	}
}
