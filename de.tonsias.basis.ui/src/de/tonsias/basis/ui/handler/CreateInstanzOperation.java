package de.tonsias.basis.ui.handler;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.dialog.CreateInstanzDialog;

public class CreateInstanzOperation {

	private IInstanz _parent;
	private IInstanz _createdInstanz;

	public CreateInstanzOperation() {

	}

	public CreateInstanzOperation(IInstanz parent) {
		_parent = parent;
	}

	@Execute
	public void execute(IEventBroker broker) {
		var dialog = new CreateInstanzDialog(Display.getCurrent().getActiveShell(), _parent);
		
		if(dialog.open() == Window.OK) {
			
		}
	}

	@CanExecute
	public boolean canExecute() {
		return true;
	}

	public IInstanz get_createdInstanz() {
		return _createdInstanz;
	}

}
