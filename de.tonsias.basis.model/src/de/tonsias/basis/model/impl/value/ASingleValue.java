package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.tonsias.basis.model.interfaces.ISingleValue;

public abstract class ASingleValue<T> implements ISingleValue<T> {

	private Set<String> _connectedInstanzes = new HashSet<>();

	private final String _ownKey;

	private T _value;

	public ASingleValue(String key) {
		_ownKey = key;
	}

	public ASingleValue(String key, T value, Set<String> connectedInstanzes) {
		_ownKey = key;
		_value = value;
		_connectedInstanzes = connectedInstanzes;
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

	@Override
	public Collection<String> getConnectedInstanzKeys() {
		return _connectedInstanzes;
	}

	@Override
	public void addConnectedInstanzKey(String key) {
		_connectedInstanzes.add(key);
	}

}
