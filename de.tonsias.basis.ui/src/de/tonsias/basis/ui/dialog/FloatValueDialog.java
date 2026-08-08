package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleFloatValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.ui.i18n.Messages;

public class FloatValueDialog extends AValueDialog<SingleFloatValue, Text> {

	public FloatValueDialog(Shell parentShell, SingleFloatValue floatValue, IInstanz instanz, Messages messages) {
		super(parentShell, floatValue, instanz, messages);
	}

	public FloatValueDialog(Shell parentShell, IInstanz instanz, Messages messages) {
		this(parentShell, null, instanz, messages);
	}

	@Override
	protected Text createValueControl(Composite composite, String valueString) {
		return TextFactory.newText(SWT.None).text(valueString)
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).create()).enabled(true).create(composite);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);

		_valueControl.addModifyListener(event -> {
			getButton(IDialogConstants.OK_ID).setEnabled(isFloat(_valueControl.getText()));
		});

		return control;
	}

	@Override
	protected void okPressed() {
		if (_value.isEmpty()) {
			_value = Optional.of(//
					_sVService.createNew(SingleFloatValue.class, //
							_instanz.getOwnKey(), //
							_nameText.getText(), //
							_valueControl.getText(), //
							IEventBrokerBridge.Type.POST//
					));
		} else {
			_iService.changeSingleValueName(_instanz.getOwnKey(), _type, _value.get().getOwnKey(), _nameText.getText(),
					IEventBrokerBridge.Type.POST);
		}
		super.okPressed();
	}

	/**
	 * The rule {@link SingleFloatValue#tryToSetValue} applies, so the button is
	 * disabled for exactly the input that would be discarded.
	 */
	private boolean isFloat(String str) {
		return str != null && str.strip().matches(SingleFloatValue.DECIMAL_PATTERN);
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_FLOAT;
	}
}
