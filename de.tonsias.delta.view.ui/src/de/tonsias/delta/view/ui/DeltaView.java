package de.tonsias.delta.view.ui;

import org.eclipse.swt.widgets.Composite;
import de.tonsias.basis.osgi.intf.IDeltaService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class DeltaView {
	
	@Inject
	IDeltaService _delta;

	@PostConstruct
	public void postConstruct(Composite parent) {
		System.out.println("Hello World!");
	}
}
