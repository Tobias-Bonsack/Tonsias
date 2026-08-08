package de.tonsias.delta.view.ui.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.delta.view.ui.tree.EventTreeContentProvider;
import de.tonsias.delta.view.ui.tree.EventTreeNodeWrapper;

/**
 * The Delta view's tree, wired exactly as {@code DeltaView} wires it: a virtual
 * {@link Tree} on a real {@link Display}, a real {@link TreeViewer}, the real
 * {@link EventTreeContentProvider} and the registered {@link IDeltaService}
 * behind it.
 * <p>
 * The rows are not a list but a nesting: everything between an open and a close
 * operation belongs under that operation, and a nested operation keeps its own
 * children to itself. That structure is only ever produced by real events, so
 * the deltas here are made the way the product makes them - by bracketing calls
 * on the services.
 * </p>
 * <p>
 * Being virtual, the viewer asks for one index at a time and the provider
 * answers by wrapping the matching event and immediately reporting how many
 * children the new node has. The tests drive that exchange and then read the
 * {@link TreeItem}s it produced.
 * </p>
 */
public class DeltaTreeSystemTest {

	private static Shell _shell;

	IInstanzService _inse;

	IEventBrokerBridge _broker;

	IDeltaService _delta;

	Tree _tree;

	TreeViewer _viewer;

	EventTreeContentProvider _provider;

	@BeforeAll
	static void beforeAll() {
		_shell = new Shell(Display.getDefault());
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
		_inse = ProductRuntime.instanzService();
		_broker = ProductRuntime.broker();
		_delta = ProductRuntime.deltaService();
		ProductRuntime.flushDeltas();

		// DeltaView.postConstruct hands the service to the wrappers the same way
		EventTreeNodeWrapper._deltaService = _delta;

		// the same wiring as DeltaView.createDeltaTree()
		_tree = new Tree(_shell, SWT.BORDER | SWT.VIRTUAL);
		_viewer = new TreeViewer(_tree);
		_provider = new EventTreeContentProvider(_viewer);
		_viewer.setContentProvider(_provider);
		_viewer.setLabelProvider(new LabelProvider());
		_viewer.setUseHashlookup(true);
	}

	@AfterEach
	void afterEach() {
		_tree.dispose();
		ProductRuntime.flushDeltas();
	}

	/** shows the log as it stands, the way {@code DeltaView} opens the view */
	private EventTreeNodeWrapper showLog() {
		EventTreeNodeWrapper root = new EventTreeNodeWrapper(IDeltaService.START_EVENT, null);
		_viewer.setInput(root);
		_viewer.setChildCount(root, root.getChildCount());
		return root;
	}

	private void openOperation() {
		_broker.send(EventConstants.OPEN_OPERATION, null);
	}

	private void closeOperation() {
		_broker.send(EventConstants.CLOSE_OPERATION, null);
	}

	/** one call on the services, which is two events in the log */
	private void oneChange() {
		_inse.createInstanz(ProductRuntime.ROOT, Type.SEND);
	}

	/** a materialised row: what the viewer put into the widget, and the widget */
	private record Row(EventTreeNodeWrapper wrapper, TreeItem item) {
	}

	private Row rowOf(EventTreeNodeWrapper root, int index) {
		_provider.updateElement(root, index);
		return rowFrom(_tree.getItem(index));
	}

	private Row rowOf(Row parent, int index) {
		_provider.updateElement(parent.wrapper(), index);
		return rowFrom(parent.item().getItem(index));
	}

	private static Row rowFrom(TreeItem item) {
		return new Row(EventTreeNodeWrapper.class.cast(item.getData()), item);
	}

	// ---------- the shape of the tree ----------

	/**
	 * The start event is the root row, and everything logged since the last save
	 * hangs below it.
	 */
	@Test
	void testTree_showsEveryTopLevelDeltaBelowTheStartEvent() {
		oneChange();

		EventTreeNodeWrapper root = showLog();

		assertThat(root.getChildCount(), is(2));
		assertThat(_tree.getItemCount(), is(2));
	}

	@Test
	void testTree_anEmptyLogHasNoRowsAtAll() {
		EventTreeNodeWrapper root = showLog();

		assertThat(root.getChildCount(), is(0));
		assertThat(_tree.getItemCount(), is(0));
	}

	/**
	 * An operation collapses everything it brackets into one row - which is why the
	 * handlers bracket at all.
	 */
	@Test
	void testTree_anOperationIsOneRowCarryingItsDeltas() {
		openOperation();
		oneChange();
		closeOperation();

		EventTreeNodeWrapper root = showLog();

		assertThat(root.getChildCount(), is(1));
		Row operation = rowOf(root, 0);
		assertThat(operation.wrapper().getChildCount(), is(2));
		assertThat(operation.item().getItemCount(), is(2));
	}

	/** A nested operation keeps its own children to itself. */
	@Test
	void testTree_aNestedOperationCountsAsOneChildOfTheOuterOne() {
		openOperation();
		oneChange(); // two children of the outer operation
		openOperation(); // one more - what it brackets belongs to it
		oneChange();
		closeOperation();
		oneChange(); // two more
		closeOperation();

		EventTreeNodeWrapper root = showLog();

		assertThat(root.getChildCount(), is(1));
		Row outer = rowOf(root, 0);
		assertThat(outer.wrapper().getChildCount(), is(5));
		assertThat(outer.item().getItemCount(), is(5));

		Row nested = rowOf(outer, 2);
		assertThat(nested.wrapper().getChildCount(), is(2));
		assertThat(nested.item().getItemCount(), is(2));
	}

	/** A delta is a leaf - only the brackets carry anything. */
	@Test
	void testTree_aPlainDeltaHasNoChildren() {
		oneChange();
		EventTreeNodeWrapper root = showLog();

		Row delta = rowOf(root, 0);

		assertThat(delta.wrapper().getChildCount(), is(0));
		assertThat(delta.item().getItemCount(), is(0));
	}

	// ---------- what the provider hands the viewer ----------

	@Test
	void testUpdateElement_wrapsTheEventAtThatIndexAndKeepsItsParent() {
		oneChange();
		EventTreeNodeWrapper root = showLog();

		Row first = rowOf(root, 0);
		Row second = rowOf(root, 1);

		assertThat(first.wrapper().getParent(), is(sameInstance(root)));
		assertThat(second.wrapper().getParent(), is(sameInstance(root)));
		assertThat(first.wrapper().toString(), is(not(second.wrapper().toString())));
	}

	/** The label the tree shows is the event's own {@code toString}. */
	@Test
	void testUpdateElement_theRowIsLabelledWithTheEventItself() {
		oneChange();
		EventTreeNodeWrapper root = showLog();
		Event logged = _delta.getDeltas().stream().skip(1).findFirst().orElseThrow();

		Row row = rowOf(root, 0);

		assertThat(row.wrapper().toString(), is(logged.toString()));
		assertThat(row.item().getText(), is(logged.toString()));
	}

	@Test
	void testUpdateChildCount_reportsTheCountToTheViewer() {
		openOperation();
		oneChange();
		closeOperation();
		EventTreeNodeWrapper root = showLog();
		Row operation = rowOf(root, 0);

		_provider.updateChildCount(operation.wrapper(), 0);

		assertThat(operation.item().getItemCount(), is(2));
	}

	@Test
	void testGetParent_isTheWrappersParent() {
		oneChange();
		EventTreeNodeWrapper root = showLog();
		Row child = rowOf(root, 0);

		assertThat(_provider.getParent(child.wrapper()), is(sameInstance(root)));
		assertThat(_provider.getParent(root), is(nullValue()));
	}

	/**
	 * The viewer never asks past the child count it was given, so an index beyond
	 * the children can only come from a bug - and it is better seen than swallowed.
	 */
	@Test
	void testUpdateElement_anIndexBeyondTheChildrenThrows() {
		EventTreeNodeWrapper root = showLog();

		assertThrows(IndexOutOfBoundsException.class, () -> _provider.updateElement(root, 0));
	}

	@Test
	void testUpdateElement_anElementThatIsNotAWrapperIsRejected() {
		assertThrows(ClassCastException.class, () -> _provider.updateElement("not a wrapper", 0));
	}

	@Test
	void testUpdateChildCount_aNullElementIsRejected() {
		assertThrows(NullPointerException.class, () -> _provider.updateChildCount(null, 0));
	}
}
