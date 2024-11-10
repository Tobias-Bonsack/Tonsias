package de.tonsias.basis.osgi.util;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.osgi.impl.DeltaServiceImpl;
import de.tonsias.basis.osgi.intf.IDeltaService;

@Component(service = IContextFunction.class, //
		property = "service.context.key=de.tonsias.basis.osgi.intf.IDeltaService")
public class DeltaServiceContextFunction extends ContextFunction {

	@Override
	public Object compute(IEclipseContext context) {
		var deltaServiceImpl = ContextInjectionFactory.make(DeltaServiceImpl.class, context);
		deltaServiceImpl.postConstruct();

		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		bundleContext.registerService(IDeltaService.class, deltaServiceImpl, null);

		return deltaServiceImpl;
	}

}
