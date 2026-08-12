package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;

public class SingleFloatValue extends ASingleValue<Float> {

	public SingleFloatValue(String key) {
		super(key);
		this.setValue(0.0f);
	}

	public SingleFloatValue(String key, float value, Set<String> connectedInstanzes) {
		super(key, value, connectedInstanzes);
	}

	/**
	 * Whether this type would read the text as a number. The rule itself lives in
	 * {@link ValueContentRules}, where the list of the same content asks it too, so
	 * the two cannot drift.
	 *
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.FLOAT, value);
	}

	@Override
	public SingleValueType getType() {
		return SingleValueType.SINGLE_FLOAT;
	}
}
