package de.tonsias.basis.osgi.test;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.util.ChangePropagationListener;

/**
 * Brings up the part of the runtime that the e4 application normally brings up
 * for us.
 * <p>
 * {@link IEventBrokerBridge} and {@link IDeltaService} are not plain
 * Declarative Services components: they are produced by
 * {@link IContextFunction}s, which only run when an {@link IEclipseContext} is
 * asked for their key. In the running product the workbench does the asking. In
 * a headless test runtime nothing does, so the context functions never execute,
 * the bridge is never registered in the service registry, and every
 * {@code @Reference IEventBrokerBridge} stays unsatisfied - which in turn keeps
 * {@code InstanzServiceImpl} and {@code SingleValueServiceImpl} from ever
 * activating.
 * </p>
 * <p>
 * Requesting {@link IDeltaService} once from the OSGi service context is
 * enough: its context function asks for the bridge before building, so both
 * functions run and register their result as an OSGi service, after which
 * Declarative Services can satisfy the dependent components and
 * {@code OsgiUtil.getService(..)} resolves as it does in the product.
 * </p>
 */
public final class E4ServiceContext {

	private static boolean _primed;

	/** the primed context, so a test bundle can put its own pieces into it */
	private static IEclipseContext _context;

	/**
	 * e4 keeps only a weak reference to an injected object, so a listener nobody
	 * holds on to can be collected in the middle of a run - and the propagation it
	 * does would then silently stop.
	 */
	private static ChangePropagationListener _listener;

	private E4ServiceContext() {
	}

	/**
	 * Idempotent; safe to call from every {@code @BeforeEach}.
	 */
	public static synchronized void prime() {
		if (_primed) {
			return;
		}

		BundleContext bundleContext = FrameworkUtil.getBundle(E4ServiceContext.class).getBundleContext();
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundleContext);
		_context = context;

		// asking for the delta service is enough: DeltaServiceContextFunction asks for
		// the bridge before it builds, so the bridge is registered on the way. Nothing
		// here repeats that order - if it were dropped over there, the whole suite
		// would stop coming up right here.
		context.get(IDeltaService.class.getName());

		// Application.e4xmi contributes this as an addon, which is what makes its
		// @EventTopic methods subscribe. Without it nothing keeps both ends of a
		// parent/child or instanz/value relation in sync, so e.g. createInstanz(..)
		// never adds the new key to its parent's child list.
		_listener = ContextInjectionFactory.make(ChangePropagationListener.class, context);

		// what the e4 application does with its application context, and the only way
		// an ExtendedObjectSupplier ever gets its own dependencies: the injector hands
		// this supplier to the one it finds for a qualifier, once, and remembers the
		// result. Without it @Translation resolves to a TranslationObjectSupplier with
		// an empty message factory, which throws on the first Messages anybody asks
		// for - and it stays that way for the rest of the run.
		ContextInjectionFactory.setDefault(context);

		_primed = true;
	}

	/**
	 * The context every extended object supplier is built out of, so a test bundle
	 * can put into it what only it can supply.
	 * <p>
	 * The one such piece is {@code UISynchronize}: without it the supplier behind
	 * {@code @UIEventTopic} subscribes, hears the event, and then drops it with a
	 * warning instead of running the method - so a part would never react to
	 * anything. It is SWT's to provide and belongs in the test bundle that has a
	 * {@code Display}, and it has to be in here before the first {@code @UIEventTopic}
	 * is injected: the supplier is built once and remembered.
	 * </p>
	 */
	public static IEclipseContext context() {
		prime();
		return _context;
	}
}
