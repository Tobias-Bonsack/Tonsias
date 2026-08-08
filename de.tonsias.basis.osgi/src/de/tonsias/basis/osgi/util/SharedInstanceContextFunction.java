package de.tonsias.basis.osgi.util;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * A context function that hands every asking context the same instance.
 * <p>
 * An {@link IEclipseContext} caches what a context function computes per
 * <em>asking</em> context, and every part context asks on its own. A function
 * that builds something new on each call therefore hands out one instance per
 * part - for a service that carries state, such as the delta log, that means
 * several logs, each holding a different part of the picture, and a save on one
 * of them leaving the others untouched. The instance is built once and put into
 * the service registry, every later call is answered out of the registry, so
 * {@code @Inject} and {@link OsgiUtil#getService(Class)} arrive at the same
 * object.
 * </p>
 * <p>
 * It is built out of this bundle's service context and never out of the context
 * that happened to ask first: a part context is disposed with its part, and
 * disposing a context un-injects everything made from it. The shared instance
 * would be left with null fields while the registry keeps handing it out.
 * </p>
 *
 * @param <T> the service interface the instance is registered under
 */
abstract class SharedInstanceContextFunction<T> extends ContextFunction {

	private final Class<T> _serviceClass;

	protected SharedInstanceContextFunction(Class<T> serviceClass) {
		_serviceClass = serviceClass;
	}

	@Override
	public final synchronized Object compute(IEclipseContext context) {
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();

		T shared = OsgiUtil.getService(_serviceClass, bundleContext);
		if (shared == null) {
			shared = create(EclipseContextFactory.getServiceContext(bundleContext));
			bundleContext.registerService(_serviceClass, shared, null);
		}
		return shared;
	}

	/**
	 * Builds the one instance. Called at most once per framework run, with the
	 * lookup already having come up empty.
	 *
	 * @param context this bundle's service context - the instance is injected out
	 *                of it and lives as long as it does, not as long as whoever
	 *                asked first
	 */
	protected abstract T create(IEclipseContext context);
}
