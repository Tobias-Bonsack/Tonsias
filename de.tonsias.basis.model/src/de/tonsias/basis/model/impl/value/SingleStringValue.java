package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.impl.ASingleValue;

public class SingleStringValue extends ASingleValue<String> {

	public SingleStringValue(String key) {
		super(key);
	}

	public SingleStringValue(String key, String value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}
}
