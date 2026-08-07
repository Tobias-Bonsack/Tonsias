package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;

public class SingleBooleanValue extends ASingleValue<Boolean> {

	public SingleBooleanValue(String key) {
		super(key);
		this.setValue(Boolean.FALSE);
	}

	public SingleBooleanValue(String key, boolean value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

	/**
	 * Only the two literals are accepted - anything else is rejected instead of
	 * being folded into {@code false}, so a typo does not silently clear the value.
	 */
	@Override
	public boolean tryToSetValue(Object value) {
		if (value instanceof Boolean b) {
			return setValue(b);
		} else if (value instanceof String s) {
			String trimmed = s.strip();
			if (Boolean.TRUE.toString().equalsIgnoreCase(trimmed)) {
				return setValue(Boolean.TRUE);
			}
			if (Boolean.FALSE.toString().equalsIgnoreCase(trimmed)) {
				return setValue(Boolean.FALSE);
			}
		}
		return false;
	}

	@Override
	public String getPath() {
		return SingleValueType.SINGLE_BOOLEAN.getPath();
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
