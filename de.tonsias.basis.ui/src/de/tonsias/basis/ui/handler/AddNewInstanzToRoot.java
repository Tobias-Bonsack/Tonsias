package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.framework.FrameworkUtil;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.ui.node.TreeNodeWrapper;

public class AddNewInstanzToRoot {

	private IInstanz _parent;
	private IInstanz _createdInstanz;

	public AddNewInstanzToRoot() {

	}

	public AddNewInstanzToRoot(IInstanz parent) {
		_parent = parent;
	}

	@Execute
	public void execute(IEventBroker broker) {
		IInstanzService instanzS = FrameworkUtil.getBundle(TreeNodeWrapper.class).getBundleContext()
				.getService(FrameworkUtil.getBundle(TreeNodeWrapper.class).getBundleContext()
						.getServiceReference(IInstanzService.class));
		IInstanz parent = _parent == null ? instanzS.getRoot() : _parent;
		_createdInstanz = instanzS.createInstanz(parent);
	}

	@CanExecute
	public boolean canExecute() {
		return true;
	}

	public IInstanz get_createdInstanz() {
		return _createdInstanz;
	}

}
