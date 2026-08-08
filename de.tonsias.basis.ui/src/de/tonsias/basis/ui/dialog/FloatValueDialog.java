package de.tonsias.basis.ui.dialog;

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

		_valueControl.addModifyListener(event -> refreshOkButton());

		return control;
	}

	@Override
	protected Object getEnteredValue() {
		return _valueControl.getText();
	}

	@Override
	protected Class<SingleFloatValue> getValueClass() {
		return SingleFloatValue.class;
	}

	/**
	 * The rule of the type itself, asked rather than restated, so the button cannot
	 * offer what {@link SingleFloatValue#tryToSetValue} would then discard - the
	 * empty field a new value opens on included.
	 */
	@Override
	protected boolean isValueAcceptable() {
		return SingleFloatValue.accepts(_valueControl.getText());
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_FLOAT;
	}
}
