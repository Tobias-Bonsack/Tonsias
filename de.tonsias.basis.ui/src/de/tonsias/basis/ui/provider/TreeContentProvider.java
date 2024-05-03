package de.tonsias.basis.ui.provider;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import jakarta.inject.Inject;

public class TreeContentProvider implements ILazyTreeContentProvider {

	@Inject
	IInstanzService _instanzService;

	@Inject
	ISingleValueService _singleService;

	TreeViewer _viewer;

	public TreeContentProvider(TreeViewer viewer) {
		_viewer = viewer;
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateElement(Object parent, int index) {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		// TODO Auto-generated method stub

	}

}
