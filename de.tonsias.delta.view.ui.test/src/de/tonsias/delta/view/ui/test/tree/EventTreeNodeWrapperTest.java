package de.tonsias.delta.view.ui.test.tree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.delta.view.ui.tree.EventTreeNodeWrapper;

@ExtendWith(MockitoExtension.class)
public class EventTreeNodeWrapperTest {

	private static IDeltaService _service;

	@BeforeEach
	void beforeEach() {
		_service = mock(IDeltaService.class);
		EventTreeNodeWrapper._deltaService = _service;
	}

	@Test
	void testGetParent() {
		EventTreeNodeWrapper mock2 = mock(EventTreeNodeWrapper.class);
		var wrapper = new EventTreeNodeWrapper(null, mock2);
		assertThat(wrapper.getParent(), is(mock2));
	}

	@ParameterizedTest
	@MethodSource("provideTestGetChildCount")
	void testGetChildCount(Collection<Event> deltaList, Event searchEvent, int expectedChildren) {
		lenient().when(_service.getDeltas()).thenReturn(deltaList);

		var wrapper = new EventTreeNodeWrapper(searchEvent, null);
		int childCount = wrapper.getChildCount();

		assertThat(childCount, is(expectedChildren));
	}

	static Stream<Arguments> provideTestGetChildCount() {
		// not open operation event
		Event mock2 = mock(Event.class);
		when(mock2.getTopic()).thenReturn("Test");
		Arguments testZeroChild = Arguments.of(List.of(mock2), mock2, 0);

		// operation
		Event openEvent = mock(Event.class);
		when(openEvent.getTopic()).thenReturn(EventConstants.OPEN_OPERATION);
		Collection<Event> col = addEvents(new LinkedList<Event>(List.of(openEvent)), 5);
		Event closeEvent = mock(Event.class);
		when(closeEvent.getTopic()).thenReturn(EventConstants.CLOSE_OPERATION);
		col.add(closeEvent);
		addEvents(col, 10);
		Arguments testOp = Arguments.of(col, openEvent, 5);

		// operation in operation
		col = addEvents(new LinkedList<Event>(List.of(openEvent)), 5);
		col.add(openEvent);
		addEvents(col, 10);
		col.add(closeEvent);
		addEvents(col, 3);
		col.add(closeEvent);
		Arguments testOpInOp = Arguments.of(col, openEvent, 9);

		return Stream.of(testZeroChild, testOp, testOpInOp);
	}

	static Collection<Event> addEvents(Collection<Event> events, int n) {
		for (int i = 0; i < n; i++) {
			Event mock = mock(Event.class);
			when(mock.getTopic()).thenReturn("Test");
			events.add(mock);
		}
		return events;
	}
}
