package de.tonsias.basis.model.impl.value;

import java.util.Set;
import java.util.regex.Pattern;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;

/**
 * A relation: the attribute of one {@link IInstanz} pointing at another one.
 * <p>
 * The value is the <em>key</em> of the target, not the target itself. Everything
 * in this model is referenced by string key rather than by object reference, and
 * this bundle has no OSGi dependency to resolve one with - the service does that,
 * through {@code IInstanzService.resolveInstanzValue}. Keeping the key here is
 * also what lets the value be persisted by the same Gson as every other one: a
 * resolved {@link IInstanz} would be written into this value's own file.
 * </p>
 */
public class SingleInstanzValue extends ASingleValue<String> {

	/** the shape of a key - see {@link #accepts} */
	private static final Pattern KEY = Pattern.compile("[0-9a-z]+");

	public SingleInstanzValue(String key) {
		super(key);
		// a fresh reference points nowhere, the way a fresh string is empty. It is not
		// a key accepts would take, so the dialog keeps OK off until one is chosen
		this.setValue("");
	}

	public SingleInstanzValue(String key, String instanzKey, Set<String> connectedInstanzes) {
		super(key, instanzKey, connectedInstanzes);
	}

	/**
	 * Whether this text has the shape of a key: base 36, lower case only, as
	 * {@code KeyServiceImpl} hands them out. Whether an instanz of that key
	 * <em>exists</em> is a question this bundle cannot ask - the key service and the
	 * instanz service both live in {@code de.tonsias.basis.osgi}, which depends on
	 * this one and not the other way round. A key that resolves to nothing is
	 * therefore accepted here and comes back empty from the service.
	 * <p>
	 * The dialog asks the same question for its OK button, so there is no second
	 * rule that could drift.
	 * </p>
	 */
	public static boolean accepts(String value) {
		if (value == null) {
			return false;
		}
		return KEY.matcher(value).matches();
	}

	@Override
	public boolean tryToSetValue(Object value) {
		return value instanceof String s && accepts(s) && setValue(s);
	}

	@Override
	public String getPath() {
		return SingleValueType.SINGLE_INSTANZ.getPath();
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
