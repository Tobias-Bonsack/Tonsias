package de.tonsias.basis.ui.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.logic.part.InstanzChoices.Choice;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.widget.InstanzChooser;
import de.tonsias.basis.ui.widget.InstanzSelectionDialog;

/**
 * The chooser on its own, for the two places that have no room to draw a tree
 * where they stand: the Instanz View, which has one line per attribute, and the
 * value column of the create dialog, which is a table cell.
 * <p>
 * Built the way JFace builds a dialog - {@code Window.create()} runs
 * {@code createDialogArea} and {@code createButtonBar}, and nothing else
 * happens before the user sees it. The widgets are then found on the shell and
 * read the way a user would see them.
 * </p>
 * <p>
 * It replaces a {@code Combo} holding every instanz of the model, flattened:
 * usable in a test model, unusable in a real one, see
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/76">#76</a>.
 * </p>
 */
public class InstanzSelectionDialogSystemTest {

	private static Display _display;

	private static Shell _shell;

	private IInstanz _branch;

	private IInstanz _leaf;

	/** a branch next to the one below, so the filter has something it must hide */
	private IInstanz _sibling;

	private InstanzChoices _choices;

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
		// the runtime is shared by the whole bundle, so this test builds a branch of
		// its own rather than reading whatever else is below the root
		_branch = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		_leaf = ProductRuntime.instanzService().createInstanz(_branch.getOwnKey(), Type.SEND);
		_sibling = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		_choices = new InstanzChoices(ProductRuntime.instanzService(), ProductRuntime.singleValueService(),
				ProductRuntime.preferenceService());
		_messages = messages();
	}

	@AfterEach
	void afterEach() {
		_built.forEach(Dialog::close);
		_built.clear();
		ProductRuntime.flushDeltas();
	}

	/** nothing chosen is no answer, so the dialog may not offer to give one */
	@Test
	void testCreate_withoutAPreselection_okIsDisabled() {
		InstanzSelectionDialog dialog = built(new InstanzSelectionDialog(_shell, _choices.tree(), _messages, ""));

		assertThat(okButton(dialog).isEnabled(), is(false));
		assertThat(dialog.getSelectedKey(), is(""));
	}

	/**
	 * Opened on a relation that already points somewhere, the dialog offers OK
	 * straight away - keeping what is there must not need a second choice.
	 */
	@Test
	void testCreate_withAPreselection_okIsEnabledAndTheTargetIsSelected() {
		InstanzSelectionDialog dialog = built(
				new InstanzSelectionDialog(_shell, _choices.tree(), _messages, _leaf.getOwnKey()));

		assertThat(dialog.getChooser().getSelectedKey(), is(_leaf.getOwnKey()));
		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	/** a key the tree does not hold is no selection - a deleted target reads so */
	@Test
	void testCreate_withAPreselectionTheTreeDoesNotHold_okIsDisabled() {
		InstanzSelectionDialog dialog = built(
				new InstanzSelectionDialog(_shell, _choices.tree(), _messages, "no-such-key"));

		assertThat(dialog.getChooser().getSelectedKey(), is(""));
		assertThat(okButton(dialog).isEnabled(), is(false));
	}

	@Test
	void testSelection_choosingAnInstanzEnablesOk() {
		InstanzSelectionDialog dialog = built(new InstanzSelectionDialog(_shell, _choices.tree(), _messages, ""));

		choose(dialog.getChooser(), _branch);

		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	/**
	 * What the caller reads afterwards: the key is what a relation stores, the
	 * label is what the caller puts on screen. Both are taken before OK closes the
	 * dialog and disposes the tree they came from.
	 */
	@Test
	void testOkPressed_handsBackTheKeyAndTheLabelOfTheChosenInstanz() {
		InstanzSelectionDialog dialog = built(new InstanzSelectionDialog(_shell, _choices.tree(), _messages, ""));
		choose(dialog.getChooser(), _leaf);

		press(okButton(dialog));

		assertThat(dialog.getSelectedKey(), is(_leaf.getOwnKey()));
		// no MODEL_VIEW_TEXT value on this instanz, so it reads as itself
		assertThat(dialog.getSelectedLabel(), is(_choices.labelOf(_leaf)));
	}

	/** the model is drawn as the tree it is, not flattened into a list */
	@Test
	void testCreate_showsTheModelAsATree() {
		InstanzSelectionDialog dialog = built(new InstanzSelectionDialog(_shell, _choices.tree(), _messages, ""));

		Choice branch = dialog.getChooser().root().find(_branch.getOwnKey()).orElseThrow();

		assertThat(branch._children().stream().map(Choice::_key).toList(), hasItem(_leaf.getOwnKey()));
	}

	/** and the filter is what makes that tree usable once the model is large */
	@Test
	void testFilter_narrowsTheTreeToWhatIsNamed() {
		InstanzSelectionDialog dialog = built(new InstanzSelectionDialog(_shell, _choices.tree(), _messages, ""));
		InstanzChooser chooser = dialog.getChooser();

		chooser.getFilterText().setText(_leaf.getOwnKey());

		List<String> shown = visibleKeys(chooser);
		assertThat("the match", shown, hasItem(_leaf.getOwnKey()));
		assertThat("and the branch above it", shown, hasItem(_branch.getOwnKey()));
		assertThat("but not a sibling branch", shown, not(hasItem(_sibling.getOwnKey())));
	}

	// ---------- building the dialog and finding its widgets ----------

	private <D extends Dialog> D built(D dialog) {
		dialog.create();
		_built.add(dialog);
		return dialog;
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

	/**
	 * Chooses an instanz the way a click does: JFace turns a click into a selection
	 * on the viewer, and it is the viewer that tells the listener judging the OK
	 * button.
	 */
	private void choose(InstanzChooser chooser, IInstanz instanz) {
		Choice choice = chooser.root().find(instanz.getOwnKey())//
				.orElseThrow(() -> new AssertionError("'" + instanz + "' is not in the tree"));
		chooser.getViewer().setSelection(new StructuredSelection(choice), true);
	}

	private List<String> visibleKeys(InstanzChooser chooser) {
		List<String> keys = new ArrayList<>();
		collectKeys(chooser.getViewer().getTree().getItems(), keys);
		return keys;
	}

	private void collectKeys(TreeItem[] items, List<String> keys) {
		for (TreeItem item : items) {
			if (item.getData() instanceof Choice choice) {
				keys.add(choice._key());
			}
			collectKeys(item.getItems(), keys);
		}
	}

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
	 * The dialog puts these onto a shell title and a field placeholder and would
	 * fail on a {@code null}. What they say is not what this test is about -
	 * {@code TranslationCoverageTest} holds the real texts to their keys.
	 */
	private Messages messages() {
		Messages messages = new Messages();
		messages.constant_cancel = "Cancel";
		messages.constant_filter = "Filter";
		messages.dialog_selectInstanz_title = "Choose instanz";
		return messages;
	}
}
