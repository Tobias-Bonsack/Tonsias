package de.tonsias.basis.model.enums;

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
}
