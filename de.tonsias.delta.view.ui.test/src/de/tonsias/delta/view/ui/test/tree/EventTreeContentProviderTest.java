package de.tonsias.delta.view.ui.test.tree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.delta.view.ui.tree.EventTreeContentProvider;
import de.tonsias.delta.view.ui.tree.EventTreeNodeWrapper;

/**
 * The provider is lazy: the viewer asks for one index at a time and the
 * provider answers by wrapping the matching event and immediately telling the
 * viewer how many children that new node has.
 */
@ExtendWith(MockitoExtension.class)
public class EventTreeContentProviderTest {

	@Mock
	IDeltaService _deltaService;

	@Mock
	TreeViewer _viewer;

	private EventTreeContentProvider _provider;

	@BeforeEach
	void beforeEach() {
		EventTreeNodeWrapper._deltaService = _deltaService;
		_provider = new EventTreeContentProvider(_viewer);
	}

	private static Event event(String topic) {
		return new Event(topic, Map.of());
	}

	private EventTreeNodeWrapper captureReplacedWrapper(Object parent, int index) {
		ArgumentCaptor<Object> child = ArgumentCaptor.captor();
		verify(_viewer).replace(same(parent), eq(index), child.capture());
		return EventTreeNodeWrapper.class.cast(child.getValue());
	}

	@Test
	void testUpdateElement_wrapsTheChildAndReportsItsChildCount() {
		Event open = event(EventConstants.OPEN_OPERATION);
		Event child = event("some/topic");
		when(_deltaService.getDeltas()).thenReturn(List.of(open, child, event(EventConstants.CLOSE_OPERATION)));
		var parent = new EventTreeNodeWrapper(open, null);

		_provider.updateElement(parent, 0);

		EventTreeNodeWrapper wrapper = captureReplacedWrapper(parent, 0);
		assertThat(wrapper.getParent(), is(sameInstance(parent)));
		assertThat(wrapper.toString(), is(child.toString()));
		verify(_viewer).setChildCount(wrapper, 0);
	}

	@Test
	void testUpdateElement_aNestedOperationKeepsItsOwnChildren() {
		Event open = event(EventConstants.OPEN_OPERATION);
		Event nested = event(EventConstants.OPEN_OPERATION);
		Event close = event(EventConstants.CLOSE_OPERATION);
		when(_deltaService.getDeltas()).thenReturn(List.of(open, nested, event("a"), event("b"), close, close));
		var parent = new EventTreeNodeWrapper(open, null);

		_provider.updateElement(parent, 0);

		EventTreeNodeWrapper wrapper = captureReplacedWrapper(parent, 0);
		verify(_viewer).setChildCount(wrapper, 2);
	}

	@Test
	void testUpdateElement_asksTheViewerForTheIndexItWasGiven() {
		Event open = event(EventConstants.OPEN_OPERATION);
		Event second = event("second");
		when(_deltaService.getDeltas())
				.thenReturn(List.of(open, event("first"), second, event(EventConstants.CLOSE_OPERATION)));
		var parent = new EventTreeNodeWrapper(open, null);

		_provider.updateElement(parent, 1);

		assertThat(captureReplacedWrapper(parent, 1).toString(), is(second.toString()));
	}

	@Test
	void testUpdateElement_theStartEventIsARootWithChildren() {
		Event start = event(IDeltaService.START_TOPIC);
		Event child = event("some/topic");
		when(_deltaService.getDeltas()).thenReturn(List.of(start, child));
		var root = new EventTreeNodeWrapper(start, null);

		_provider.updateElement(root, 0);

		assertThat(captureReplacedWrapper(root, 0).toString(), is(child.toString()));
	}

	/**
	 * The {@code event == null} guard in {@code updateElement} is unreachable -
	 * {@code getChildAt} indexes a list and throws instead of returning null. The
	 * viewer never asks beyond the reported child count, so this is a note, not a
	 * defect.
	 */
	@Test
	void testUpdateElement_indexBeyondTheChildrenThrows() {
		Event open = event(EventConstants.OPEN_OPERATION);
		when(_deltaService.getDeltas()).thenReturn(List.of(open, event(EventConstants.CLOSE_OPERATION)));
		var parent = new EventTreeNodeWrapper(open, null);

		assertThrows(IndexOutOfBoundsException.class, () -> _provider.updateElement(parent, 0));
		verify(_viewer, never()).replace(any(), anyInt(), any());
	}

	@Test
	void testUpdateChildCount_setsTheCountWhenItChanged() {
		Event open = event(EventConstants.OPEN_OPERATION);
		when(_deltaService.getDeltas()).thenReturn(List.of(open, event("a"), event(EventConstants.CLOSE_OPERATION)));
		var element = new EventTreeNodeWrapper(open, null);

		_provider.updateChildCount(element, 0);

		verify(_viewer).setChildCount(element, 1);
	}

	@Test
	void testUpdateChildCount_anUnchangedCountDoesNotTouchTheViewer() {
		Event open = event(EventConstants.OPEN_OPERATION);
		when(_deltaService.getDeltas()).thenReturn(List.of(open, event("a"), event(EventConstants.CLOSE_OPERATION)));
		var element = new EventTreeNodeWrapper(open, null);

		_provider.updateChildCount(element, 1);

		verifyNoMoreInteractions(_viewer);
	}

	@Test
	void testUpdateChildCount_aLeafReportsNoChildren() {
		var element = new EventTreeNodeWrapper(event("some/topic"), null);

		_provider.updateChildCount(element, -1);

		verify(_viewer).setChildCount(element, 0);
	}

	@Test
	void testGetParent_isTheWrappersParent() {
		var parent = new EventTreeNodeWrapper(event(EventConstants.OPEN_OPERATION), null);
		var child = new EventTreeNodeWrapper(event("some/topic"), parent);

		assertThat(_provider.getParent(child), is(sameInstance(parent)));
		assertThat(_provider.getParent(parent), is(nullValue()));
	}

	@Test
	void testUpdateElement_aNonWrapperElementIsRejected() {
		assertThrows(ClassCastException.class, () -> _provider.updateElement("not a wrapper", 0));
	}

	@Test
	void testUpdateChildCount_isRejectedForANullElement() {
		assertThrows(NullPointerException.class, () -> _provider.updateChildCount(null, 0));
	}
}
