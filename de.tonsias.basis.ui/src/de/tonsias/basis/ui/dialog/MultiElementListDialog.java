package de.tonsias.basis.ui.dialog;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.widget.MultiElementList;

/**
 * A {@link MultiElementList} on its own, for the place that cannot show the list
 * where it stands: the value column of the create dialog, which is a table cell.
 * The same part {@link de.tonsias.basis.ui.widget.InstanzSelectionDialog} plays
 * for a relation - a cell holds one line, and a list is not one line.
 * <p>
 * It is the list alone: no name, no key, and nothing written anywhere. The
 * instanz the elements will belong to does not exist yet when this opens, which
 * is exactly why {@link AMultiValueDialog} cannot stand in for it.
 * </p>
 *
 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/88">#88</a>
 */
public class MultiElementListDialog extends Dialog {

	private final ValueContentType _content;

	private final Collection<?> _initialElements;

	private final Messages _messages;

	private final Supplier<InstanzChoices> _choices;

	private MultiElementList _list;

	/** read off the widget before it is disposed, which closing the dialog does */
	private List<Object> _elements = List.of();

	/**
	 * @param content  which of the five the elements are, the one thing the widget
	 *                 needs to know
	 * @param elements what the cell holds today, empty for a cell nobody edited yet
	 */
	public MultiElementListDialog(Shell parentShell, ValueContentType content, Collection<?> elements,
			Messages messages, Supplier<InstanzChoices> choices) {
		super(parentShell);
		_content = content;
		_initialElements = elements;
		_messages = messages;
		_choices = choices;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(_messages.dialog_value_elements);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, 400);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		_list = new MultiElementList(composite, _content, _messages, _choices);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_list);
		_list.setElements(_initialElements);

		return composite;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		getButton(IDialogConstants.CANCEL_ID).setText(_messages.constant_cancel);
		return buttonBar;
	}

	/**
	 * OK is always offered: the widget refuses at the door what the type would not
	 * read and what is already in the list, so whatever stands in it is a list the
	 * type takes - the empty one included, the way it is in
	 * {@link AMultiValueDialog}.
	 */
	@Override
	protected void okPressed() {
		_elements = _list.getElements();
		super.okPressed();
	}

	/** @return the list as OK left it, empty unless OK was pressed */
	public List<Object> getElements() {
		return _elements;
	}

	/**
	 * The widget the elements are entered with, so a test can drive it the way a
	 * user would rather than reach past it into the dialog.
	 */
	public MultiElementList getElementList() {
		return _list;
	}
}
