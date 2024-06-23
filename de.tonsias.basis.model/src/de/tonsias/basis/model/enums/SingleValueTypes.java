package de.tonsias.basis.model.enums;

import java.util.Arrays;
import java.util.Optional;

import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;

public enum SingleValueTypes {

	SINGLE_STRING(SingleStringValue.class, "single_value/string/");

	private final Class<? extends ISingleValue<?>> _clazz;

	private final String _path;

	SingleValueTypes(Class<? extends ISingleValue<?>> clazz, String path) {
		_clazz = clazz;
		_path = path;
	}

	public Class<? extends ISingleValue<?>> getClazz() {
		return _clazz;
	}

	public final String getPath() {
		return _path;
	}

	public static Optional<SingleValueTypes> getByClass(Class<? extends ISingleValue<?>> clazz) {
		if (clazz == null) {
			return Optional.empty();
		}
		return Arrays.stream(SingleValueTypes.values()).filter(e -> clazz == e.getClazz()).findFirst();
	}
}
