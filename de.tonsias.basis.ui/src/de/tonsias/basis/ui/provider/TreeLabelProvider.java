package de.tonsias.basis.ui.provider;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.node.TreeNodeWrapper;

public class TreeLabelProvider implements ILabelProvider {

	IBasicPreferenceService _prefService = OsgiUtil.getService(IBasicPreferenceService.class);

	IInstanzService _instanzService = OsgiUtil.getService(IInstanzService.class);

	ISingleValueService _singleServise = OsgiUtil.getService(ISingleValueService.class);

	InstanzChoices _choices = new InstanzChoices(_instanzService, _singleServise, _prefService);

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Image getImage(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Values read as themselves; an instanz reads by the rule in
	 * {@link InstanzChoices#labelOf(IInstanz)}, which the chooser of a relation is
	 * filled from as well - the same instanz has to read the same in both places.
	 */
	@Override
	public String getText(Object element) {
		TreeNodeWrapper treeNodeWrapper = (TreeNodeWrapper) element;
		IObject object = treeNodeWrapper.getObject();
		if (!(object instanceof IInstanz instanz)) {
			return treeNodeWrapper.toString();
		}
		return _choices.labelOf(instanz);
	}

}
