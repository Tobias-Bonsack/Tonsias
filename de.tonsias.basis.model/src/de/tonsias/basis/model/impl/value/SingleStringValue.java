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
