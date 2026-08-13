package de.tonsias.basis.ui.dialog;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class MultiStringValueDialog extends AMultiValueDialog<MultiStringValue> {

	public MultiStringValueDialog(Shell parentShell, IInstanz parent, Messages messages) {
		this(parentShell, null, parent, messages);
	}

	public MultiStringValueDialog(Shell parentShell, MultiStringValue value, IInstanz parent, Messages messages) {
		super(parentShell, value, parent, messages);
	}

	@Override
	MultiValueType getType() {
		return MultiValueType.MULTI_STRING;
	}

	@Override
	protected Class<MultiStringValue> getValueClass() {
		return MultiStringValue.class;
	}
}