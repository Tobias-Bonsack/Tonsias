package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;
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
 * <p>
 * {@code MultiInstanzValue} is the same relation pointing at several instanzes at
 * once.
 * </p>
 */
public class SingleInstanzValue extends ASingleValue<String> {

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
	 * The dialog asks the same question for its OK button, and so does the list of
	 * the same content, so there is no second rule that could drift - it lives in
	 * {@link ValueContentRules}. Taking the empty string back is the wider question
	 * that {@code tryToSetValue} asks: pointing nowhere is a state this value has -
	 * it is the one a fresh one starts in - and it is where a reference is put back
	 * to when its target is deleted, so the way in has to exist. Nothing chosen must
	 * still not become a value, which is what this narrower question is for.
	 * </p>
	 *
	 * @see ValueContentRules#accepts(ValueContentType, String)
	 */
	public static boolean accepts(String value) {
		return ValueContentRules.accepts(ValueContentType.INSTANZ, value);
	}

	@Override
	public SingleValueType getType() {
		return SingleValueType.SINGLE_INSTANZ;
	}
}
