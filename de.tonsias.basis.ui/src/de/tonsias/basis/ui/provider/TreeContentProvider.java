package de.tonsias.basis.ui.provider;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.ui.node.TreeNodeWrapper;

public class TreeContentProvider implements ILazyTreeContentProvider {

	IInstanzService _instanzService;

	ISingleValueService _singleService;

	TreeViewer _viewer;

	public TreeContentProvider(TreeViewer viewer) {
		_viewer = viewer;
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		ServiceReference<IInstanzService> instanzServiceRef = bundleContext.getServiceReference(IInstanzService.class);
		if (instanzServiceRef != null) {
			_instanzService = bundleContext.getService(instanzServiceRef);
		}

		ServiceReference<ISingleValueService> singleServiceRef = bundleContext
				.getServiceReference(ISingleValueService.class);
		if (singleServiceRef != null) {
			_singleService = bundleContext.getService(singleServiceRef);
		}
	}

	@Override
	public Object getParent(Object element) {
		return ((TreeNodeWrapper) element).getParent();
	}

	@Override
	public void updateElement(Object parent, int index) {
		TreeNodeWrapper child = ((TreeNodeWrapper) parent).getChildAt(index);
		if (child == null) {
			return;
		}
		_viewer.replace(parent, index, child);
		updateChildCount(child, -1);
	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		int childCount = ((TreeNodeWrapper) element).getChildCount();
		if (currentChildCount != childCount) {
			_viewer.setChildCount(element, childCount);
		}
	}

}
