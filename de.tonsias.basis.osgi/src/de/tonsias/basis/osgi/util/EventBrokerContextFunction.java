package de.tonsias.basis.osgi.util;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.osgi.impl.EventBrokerBridgeImpl;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;

/**
 * There is exactly one bridge, see {@link SharedInstanceContextFunction} - the
 * services take theirs from the registry, the parts get theirs injected, and
 * {@link IEventBrokerBridge#unSubscribe(org.osgi.service.event.EventHandler)}
 * only reaches a handler that was subscribed through the same instance.
 */
@Component(service = IContextFunction.class, //
		property = "service.context.key=de.tonsias.basis.osgi.intf.IEventBrokerBridge")
public class EventBrokerContextFunction extends SharedInstanceContextFunction<IEventBrokerBridge> {

	public EventBrokerContextFunction() {
		super(IEventBrokerBridge.class);
	}

	@Override
	protected IEventBrokerBridge create(IEclipseContext context) {
		return ContextInjectionFactory.make(EventBrokerBridgeImpl.class, context);
	}
}
