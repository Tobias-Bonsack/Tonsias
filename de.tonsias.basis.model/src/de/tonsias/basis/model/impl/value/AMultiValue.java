package de.tonsias.basis.model.impl.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.interfaces.IMultiValue;

/**
 * A value holding a list of one content. What it holds is decided by the type its
 * subclass answers, so every {@code tryTo...} is written once here rather than
 * five times below - out of the same {@link ValueContentRules} the single values
 * ask.
 * <p>
 * Every concrete subclass is non-generic and binds {@code T} in its
 * {@code extends} clause. That is what lets Gson resolve the erased
 * {@code _values} field: the save service hands over the concrete class, and Gson
 * walks the superclass chain to learn that {@code List<T>} is a
 * {@code List<Float>}. A subclass that left {@code T} open would come back
 * holding {@code Double}s - the same trap {@code ASingleValue} has always had,
 * one level deeper.
 * </p>
 */
public abstract class AMultiValue<T> extends AValue implements IMultiValue<T> {

	// no field initializer: Gson allocates without running a constructor, so a json
	// that does not name the field leaves it null - see AValue and
	// https://github.com/Tobias-Bonsack/Tonsias/issues/61
	private List<T> _values;

	public AMultiValue(String key) {
		super(key);
	}

	public AMultiValue(String key, Collection<? extends T> values, Set<String> connectedInstanzes) {
		super(key, connectedInstanzes);
		_values = withoutDuplicates(values);
	}

	private List<T> values() {
		if (_values == null) {
			_values = new ArrayList<>();
		}
		return _values;
	}

	private List<T> withoutDuplicates(Collection<? extends T> candidates) {
		List<T> wanted = new ArrayList<>();
		for (T candidate : candidates) {
			if (candidate != null && !wanted.contains(candidate)) {
				wanted.add(candidate);
			}
		}
		return wanted;
	}

	@Override
	public List<T> getValues() {
		return Collections.unmodifiableList(values());
	}

	@Override
	public boolean addValue(T value) {
		if (value == null || values().contains(value)) {
			return false;
		}
		return values().add(value);
	}

	@Override
	public boolean removeValue(T value) {
		return values().remove(value);
	}

	@Override
	public boolean setValues(Collection<? extends T> values) {
		List<T> wanted = withoutDuplicates(values);
		if (wanted.equals(values())) {
			return false;
		}
		_values = wanted;
		return true;
	}

	@Override
	public boolean contains(T value) {
		return values().contains(value);
	}

	@Override
	public int size() {
		return values().size();
	}

	@Override
	public boolean tryToAddValue(Object value) {
		return converted(value).map(this::addValue).orElse(Boolean.FALSE);
	}

	@Override
	public boolean tryToRemoveValue(Object value) {
		return converted(value).map(this::removeValue).orElse(Boolean.FALSE);
	}

	/**
	 * One element the type will not read makes the whole call fail rather than
	 * landing a half taken-over list: what a caller hands over is one list, and a
	 * list that is partly the old one and partly the new one is a state nobody asked
	 * for.
	 */
	@Override
	public boolean tryToSetValues(Collection<?> values) {
		List<T> wanted = new ArrayList<>();
		for (Object raw : values) {
			Optional<T> element = converted(raw);
			if (element.isEmpty()) {
				return false;
			}
			wanted.add(element.get());
		}
		return setValues(wanted);
	}

	/**
	 * What the type reads out of this object, if anything - and for a relation, not
	 * the empty string. A single relation has that as a state: it is where one is
	 * put back to when its target is deleted. A list says the same thing by having
	 * no element, so an empty one would be a second spelling of it, and an element
	 * pointing nowhere is not something anybody puts in a list on purpose. Asked
	 * here rather than in {@code tryToAddValue} alone, so setting the whole list
	 * cannot get in past the rule that adding one element obeys.
	 */
	@SuppressWarnings("unchecked") // the rules only ever hand back what this content type holds
	private Optional<T> converted(Object raw) {
		ValueContentType content = getType().getContentType();
		return ValueContentRules.convert(content, raw)//
				.filter(value -> content != ValueContentType.INSTANZ || !"".equals(value))//
				.map(value -> (T) value);
	}

	@Override
	protected Object getContent() {
		return getValues();
	}
}
