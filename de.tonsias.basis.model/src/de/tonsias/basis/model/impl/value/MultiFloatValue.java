package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.Set;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.ValueContentType;

public class MultiFloatValue extends AMultiValue<Float> {

	public MultiFloatValue(String key) {
		super(key);
	}

	public MultiFloatValue(String key, Collection<Float> values, Set<String> connectedInstanzes) {
		super(key, values, connectedInstanzes);
	}

	/**
	 * Whether this type would read the text as a number - the same rule
	 * {@link SingleFloatValue} asks, out of the same place.
	 *
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.FLOAT, value);
	}

	@Override
	public MultiValueType getType() {
		return MultiValueType.MULTI_FLOAT;
	}
}
