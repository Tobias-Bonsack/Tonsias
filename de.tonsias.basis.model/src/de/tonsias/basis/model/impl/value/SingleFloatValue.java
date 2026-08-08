package de.tonsias.basis.model.impl.value;

import java.util.Set;
import java.util.regex.Pattern;

import de.tonsias.basis.model.enums.SingleValueType;

public class SingleFloatValue extends ASingleValue<Float> {

	/**
	 * The decimal notation this type accepts as text, shared with the dialog that
	 * greys out its OK button on anything else.
	 */
	public static final String DECIMAL_PATTERN = "-?\\d+(\\.\\d+)?";

	private static final Pattern DECIMAL = Pattern.compile(DECIMAL_PATTERN);

	public SingleFloatValue(String key) {
		super(key);
		this.setValue(0.0f);
	}

	public SingleFloatValue(String key, float value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

	/**
	 * Only decimal notation is accepted - {@link Float#parseFloat} would also read
	 * "NaN", "Infinity", "1e5", "3f" and "0x1p3", none of which anybody types into
	 * a value field on purpose. They are rejected instead of being folded into a
	 * surprising number.
	 */
	@Override
	public boolean tryToSetValue(Object value) {
		if (value instanceof Float f) {
			return setValue(f);
		} else if (value instanceof String s) {
			String trimmed = s.strip();
			if (DECIMAL.matcher(trimmed).matches()) {
				return setValue(Float.valueOf(trimmed));
			}
		}
		return false;
	}

	@Override
	public String getPath() {
		return SingleValueType.SINGLE_FLOAT.getPath();
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
