package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.Set;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.ValueContentType;

public class MultiStringValue extends AMultiValue<String> {

	// a fresh list is empty, the way a fresh string is "" - there is no first
	// element that could be meant, and the owner adds what belongs in it
	public MultiStringValue(String key) {
		super(key);
	}

	public MultiStringValue(String key, Collection<String> values, Set<String> connectedInstanzes) {
		super(key, values, connectedInstanzes);
	}

	/**
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.STRING, value);
	}

	@Override
	public MultiValueType getType() {
		return MultiValueType.MULTI_STRING;
	}
}
