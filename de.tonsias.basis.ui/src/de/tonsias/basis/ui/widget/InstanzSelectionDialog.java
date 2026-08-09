package de.tonsias.basis.ui.widget;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.part.InstanzChoices.Choice;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * An {@link InstanzChooser} on its own, for the two places that cannot show the
 * tree where they stand: the Instanz View, which has one line per attribute,
 * and the value column of the create dialog, which is a table cell.
 * <p>
 * What it hands back is the key of the chosen instanz - the label is the tree's
 * business and is never stored.
 * </p>
 */
public class InstanzSelectionDialog extends Dialog {

	private final Choice _root;

	private final Messages _messages;

	private final String _preselectedKey;

	private InstanzChooser _chooser;

	/** read off the chooser before it is disposed, which closing the dialog does */
	private String _selectedKey = "";

	private String _selectedLabel = "";

	/**
	 * @param root           the model as {@code InstanzChoices.tree()} hands it over
	 * @param preselectedKey where the relation points today, empty for none
	 */
	public InstanzSelectionDialog(Shell parentShell, Choice root, Messages messages, String preselectedKey) {
		super(parentShell);
		_root = root;
		_messages = messages;
		_preselectedKey = preselectedKey == null ? "" : preselectedKey;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(_messages.dialog_selectInstanz_title);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 500);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		_chooser = new InstanzChooser(composite, _root, _messages.constant_filter);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chooser);
		_chooser.setSelectedKey(_preselectedKey);
		_chooser.onSelectionChanged(this::refreshOkButton);

		return composite;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(IDialogConstants.CANCEL_ID).setText(_messages.constant_cancel);

		// the dialog area is built first, so there is a selection to judge by now -
		// opening on a relation that already points somewhere may offer OK straight away
		refreshOkButton();

		return buttonBar;
	}

	private void refreshOkButton() {
		getButton(IDialogConstants.OK_ID).setEnabled(!_chooser.getSelectedKey().isEmpty());
	}

	@Override
	protected void okPressed() {
		_selectedKey = _chooser.getSelectedKey();
		_selectedLabel = _chooser.getSelectedLabel();
		super.okPressed();
	}

	/** @return the key of the chosen instanz, empty unless OK was pressed */
	public String getSelectedKey() {
		return _selectedKey;
	}

	/** @return how that instanz read in the tree, for whoever has to show it */
	public String getSelectedLabel() {
		return _selectedLabel;
	}

	public InstanzChooser getChooser() {
		return _chooser;
	}
}
