package de.tonsias.delta.view.ui.toolbar;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import de.tonsias.delta.view.ui.DeltaView;

public class RefreshHandler {

	@Execute
	public void execute(MPart part) {
		if (part.getObject() instanceof DeltaView view) {
			view.updateTree();
		}
	}

}
