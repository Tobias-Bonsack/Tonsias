package de.tonsias.basis.ui.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic;
import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic.TableRecord;
import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.basis.ui.dialog.CreateInstanzDialog;
import de.tonsias.basis.ui.dialog.MultiElementListDialog;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * The value column of the "create instanz" dialog, on a real {@link Display}.
 * <p>
 * The column is one column for all ten types and its editor is not: eight of
 * them are typed into the cell, a relation is chosen from a tree, and a list is
 * edited in a dialog of its own. A {@link TextCellEditor} takes a
 * {@code String} and nothing else, so a row holding a list handed to one is an
 * assertion inside JFace the moment the cell is clicked - which is what
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/88">#88</a> was.
 * </p>
 */
public class CreateInstanzDialogSystemTest {

	private static Display _display;

	private static Shell _shell;

	private IInstanz _parent;

	private Messages _messages;

	private final List<Dialog> _built = new ArrayList<>();

	@BeforeAll
	static void beforeAll() {
		_display = Display.getDefault();
		_shell = new Shell(_display);
	}

	@AfterAll
	static void afterAll() {
		if (_shell != null && !_shell.isDisposed()) {
			_shell.dispose();
		}
	}

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		// the runtime is shared by the whole bundle, so every dialog creates below an
		// instanz of its own rather than below the root everyone else writes to
		_parent = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		_messages = messages();
	}

	@AfterEach
	void afterEach() {
		_built.forEach(Dialog::close);
		_built.clear();
		ProductRuntime.flushDeltas();
	}

	// ---------- which editor a row gets ----------

	/**
	 * The row the crash came out of: a list under any of the four contents that are
	 * typed one by one. What the cell holds is the list, and it survives the editor
	 * unchanged - it is what {@code createNew} is handed on OK.
	 */
	@Test
	void testValueCellEditor_aListRowTakesTheListItHolds() {
		CreateInstanzDialog dialog = built();
		TableRecord row = rowOfType(dialog, MultiValueType.MULTI_STRING);

		CellEditor editor = dialog.valueCellEditor(row);
		editor.setValue(List.of("a", "b"));

		assertThat(editor, is(not(instanceOf(TextCellEditor.class))));
		assertThat(editor.getValue(), is(List.of("a", "b")));
	}

	/**
	 * A list of relations is a list first: it is edited element by element in the
	 * list dialog, not in the chooser that picks the one target of a single
	 * relation.
	 */
	@Test
	void testValueCellEditor_aListOfRelationsIsAListAndNotAChooser() {
		CreateInstanzDialog dialog = built();
		TableRecord row = rowOfType(dialog, MultiValueType.MULTI_INSTANZ);
		String target = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND).getOwnKey();

		CellEditor editor = dialog.valueCellEditor(row);
		editor.setValue(List.of(target));

		assertThat(editor.getValue(), is(List.of(target)));
	}

	@Test
	void testValueCellEditor_aTypedRowIsStillTypedIntoTheCell() {
		CreateInstanzDialog dialog = built();
		TableRecord row = rowOfType(dialog, SingleValueType.SINGLE_STRING);

		CellEditor editor = dialog.valueCellEditor(row);
		editor.setValue("content");

		assertThat(editor, is(instanceOf(TextCellEditor.class)));
		assertThat(editor.getValue(), is("content"));
	}

	/** @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/75">#75</a> */
	@Test
	void testValueCellEditor_aRelationIsStillChosenFromTheTree() {
		CreateInstanzDialog dialog = built();
		TableRecord row = rowOfType(dialog, SingleValueType.SINGLE_INSTANZ);

		CellEditor editor = dialog.valueCellEditor(row);

		assertThat(editor, is(instanceOf(DialogCellEditor.class)));
	}

	// ---------- the dialog the cell opens ----------

	@Test
	void testListDialog_opensHoldingWhatTheCellHolds() {
		MultiElementListDialog dialog = built(listDialog(List.of("a", "b")));

		assertThat(dialog.getElementList().getElements(), contains("a", "b"));
	}

	@Test
	void testListDialog_okHandsTheWholeListBack() {
		MultiElementListDialog dialog = built(listDialog(List.of("a")));
		dialog.getElementList().setElements(List.of("a", "b"));

		press(okButton(dialog));

		assertThat(dialog.getElements(), contains("a", "b"));
	}

	/**
	 * Cancelling hands nothing back - the cell editor keeps what the cell held, so
	 * an empty answer here would empty the row.
	 */
	@Test
	void testListDialog_withoutOkNothingIsHandedBack() {
		MultiElementListDialog dialog = built(listDialog(List.of("a")));

		assertThat(dialog.getElements(), is(empty()));
	}

	// ---------- helpers ----------

	private CreateInstanzDialog built() {
		CreateInstanzDialog dialog = new CreateInstanzDialog(_shell, _parent, _messages);
		dialog.create();
		_built.add(dialog);
		return dialog;
	}

	private <D extends Dialog> D built(D dialog) {
		dialog.create();
		_built.add(dialog);
		return dialog;
	}

	private MultiElementListDialog listDialog(List<?> elements) {
		return new MultiElementListDialog(_shell, ValueContentType.STRING, elements, _messages, this::choices);
	}

	private InstanzChoices choices() {
		return new InstanzChoices(ProductRuntime.instanzService(), ProductRuntime.singleValueService(),
				ProductRuntime.preferenceService());
	}

	/** the first row of the table, switched over the way the type column does it */
	private TableRecord rowOfType(CreateInstanzDialog dialog, IValueType type) {
		CreateInstanzDialogLogic logic = dialog.getLogic();
		TableRecord row = logic.getInput().iterator().next();
		logic.setType(row, type);
		return row;
	}

	private Button okButton(Dialog dialog) {
		return controls(dialog.getShell()).stream()//
				.filter(Button.class::isInstance).map(Button.class::cast)//
				.filter(button -> Integer.valueOf(IDialogConstants.OK_ID).equals(button.getData()))//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no OK button in " + dialog.getClass().getSimpleName()));
	}

	private void press(Button button) {
		button.notifyListeners(SWT.Selection, new Event());
	}

	/** every control below {@code root}, in the order the dialog created them */
	private List<Control> controls(Composite root) {
		List<Control> all = new ArrayList<>();
		for (Control child : root.getChildren()) {
			all.add(child);
			if (child instanceof Composite composite) {
				all.addAll(controls(composite));
			}
		}
		return all;
	}

	/**
	 * Everything the dialog puts onto a widget, filled in rather than translated -
	 * {@code TranslationCoverageTest} is what holds the real texts to their keys.
	 * The type column offers all ten, so all ten labels have to be there.
	 */
	private Messages messages() {
		Messages messages = new Messages();
		messages.constant_add = "Add";
		messages.constant_cancel = "Cancel";
		messages.constant_down = "Down";
		messages.constant_filter = "Filter";
		messages.constant_key = "Key";
		messages.constant_name = "Name";
		messages.constant_noInstanz = "nothing chosen";
		messages.constant_parameterName = "Parameter";
		messages.constant_remove = "Remove";
		messages.constant_type = "Type";
		messages.constant_type_boolean = "Boolean";
		messages.constant_type_float = "Float";
		messages.constant_type_instanz = "Instanz";
		messages.constant_type_integer = "Integer";
		messages.constant_type_multi_boolean = "Boolean list";
		messages.constant_type_multi_float = "Float list";
		messages.constant_type_multi_instanz = "Instanz list";
		messages.constant_type_multi_integer = "Integer list";
		messages.constant_type_multi_string = "String list";
		messages.constant_type_string = "String";
		messages.constant_up = "Up";
		messages.constant_value = "Value";
		messages.dialog_createInstanz_title = "Create instanz";
		messages.dialog_value_duplicateElement = "element already in the list";
		messages.dialog_value_elements = "Elements";
		return messages;
	}
}
