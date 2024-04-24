package de.tonsias.basis.ui;

import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IInstanzService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class test {

	@Inject
	IInstanzService _instanzService;

	@PostConstruct
	public void postConstruct(Composite parent) {
		
		Tree tree = new Tree(parent, SWT.V_SCROLL);
		
		
		ButtonFactory.newButton(SWT.None).text("button").onSelect(e -> createRoot()).create(parent);
	}

	private void createRoot() {
		_instanzService.getRoot();
	}
	
	private TreeItem createItem(IInstanz instanz) {
		return null;
	}

}
