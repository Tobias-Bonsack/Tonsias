package de.tonsias.basis.ui.provider;

import java.util.Optional;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.impl.value.SingleStringValue;
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

	@Override
	public String getText(Object element) {
		TreeNodeWrapper treeNodeWrapper = (TreeNodeWrapper) element;
		IObject object = treeNodeWrapper.getObject();
		Optional<String> textValue = _prefService.getValue(IBasicPreferenceService.MODEL_VIEW_TEXT, String.class);
		if (!(object instanceof IInstanz) || textValue.isEmpty()) {
			return treeNodeWrapper.toString();
		}

		if (textValue.isPresent()) {
			String nameKey = ((IInstanz) object).getSingleValues(SingleValueTypes.SINGLE_STRING).inverse()
					.get(textValue.get());
			Optional<SingleStringValue> nameValue = _singleServise.resolveKey(SingleValueTypes.SINGLE_STRING.getPath(),
					nameKey, SingleStringValue.class);
			return nameValue.isEmpty() ? treeNodeWrapper.toString() : nameValue.get().getValue();
		}
		return treeNodeWrapper.toString();
	}

}
