package de.tonsias.basis.ui.dialog;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class MultiIntegerValueDialog extends AMultiValueDialog<MultiIntegerValue> {

	public MultiIntegerValueDialog(Shell parentShell, IInstanz parent, Messages messages) {
		this(parentShell, null, parent, messages);
	}

	public MultiIntegerValueDialog(Shell parentShell, MultiIntegerValue value, IInstanz parent, Messages messages) {
		super(parentShell, value, parent, messages);
	}

	@Override
	MultiValueType getType() {
		return MultiValueType.MULTI_INTEGER;
	}

	@Override
	protected Class<MultiIntegerValue> getValueClass() {
		return MultiIntegerValue.class;
	}
}