package de.tonsias.basis.osgi.impl.util;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.osgi.impl.EventBrokerBridgeImpl;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;

@Component(service = IContextFunction.class, //
		property = "service.context.key=de.tonsias.basis.osgi.intf.IEventBrokerBridge")
public class EventBrokerContextFunction extends ContextFunction {

	@Override
	public Object compute(IEclipseContext context) {
		var eventBrokerBridge = ContextInjectionFactory.make(EventBrokerBridgeImpl.class, context);

		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		bundleContext.registerService(IEventBrokerBridge.class, eventBrokerBridge, null);

		return eventBrokerBridge;
	}
}
