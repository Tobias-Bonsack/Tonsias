package de.tonsias.basis.model.enums;

import java.util.Arrays;
import java.util.Optional;

import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;

public enum SingleValueType {

	SINGLE_STRING(SingleStringValue.class, "single_value/string/"),
	SINGLE_INTEGER(SingleIntegerValue.class, "single_value/integer/");

	private final Class<? extends ISingleValue<?>> _clazz;

	private final String _path;

	SingleValueType(Class<? extends ISingleValue<?>> clazz, String path) {
		_clazz = clazz;
		_path = path;
	}

	public Class<? extends ISingleValue<?>> getClazz() {
		return _clazz;
	}

	public final String getPath() {
		return _path;
	}

	public static Optional<SingleValueType> getByClass(
			@SuppressWarnings("rawtypes") Class<? extends ISingleValue> clazz) {
		if (clazz == null) {
			return Optional.empty();
		}
		return Arrays.stream(SingleValueType.values()).filter(e -> clazz == e.getClazz()).findFirst();
	}
}
