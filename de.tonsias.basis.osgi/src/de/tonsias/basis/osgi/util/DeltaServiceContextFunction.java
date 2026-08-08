package de.tonsias.basis.osgi.util;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.osgi.impl.DeltaServiceImpl;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;

/**
 * There is exactly one delta log in the application, see
 * {@link SharedInstanceContextFunction} - the Delta view renders the same log
 * that the save handler writes out.
 */
@Component(service = IContextFunction.class, //
		property = "service.context.key=de.tonsias.basis.osgi.intf.IDeltaService")
public class DeltaServiceContextFunction extends SharedInstanceContextFunction<IDeltaService> {

	public DeltaServiceContextFunction() {
		super(IDeltaService.class);
	}

	@Override
	protected IDeltaService create(IEclipseContext context) {
		// The impl also injects IInstanzService and ISingleValueService. Those two are
		// DS components with a mandatory reference to IEventBrokerBridge, so they only
		// activate once the bridge stands in the service registry - and it gets there
		// by a context being asked for it, which is what this line does. Asking here
		// keeps the order written down: leaving it to the injection of the impl's own
		// bridge field would make start-up depend on the order in which
		// Class#getDeclaredFields() hands the three fields out, and that order is
		// explicitly not guaranteed. See
		// https://github.com/Tobias-Bonsack/Tonsias/issues/56
		context.get(IEventBrokerBridge.class.getName());

		DeltaServiceImpl deltaService = ContextInjectionFactory.make(DeltaServiceImpl.class, context);
		deltaService.postConstruct();
		return deltaService;
	}

}
