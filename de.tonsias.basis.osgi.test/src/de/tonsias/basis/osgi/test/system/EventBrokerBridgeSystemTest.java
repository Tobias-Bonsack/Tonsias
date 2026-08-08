package de.tonsias.basis.osgi.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.test.EventRecorder;
import de.tonsias.basis.osgi.test.ProductRuntime;

/**
 * The bridge every service fires through, against the real e4 broker behind it.
 * <p>
 * It is a thin facade, and the two things worth pinning down are exactly the
 * two a facade can get wrong: that an event actually arrives, and that the
 * {@code headless} flag survives the call. Without that flag e4 only delivers
 * on the UI thread, and no service-side listener - not
 * {@code ChangePropagationListener}, not {@code DeltaServiceImpl} - would ever
 * be called in a headless run.
 * </p>
 */
public class EventBrokerBridgeSystemTest {

	private static final String TOPIC = "test/bridge/topic";

	private static final String OTHER_TOPIC = "test/bridge/other";

	IEventBrokerBridge _bridge;

	EventRecorder _recorder;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_bridge = ProductRuntime.broker();
		_recorder = EventRecorder.subscribeTo(_bridge, TOPIC);
	}

	@AfterEach
	void afterEach() {
		_recorder.unsubscribe();
	}

	@Test
	void testSend_deliversSynchronouslyBeforeItReturns() {
		assertThat(_bridge.send(TOPIC, "data"), is(true));

		// no waiting: send is what the services use when the follow-up chain has to
		// have run by the time the call comes back
		assertThat(_recorder.topics(), contains(TOPIC));
		assertThat(_recorder.events().get(0).getProperty(IEventBroker.DATA), is("data"));
	}

	@Test
	void testPost_deliversAsynchronously() {
		assertThat(_bridge.post(TOPIC, "data"), is(true));

		_recorder.awaitCount(1);

		assertThat(_recorder.topics(), contains(TOPIC));
		assertThat(_recorder.events().get(0).getProperty(IEventBroker.DATA), is("data"));
	}

	/**
	 * A handler subscribed headless has to be called on the thread that sent the
	 * event - the test thread here, the service thread in the product.
	 */
	@Test
	void testSubscribe_headlessHandlerRunsOnTheSendingThread() {
		Thread[] handled = new Thread[1];
		_bridge.subscribe(OTHER_TOPIC, event -> handled[0] = Thread.currentThread(), true);

		_bridge.send(OTHER_TOPIC, "data");

		assertThat(handled[0], is(sameInstance(Thread.currentThread())));
	}

	@Test
	void testSubscribe_onlyTheSubscribedTopicArrives() {
		_bridge.send(OTHER_TOPIC, "data");

		assertThat(_recorder.events(), hasSize(0));
	}

	@Test
	void testUnSubscribe_stopsTheDelivery() {
		_bridge.send(TOPIC, "first");
		assertThat(_bridge.unSubscribe(_recorder), is(true));

		_bridge.send(TOPIC, "second");

		assertThat(_recorder.topics(), contains(TOPIC));
	}

	/**
	 * Parts that need e4's own broker - for {@code @UIEventTopic} delivery - get it
	 * from here rather than injecting a second one.
	 */
	@Test
	void testGetEclipseBroker_isTheBrokerTheBridgeSendsThrough() {
		IEventBroker eclipseBroker = _bridge.getEclipseBroker();

		assertThat(eclipseBroker, is(notNullValue()));
		assertThat(_bridge.getEclipseBroker(), is(sameInstance(eclipseBroker)));

		eclipseBroker.send(TOPIC, "straight through");

		assertThat(_recorder.topics(), contains(TOPIC));
	}
}
