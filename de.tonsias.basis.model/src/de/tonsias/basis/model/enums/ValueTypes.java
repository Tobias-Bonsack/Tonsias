package de.tonsias.basis.model.enums;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import de.tonsias.basis.model.interfaces.IValue;

/**
 * The two type enums as one list.
 * <p>
 * Written as a class beside them rather than as static methods on
 * {@link IValueType}, which would make the interface depend on both of its
 * implementations.
 * </p>
 */
public final class ValueTypes {

	/**
	 * Single first, then multi, each in declaration order - and this is the order
	 * the type combo in the create dialog offers, which is why it is written down
	 * here rather than read off an ordinal.
	 */
	private static final List<IValueType> ALL = buildAll();

	private ValueTypes() {
	}

	private static List<IValueType> buildAll() {
		List<IValueType> all = new ArrayList<>();
		Collections.addAll(all, SingleValueType.values());
		Collections.addAll(all, MultiValueType.values());
		return Collections.unmodifiableList(all);
	}

	public static List<IValueType> valuesList() {
		return ALL;
	}

	public static IValueType[] values() {
		return ALL.toArray(new IValueType[0]);
	}

	/** the types of one kind, for the two menus and the two groups in the views */
	public static IValueType[] of(boolean multi) {
		if (multi) {
			return MultiValueType.values();
		}
		return SingleValueType.values();
	}

	/**
	 * Compared by identity, the way both enums compare: a subclass of a value class
	 * is not that value class, and there is no type it could be resolved to.
	 *
	 * @return the type whose class this is, or empty for a value the model does not
	 *         know
	 */
	public static Optional<IValueType> byClass(Class<? extends IValue> clazz) {
		if (clazz == null) {
			return Optional.empty();
		}
		return ALL.stream().filter(type -> clazz == type.getClazz()).findFirst();
	}

	/** @return the type of that name, out of either enum */
	public static Optional<IValueType> byName(String name) {
		if (name == null) {
			return Optional.empty();
		}
		return ALL.stream().filter(type -> name.equals(type.name())).findFirst();
	}
}
