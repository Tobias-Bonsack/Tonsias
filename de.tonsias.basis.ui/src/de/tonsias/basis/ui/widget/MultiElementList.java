package de.tonsias.basis.ui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.jface.widgets.CompositeFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.impl.value.ValueContentRules;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * The one widget every list of values is edited with: a table of what is in the
 * list, one input for what should go in next, and the four buttons that put it
 * there, take it out and move it around.
 * <p>
 * Which input that is comes from the {@link ValueContentType} alone - a text
 * field for the ones that are typed, a check box for the two literals, and the
 * chooser a relation is picked from. Five contents rather than ten types, so the
 * kind of value never enters into it.
 * </p>
 * <p>
 * What the list holds is what the model would hold: the input goes through
 * {@link ValueContentRules} before it lands, so an element is a {@code Float} and
 * not the text somebody typed, and two spellings of one number are one duplicate.
 * </p>
 */
public class MultiElementList extends Composite {

	private final ValueContentType _content;

	private final Messages _messages;

	/** the instanzen a relation can be pointed at, null for every other content */
	private final Supplier<InstanzChoices> _choices;

	private final List<Object> _elements = new ArrayList<>();

	private final TableViewer _viewer;

	private final Control _input;

	private final Label _message;

	private final Button _remove;

	private final Button _up;

	private final Button _down;

	/** the key the chooser last handed back - the relation input's content */
	private String _chosenKey = "";

	private Runnable _onChanged = () -> {
	};

	public MultiElementList(Composite parent, ValueContentType content, Messages messages,
			Supplier<InstanzChoices> choices) {
		super(parent, SWT.NONE);
		_content = content;
		_messages = messages;
		_choices = choices;

		GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 5).applyTo(this);

		Composite inputRow = CompositeFactory.newComposite(SWT.NONE)
				.layout(GridLayoutFactory.fillDefaults().numColumns(2).create()).create(this);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(inputRow);
		_input = createInput(inputRow);
		ButtonFactory.newButton(SWT.PUSH).text(_messages.constant_add).onSelect(event -> addEntered())
				.create(inputRow);

		_message = LabelFactory.newLabel(SWT.NONE).text("").create(this);
		_message.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_message);

		_viewer = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 120).applyTo(_viewer.getTable());
		_viewer.setContentProvider(ArrayContentProvider.getInstance());
		_viewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return labelOf(element);
			}
		});
		_viewer.setInput(_elements);
		_viewer.addSelectionChangedListener(event -> refreshButtons());

		Composite buttons = CompositeFactory.newComposite(SWT.NONE)
				.layout(GridLayoutFactory.fillDefaults().numColumns(1).create()).create(this);
		GridDataFactory.fillDefaults().applyTo(buttons);
		_remove = ButtonFactory.newButton(SWT.PUSH).text(_messages.constant_remove).onSelect(event -> removeSelected())
				.create(buttons);
		_up = ButtonFactory.newButton(SWT.PUSH).text(_messages.constant_up).onSelect(event -> move(-1))
				.create(buttons);
		_down = ButtonFactory.newButton(SWT.PUSH).text(_messages.constant_down).onSelect(event -> move(1))
				.create(buttons);

		refreshButtons();
	}

	private Control createInput(Composite parent) {
		switch (_content) {
		case BOOLEAN:
			Button check = ButtonFactory.newButton(SWT.CHECK).text(_messages.constant_value).create(parent);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(check);
			return check;
		case INSTANZ:
			Button choose = ButtonFactory.newButton(SWT.PUSH).text(_messages.constant_noInstanz)
					.onSelect(event -> chooseTarget()).create(parent);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(choose);
			return choose;
		default:
			Text text = TextFactory.newText(SWT.BORDER).create(parent);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
			return text;
		}
	}

	private void chooseTarget() {
		InstanzSelectionDialog dialog = new InstanzSelectionDialog(getShell(), _choices.get().tree(), _messages,
				_chosenKey);
		if (dialog.open() != Window.OK) {
			return;
		}
		_chosenKey = dialog.getSelectedKey();
		((Button) _input).setText(labelOf(_chosenKey));
	}

	/** what the input holds right now, in whatever shape it holds it */
	private Object enteredValue() {
		switch (_content) {
		case BOOLEAN:
			return Boolean.valueOf(((Button) _input).getSelection());
		case INSTANZ:
			return _chosenKey;
		default:
			return ((Text) _input).getText();
		}
	}

	/**
	 * Takes what the input holds into the list, or says why it did not. A duplicate
	 * is the one refusal worth a word: the model would answer {@code false} and drop
	 * it without a sound.
	 */
	private void addEntered() {
		Optional<Object> element = ValueContentRules.convert(_content, enteredValue());
		if (element.isEmpty() || isEmptyRelation(element.get())) {
			// nothing the type would read, and nothing chosen - the input is where that
			// shows, there is nothing to say about a list it never reached
			_message.setText("");
			return;
		}
		if (_elements.contains(element.get())) {
			_message.setText(_messages.dialog_value_duplicateElement);
			return;
		}

		_message.setText("");
		_elements.add(element.get());
		changed();
	}

	/** a list says "points nowhere" by being empty, so there is no empty element */
	private boolean isEmptyRelation(Object element) {
		return _content == ValueContentType.INSTANZ && "".equals(element);
	}

	private void removeSelected() {
		Object selected = _viewer.getStructuredSelection().getFirstElement();
		if (selected == null) {
			return;
		}
		_elements.remove(selected);
		_message.setText("");
		changed();
	}

	private void move(int offset) {
		Object selected = _viewer.getStructuredSelection().getFirstElement();
		if (selected == null) {
			return;
		}
		int from = _elements.indexOf(selected);
		int to = from + offset;
		if (to < 0 || to >= _elements.size()) {
			return;
		}
		_elements.remove(from);
		_elements.add(to, selected);
		changed();
		_viewer.setSelection(new org.eclipse.jface.viewers.StructuredSelection(selected));
	}

	private void changed() {
		_viewer.refresh();
		refreshButtons();
		_onChanged.run();
	}

	private void refreshButtons() {
		Object selected = _viewer.getStructuredSelection().getFirstElement();
		int index = selected == null ? -1 : _elements.indexOf(selected);
		_remove.setEnabled(index >= 0);
		_up.setEnabled(index > 0);
		_down.setEnabled(index >= 0 && index < _elements.size() - 1);
	}

	/** a relation reads as its target does everywhere else */
	private String labelOf(Object element) {
		if (_content != ValueContentType.INSTANZ) {
			return String.valueOf(element);
		}
		if (_choices == null) {
			return String.valueOf(element);
		}
		return _choices.get().labelOf(String.valueOf(element)).orElse(_messages.constant_noInstanz);
	}

	/** the list as it stands, in the order it stands in */
	public List<Object> getElements() {
		return List.copyOf(_elements);
	}

	public void setElements(Collection<?> elements) {
		_elements.clear();
		_elements.addAll(elements);
		_viewer.refresh();
		refreshButtons();
	}

	/** what to run when the list changed - the dialog re-judges its OK button */
	public void onChanged(Runnable listener) {
		_onChanged = listener;
	}
}
