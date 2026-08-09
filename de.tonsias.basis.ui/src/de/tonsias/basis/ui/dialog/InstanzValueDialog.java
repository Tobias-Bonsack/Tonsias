package de.tonsias.basis.ui.dialog;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.widget.InstanzChooser;

/**
 * The dialog of a relation. Its value is not typed but chosen: the tree holds
 * the instanzen of the model, and what the value stores is the key behind the
 * selected entry.
 * <p>
 * {@link BooleanValueDialog} is the shape this follows - the other one whose
 * value control is not a {@code Text}. The chooser sits in the dialog itself
 * rather than behind a button: this is already a dialog, and one that opens
 * another to fill a single field would ask for two OKs for one decision.
 * </p>
 */
public class InstanzValueDialog extends AValueDialog<SingleInstanzValue, InstanzChooser> {

	private final Messages _dialogMessages;

	public InstanzValueDialog(Shell parentShell, SingleInstanzValue instanzValue, IInstanz instanz,
			Messages messages) {
		super(parentShell, instanzValue, instanz, messages);
		_dialogMessages = messages;
	}

	public InstanzValueDialog(Shell parentShell, IInstanz instanz, Messages messages) {
		this(parentShell, null, instanz, messages);
	}

	/**
	 * @param valueString the stored target key, empty for a new reference - the
	 *                    matching entry is preselected, and nothing is when the key
	 *                    points at an instanz the walk did not reach
	 */
	@Override
	protected InstanzChooser createValueControl(Composite composite, String valueString) {
		InstanzChoices choices = new InstanzChoices(_iService, _sVService,
				OsgiUtil.getService(IBasicPreferenceService.class));

		InstanzChooser chooser = new InstanzChooser(composite, choices.tree(), _dialogMessages.constant_filter);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 250).applyTo(chooser);
		chooser.setSelectedKey(valueString);
		return chooser;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);

		// a tree is never typed into, so it is the selection that has to put the
		// question to refreshOkButton - the same job the modify listener of the
		// numeric dialogs does
		_valueControl.onSelectionChanged(this::refreshOkButton);

		return control;
	}

	/**
	 * The key behind the selection, and the empty string while there is none -
	 * which is what a new dialog opens on and what {@link #isValueAcceptable()}
	 * refuses.
	 */
	@Override
	protected Object getEnteredValue() {
		return _valueControl.getSelectedKey();
	}

	@Override
	protected Class<SingleInstanzValue> getValueClass() {
		return SingleInstanzValue.class;
	}

	/**
	 * The rule of the type itself, asked rather than restated, so the button cannot
	 * offer what {@link SingleInstanzValue#tryToSetValue} would then take as
	 * "points nowhere" - the empty selection a new reference opens on included.
	 */
	@Override
	protected boolean isValueAcceptable() {
		return SingleInstanzValue.accepts((String) getEnteredValue());
	}

	@Override
	SingleValueType getType() {
		return SingleValueType.SINGLE_INSTANZ;
	}
}
