package de.tonsias.basis.ui.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.Arrays;
import java.util.List;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService.Key;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.test.E4ServiceContext;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.basis.ui.part.ModelView;

/**
 * The Model view part itself, built the way e4 builds it: through
 * {@link ContextInjectionFactory} out of a part {@link IEclipseContext}, with
 * its {@code @PostConstruct} handed a real {@link Composite} on a real
 * {@link Display}. Nothing about the part is passed in - every service it uses
 * is injected, exactly as the workbench injects it.
 * <p>
 * What is checked is what the user would see: an attribute created on an instanz
 * is under that instanz in the tree, right away and without a second operation
 * to shake it loose. The view listens for the change on the instanz rather than
 * for the value being created, because between those two lies the propagation
 * that hangs the attribute on the instanz - a view refreshing before it stays one
 * operation behind, which is
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/89">#89</a>.
 * </p>
 * <p>
 * The values are created on the root, because that is the node the tree is
 * rooted at: a virtual tree only ever asks about what is on screen, and nothing
 * below the root is expanded here.
 * </p>
 */
public class ModelViewSystemTest {

	private static Display _display;

	private static Shell _shell;

	private IEclipseContext _context;

	private IEclipseContext _partContext;

	private IBasicPreferenceService _prefs;

	private Composite _parent;

	private String _showValues;

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
	void beforeEach() throws BackingStoreException {
		ProductRuntime.start();
		// what the workbench puts into its context, and what @UIEventTopic needs to
		// run a part's method rather than drop the event with a warning
		E4ServiceContext.context().set(UISynchronize.class, new DisplaySynchronize());
		_prefs = ProductRuntime.preferenceService();
		_showValues = _prefs.getValue(Key.SHOW_VALUES.getKey(), String.class).orElse("true");
		// the tree shows attributes at all only while this is on, and the preference
		// node is shared by the whole bundle
		_prefs.saveAsToString(Key.SHOW_VALUES.getKey(), Boolean.TRUE);

		// the context of the bundle the part lives in, the way the workbench builds
		// one: what an e4 service context can hand out is what its own bundle can see,
		// and the part needs more than the test bundle wires in - the message factory
		// behind @Translation among it
		_context = E4ServiceContext.context();
		_parent = new Composite(_shell, SWT.NONE);
	}

	@AfterEach
	void afterEach() throws BackingStoreException {
		// first the context: disposing it takes the part's event handlers off the bus,
		// so nothing reaches the tree after its parent is gone
		if (_partContext != null) {
			_partContext.dispose();
			_partContext = null;
		}
		_parent.dispose();
		_prefs.saveAsToString(Key.SHOW_VALUES.getKey(), _showValues);
		ProductRuntime.flushDeltas();
	}

	/**
	 * The single value half of the same promise - it is the case that looked like it
	 * worked, because a value named after the model view preference did refresh the
	 * tree. Any other name did not.
	 */
	@Test
	void testNewSingleValue_isUnderItsInstanzWithoutAFurtherOperation() {
		openView();
		int before = treeOf().getItemCount();

		ProductRuntime.singleValueService().createNew(SingleStringValue.class, ProductRuntime.ROOT, "eigenschaft",
				"wert", Type.SEND);

		assertThat(treeOf().getItemCount(), is(before + 1));
	}

	@Test
	void testNewMultiValue_isUnderItsInstanzWithoutAFurtherOperation() {
		openView();
		int before = treeOf().getItemCount();

		ProductRuntime.multiValueService().createNew(MultiStringValue.class, ProductRuntime.ROOT, "liste",
				List.of("a", "b"), Type.SEND);

		assertThat(treeOf().getItemCount(), is(before + 1));
	}

	/** and a new instanz is a child like any other */
	@Test
	void testNewInstanz_isUnderItsParentWithoutAFurtherOperation() {
		openView();
		int before = treeOf().getItemCount();

		ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND);

		assertThat(treeOf().getItemCount(), is(before + 1));
	}

	/**
	 * A deleted attribute goes the same way it came: the instanz loses the key, and
	 * the tree loses the row.
	 */
	@Test
	void testDeletedValue_leavesTheTreeWithoutAFurtherOperation() {
		SingleStringValue value = ProductRuntime.singleValueService().createNew(SingleStringValue.class,
				ProductRuntime.ROOT, "vergaenglich", "wert", Type.SEND);
		openView();
		int before = treeOf().getItemCount();

		ProductRuntime.singleValueService().deleteValue(value, Type.SEND);

		assertThat(treeOf().getItemCount(), is(before - 1));
	}

	// ---------- the context menu ----------

	/**
	 * The way a user deletes an attribute. What a tree item carries is the node of
	 * the tree, never the object it stands for, and the menu used to cast straight
	 * to the value - so this threw before it deleted anything.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/90">#90</a>
	 */
	@Test
	void testDeleteMenu_takesTheSelectedAttributeOffItsInstanz() {
		SingleStringValue value = ProductRuntime.singleValueService().createNew(SingleStringValue.class,
				ProductRuntime.ROOT, "zu loeschen", "wert", Type.SEND);
		Tree tree = openViewOn(value);

		press(deleteItem(tree));

		assertThat(ProductRuntime.instanzService().getRoot().getValues(SingleValueType.SINGLE_STRING),
				not(hasKey(value.getOwnKey())));
		assertThat(labels(), not(hasItem(startsWith(value.getOwnKey() + " "))));
	}

	/**
	 * A selection that is no attribute is nothing to delete - the item is disabled
	 * over an instanz, and an item that is fired anyway must not act on the wrong
	 * node either.
	 */
	@Test
	void testDeleteMenu_overAnInstanzDeletesNothing() {
		SingleStringValue value = ProductRuntime.singleValueService().createNew(SingleStringValue.class,
				ProductRuntime.ROOT, "bleibt", "wert", Type.SEND);
		String childKey = ProductRuntime.instanzService().createInstanz(ProductRuntime.ROOT, Type.SEND).getOwnKey();
		openView();
		Tree tree = treeOf();
		tree.setSelection(itemOf(childKey));

		press(deleteItem(tree));

		assertThat(ProductRuntime.instanzService().getRoot().getValues(SingleValueType.SINGLE_STRING),
				hasKey(value.getOwnKey()));
		assertThat(ProductRuntime.instanzService().resolveKey(childKey).isPresent(), is(true));
	}

	/**
	 * With the preference off an attribute is no child of its instanz, and the tree
	 * says so from both ends: it is neither counted nor handed out at any index.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/91">#91</a>
	 */
	@Test
	void testShowValuesOff_anAttributeIsNoRowOfTheTree() throws BackingStoreException {
		_prefs.saveAsToString(Key.SHOW_VALUES.getKey(), Boolean.FALSE);
		SingleStringValue value = ProductRuntime.singleValueService().createNew(SingleStringValue.class,
				ProductRuntime.ROOT, "versteckt", "wert", Type.SEND);
		openView();

		assertThat(labels(), not(hasItem(startsWith(value.getOwnKey() + " "))));
	}

	// ---------- helpers ----------

	/**
	 * The workbench hands the part's {@link Composite} over in a static context of
	 * its own and lets the injector build the part and run its
	 * {@code @PostConstruct}; nothing here is called by hand.
	 * <p>
	 * The static context is not decoration. e4 injects an
	 * {@code ExtendedObjectSupplier} - the one behind {@code @Translation} among
	 * them - out of exactly that context, once, and remembers it; built without one
	 * the supplier keeps its own {@code @Inject} fields empty and throws on every
	 * {@code Messages} anybody asks it for afterwards.
	 * </p>
	 */
	private ModelView openView() {
		_partContext = _context.createChild("ModelView");
		IEclipseContext arguments = _partContext.createChild("ModelView arguments");
		arguments.set(Composite.class, _parent);
		return ContextInjectionFactory.make(ModelView.class, _partContext, arguments);
	}

	private Tree treeOf() {
		Control[] children = _parent.getChildren();
		assertThat("the view puts exactly one control on its parent", children, arrayWithSize(1));
		return (Tree) children[0];
	}

	/** opens the view with the node of {@code object} selected, as a click would */
	private Tree openViewOn(IObject object) {
		openView();
		Tree tree = treeOf();
		tree.setSelection(itemOf(object.getOwnKey()));
		return tree;
	}

	/**
	 * The row a key stands on. Every label starts with the key of what it shows, so
	 * there is nothing to reach past the tree for.
	 */
	private TreeItem itemOf(String ownKey) {
		for (TreeItem item : treeOf().getItems()) {
			if (materialized(item).startsWith(ownKey + " ") || materialized(item).equals(ownKey)) {
				return item;
			}
		}
		throw new AssertionError("no row for " + ownKey + " in " + labels());
	}

	private List<String> labels() {
		return Arrays.stream(treeOf().getItems()).map(this::materialized).toList();
	}

	/**
	 * A virtual tree fills an item in when it is drawn, and nothing is drawn on a
	 * shell nobody opened - asking for the text is what makes SWT ask the viewer for
	 * the element.
	 */
	private String materialized(TreeItem item) {
		return item.getText();
	}

	/**
	 * The "delete value" item of the context menu. The view builds it last, behind
	 * the create item and the two cascades.
	 */
	private MenuItem deleteItem(Tree tree) {
		MenuItem[] items = tree.getMenu().getItems();
		assertThat("create instanz, both value cascades and delete", items, arrayWithSize(4));
		return items[3];
	}

	private void press(MenuItem item) {
		item.notifyListeners(SWT.Selection, new Event());
	}

	/**
	 * The {@code Display} as e4 wants to see it. In the product the workbench puts
	 * one of these into the application context; here the tests run on the display
	 * thread themselves, so every one of these calls straight through.
	 */
	private static class DisplaySynchronize extends UISynchronize {

		@Override
		public void syncExec(Runnable runnable) {
			_display.syncExec(runnable);
		}

		@Override
		public void asyncExec(Runnable runnable) {
			_display.asyncExec(runnable);
		}

		@Override
		protected boolean isUIThread(Thread thread) {
			return _display.getThread() == thread;
		}

		@Override
		protected void showBusyWhile(Runnable runnable) {
			runnable.run();
		}

		@Override
		protected boolean dispatchEvents() {
			return _display.readAndDispatch();
		}
	}
}
