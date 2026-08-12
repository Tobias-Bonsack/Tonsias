package de.tonsias.basis.ui.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tonsias.basis.logic.part.InstanzChoices.Choice;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleBooleanValue;
import de.tonsias.basis.model.impl.value.SingleFloatValue;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.basis.ui.dialog.AValueDialog;
import de.tonsias.basis.ui.dialog.BooleanValueDialog;
import de.tonsias.basis.ui.dialog.FloatValueDialog;
import de.tonsias.basis.ui.dialog.InstanzValueDialog;
import de.tonsias.basis.ui.dialog.IntegerValueDialog;
import de.tonsias.basis.ui.dialog.StringValueDialog;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.widget.InstanzChooser;

/**
 * The value dialogs on a real {@link Display}, built the way JFace builds them:
 * {@code Window.create()} runs {@code createDialogArea} and
 * {@code createButtonBar}, which is everything that happens before the user
 * sees the dialog. Nothing is called by hand afterwards - the widgets are found
 * on the shell and read the way a user would see them.
 * <p>
 * What is checked is the one promise the OK button makes: it is offered for
 * exactly the input the type's {@code tryToSetValue} would take. It used to
 * make that promise only from the first keystroke on, so a dialog opened for a
 * new value offered OK over an empty field and created a value silently left at
 * the start value of its type. See
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/62">#62</a>.
 * </p>
 * <p>
 * The name is part of the same promise: the button is on offer when the name is
 * free <em>and</em> the value acceptable. Each check used to set the button from
 * its own result alone, so whichever field was touched last had the last word -
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/67">#67</a>.
 * </p>
 * <p>
 * The rest is what the dialog says and does: the reason a name is refused, on a
 * widget that shows it (<a href=
 * "https://github.com/Tobias-Bonsack/Tonsias/issues/71">#71</a>), and what
 * pressing OK leaves behind - for a stored value that used to be the name alone,
 * with the value in the field read for the button and then dropped (<a href=
 * "https://github.com/Tobias-Bonsack/Tonsias/issues/72">#72</a>).
 * </p>
 */
public class ValueDialogSystemTest {

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

	// ---------- opening a dialog for a new value ----------

	/**
	 * The empty field a new value opens on is the one {@code tryToSetValue}
	 * rejects, so OK may not be on offer for it.
	 */
	@Test
	void testCreate_newFloatValue_okIsDisabledOverTheEmptyField() {
		FloatValueDialog dialog = built(new FloatValueDialog(_shell, _instanz, _messages));

		assertThat(okButton(dialog).isEnabled(), is(false));
	}

	@Test
	void testCreate_newIntegerValue_okIsDisabledOverTheEmptyField() {
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, _instanz, _messages));

		assertThat(okButton(dialog).isEnabled(), is(false));
	}

	/** An empty string is a string - nothing here is rejected. */
	@Test
	void testCreate_newStringValue_okIsEnabled() {
		StringValueDialog dialog = built(new StringValueDialog(_shell, _instanz, _messages));

		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	/** A check box cannot hold an invalid state, cleared is a value like any other. */
	@Test
	void testCreate_newBooleanValue_okIsEnabled() {
		BooleanValueDialog dialog = built(new BooleanValueDialog(_shell, _instanz, _messages));

		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	// ---------- opening a dialog on an existing value ----------

	/**
	 * Editing a stored value must not start out blocked: what is in the field came
	 * out of the model and is by definition acceptable.
	 */
	@Test
	void testCreate_existingFloatValue_okIsEnabledOverTheStoredValue() {
		SingleFloatValue value = ProductRuntime.singleValueService().createNew(SingleFloatValue.class,
				_instanz.getOwnKey(), "stored float", "3.5", Type.SEND);

		FloatValueDialog dialog = built(new FloatValueDialog(_shell, value, _instanz, _messages));

		assertThat(valueText(dialog).getText(), is("3.5"));
		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	@Test
	void testCreate_existingIntegerValue_okIsEnabledOverTheStoredValue() {
		SingleIntegerValue value = ProductRuntime.singleValueService().createNew(SingleIntegerValue.class,
				_instanz.getOwnKey(), "stored integer", "42", Type.SEND);

		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, value, _instanz, _messages));

		assertThat(valueText(dialog).getText(), is("42"));
		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	// ---------- the button and the model agree, whatever is typed ----------

	/**
	 * Both ends of the same rule: whatever stands in the field, the button says
	 * what the model would do with it. The untouched empty field is in the list, so
	 * the state before the first keystroke is held to the same standard as every
	 * one after it.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "", " ", "3.14", "-3.14", "42", "0.0", "3,14", "NaN", "Infinity", "1e5", "3f", "0x1p3",
			".", "-", "1.2.3", "10000000000000000000000000000000000000000" })
	void testValueControl_floatOkFollowsWhatTheModelWouldTake(String typed) {
		FloatValueDialog dialog = built(new FloatValueDialog(_shell, _instanz, _messages));

		valueText(dialog).setText(typed);

		assertThat("OK for '" + typed + "'", okButton(dialog).isEnabled(), is(floatTakes(typed)));
	}

	/**
	 * A leading plus and numbers outside the {@code int} range are in the list: the
	 * dialog used to answer them from a pattern of its own, which took
	 * "99999999999" that {@code Integer.valueOf} then threw on, and refused "+5"
	 * that it would have read.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/68">#68</a>
	 */
	@ParameterizedTest
	@ValueSource(strings = { "", " ", "42", "-42", "0", "3.14", "abc", "1,000", "-", "4 2", "+5", "99999999999",
			"2147483647", "2147483648" })
	void testValueControl_integerOkFollowsWhatTheModelWouldTake(String typed) {
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, _instanz, _messages));

		valueText(dialog).setText(typed);

		assertThat("OK for '" + typed + "'", okButton(dialog).isEnabled(), is(integerTakes(typed)));
	}

	// ---------- name and value are judged together ----------

	/**
	 * A name another value of the same type already carries is not free - the
	 * {@code BiMap} on the instanz holds names unique, so a second value under it
	 * would push the first one out. Typing a perfectly good value afterwards must
	 * not hand OK back; the two checks used to overwrite each other's verdict.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/67">#67</a>
	 */
	@Test
	void testNameControl_usedNameDisablesOk_andTheValueFieldDoesNotHandItBack() {
		ProductRuntime.singleValueService().createNew(SingleIntegerValue.class, _instanz.getOwnKey(), "taken name", "1",
				Type.SEND);
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, _instanz, _messages));

		nameText(dialog).setText("taken name");
		assertThat("a used name", okButton(dialog).isEnabled(), is(false));

		valueText(dialog).setText("42");
		assertThat("a number does not make the name free", okButton(dialog).isEnabled(), is(false));
	}

	/**
	 * The other way round: a value the model would discard stays discarded while
	 * the name is typed.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/67">#67</a>
	 */
	@Test
	void testValueControl_rejectedValueDisablesOk_andTheNameFieldDoesNotHandItBack() {
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, _instanz, _messages));

		valueText(dialog).setText("abc");
		assertThat("not a number", okButton(dialog).isEnabled(), is(false));

		nameText(dialog).setText("a free name");
		assertThat("a free name does not make 'abc' a number", okButton(dialog).isEnabled(), is(false));
	}

	/** Both sides content is the one case OK is on offer for. */
	@Test
	void testNameAndValueControl_bothAcceptable_okIsEnabled() {
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, _instanz, _messages));

		nameText(dialog).setText("a free name");
		valueText(dialog).setText("42");

		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	/**
	 * The name in the field of an existing value is that value's own, so it may not
	 * count as taken against itself - the dialog would open on a name it refuses.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/67">#67</a>
	 */
	@Test
	void testNameControl_existingValueKeepsItsOwnName() {
		SingleStringValue value = ProductRuntime.singleValueService().createNew(SingleStringValue.class,
				_instanz.getOwnKey(), "own name", "text", Type.SEND);

		StringValueDialog dialog = built(new StringValueDialog(_shell, value, _instanz, _messages));

		assertThat(nameText(dialog).getText(), is("own name"));
		assertThat("untouched", okButton(dialog).isEnabled(), is(true));

		nameText(dialog).setText("own name");
		assertThat("typed again", okButton(dialog).isEnabled(), is(true));
	}

	/** The name of another value is taken for an existing value too. */
	@Test
	void testNameControl_existingValueTakingAnotherName() {
		ProductRuntime.singleValueService().createNew(SingleStringValue.class, _instanz.getOwnKey(), "neighbour",
				"text", Type.SEND);
		SingleStringValue value = ProductRuntime.singleValueService().createNew(SingleStringValue.class,
				_instanz.getOwnKey(), "own name", "text", Type.SEND);

		StringValueDialog dialog = built(new StringValueDialog(_shell, value, _instanz, _messages));
		nameText(dialog).setText("neighbour");

		assertThat(okButton(dialog).isEnabled(), is(false));
	}

	/**
	 * The button is only half the answer - the dialog also has to say why. The
	 * reason used to go to {@code Text.setMessage}, the placeholder SWT draws over
	 * an empty field, so it was never on screen in the one case it was set for.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/71">#71</a>
	 */
	@Test
	void testNameControl_usedNameIsSaidInSoManyWords() {
		ProductRuntime.singleValueService().createNew(SingleIntegerValue.class, _instanz.getOwnKey(), "taken name", "1",
				Type.SEND);
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, _instanz, _messages));

		assertThat("nothing to say about a free name", nameMessage(dialog).getText(), is(""));

		nameText(dialog).setText("taken name");
		assertThat(nameMessage(dialog).getText(), is(_messages.dialog_value_usedName));

		nameText(dialog).setText("a free name");
		assertThat("taken back", nameMessage(dialog).getText(), is(""));
	}

	// ---------- what pressing OK leaves behind ----------

	/**
	 * A dialog opened on a stored value used to hand on the name alone: the number
	 * in the field was read for the OK button and then dropped, and the dialog
	 * closed as if it had done something.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/72">#72</a>
	 */
	@Test
	void testOkPressed_existingIntegerValue_writesTheValueBack() {
		SingleIntegerValue value = ProductRuntime.singleValueService().createNew(SingleIntegerValue.class,
				_instanz.getOwnKey(), "count", "1", Type.SEND);
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, value, _instanz, _messages));

		valueText(dialog).setText("99");
		press(okButton(dialog));

		assertThat(value.getValue(), is(99));
	}

	/** the same for the check box, which is read differently */
	@Test
	void testOkPressed_existingBooleanValue_writesTheCheckBoxBack() {
		SingleBooleanValue value = ProductRuntime.singleValueService().createNew(SingleBooleanValue.class,
				_instanz.getOwnKey(), "flag", Boolean.FALSE, Type.SEND);
		BooleanValueDialog dialog = built(new BooleanValueDialog(_shell, value, _instanz, _messages));

		checkBox(dialog).setSelection(true);
		press(okButton(dialog));

		assertThat(value.getValue(), is(true));
	}

	/** and the name, which is what the else branch did do all along */
	@Test
	void testOkPressed_existingValue_writesTheNameBack() {
		SingleIntegerValue value = ProductRuntime.singleValueService().createNew(SingleIntegerValue.class,
				_instanz.getOwnKey(), "count", "1", Type.SEND);
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, value, _instanz, _messages));

		nameText(dialog).setText("counter");
		press(okButton(dialog));

		assertThat(_instanz.getValues(SingleValueType.SINGLE_INTEGER).get(value.getOwnKey()), is("counter"));
	}

	/**
	 * A new value is still created from both fields. The name travels to the
	 * instanz on a posted event, which is {@code ChangePropagationListener}'s and
	 * has its own tests - what is asserted here is what the dialog itself left.
	 */
	@Test
	void testOkPressed_newValue_createsItFromBothFields() {
		IntegerValueDialog dialog = built(new IntegerValueDialog(_shell, _instanz, _messages));

		nameText(dialog).setText("fresh");
		valueText(dialog).setText("7");
		press(okButton(dialog));

		assertThat(dialog.getSingleValue().getValue(), is(7));
		assertThat(dialog.getSingleValue().getConnectedInstanzKeys(), contains(_instanz.getOwnKey()));
	}

	// ---------- the relation, which is chosen rather than typed ----------

	/**
	 * A new relation opens on no selection, which is the one thing
	 * {@link SingleInstanzValue#accepts} refuses - so OK may not be on offer for
	 * it, the same rule the empty number field is held to.
	 */
	@Test
	void testCreate_newInstanzValue_okIsDisabledWithoutASelection() {
		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, _instanz, _messages));

		assertThat(chooser(dialog).getSelectedKey(), is(""));
		assertThat(okButton(dialog).isEnabled(), is(false));
	}

	/** and is on offer as soon as one is chosen */
	@Test
	void testValueControl_choosingAnInstanzEnablesOk() {
		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, _instanz, _messages));

		choose(chooser(dialog), _instanz);

		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	/**
	 * Every instanz of the model is on offer, and it is offered where it sits: the
	 * chooser draws the tree rather than a flat list of everything, which is what
	 * keeps it usable once a model grows.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/76">#76</a>
	 */
	@Test
	void testCreate_newInstanzValue_offersTheModelAsATree() {
		IInstanz child = ProductRuntime.instanzService().createInstanz(_instanz.getOwnKey(), Type.SEND);
		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, _instanz, _messages));

		Choice root = chooser(dialog).root();

		assertThat("the walk starts at the root", root._key(), is(ProductRuntime.ROOT));
		assertThat(root.find(_instanz.getOwnKey()).orElseThrow()._children().stream().map(Choice::_key).toList(),
				contains(child.getOwnKey()));
	}

	/**
	 * The filter is the other half of the answer to a large model: typing narrows
	 * the tree to what is named, and keeps the branch above a match so the match is
	 * not hidden with its parent.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/76">#76</a>
	 */
	@Test
	void testValueControl_theFilterNarrowsTheTreeAndKeepsThePathToAMatch() {
		IInstanz child = ProductRuntime.instanzService().createInstanz(_instanz.getOwnKey(), Type.SEND);
		IInstanz stranger = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, _instanz, _messages));
		InstanzChooser chooser = chooser(dialog);

		chooser.getFilterText().setText(child.getOwnKey());

		List<String> shown = visibleKeys(chooser);
		assertThat("the match itself", shown, hasItem(child.getOwnKey()));
		assertThat("and the branch leading to it", shown, hasItem(_instanz.getOwnKey()));
		assertThat("but nothing else", shown, not(hasItem(stranger.getOwnKey())));
	}

	/** an empty filter is no filter - everything is back */
	@Test
	void testValueControl_clearingTheFilterShowsTheWholeTreeAgain() {
		IInstanz stranger = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, _instanz, _messages));
		InstanzChooser chooser = chooser(dialog);

		chooser.getFilterText().setText(_instanz.getOwnKey());
		chooser.getFilterText().setText("");

		assertThat(visibleKeys(chooser), hasItem(stranger.getOwnKey()));
	}

	/**
	 * A dialog opened on a stored relation shows where it points, so the user is
	 * not asked to choose again to keep what is already there.
	 */
	@Test
	void testCreate_existingInstanzValue_preselectsTheTarget() {
		IInstanz target = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		SingleInstanzValue value = ProductRuntime.singleValueService().createNew(SingleInstanzValue.class,
				_instanz.getOwnKey(), "stored reference", target.getOwnKey(), Type.SEND);

		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, value, _instanz, _messages));

		assertThat(chooser(dialog).getSelectedKey(), is(target.getOwnKey()));
		assertThat(chooser(dialog).getSelectedLabel(), is(target.toString()));
		assertThat(okButton(dialog).isEnabled(), is(true));
	}

	/** what OK leaves behind is the key of the chosen instanz, not its label */
	@Test
	void testOkPressed_existingInstanzValue_writesTheChosenKeyBack() {
		IInstanz first = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		IInstanz second = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		SingleInstanzValue value = ProductRuntime.singleValueService().createNew(SingleInstanzValue.class,
				_instanz.getOwnKey(), "stored reference", first.getOwnKey(), Type.SEND);

		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, value, _instanz, _messages));
		choose(chooser(dialog), second);
		press(okButton(dialog));

		assertThat(value.getValue(), is(second.getOwnKey()));
		assertThat(ProductRuntime.instanzService().resolveInstanzValue(value), is(Optional.of(second)));
	}

	/** and a new one is created pointing at it */
	@Test
	void testOkPressed_newInstanzValue_createsItPointingAtTheChosenInstanz() {
		IInstanz target = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);
		InstanzValueDialog dialog = built(new InstanzValueDialog(_shell, _instanz, _messages));

		nameText(dialog).setText("points at");
		choose(chooser(dialog), target);
		press(okButton(dialog));

		assertThat(dialog.getSingleValue().getValue(), is(target.getOwnKey()));
		assertThat(dialog.getSingleValue().getConnectedInstanzKeys(), contains(_instanz.getOwnKey()));
		assertThat("and the target records the other end",
				target.getReferencingValueKeys(), hasItem(dialog.getSingleValue().getOwnKey()));
	}

	// ---------- what the model would do ----------

	/**
	 * Whether {@link SingleFloatValue} would take this text. The value is seeded
	 * away from everything the test types, because {@code setValue} answers
	 * {@code false} both for "rejected" and for "already that value" - deliberately,
	 * it is what keeps the services from re-entering each other - and
	 * {@code tryToSetValue} passes that on.
	 */
	private boolean floatTakes(String typed) {
		return new SingleFloatValue("float-rule", Float.MIN_VALUE, Set.of()).tryToSetValue(typed);
	}

	/** @see #floatTakes(String) */
	private boolean integerTakes(String typed) {
		return new SingleIntegerValue("integer-rule", Integer.MIN_VALUE, Set.of()).tryToSetValue(typed);
	}

	// ---------- building the dialog and finding its widgets ----------

	/**
	 * Builds the widgets without putting the dialog on screen:
	 * {@code Window.create()} is what {@code open()} does first, and the part after
	 * it only shows and pumps the event loop.
	 */
	private <D extends AValueDialog<?, ?>> D built(D dialog) {
		dialog.create();
		_built.add(dialog);
		return dialog;
	}

	/**
	 * The OK button, found the way JFace marks it: {@code Dialog.createButton} puts
	 * the button id into the widget's data. {@code Dialog.getButton(int)} is
	 * protected, and matching on the label would depend on the locale.
	 */
	private Button okButton(Dialog dialog) {
		return controls(dialog.getShell()).stream()//
				.filter(Button.class::isInstance).map(Button.class::cast)//
				.filter(button -> Integer.valueOf(IDialogConstants.OK_ID).equals(button.getData()))//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no OK button in " + dialog.getClass().getSimpleName()));
	}

	/**
	 * The field the value is typed into. It is the last {@link Text} the dialog
	 * creates - key and name come first, both from {@code AValueDialog} - and the
	 * count is asserted so a fourth field fails here rather than silently moving
	 * the test onto the wrong widget.
	 */
	private Text valueText(Dialog dialog) {
		List<Text> texts = texts(dialog);
		assertThat("key, name and value", texts, hasSize(3));
		return texts.get(2);
	}

	/**
	 * The name field, the second one every dialog has - see
	 * {@link #valueText(Dialog)}. It is read off the shorter list too, because a
	 * dialog whose value is chosen rather than typed has no third {@link Text} at
	 * all.
	 */
	private Text nameText(Dialog dialog) {
		return texts(dialog).get(1);
	}

	/** the tree an {@link InstanzValueDialog} chooses its value in */
	private InstanzChooser chooser(Dialog dialog) {
		return controls(dialog.getShell()).stream()//
				.filter(InstanzChooser.class::isInstance).map(InstanzChooser.class::cast)//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no chooser in " + dialog.getClass().getSimpleName()));
	}

	/**
	 * Chooses an instanz the way a click does: JFace turns a click into a selection
	 * on the viewer, and it is the viewer that tells the listener judging the OK
	 * button. Selecting the tree item alone would move the highlight and say
	 * nothing.
	 */
	private void choose(InstanzChooser chooser, IInstanz instanz) {
		Choice choice = chooser.root().find(instanz.getOwnKey())//
				.orElseThrow(() -> new AssertionError("'" + instanz + "' is not in the tree"));
		chooser.getViewer().setSelection(new StructuredSelection(choice), true);
	}

	/**
	 * The keys the tree is showing right now - what the filter left of it. Read off
	 * the SWT items rather than off the model, because that is where the filter has
	 * had its say.
	 */
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

	/**
	 * The label carrying the reason a name is refused. The dialog creates it right
	 * after the name field, so it is the next control in creation order.
	 */
	private Label nameMessage(Dialog dialog) {
		List<Control> all = controls(dialog.getShell());
		Control next = all.get(all.indexOf(nameText(dialog)) + 1);
		assertThat("the label after the name field", next, instanceOf(Label.class));
		return (Label) next;
	}

	/** the check box a {@link BooleanValueDialog} enters its value with */
	private Button checkBox(Dialog dialog) {
		return controls(dialog.getShell()).stream()//
				.filter(Button.class::isInstance).map(Button.class::cast)//
				.filter(button -> (button.getStyle() & SWT.CHECK) != 0)//
				.findFirst()//
				.orElseThrow(() -> new AssertionError("no check box in " + dialog.getClass().getSimpleName()));
	}

	/**
	 * Presses a button the way a click does - JFace turns the selection into
	 * {@code buttonPressed(id)} and, for OK, into {@code okPressed()}.
	 */
	private void press(Button button) {
		button.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * Key and name come from {@code AValueDialog} and are there in every dialog; a
	 * third one is the value field of the dialogs that are typed into.
	 */
	private List<Text> texts(Dialog dialog) {
		List<Text> texts = controls(dialog.getShell()).stream()//
				.filter(Text.class::isInstance).map(Text.class::cast)//
				.toList();
		assertThat("key and name", texts, hasSize(greaterThanOrEqualTo(2)));
		return texts;
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
	 * {@code null}. What they say is not what this test is about, so they are
	 * filled in rather than translated - {@code TranslationCoverageTest} is what
	 * holds the real texts to their keys.
	 */
	private Messages messages() {
		Messages messages = new Messages();
		messages.constant_cancel = "Cancel";
		messages.constant_filter = "Filter";
		messages.constant_key = "Key";
		messages.constant_name = "Name";
		messages.constant_singleValue = "Value";
		messages.dialog_value_instanzSide = "Instanz";
		messages.dialog_value_usedName = "name already used";
		messages.dialog_value_valueSide = "Value";
		return messages;
	}
}
