package de.tonsias.basis.ui.dialog;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleBooleanValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class BooleanValueDialog extends AValueDialog<SingleBooleanValue, Button> {

	public BooleanValueDialog(Shell parentShell, SingleBooleanValue booleanValue, IInstanz instanz, Messages messages) {
		super(parentShell, booleanValue, instanz, messages);
	}

	public BooleanValueDialog(Shell parentShell, IInstanz instanz, Messages messages) {
		this(parentShell, null, instanz, messages);
	}

	/**
	 * A check box cannot hold an invalid state, so unlike the other dialogs this one
	 * needs no validating listener on top of the name check of the base class.
	 */
	@Override
	protected Button createValueControl(Composite composite, String valueString) {
		Button check = ButtonFactory.newButton(SWT.CHECK)//
				.text("")//
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
				.create(composite);
		// empty for a new value, which parses to false - the default of the model too
		check.setSelection(Boolean.parseBoolean(valueString));
		return check;
	}

	@Override
	protected Object getEnteredValue() {
		return _valueControl.getSelection();
	}

	@Override
	protected Class<SingleBooleanValue> getValueClass() {
		return SingleBooleanValue.class;
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_BOOLEAN;
	}
}
