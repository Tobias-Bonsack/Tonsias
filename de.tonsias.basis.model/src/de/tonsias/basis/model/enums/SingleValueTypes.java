package de.tonsias.basis.model.enums;

import java.util.Arrays;
import java.util.Optional;

import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;

public enum SingleValueTypes {

	SINGLE_STRING(SingleStringValue.class);

	private final Class<? extends ISingleValue<?>> _clazz;

	SingleValueTypes(Class<? extends ISingleValue<?>> clazz) {
		_clazz = clazz;
	}

	public Class<? extends ISingleValue<?>> getClazz() {
		return _clazz;
	}

	public static Optional<SingleValueTypes> getByClass(Class<? extends ISingleValue<?>> clazz) {
		if (clazz == null) {
			return Optional.empty();
		}
		return Arrays.stream(SingleValueTypes.values()).filter(e -> clazz == e.getClazz()).findFirst();
	}
}
