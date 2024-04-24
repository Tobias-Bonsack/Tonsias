package de.tonsias.basis.model.impl.value;

import java.util.Collection;

import de.tonsias.basis.model.impl.ASingleValue;

public class SingleStringValue extends ASingleValue<String> {

	public SingleStringValue(String key) {
		super(key);
	}

	public SingleStringValue(String key, String value, Collection<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

}
