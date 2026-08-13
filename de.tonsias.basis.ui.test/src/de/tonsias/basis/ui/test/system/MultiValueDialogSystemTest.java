package de.tonsias.basis.ui.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.basis.ui.dialog.AMultiValueDialog;
import de.tonsias.basis.ui.dialog.MultiIntegerValueDialog;
import de.tonsias.basis.ui.dialog.MultiStringValueDialog;
import de.tonsias.basis.ui.i18n.Messages;

/**
 * The list dialogs on a real {@link Display}, built the way JFace builds them.
 * The widgets are found on the shell and driven the way a user would drive them.
 * <p>
 * The promise is the single value dialogs' promise, one level up: OK is offered
 * for exactly the list the type would take. What differs is that an
 * <em>empty</em> list is such a list - it is what a list says instead of a
 * default value - while a single element the type will not read, or one that is
 * already in the list, is not.
 * </p>
 */
public class MultiValueDialogSystemTest {

	private static Display _display;

	private static Shell _shell;

	private IInstanz _instanz;

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
		// the runtime is shared by the whole bundle, so every dialog gets an instanz of
		// its own rather than the root everyone else writes to
		_instanz = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		_messages = messages();
	}

	@AfterEach
	void afterEach() {
		_built.forEach(Dialog::close);
		_built.clear();
		ProductRuntime.flushDeltas();
	}

	// ---------- what the OK button promises ----------

	/**
	 * An empty list is a value, the way {@code ""} and {@code false} are - a list
	 * filled later is a thing somebody means to create.
	 */
	@Test
	void testCreate_newList_okIsEnabledOverTheEmptyList() {
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, _instanz, _messages));

		assertThat(okButton(dialog).isEnabled(), is(true));
		assertThat(dialog.getEnteredElements(), is(empty()));
	}

	@Test
	void testAdd_takesTheElementIntoTheList() {
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, _instanz, _messages));

		add(dialog, "a");
		add(dialog, "b");

		assertThat(elements(dialog), contains("a", "b"));
		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	/**
	 * A duplicate is the one refusal worth a word: the model would answer
	 * {@code false} and drop it without a sound.
	 */
	@Test
	void testAdd_aDuplicateIsRefusedWithAReason() {
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, _instanz, _messages));
		add(dialog, "a");

		add(dialog, "a");

		assertThat(elements(dialog), contains("a"));
		assertThat(elementMessage(dialog).getText(), is(_messages.dialog_value_duplicateElement));
	}

	@Test
	void testAdd_whatTheTypeWillNotReadDoesNotGetIn() {
		MultiIntegerValueDialog dialog = built(new MultiIntegerValueDialog(_shell, _instanz, _messages));

		add(dialog, "abc");

		assertThat(elements(dialog), is(empty()));
	}

	/** "42" and 42 are one element, so the text is converted on the way in */
	@Test
	void testAdd_theElementIsStoredAsTheModelWouldStoreIt() {
		MultiIntegerValueDialog dialog = built(new MultiIntegerValueDialog(_shell, _instanz, _messages));

		add(dialog, "42");

		assertThat(elements(dialog), contains(42));
	}

	/**
	 * The name half of the promise: a name already taken on this instanz turns the
	 * button off however good the list is.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/67">#67</a>
	 */
	@Test
	void testName_alreadyUsedTurnsOkOff() {
		ProductRuntime.multiValueService().createNew(MultiStringValue.class, _instanz.getOwnKey(), "taken",
				List.of("x"), Type.SEND);
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, _instanz, _messages));

		nameText(dialog).setText("taken");

		assertThat(okButton(dialog).isEnabled(), is(false));
		assertThat(nameMessage(dialog).getText(), is(_messages.dialog_value_usedName));
	}

	/**
	 * The name side is per type: the same name under a single value and under a
	 * list are two different attributes, in two different maps.
	 */
	@Test
	void testName_theSameNameUnderASingleValueIsFree() {
		ProductRuntime.singleValueService().createNew(de.tonsias.basis.model.impl.value.SingleStringValue.class,
				_instanz.getOwnKey(), "taken", "x", Type.SEND);
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, _instanz, _messages));

		nameText(dialog).setText("taken");

		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	// ---------- what OK leaves behind ----------

	@Test
	void testOkPressed_newValue_createsItHoldingTheWholeList() {
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, _instanz, _messages));
		nameText(dialog).setText("words");
		add(dialog, "a");
		add(dialog, "b");

		press(okButton(dialog));

		MultiStringValue created = dialog.getMultiValue();
		assertThat(created.getValues(), contains("a", "b"));
		assertThat(_instanz.getValues(MultiValueType.MULTI_STRING).get(created.getOwnKey()), is("words"));
	}

	@Test
	void testOpen_existingValue_showsWhatItHolds() {
		MultiStringValue existing = ProductRuntime.multiValueService().createNew(MultiStringValue.class,
				_instanz.getOwnKey(), "words", List.of("a", "b"), Type.SEND);

		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, existing, _instanz, _messages));

		assertThat(elements(dialog), contains("a", "b"));
		assertThat(nameText(dialog).getText(), is("words"));
	}

	@Test
	void testOkPressed_existingValue_writesTheListAndTheNameBack() {
		MultiStringValue existing = ProductRuntime.multiValueService().createNew(MultiStringValue.class,
				_instanz.getOwnKey(), "words", List.of("a"), Type.SEND);
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, existing, _instanz, _messages));

		nameText(dialog).setText("renamed");
		add(dialog, "b");
		press(okButton(dialog));

		assertThat(existing.getValues(), contains("a", "b"));
		assertThat(_instanz.getValues(MultiValueType.MULTI_STRING).get(existing.getOwnKey()), is("renamed"));
	}

	/**
	 * The Instanz View queues what its widgets did and applies it all on save, so
	 * there the dialog must not write on its own - it hands the list over instead.
	 */
	@Test
	void testOkPressed_withoutWriteOnOk_changesNothingAndHandsTheListOver() {
		MultiStringValue existing = ProductRuntime.multiValueService().createNew(MultiStringValue.class,
				_instanz.getOwnKey(), "words", List.of("a"), Type.SEND);
		MultiStringValueDialog dialog = built(new MultiStringValueDialog(_shell, existing, _instanz, _messages));
		dialog.setWriteOnOk(false);

		add(dialog, "b");
		press(okButton(dialog));

		assertThat("the value is untouched", existing.getValues(), contains("a"));
		assertThat(dialog.getEnteredElements(), contains("a", "b"));
	}

	// ---------- helpers ----------

	private <D extends AMultiValueDialog<?>> D built(D dialog) {
		dialog.create();
		_built.add(dialog);
		return dialog;
	}

	/** types into the element input and presses Add, the way a user would */
	private void add(AMultiValueDialog<?> dialog, String text) {
		elementInput(dialog).setText(text);
		press(addButton(dialog));
	}

	private List<Object> elements(AMultiValueDialog<?> dialog) {
		return dialog.getValueControl().getElements();
	}

	/** key, name, and the element input of the lists that are typed into */
	private Text elementInput(Dialog dialog) {
		List<Text> texts = texts(dialog);
		assertThat("key, name and the element input", texts, hasSize(3));
		return texts.get(2);
	}

	private Text nameText(Dialog dialog) {
		return texts(dialog).get(1);
	}

	private List<Text> texts(Dialog dialog) {
		List<Text> texts = controls(dialog.getShell()).stream()//
				.filter(Text.class::isInstance).map(Text.class::cast)//
				.toList();
		assertThat("key and name", texts, hasSize(greaterThanOrEqualTo(2)));
		return texts;
	}

	private Button okButton(Dialog dialog) {
		return controls(dialog.getShell()).stream()//
				.filter(Button.class::isInstance).map(Button.class::cast)//
				.filter(button -> Integer.valueOf(IDialogConstants.OK_ID).equals(button.getData()))//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no OK button in " + dialog.getClass().getSimpleName()));
	}

	private Button addButton(Dialog dialog) {
		return controls(dialog.getShell()).stream()//
				.filter(Button.class::isInstance).map(Button.class::cast)//
				.filter(button -> _messages.constant_add.equals(button.getText()))//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no add button in " + dialog.getClass().getSimpleName()));
	}

	/**
	 * The label carrying the reason an element is refused. The widget creates it
	 * right after the input row, so it is the first label whose text is either empty
	 * or the duplicate message.
	 */
	private Label elementMessage(Dialog dialog) {
		return controls(dialog.getShell()).stream()//
				.filter(Label.class::isInstance).map(Label.class::cast)//
				.filter(label -> _messages.dialog_value_duplicateElement.equals(label.getText()))//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no element message showing in "
						+ dialog.getClass().getSimpleName()));
	}

	/**
	 * The label carrying the reason a name is refused. The dialog creates it right
	 * after the name field, so it is the next control in creation order.
	 */
	private Label nameMessage(Dialog dialog) {
		List<Control> all = controls(dialog.getShell());
		Control next = all.get(all.indexOf(nameText(dialog)) + 1);
		assertThat("the label after the name field", next, org.hamcrest.Matchers.instanceOf(Label.class));
		return (Label) next;
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
	 * The dialog puts every one of these onto a label and would fail on a
	 * {@code null}. What they say is not what this test is about, so they are filled
	 * in rather than translated - {@code TranslationCoverageTest} is what holds the
	 * real texts to their keys.
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
		messages.constant_remove = "Remove";
		messages.constant_singleValue = "Value";
		messages.constant_up = "Up";
		messages.constant_value = "Value";
		messages.dialog_value_duplicateElement = "element already in the list";
		messages.dialog_value_elements = "Elements";
		messages.dialog_value_instanzSide = "Instanz";
		messages.dialog_value_usedName = "name already used";
		messages.dialog_value_valueSide = "Value";
		return messages;
	}
}
