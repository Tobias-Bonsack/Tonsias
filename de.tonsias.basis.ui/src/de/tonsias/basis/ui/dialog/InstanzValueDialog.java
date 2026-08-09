package de.tonsias.basis.ui.dialog;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.logic.part.InstanzChoices.Choice;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * The dialog of a relation. Its value is not typed but chosen: the combo box
 * holds the instanzen of the model, and what the value stores is the key behind
 * the selected entry.
 * <p>
 * {@link BooleanValueDialog} is the shape this follows - the other one whose
 * value control is not a {@code Text}.
 * </p>
 */
public class InstanzValueDialog extends AValueDialog<SingleInstanzValue, Combo> {

	/** in the order of the combo box entries, so the index maps onto the key */
	private List<Choice> _choices = List.of();

	public InstanzValueDialog(Shell parentShell, SingleInstanzValue instanzValue, IInstanz instanz,
			Messages messages) {
		super(parentShell, instanzValue, instanz, messages);
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
	protected Combo createValueControl(Composite composite, String valueString) {
		_choices = new InstanzChoices(_iService, _sVService, OsgiUtil.getService(IBasicPreferenceService.class))
				.choices();

		Combo combo = new Combo(composite, SWT.READ_ONLY);
		combo.setItems(_choices.stream().map(Choice::_label).toArray(String[]::new));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(combo);

		for (int i = 0; i < _choices.size(); i++) {
			if (_choices.get(i)._key().equals(valueString)) {
				combo.select(i);
				break;
			}
		}
		return combo;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);

		// a read-only combo is never typed into, so it is the selection that has to put
		// the question to refreshOkButton - the same job the modify listener of the
		// numeric dialogs does
		_valueControl.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> refreshOkButton()));

		return control;
	}

	/**
	 * The key behind the selection, and the empty string while there is none -
	 * which is what a new dialog opens on and what {@link #isValueAcceptable()}
	 * refuses.
	 */
	@Override
	protected Object getEnteredValue() {
		int index = _valueControl.getSelectionIndex();
		return index < 0 ? "" : _choices.get(index)._key();
	}

	@Override
	protected Class<SingleInstanzValue> getValueClass() {
		return SingleInstanzValue.class;
	}

	/**
	 * The rule of the type itself, asked rather than restated, so the button cannot
	 * offer what {@link SingleInstanzValue#tryToSetValue} would then discard - the
	 * empty selection a new reference opens on included.
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
