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
 * Requesting the two keys once from the OSGi service context is enough: both
 * context functions register their result as an OSGi service, after which
 * Declarative Services can satisfy the dependent components and
 * {@code OsgiUtil.getService(..)} resolves as it does in the product.
 * </p>
 */
public final class E4ServiceContext {

	private static boolean _primed;

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

		// order matters: the delta service depends on the bridge
		context.get(IEventBrokerBridge.class.getName());
		context.get(IDeltaService.class.getName());

		// Application.e4xmi contributes this as an addon, which is what makes its
		// @EventTopic methods subscribe. Without it nothing keeps both ends of a
		// parent/child or instanz/value relation in sync, so e.g. createInstanz(..)
		// never adds the new key to its parent's child list.
		_listener = ContextInjectionFactory.make(ChangePropagationListener.class, context);

		_primed = true;
	}
}
