package de.tonsias.basis.ui.dialog;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiBooleanValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class MultiBooleanValueDialog extends AMultiValueDialog<MultiBooleanValue> {

	public MultiBooleanValueDialog(Shell parentShell, IInstanz parent, Messages messages) {
		this(parentShell, null, parent, messages);
	}

	public MultiBooleanValueDialog(Shell parentShell, MultiBooleanValue value, IInstanz parent, Messages messages) {
		super(parentShell, value, parent, messages);
	}

	@Override
	MultiValueType getType() {
		return MultiValueType.MULTI_BOOLEAN;
	}

	@Override
	protected Class<MultiBooleanValue> getValueClass() {
		return MultiBooleanValue.class;
	}
}