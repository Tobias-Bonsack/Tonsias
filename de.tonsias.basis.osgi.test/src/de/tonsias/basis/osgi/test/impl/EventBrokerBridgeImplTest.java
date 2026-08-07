package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.EventHandler;

import de.tonsias.basis.osgi.impl.EventBrokerBridgeImpl;

/**
 * The bridge is a thin facade, so what is worth pinning down is that it
 * forwards to the e4 broker unchanged - especially the {@code headless} flag on
 * {@code subscribe}, without which no service-side listener is ever called.
 */
@ExtendWith(MockitoExtension.class)
public class EventBrokerBridgeImplTest {

	@Mock
	IEventBroker _broker;

	@Mock
	EventHandler _handler;

	@InjectMocks
	EventBrokerBridgeImpl _bridge;

	@Test
	void testSend_forwardsAndReturnsTheResult() {
		when(_broker.send("topic", "data")).thenReturn(true);

		assertThat(_bridge.send("topic", "data"), is(true));

		verify(_broker).send("topic", "data");
	}

	@Test
	void testPost_forwardsAndReturnsTheResult() {
		when(_broker.post("topic", "data")).thenReturn(false);

		assertThat(_bridge.post("topic", "data"), is(false));

		verify(_broker).post("topic", "data");
	}

	@Test
	void testSubscribe_forwardsWithoutFilterAndKeepsHeadlessFlag() {
		when(_broker.subscribe("topic", null, _handler, true)).thenReturn(true);

		assertThat(_bridge.subscribe("topic", _handler, true), is(true));

		verify(_broker).subscribe("topic", null, _handler, true);
	}

	@Test
	void testUnSubscribe_forwards() {
		when(_broker.unsubscribe(_handler)).thenReturn(true);

		assertThat(_bridge.unSubscribe(_handler), is(true));

		verify(_broker).unsubscribe(_handler);
	}

	@Test
	void testGetEclipseBroker_isTheInjectedOne() {
		assertThat(_bridge.getEclipseBroker(), is(sameInstance(_broker)));
	}
}
