package de.tonsias.basis.osgi.util;

import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiUtil {
	public static <T> T getService(Class<T> clazz) {
		BundleContext context = FrameworkUtil.getBundle(clazz).getBundleContext();
		return getService(clazz, context);
	}

	public static <T> T getService(Class<T> clazz, BundleContext context) {
		ServiceReference<T> serviceReference = context.getServiceReference(clazz);
		if (context == null || serviceReference == null) {
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
