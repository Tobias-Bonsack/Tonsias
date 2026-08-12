package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;

public class SingleStringValue extends ASingleValue<String> {

	public SingleStringValue(String key) {
		super(key);
		this.setValue("");
	}

	public SingleStringValue(String key, String value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

	@Override
	public SingleValueType getType() {
		return SingleValueType.SINGLE_STRING;
	}
}
