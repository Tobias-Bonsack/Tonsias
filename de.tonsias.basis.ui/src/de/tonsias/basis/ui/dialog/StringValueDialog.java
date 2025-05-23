package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.ui.i18n.Messages;

public class StringValueDialog extends AValueDialog<SingleStringValue> {

	public StringValueDialog(Shell parentShell, SingleStringValue stringValue, IInstanz instanz, Messages messages) {
		super(parentShell, stringValue, instanz, messages);
	}

	public StringValueDialog(Shell shell, IInstanz parentObject, Messages messages) {
		this(shell, null, parentObject, messages);
	}

	@Override
	protected void okPressed() {
		if (_value.isEmpty()) {
			_value = Optional.of(//
					_sVService.createNew(SingleStringValue.class, _instanz.getOwnKey(), _valueText.getText()));
			_iService.putSingleValue(_instanz.getOwnKey(), _type, _value.get().getOwnKey(), _nameText.getText(),
					IEventBrokerBridge.Type.POST);
		} else {
			_iService.changeSingleValueName(_instanz.getOwnKey(), _type, _value.get().getOwnKey(), _nameText.getText(),
					IEventBrokerBridge.Type.POST);
		}

		super.okPressed();
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_STRING;
	}
}
