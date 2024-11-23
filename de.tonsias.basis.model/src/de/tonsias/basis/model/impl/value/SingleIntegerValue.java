package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;

public class SingleIntegerValue extends ASingleValue<Integer> {

	public SingleIntegerValue(String key) {
		super(key);
		this.setValue(0);
	}

	public SingleIntegerValue(String key, int value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

	@Override
	public boolean tryToSetValue(Object value) {
		return (value instanceof Integer) && setValue((Integer) value);
	}

	@Override
	public String getPath() {
		return SingleValueType.SINGLE_INTEGER.getPath();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getOwnKey()).append(" ");
		builder.append(this.getValue()).append(" ");
		String[] string = this.getClass().toString().split("\\.");
		builder.append(": ").append(string[string.length - 1]);
		return builder.toString();
	}

}
