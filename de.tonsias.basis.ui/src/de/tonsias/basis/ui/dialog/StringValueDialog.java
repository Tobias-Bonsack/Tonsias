package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.SingleValueTypes;
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
			_value = Optional.of(_sVService.createNew(SingleStringValue.class, _instanz, _nameText.getText()));
		} else {
			_instanz.getSingleValues(_type).put(_value.get().getOwnKey(), _nameText.getText());
		}

		_value.get().setValue(_valueText.getText());
		super.okPressed();
	}

	@Override
	SingleValueTypes getType() {
		return SingleValueTypes.SINGLE_STRING;
	}
}
