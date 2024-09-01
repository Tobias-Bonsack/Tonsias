package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.interfaces.IInstanz;

public class IntegerValueDialog extends AValueDialog<SingleIntegerValue> {

	public IntegerValueDialog(Shell parentShell, SingleIntegerValue stringValue, IInstanz instanz) {
		super(parentShell, stringValue, instanz);
	}

	public IntegerValueDialog(Shell parentShell, IInstanz instanz) {
		this(parentShell, null, instanz);
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
			_value = Optional.of(_sVService.createNew(SingleIntegerValue.class, _instanz, _nameText.getText()));
		} else {
			_instanz.getSingleValues(_type).put(_value.get().getOwnKey(), _nameText.getText());
		}

		_value.get().setValue(Integer.valueOf(_valueText.getText()));
		super.okPressed();
	}

	private boolean isInteger(String str) {
		return str != null && str.matches("-?\\d+");
	}

	@Override
	SingleValueTypes getType() {
		return SingleValueTypes.SINGLE_INTEGER;
	}
}
