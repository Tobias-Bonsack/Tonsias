package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.Collections;

import de.tonsias.basis.model.impl.ASingleValue;

public class SingleStringValue extends ASingleValue<String> {

	private Collection<String> _connectedInstanzes = Collections.emptyList();

	public SingleStringValue(String key) {
		super(key);
	}

	public SingleStringValue(String key, String value, Collection<String> _connectedInstanzes) {
		super(key, value);
		this._connectedInstanzes = _connectedInstanzes;
	}

	@Override
	public Collection<String> getConnectedInstanzKeys() {
		return _connectedInstanzes;
	}

}
