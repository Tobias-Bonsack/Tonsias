package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.ui.i18n.Messages;

public class IntegerValueDialog extends AValueDialog<SingleIntegerValue, Text> {

	public IntegerValueDialog(Shell parentShell, SingleIntegerValue stringValue, IInstanz instanz, Messages messages) {
		super(parentShell, stringValue, instanz, messages);
	}

	public IntegerValueDialog(Shell parentShell, IInstanz instanz, Messages messages) {
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

		_valueControl.addModifyListener(event -> refreshOkButton());

		return control;
	}

	@Override
	protected void okPressed() {
		if (_value.isEmpty()) {
			_value = Optional.of(//
					_sVService.createNew(SingleIntegerValue.class, //
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
	 * The rule of the type itself, asked rather than restated, so the button cannot
	 * offer what {@link SingleIntegerValue#tryToSetValue} would then discard - the
	 * empty field a new value opens on included.
	 */
	@Override
	protected boolean isValueAcceptable() {
		return SingleIntegerValue.accepts(_valueControl.getText());
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_INTEGER;
	}
}
