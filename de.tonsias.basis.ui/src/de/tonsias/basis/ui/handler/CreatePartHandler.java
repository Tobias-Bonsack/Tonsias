package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

public class CreatePartHandler {

	static final String PART_ID = "partID";

	@Execute
	public void execute(MDirectMenuItem item, EPartService pService) {
		MPart part = pService.createPart(item.getPersistedState().get(PART_ID));
		pService.showPart(part, PartState.ACTIVATE);

		System.out.println(this.getClass().getSimpleName());
	}

}
