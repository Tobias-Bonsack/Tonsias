package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.Set;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.ValueContentType;

public class MultiIntegerValue extends AMultiValue<Integer> {

	public MultiIntegerValue(String key) {
		super(key);
	}

	public MultiIntegerValue(String key, Collection<Integer> values, Set<String> connectedInstanzes) {
		super(key, values, connectedInstanzes);
	}

	/**
	 * Whether this type would read the text as a number - the same rule
	 * {@link SingleIntegerValue} asks, out of the same place.
	 *
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.INTEGER, value);
	}

	@Override
	public MultiValueType getType() {
		return MultiValueType.MULTI_INTEGER;
	}
}
