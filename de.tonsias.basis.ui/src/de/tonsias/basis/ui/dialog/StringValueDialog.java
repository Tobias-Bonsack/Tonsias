package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;

public class StringValueDialog extends AValueDialog<SingleStringValue> {

	public StringValueDialog(Shell parentShell, SingleStringValue stringValue, IInstanz instanz) {
		super(parentShell, stringValue, instanz);
	}

	public StringValueDialog(Shell shell, IInstanz parentObject) {
		this(shell, null, parentObject);
	}

	@Override
	protected void okPressed() {
		if (_value.isEmpty()) {
			_value = Optional.of(//
					_sVService.createNew(SingleStringValue.class, _instanz.getOwnKey(), _valueText.getText()));
		}
		_iService.putSingleValue(_instanz.getOwnKey(), _type, _value.get().getOwnKey(), _valueText.getText());

		super.okPressed();
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_STRING;
	}
}
