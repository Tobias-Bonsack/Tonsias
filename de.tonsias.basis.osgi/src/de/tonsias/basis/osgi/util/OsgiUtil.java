package de.tonsias.basis.osgi.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class OsgiUtil {
	public static <T> T getService(Class<T> clazz) {
		BundleContext context = FrameworkUtil.getBundle(clazz).getBundleContext();
		ServiceReference<T> serviceReference = context.getServiceReference(clazz);
		if (context == null || serviceReference == null) {
			return null;
		}
		return context.getService(serviceReference);
	}
}
