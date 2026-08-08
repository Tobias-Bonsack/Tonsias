package de.tonsias.basis.data.access.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * The persistence services as the running framework hands them out.
 * <p>
 * They are plain Declarative Services components with no references of their
 * own, so the registry is the only thing that has to be up for them to resolve
 * - no e4 context, no priming. Taking them from there rather than constructing
 * them keeps the tests on the same instances every other bundle gets, and would
 * notice a component that stopped being registered at all.
 * </p>
 */
final class RegisteredServices {

	private RegisteredServices() {
	}

	static <T> T get(Class<T> type) {
		BundleContext context = FrameworkUtil.getBundle(RegisteredServices.class).getBundleContext();
		ServiceReference<T> reference = context.getServiceReference(type);
		assertThat("no service registered for " + type.getName(), reference, is(not(nullValue())));

		T service = context.getService(reference);
		assertThat("service reference without a service: " + type.getName(), service, is(not(nullValue())));
		return service;
	}
}
