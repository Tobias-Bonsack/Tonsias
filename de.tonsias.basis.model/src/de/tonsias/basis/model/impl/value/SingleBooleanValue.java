package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;

public class SingleBooleanValue extends ASingleValue<Boolean> {

	public SingleBooleanValue(String key) {
		super(key);
		this.setValue(Boolean.FALSE);
	}

	public SingleBooleanValue(String key, boolean value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

	/**
	 * Whether this type would read the text: only the two literals, so a typo is
	 * rejected instead of being folded into {@code false}. The rule itself lives in
	 * {@link ValueContentRules}, where the list of the same content asks it too.
	 *
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.BOOLEAN, value);
	}

	@Override
	public SingleValueType getType() {
		return SingleValueType.SINGLE_BOOLEAN;
	}
}
