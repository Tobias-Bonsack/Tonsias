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

	/**
	 * Whether this type would read the text as a number, which is exactly what
	 * {@link Integer#valueOf} takes: digits with an optional sign, and nothing
	 * outside the {@code int} range. The dialog asks the same question for its OK
	 * button, so there is no second rule that could drift - a matter of "99999999999"
	 * being offered and then silently landing as 0.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>
	 */
	public static boolean accepts(String value) {
		if (value == null) {
			return false;
		}
		try {
			Integer.valueOf(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean tryToSetValue(Object value) {
		if (value instanceof Integer i) {
			return setValue(i);
		} else if (value instanceof String s && accepts(s)) {
			return setValue(Integer.valueOf(s));
		}
		return false;
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
