package de.tonsias.basis.ui.handler;

import java.util.List;

import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public class DynamicPartMenuContributor {

	@AboutToShow
	public void aboutToShow(List<MMenuElement> items, EModelService modelService, MApplication application) {
		List<MPartDescriptor> parts = application.getDescriptors();
		for (MPartDescriptor part : parts) {
			MDirectMenuItem directItem = modelService.createModelElement(MDirectMenuItem.class);
			directItem.setLabel(part.getLabel());
			directItem.setEnabled(true);
			directItem.setVisible(true);
			directItem.getPersistedState().put("persistState", "false");
			directItem.getPersistedState().put(CreatePartHandler.PART_ID, part.getElementId());
			directItem.setContributionURI(
					"bundleclass://de.tonsias.basis.ui/de.tonsias.basis.ui.handler.CreatePartHandler");

			items.add(directItem);
		}
	}
}