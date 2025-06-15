package de.tonsias.basis.ui.dialog;

import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;

public abstract class AValueDialog<T extends ISingleValue<?>> extends Dialog {

	IKeyService _keyService = OsgiUtil.getService(IKeyService.class);

	ISingleValueService _sVService = OsgiUtil.getService(ISingleValueService.class);

	IInstanzService _iService = OsgiUtil.getService(IInstanzService.class);

	Optional<T> _value;

	IInstanz _instanz;

	Text _nameText;

	Text _valueText;

	SingleValueType _type;

	private Messages _messages;

	protected AValueDialog(Shell parentShell, T singleValue, IInstanz parent, Messages messages) {
		super(parentShell);
		_messages = messages;
		_value = Optional.ofNullable(singleValue);
		_instanz = parent;
		_type = getType();
	}

	abstract SingleValueType getType();
	
	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point point = new Point(400, 600);
		getShell().setMinimumSize(point);
		return point;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
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

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(IDialogConstants.CANCEL_ID).setText(_messages.constant_cancel);
		return buttonBar;
	}

	private void createInstanzPart(Composite composite) {
		Label instanzLabel = LabelFactory.newLabel(SWT.None).text(_messages.dialog_value_instanzSide).create(composite);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(instanzLabel);

		Label keyLabel = LabelFactory.newLabel(SWT.None).text(_messages.constant_key).create(composite);
		GridDataFactory.fillDefaults().applyTo(keyLabel);

		String keyString = _value.map(v -> v.getOwnKey()).orElse(_keyService.previewNextKey());
		Text keyText = TextFactory.newText(SWT.None).text(keyString).enabled(false).create(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(keyText);

		Label nameLabel = LabelFactory.newLabel(SWT.None).text(_messages.constant_name).create(composite);
		GridDataFactory.fillDefaults().applyTo(nameLabel);

		String name = _instanz.getSingleValues(_type).getOrDefault(keyString, "");
		_nameText = TextFactory.newText(SWT.SEARCH).text(name).enabled(true).create(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_nameText);
		_nameText.addModifyListener(modifyEvent -> {
			BiMap<String, String> biMap = _instanz.getSingleValues(_type);
			if (biMap.inverse().containsKey(_nameText.getText())) {
				_nameText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				_nameText.setMessage(_messages.dialog_value_usedName);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			} else {
				_nameText.setBackground(null);
				_nameText.setMessage("");
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});
	}

	private void createSingleValuePart(Composite composite) {
		Label singleValueLabel = LabelFactory.newLabel(SWT.None).text(_messages.dialog_value_valueSide)
				.create(composite);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(singleValueLabel);

		Label valueLabel = LabelFactory.newLabel(SWT.None).text(_messages.constant_singleValue).create(composite);
		GridDataFactory.fillDefaults().applyTo(valueLabel);

		String valueString = _value.map(v -> v.getValue().toString()).orElse("");
		_valueText = getValueText(composite, valueString);
	}

	protected Text getValueText(Composite composite, String valueString) {
		return TextFactory.newText(SWT.None).text(valueString).layoutData(GridDataFactory.fillDefaults().grab(true, false).create()).enabled(true).create(composite);
	}

	public T getSingleValue() {
		return _value.get();
	}

}
