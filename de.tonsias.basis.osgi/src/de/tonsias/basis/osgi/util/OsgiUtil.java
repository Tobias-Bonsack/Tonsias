package de.tonsias.basis.osgi.util;

import java.util.function.Consumer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiUtil {
	/**
	 * Look up a service without assuming a framework is present.
	 *
	 * @return the service, or <code>null</code> if the class was not loaded by a
	 *         bundle class loader, the bundle is not started, or no service is
	 *         registered
	 */
	public static <T> T getService(Class<T> clazz) {
		Bundle bundle = FrameworkUtil.getBundle(clazz);
		if (bundle == null) {
			return null;
		}
		return getService(clazz, bundle.getBundleContext());
	}

	/**
	 * @return the service, or <code>null</code> if the context is
	 *         <code>null</code> or no service is registered
	 */
	public static <T> T getService(Class<T> clazz, BundleContext context) {
		if (context == null) {
			return null;
		}
		ServiceReference<T> serviceReference = context.getServiceReference(clazz);
		if (serviceReference == null) {
			return null;
		}
		return context.getService(serviceReference);
	}

	/**
	 * You should be sure that OSGI is not init yet. Uses
	 * {@link ServiceTracker#addingService(ServiceReference)}
	 */
	public static <T> void lazyLoading(Class<T> clazz, Consumer<T> consumer) {
		BundleContext context = FrameworkUtil.getBundle(clazz).getBundleContext();
		if (context == null) {
			return;
		}

		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, clazz, null) {
			@Override
			public T addingService(ServiceReference<T> reference) {
				T service = super.addingService(reference);
				consumer.accept(service);
				return service;
			}
		};
		tracker.open();
	}
}
