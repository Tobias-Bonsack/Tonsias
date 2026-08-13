package de.tonsias.basis.ui.dialog;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class MultiFloatValueDialog extends AMultiValueDialog<MultiFloatValue> {

	public MultiFloatValueDialog(Shell parentShell, IInstanz parent, Messages messages) {
		this(parentShell, null, parent, messages);
	}

	public MultiFloatValueDialog(Shell parentShell, MultiFloatValue value, IInstanz parent, Messages messages) {
		super(parentShell, value, parent, messages);
	}

	@Override
	MultiValueType getType() {
		return MultiValueType.MULTI_FLOAT;
	}

	@Override
	protected Class<MultiFloatValue> getValueClass() {
		return MultiFloatValue.class;
	}
}