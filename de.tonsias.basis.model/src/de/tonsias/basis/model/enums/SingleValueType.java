package de.tonsias.basis.model.enums;

import java.util.Arrays;
import java.util.Optional;

import de.tonsias.basis.model.impl.value.SingleBooleanValue;
import de.tonsias.basis.model.impl.value.SingleFloatValue;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;

/**
 * The five types that hold one content each. {@link MultiValueType} is the same
 * five as a list; {@link ValueTypes} is the two of them together.
 */
public enum SingleValueType implements IValueType {

	SINGLE_STRING(SingleStringValue.class, "single_value/string/", ValueContentType.STRING),
	SINGLE_INTEGER(SingleIntegerValue.class, "single_value/integer/", ValueContentType.INTEGER),
	SINGLE_BOOLEAN(SingleBooleanValue.class, "single_value/boolean/", ValueContentType.BOOLEAN),
	SINGLE_FLOAT(SingleFloatValue.class, "single_value/float/", ValueContentType.FLOAT),
	// a relation rather than a literal: the value is the key of another instanz.
	// The folder sits below single_value/, so it does not meet the instanz/ one
	// InstanzServiceImpl writes into
	SINGLE_INSTANZ(SingleInstanzValue.class, "single_value/instanz/", ValueContentType.INSTANZ);

	private final Class<? extends ISingleValue<?>> _clazz;

	private final String _path;

	private final ValueContentType _contentType;

	SingleValueType(Class<? extends ISingleValue<?>> clazz, String path, ValueContentType contentType) {
		_clazz = clazz;
		_path = path;
		_contentType = contentType;
	}

	/**
	 * Narrowed from {@link IValueType#getClazz()}: everything this enum names holds
	 * one content, so a caller that already knows it has a single type needs no cast
	 * to hand the class to the single value service.
	 */
	@Override
	public Class<? extends ISingleValue<?>> getClazz() {
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
		return false;
	}

	public static Optional<SingleValueType> getByClass(
			@SuppressWarnings("rawtypes") Class<? extends ISingleValue> clazz) {
		if (clazz == null) {
			return Optional.empty();
		}
		return Arrays.stream(SingleValueType.values()).filter(e -> clazz == e.getClazz()).findFirst();
	}
}
