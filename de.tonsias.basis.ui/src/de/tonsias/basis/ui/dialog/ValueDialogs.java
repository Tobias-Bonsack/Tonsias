package de.tonsias.basis.ui.dialog;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.MultiBooleanValue;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IMultiValue;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * Which dialog edits which type - the one switch over all ten, so nobody else
 * needs one.
 */
public final class ValueDialogs {

	private ValueDialogs() {
	}

	public static AValueDialogBase<?, ?> create(IValueType type, Shell shell, IInstanz parent, Messages messages) {
		if (type instanceof SingleValueType single) {
			switch (single) {
			case SINGLE_STRING:
				return new StringValueDialog(shell, parent, messages);
			case SINGLE_INTEGER:
				return new IntegerValueDialog(shell, parent, messages);
			case SINGLE_BOOLEAN:
				return new BooleanValueDialog(shell, parent, messages);
			case SINGLE_FLOAT:
				return new FloatValueDialog(shell, parent, messages);
			case SINGLE_INSTANZ:
				return new InstanzValueDialog(shell, parent, messages);
			default:
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
		}
		if (type instanceof MultiValueType multi) {
			switch (multi) {
			case MULTI_STRING:
				return new MultiStringValueDialog(shell, parent, messages);
			case MULTI_INTEGER:
				return new MultiIntegerValueDialog(shell, parent, messages);
			case MULTI_BOOLEAN:
				return new MultiBooleanValueDialog(shell, parent, messages);
			case MULTI_FLOAT:
				return new MultiFloatValueDialog(shell, parent, messages);
			case MULTI_INSTANZ:
				return new MultiInstanzValueDialog(shell, parent, messages);
			default:
				throw new IllegalArgumentException("Unexpected value: " + type);
			}
		}
		throw new IllegalArgumentException("Unexpected value: " + type);
	}

	/**
	 * The dialog on a list that is already there, so it opens holding what the list
	 * holds.
	 */
	public static AMultiValueDialog<?> edit(IMultiValue<?> value, Shell shell, IInstanz parent, Messages messages) {
		switch (value.getType()) {
		case MULTI_STRING:
			return new MultiStringValueDialog(shell, (MultiStringValue) value, parent, messages);
		case MULTI_INTEGER:
			return new MultiIntegerValueDialog(shell, (MultiIntegerValue) value, parent, messages);
		case MULTI_BOOLEAN:
			return new MultiBooleanValueDialog(shell, (MultiBooleanValue) value, parent, messages);
		case MULTI_FLOAT:
			return new MultiFloatValueDialog(shell, (MultiFloatValue) value, parent, messages);
		case MULTI_INSTANZ:
			return new MultiInstanzValueDialog(shell, (MultiInstanzValue) value, parent, messages);
		default:
			throw new IllegalArgumentException("Unexpected value: " + value.getType());
		}
	}
}
