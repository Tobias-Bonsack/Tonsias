package de.tonsias.basis.ui.dialog;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class StringValueDialog extends AValueDialog<SingleStringValue, Text> {

	public StringValueDialog(Shell parentShell, SingleStringValue stringValue, IInstanz instanz, Messages messages) {
		super(parentShell, stringValue, instanz, messages);
	}

	public StringValueDialog(Shell shell, IInstanz parentObject, Messages messages) {
		this(shell, null, parentObject, messages);
	}

	@Override
	protected Object getEnteredValue() {
		return _valueControl.getText();
	}

	@Override
	protected Class<SingleStringValue> getValueClass() {
		return SingleStringValue.class;
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_STRING;
	}
	
	@Override
	protected Text createValueControl(Composite composite, String valueString) {
		Text text = TextFactory.newText(SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL).layoutData(GridDataFactory.fillDefaults().grab(true, true).create()).text(valueString).enabled(true).create(composite);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR && e.stateMask == SWT.CONTROL) {
					e.doit = false;
					okPressed();
				}
			}
		});
		return text;
	}
}
