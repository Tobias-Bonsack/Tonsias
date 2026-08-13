package de.tonsias.basis.model.enums;

import java.util.Arrays;
import java.util.Optional;

import de.tonsias.basis.model.impl.value.MultiBooleanValue;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.interfaces.IMultiValue;

/**
 * The same five contents {@link SingleValueType} names, each as a list.
 * {@link ValueTypes} is the two of them together.
 * <p>
 * The folders sit below {@code multi_value/}, so they meet neither the
 * {@code single_value/} ones nor the {@code instanz/} one the instanz service
 * writes into - the services find a value whose type they no longer know by
 * trying one folder after the other, which only works while the paths are unique
 * across both enums.
 * </p>
 */
public enum MultiValueType implements IValueType {

	MULTI_STRING(MultiStringValue.class, "multi_value/string/", ValueContentType.STRING),
	MULTI_INTEGER(MultiIntegerValue.class, "multi_value/integer/", ValueContentType.INTEGER),
	MULTI_BOOLEAN(MultiBooleanValue.class, "multi_value/boolean/", ValueContentType.BOOLEAN),
	MULTI_FLOAT(MultiFloatValue.class, "multi_value/float/", ValueContentType.FLOAT),
	// a list of relations: every element is the key of another instanz
	MULTI_INSTANZ(MultiInstanzValue.class, "multi_value/instanz/", ValueContentType.INSTANZ);

	private final Class<? extends IMultiValue<?>> _clazz;

	private final String _path;

	private final ValueContentType _contentType;

	MultiValueType(Class<? extends IMultiValue<?>> clazz, String path, ValueContentType contentType) {
		_clazz = clazz;
		_path = path;
		_contentType = contentType;
	}

	/**
	 * Narrowed from {@link IValueType#getClazz()}: everything this enum names holds
	 * a list, so a caller that already knows it has a multi type needs no cast to
	 * hand the class to the multi value service.
	 */
	@Override
	public Class<? extends IMultiValue<?>> getClazz() {
		return _clazz;
	}

	@Override
	public final String getPath() {
		return _path;
	}

	@Override
	public ValueContentType getContentType() {
		return _contentType;
	}

	@Override
	public boolean isMulti() {
		return true;
	}

	public static Optional<MultiValueType> getByClass(@SuppressWarnings("rawtypes") Class<? extends IMultiValue> clazz) {
		if (clazz == null) {
			return Optional.empty();
		}
		return Arrays.stream(MultiValueType.values()).filter(e -> clazz == e.getClazz()).findFirst();
	}
}
