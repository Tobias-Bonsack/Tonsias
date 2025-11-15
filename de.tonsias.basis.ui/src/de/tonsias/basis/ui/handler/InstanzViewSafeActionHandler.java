package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.ui.part.InstanzView;
import jakarta.inject.Inject;

public class InstanzViewSafeActionHandler {

	@Inject
	private MPart activePart;

	@Execute
	public void execute(Shell shell) {
		if (activePart.getObject() instanceof InstanzView view) {
			view.performSafeAction(0);
		}
	}
}
