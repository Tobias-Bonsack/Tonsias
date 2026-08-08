package de.tonsias.delta.view.ui.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.FrameworkUtil;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.delta.view.ui.DeltaView;
import de.tonsias.delta.view.ui.tree.EventTreeNodeWrapper;

/**
 * The Delta view part itself, built the way e4 builds it: created through
 * {@link ContextInjectionFactory} out of a part {@link IEclipseContext}, with
 * its {@code @PostConstruct} handed a real {@link Composite} on a real
 * {@link Display}.
 * <p>
 * Nothing about the part is passed in by the test - the {@link IDeltaService}
 * it renders is injected, exactly as the workbench injects it. What is checked
 * is what the user would see: the view comes up empty and then follows the log
 * as the model changes underneath it.
 * </p>
 * <p>
 * <b>One thing this exposed and now pins down:</b>
 * {@code DeltaServiceContextFunction} builds a <em>new</em>
 * {@code DeltaServiceImpl} on every compute, and an e4 context caches a
 * function's result per asking context. A part context is its own asking
 * context, so the view does not render the instance
 * {@code OsgiUtil.getService(IDeltaService.class)} hands out - it renders one of
 * its own. The two stay in step only because both subscribe to the same topics
 * and both clear on {@code SAVE_ALL}. The tests below therefore open the view
 * first and change the model afterwards, which is also the order the product
 * runs in. See
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/52">#52</a>; once
 * that is fixed the view can be opened on an already filled log again.
 * </p>
 */
public class DeltaViewSystemTest {

	private static Display _display;

	private static Shell _shell;

	IEclipseContext _context;

	IEclipseContext _partContext;

	IInstanzService _inse;

	IEventBrokerBridge _broker;

	Composite _parent;

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
		_inse = ProductRuntime.instanzService();
		_broker = ProductRuntime.broker();
		ProductRuntime.flushDeltas();

		_context = EclipseContextFactory
				.getServiceContext(FrameworkUtil.getBundle(DeltaViewSystemTest.class).getBundleContext());
		_parent = new Composite(_shell, SWT.NONE);
	}

	@AfterEach
	void afterEach() {
		if (_partContext != null) {
			_partContext.dispose();
			_partContext = null;
		}
		_parent.dispose();
		ProductRuntime.flushDeltas();
	}

	/**
	 * The workbench puts the part's {@link Composite} into the part context and
	 * lets the injector build the part and run its {@code @PostConstruct}; nothing
	 * here is called by hand.
	 */
	private DeltaView openView() {
		_partContext = _context.createChild("DeltaView");
		_partContext.set(Composite.class, _parent);
		return ContextInjectionFactory.make(DeltaView.class, _partContext);
	}

	private Tree treeOf() {
		Control[] children = _parent.getChildren();
		assertThat("the view puts exactly one control on its parent", children, arrayWithSize(1));
		return (Tree) children[0];
	}

	/** the service the view was injected with, which is what it renders */
	private IDeltaService shownLog() {
		return EventTreeNodeWrapper._deltaService;
	}

	private void oneChange() {
		_inse.createInstanz(ProductRuntime.ROOT, Type.SEND);
	}

	/** refreshes the way the toolbar's refresh handler does, and lets it run */
	private void refresh(DeltaView view) {
		view.updateTree();
		while (_display.readAndDispatch()) {
			// the refresh is posted with asyncExec, so the loop has to turn once
		}
	}

	/**
	 * The service is not handed in, it is injected - if the context could not
	 * supply it, the view would come up rendering nothing.
	 */
	@Test
	void testPostConstruct_getsADeltaServiceInjectedAndHandsItToTheWrappers() {
		openView();

		assertThat(shownLog(), is(notNullValue()));
		assertThat(shownLog().getDeltas(), contains(IDeltaService.START_EVENT));
	}

	@Test
	void testPostConstruct_buildsAVirtualTreeOnItsParent() {
		openView();

		Tree tree = treeOf();
		assertThat((tree.getStyle() & SWT.VIRTUAL) != 0, is(true));
	}

	@Test
	void testPostConstruct_opensOnAnEmptyLogWithNoRows() {
		openView();

		assertThat(treeOf().getItemCount(), is(0));
	}

	/**
	 * A change made while the view is open is two deltas, and each of them is a row
	 * of its own as long as nothing brackets them.
	 */
	@Test
	void testUpdateTree_picksUpDeltasLoggedAfterTheViewWasOpened() {
		DeltaView view = openView();

		oneChange();
		refresh(view);

		assertThat(shownLog().getDeltas(), hasSize(3));
		assertThat(treeOf().getItemCount(), is(2));
	}

	/** One operation is one row, however much it brackets. */
	@Test
	void testUpdateTree_bracketedChangesBecomeASingleRow() {
		DeltaView view = openView();

		_broker.send(EventConstants.OPEN_OPERATION, null);
		oneChange();
		oneChange();
		_broker.send(EventConstants.CLOSE_OPERATION, null);
		refresh(view);

		assertThat(treeOf().getItemCount(), is(1));
	}

	/**
	 * A save empties the log, and the view has to go back to showing nothing rather
	 * than keeping rows whose events are gone. {@code SAVE_ALL} is the marker the
	 * toolbar handler sends, and every delta service listens for it.
	 */
	@Test
	void testUpdateTree_afterASaveAllTheTreeIsEmptyAgain() {
		DeltaView view = openView();
		oneChange();
		refresh(view);
		assertThat(treeOf().getItemCount(), is(2));

		_broker.send(EventConstants.SAVE_ALL, "save");
		refresh(view);

		assertThat(shownLog().getDeltas(), contains(IDeltaService.START_EVENT));
		assertThat(treeOf().getItemCount(), is(0));
	}

	/** Refreshing without a change in between leaves the tree as it was. */
	@Test
	void testUpdateTree_withoutANewDeltaChangesNothing() {
		DeltaView view = openView();
		oneChange();
		refresh(view);

		refresh(view);

		assertThat(treeOf().getItemCount(), is(2));
	}

}
