package de.tonsias.basis.model.impl.value;

import java.util.Set;

import de.tonsias.basis.model.interfaces.ISingleValue;

/**
 * A value holding one content. What it holds is decided by the type its subclass
 * answers, so {@link #tryToSetValue} is written once here rather than five times
 * below.
 * <p>
 * Every concrete subclass is non-generic and binds {@code T} in its
 * {@code extends} clause. That is what lets Gson resolve the erased
 * {@code _value} field: the save service hands over the concrete class, and Gson
 * walks the superclass chain to learn that {@code T} is a {@code Float}. A
 * subclass that left {@code T} open would come back holding a {@code Double}.
 * </p>
 */
public abstract class ASingleValue<T> extends AValue implements ISingleValue<T> {

	private T _value;

	public ASingleValue(String key) {
		super(key);
	}

	public ASingleValue(String key, T value, Set<String> connectedInstanzes) {
		super(key, connectedInstanzes);
		_value = value;
	}

	@Override
	public boolean setValue(T value) {
		if (value.equals(_value)) {
			return false;
		}

		_value = value;
		return true;
	}

	@Override
	public T getValue() {
		return _value;
	}

	/**
	 * The rule is the content type's, asked of {@link ValueContentRules} so the list
	 * of the same content cannot answer it differently.
	 */
	@SuppressWarnings("unchecked") // the rules only ever hand back what this content type holds
	@Override
	public final boolean tryToSetValue(Object value) {
		return ValueContentRules.convert(getType().getContentType(), value)//
				.map(converted -> setValue((T) converted))//
				.orElse(Boolean.FALSE);
	}

	@Override
	protected Object getContent() {
		return getValue();
	}
}
