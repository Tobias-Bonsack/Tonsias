package de.tonsias.delta.view.ui.tree;

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

		boolean foundEvent = false;
		int pos = 0;
		for (var event : _deltaService.getDeltas()) {
			if (!foundEvent && event == _event) {
				foundEvent = true;
				pos = 0;
			} else if (foundEvent && event.getTopic().equals(EventConstants.CLOSE_OPERATION)) {
				break;
			} else {
				pos++;
			}
		}

		return pos;
	}

	public Event getChildAt(int index) {
		boolean foundEvent = false;
		int pos = 0;
		for (var event : _deltaService.getDeltas()) {
			if (!foundEvent && event == _event) {
				foundEvent = true;
				pos = 0;
			} else if (foundEvent && index == pos) {
				return event;
			} else {
				pos++;
			}
		}
		return null;
	}

	public EventTreeNodeWrapper getParent() {
		return _parent;
	}

	@Override
	public String toString() {
		return _event.toString();
	}

}
