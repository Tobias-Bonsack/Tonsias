package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class IntegerValueDialog extends AValueDialog<SingleIntegerValue> {

	public IntegerValueDialog(Shell parentShell, SingleIntegerValue stringValue, IInstanz instanz, Messages messages) {
		super(parentShell, stringValue, instanz, messages);
	}

	public IntegerValueDialog(Shell parentShell, IInstanz instanz, Messages messages) {
		this(parentShell, null, instanz, messages);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);

		_valueText.addModifyListener(event -> {
			if (_valueText.getText().isBlank() || !isInteger(_valueText.getText())) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			} else if (isInteger(_valueText.getText())) {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});

		return control;
	}

	@Override
	protected void okPressed() {
		if (_value.isEmpty()) {
			_value = Optional.of(//
					_sVService.createNew(SingleIntegerValue.class, _instanz.getOwnKey(), _valueText.getText()));
			_iService.putSingleValue(_instanz.getOwnKey(), _type, _value.get().getOwnKey(), _nameText.getText());
		} else {
			_iService.changeSingleValueName(_instanz.getOwnKey(), _type, _value.get().getOwnKey(), _nameText.getText());
		}
		super.okPressed();
	}

	private boolean isInteger(String str) {
		return str != null && str.matches("-?\\d+");
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_INTEGER;
	}
}
