package de.tonsias.basis.ui.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;

public class StringValueDialog extends Dialog {

	private SingleStringValue _value;

	private IInstanz _instanz;

	private Text _nameText;

	private Text _valueText;

	public StringValueDialog(Shell parentShell, SingleStringValue value, IInstanz instanz) {
		super(parentShell);
		_value = value;
		_instanz = instanz;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 10).extendedMargins(10, 10, 10, 10)
				.applyTo(composite);

		createInstanzPart(composite);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(separator);

		createSingleValuePart(composite);
		return composite;
	}

	private void createInstanzPart(Composite composite) {
		Label instanzLabel = LabelFactory.newLabel(SWT.None).text("Instanz-Seite").create(composite);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(instanzLabel);

		Label keyLabel = LabelFactory.newLabel(SWT.None).text("Key").create(composite);
		GridDataFactory.fillDefaults().applyTo(keyLabel);
		Text keyText = TextFactory.newText(SWT.None).text(_value.getOwnKey()).enabled(false).create(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(keyText);

		Label nameLabel = LabelFactory.newLabel(SWT.None).text("Name").create(composite);
		GridDataFactory.fillDefaults().applyTo(nameLabel);
		String name = _instanz.getSingleValues(SingleValueTypes.SINGLE_STRING).get(_value.getOwnKey());
		_nameText = TextFactory.newText(SWT.SEARCH).text(name).enabled(true).create(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_nameText);
		_nameText.addModifyListener(modifyEvent -> {
			BiMap<String, String> biMap = _instanz.getSingleValues(SingleValueTypes.SINGLE_STRING);
			if (biMap.inverse().containsKey(_nameText.getText())) {
				_nameText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				_nameText.setMessage("Name bereits genutzt!");
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			} else {
				_nameText.setBackground(null);
				_nameText.setMessage("");
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});
	}

	private void createSingleValuePart(Composite composite) {
		Label singleValueLabel = LabelFactory.newLabel(SWT.None).text("Value-Seite").create(composite);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(singleValueLabel);

		Label valueLabel = LabelFactory.newLabel(SWT.None).text("Value").create(composite);
		GridDataFactory.fillDefaults().applyTo(valueLabel);
		_valueText = TextFactory.newText(SWT.None).text(_value.getValue()).enabled(true).create(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_valueText);
	}

	@Override
	protected void okPressed() {
		_value.setValue(_valueText.getText());
		_instanz.getSingleValues(SingleValueTypes.SINGLE_STRING).put(_value.getOwnKey(), _nameText.getText());
		super.okPressed();
	}

}
