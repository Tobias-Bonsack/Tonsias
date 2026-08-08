package de.tonsias.basis.osgi.util;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.osgi.impl.DeltaServiceImpl;
import de.tonsias.basis.osgi.intf.IDeltaService;

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
		// the impl also injects IInstanzService and ISingleValueService, and those only
		// activate once the bridge is registered - which happens while its own field is
		// injected. That the bridge comes first is the field order and nothing else,
		// see https://github.com/Tobias-Bonsack/Tonsias/issues/56
		DeltaServiceImpl deltaService = ContextInjectionFactory.make(DeltaServiceImpl.class, context);
		deltaService.postConstruct();
		return deltaService;
	}

}
