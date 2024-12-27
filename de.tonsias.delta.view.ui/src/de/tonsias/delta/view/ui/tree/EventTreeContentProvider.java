package de.tonsias.delta.view.ui.tree;

import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.osgi.service.event.Event;

public class EventTreeContentProvider implements ILazyTreeContentProvider {

	private final TreeViewer _viewer;

	public EventTreeContentProvider(TreeViewer viewer) {
		_viewer = viewer;
	}

	@Override
	public void updateElement(Object parent, int index) {
		EventTreeNodeWrapper wrapper = EventTreeNodeWrapper.class.cast(parent);
		Event event = wrapper.getChildAt(index);
		if (event == null) {
			return;
		}
		EventTreeNodeWrapper childWrapper = new EventTreeNodeWrapper(event, wrapper);
		_viewer.replace(parent, index, childWrapper);
		updateChildCount(childWrapper, -1);
	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		int childCount = EventTreeNodeWrapper.class.cast(element).getChildCount();
		if (childCount != currentChildCount) {
			_viewer.setChildCount(element, childCount);
		}
	}

	@Override
	public Object getParent(Object element) {
		return EventTreeNodeWrapper.class.cast(element).getParent();
	}

}
