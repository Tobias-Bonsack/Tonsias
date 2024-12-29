package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

public class CreatePartHandler {

	static final String PART_ID = "partID";

	@Execute
	@SuppressWarnings("unchecked")
	public void execute(MDirectMenuItem item, EModelService mService, EPartService pService, MApplication app) {
		MPart foundPart = pService.findPart(item.getPersistedState().get(PART_ID));
		if (foundPart == null) {
			foundPart = pService.createPart(item.getPersistedState().get(PART_ID));
			MUIElement muiElement = mService.find("de.tonsias.basis.ui.partstack.1", app);
			if (muiElement != null) {
				muiElement.setOnTop(true);
				foundPart.setParent((MElementContainer<MUIElement>) muiElement);
			}
		}
		pService.showPart(foundPart, PartState.ACTIVATE);
	}

}
