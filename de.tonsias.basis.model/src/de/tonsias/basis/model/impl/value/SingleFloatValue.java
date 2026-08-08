package de.tonsias.basis.model.impl.value;

import java.util.Set;
import java.util.regex.Pattern;

import de.tonsias.basis.model.enums.SingleValueType;

public class SingleFloatValue extends ASingleValue<Float> {

	/** the decimal notation this type accepts as text - see {@link #accepts} */
	private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

	public SingleFloatValue(String key) {
		super(key);
		this.setValue(0.0f);
	}

	public SingleFloatValue(String key, float value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

	/**
	 * Whether this type would read the text as a number. Only decimal notation is
	 * accepted - {@link Float#parseFloat} would also read "NaN", "Infinity", "1e5",
	 * "3f" and "0x1p3", none of which anybody types into a value field on purpose -
	 * and only what stays a finite number: a one followed by forty zeros passes the
	 * notation but parses to {@code Infinity}, which is the surprising number this
	 * type promises not to store. The dialog asks the same question for its OK
	 * button, so there is no second rule that could drift.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>
	 */
	public static boolean accepts(String value) {
		if (value == null) {
			return false;
		}
		String trimmed = value.strip();
		return DECIMAL.matcher(trimmed).matches() && Float.isFinite(Float.parseFloat(trimmed));
	}

	@Override
	public boolean tryToSetValue(Object value) {
		if (value instanceof Float f) {
			// the same rule as for text: what the type will not read, it will not store
			// from a caller that already holds the float either
			return Float.isFinite(f) && setValue(f);
		} else if (value instanceof String s && accepts(s)) {
			return setValue(Float.valueOf(s.strip()));
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
