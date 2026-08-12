package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.Set;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.ValueContentType;

/**
 * A list of the two literals - which, holding no duplicates, is at most two
 * elements long. It exists because the five contents are offered alike and an
 * exception would be the surprise, not the rule.
 */
public class MultiBooleanValue extends AMultiValue<Boolean> {

	public MultiBooleanValue(String key) {
		super(key);
	}

	public MultiBooleanValue(String key, Collection<Boolean> values, Set<String> connectedInstanzes) {
		super(key, values, connectedInstanzes);
	}

	/**
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.BOOLEAN, value);
	}

	@Override
	public MultiValueType getType() {
		return MultiValueType.MULTI_BOOLEAN;
	}
}
