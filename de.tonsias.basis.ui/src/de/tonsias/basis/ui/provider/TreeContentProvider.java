package de.tonsias.basis.ui.provider;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.node.TreeNodeWrapper;

public class TreeContentProvider implements ILazyTreeContentProvider {

	IInstanzService _instanzService = OsgiUtil.getService(IInstanzService.class);

	ISingleValueService _singleService = OsgiUtil.getService(ISingleValueService.class);

	TreeViewer _viewer;

	public TreeContentProvider(TreeViewer viewer) {
		_viewer = viewer;
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
