package de.tonsias.basis.ui;

import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import de.tonsias.basis.osgi.intf.IInstanzService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class test {

	@Inject
	IInstanzService _instanzService;

	@PostConstruct
	public void postConstruct(Composite parent) {
		ButtonFactory.newButton(SWT.None).text("button").onSelect(e -> createRoot()).create(parent);
	}

	private void createRoot() {
		_instanzService.getRoot();
	}

}
