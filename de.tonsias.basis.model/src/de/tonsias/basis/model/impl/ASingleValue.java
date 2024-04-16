package de.tonsias.basis.model.impl;

import de.tonsias.basis.model.interfaces.ISingleValue;

public abstract class ASingleValue<T> implements ISingleValue<T> {

	private final String _ownKey;

	private T _value;

	public ASingleValue(String key) {
		_ownKey = key;
	}

	public ASingleValue(String key, T value) {
		_ownKey = key;
		_value = value;
	}

	@Override
	public boolean setValue(T value) {
		if (value.equals(_value)) {
			return false;
		}

		_value = value;
		return true;
	}

	@Override
	public T getValue() {
		return _value;
	}
	
	@Override
	public String getOwnKey() {
		return _ownKey;
	}
}
