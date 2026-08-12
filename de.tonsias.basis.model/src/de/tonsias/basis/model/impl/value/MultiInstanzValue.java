package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.Set;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.interfaces.IInstanz;

/**
 * A relation pointing at several {@link IInstanz}es at once: every element is the
 * <em>key</em> of a target, the way {@link SingleInstanzValue} holds one.
 * <p>
 * Both ends are kept in sync by {@code ChangePropagationListener}: every target
 * records this value's key in {@code IInstanz.getReferencingValueKeys()}. That
 * set holds each value key at most once, and this class refusing duplicates is
 * what makes that sound - a list that could point at the same instanz twice would
 * need the target to count how often.
 * </p>
 * <p>
 * Unlike the single relation there is no "points nowhere" element. Pointing
 * nowhere is what an empty list already is, so the empty string would be a second
 * spelling of it; a target that is deleted has its element taken out rather than
 * blanked.
 * </p>
 */
public class MultiInstanzValue extends AMultiValue<String> {

	public MultiInstanzValue(String key) {
		super(key);
	}

	public MultiInstanzValue(String key, Collection<String> instanzKeys, Set<String> connectedInstanzes) {
		super(key, instanzKeys, connectedInstanzes);
	}

	/**
	 * Whether this text has the shape of a key - the same rule
	 * {@link SingleInstanzValue} asks, out of the same place.
	 *
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.INSTANZ, value);
	}

	/**
	 * Takes a key and refuses the empty string - see the class comment: an element
	 * pointing nowhere would say what leaving it out already says.
	 */
	@Override
	public boolean tryToAddValue(Object value) {
		return value instanceof String key && accepts(key) && super.tryToAddValue(key);
	}

	@Override
	public MultiValueType getType() {
		return MultiValueType.MULTI_INSTANZ;
	}
}
