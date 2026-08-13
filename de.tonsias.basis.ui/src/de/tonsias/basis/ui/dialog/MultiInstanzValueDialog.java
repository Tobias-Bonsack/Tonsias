package de.tonsias.basis.ui.dialog;

import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.ui.i18n.Messages;

public class MultiInstanzValueDialog extends AMultiValueDialog<MultiInstanzValue> {

	public MultiInstanzValueDialog(Shell parentShell, IInstanz parent, Messages messages) {
		this(parentShell, null, parent, messages);
	}

	public MultiInstanzValueDialog(Shell parentShell, MultiInstanzValue value, IInstanz parent, Messages messages) {
		super(parentShell, value, parent, messages);
	}

	@Override
	MultiValueType getType() {
		return MultiValueType.MULTI_INSTANZ;
	}

	@Override
	protected Class<MultiInstanzValue> getValueClass() {
		return MultiInstanzValue.class;
	}
}