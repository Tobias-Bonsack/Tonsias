package de.tonsias.delta.view.ui.tree;

import java.util.LinkedList;

import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;

public class EventTreeNodeWrapper {

	public static IDeltaService _deltaService;

	private final EventTreeNodeWrapper _parent;

	private final Event _event;

	public EventTreeNodeWrapper(Event event, EventTreeNodeWrapper parent) {
		_parent = parent;
		_event = event;
	}

	public int getChildCount() {
		if (!_event.getTopic().equals(EventConstants.OPEN_OPERATION)) {
			return 0;
		}

		return getChildren().size();
	}

	public Event getChildAt(int index) {
		return getChildren().get(index);
	}

	private LinkedList<Event> getChildren() {
		var children = new LinkedList<Event>();

		boolean foundEvent = false;
		int operations = 1;

		loop: for (var event : _deltaService.getDeltas()) {

			// Step 1: search for start
			if (!foundEvent && event == _event) {
				foundEvent = true;
				continue;
			} else if (!foundEvent) {
				continue;
			}

			// Step 2: search till operation end
			switch (event.getTopic()) {
			case EventConstants.CLOSE_OPERATION:
				if (--operations == 0) {
					break loop;
				}
				break;
			case EventConstants.OPEN_OPERATION:
				if (operations++ == 1) {
					children.add(event);
				}
				break;
			default:
				if (operations == 1) {
					children.add(event);
				}
				break;
			}
		}

		return children;
	}

	public EventTreeNodeWrapper getParent() {
		return _parent;
	}

	@Override
	public String toString() {
		return _event.toString();
	}

}
