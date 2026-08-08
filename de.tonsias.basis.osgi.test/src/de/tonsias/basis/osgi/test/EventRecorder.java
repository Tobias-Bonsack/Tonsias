package de.tonsias.basis.osgi.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;

/**
 * A real {@link EventHandler} on the real broker, keeping every event that
 * passes so a test can look at a whole propagation chain instead of a single
 * call.
 * <p>
 * The recorded <em>order</em> is not asserted anywhere: a mutating service call
 * is dispatched to all handlers of its topic, and whether this recorder or
 * {@code ChangePropagationListener} - which re-enters the services and thereby
 * produces the follow-up events - is served first is up to the event admin. The
 * <em>set</em> of events is deterministic, and that is what the tests pin down:
 * a missing entry means the propagation stopped early, an extra one means a
 * loop guard did not hold.
 * </p>
 */
public final class EventRecorder implements EventHandler {

	/** how long {@link #awaitCount(int)} waits for asynchronously posted events */
	private static final long TIMEOUT_MS = 5_000;

	private final Object _lock = new Object();

	private final List<Event> _events = new ArrayList<>();

	private final IEventBrokerBridge _bridge;

	private EventRecorder(IEventBrokerBridge bridge) {
		_bridge = bridge;
	}

	/**
	 * @return a recorder subscribed to the same two wildcard topics
	 *         {@link IDeltaService} listens on, headless - the tests do not run in
	 *         a UI thread
	 */
	public static EventRecorder subscribeToAllDeltas(IEventBrokerBridge bridge) {
		return subscribeTo(bridge, InstanzEventConstants.ALL_DELTA_TOPIC, SingleValueEventConstants.ALL_DELTA_TOPIC);
	}

	/**
	 * @return a recorder that also sees the {@link EventConstants#OPEN_OPERATION} /
	 *         {@link EventConstants#CLOSE_OPERATION} brackets around those deltas
	 */
	public static EventRecorder subscribeToAllDeltasAndOperations(IEventBrokerBridge bridge) {
		return subscribeTo(bridge, InstanzEventConstants.ALL_DELTA_TOPIC, SingleValueEventConstants.ALL_DELTA_TOPIC,
				EventConstants.OPEN_OPERATION, EventConstants.CLOSE_OPERATION);
	}

	public static EventRecorder subscribeTo(IEventBrokerBridge bridge, String... topics) {
		EventRecorder recorder = new EventRecorder(bridge);
		for (String topic : topics) {
			bridge.subscribe(topic, recorder, true);
		}
		return recorder;
	}

	/**
	 * The broker outlives the test class, so a recorder that is not detached keeps
	 * recording the next test's events.
	 */
	public void unsubscribe() {
		_bridge.unSubscribe(this);
	}

	@Override
	public void handleEvent(Event event) {
		synchronized (_lock) {
			_events.add(event);
			_lock.notifyAll();
		}
	}

	/** drops everything recorded so far, e.g. the events of a test's set-up */
	public void clear() {
		synchronized (_lock) {
			_events.clear();
		}
	}

	public List<Event> events() {
		synchronized (_lock) {
			return List.copyOf(_events);
		}
	}

	public List<String> topics() {
		return events().stream().map(Event::getTopic).toList();
	}

	/** @return the payloads of every recorded event on that topic */
	public <T> List<T> dataOf(String topic, Class<T> type) {
		return events().stream()//
				.filter(event -> topic.equals(event.getTopic()))//
				.map(event -> type.cast(event.getProperty(IEventBroker.DATA)))//
				.toList();
	}

	/** @return the payload of the one event expected on that topic */
	public <T> T onlyDataOf(String topic, Class<T> type) {
		List<T> data = dataOf(topic, type);
		assertThat("recorded events on " + topic, data, hasSize(1));
		return data.get(0);
	}

	/**
	 * Waits until the given number of events has arrived, for use with
	 * {@link IEventBrokerBridge.Type#POST} and with the {@code Job}s the view logic
	 * schedules. Returns early on timeout and leaves the missing events to the
	 * assertion that follows.
	 */
	public void awaitCount(int expected) {
		awaitUntil(() -> _events.size() >= expected);
	}

	/**
	 * Waits for an event on that topic - for work that runs in a {@code Job} and
	 * ends by announcing itself, where the number of events on the way there is not
	 * known up front.
	 */
	public void awaitTopic(String topic) {
		awaitUntil(() -> _events.stream().anyMatch(event -> topic.equals(event.getTopic())));
	}

	private void awaitUntil(BooleanSupplier condition) {
		long deadline = System.currentTimeMillis() + TIMEOUT_MS;
		synchronized (_lock) {
			while (!condition.getAsBoolean()) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					return;
				}
				try {
					_lock.wait(remaining);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}
}
