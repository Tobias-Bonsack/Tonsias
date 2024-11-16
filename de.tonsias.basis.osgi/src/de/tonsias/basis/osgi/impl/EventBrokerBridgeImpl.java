package de.tonsias.basis.osgi.impl;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.EventHandler;

import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import jakarta.inject.Inject;

public class EventBrokerBridgeImpl implements IEventBrokerBridge {

	@Inject
	private IEventBroker _broker;

	@Override
	public boolean send(String topic, Object data) {
		return _broker.send(topic, data);
	}

	@Override
	public boolean post(String topic, Object data) {
		return _broker.post(topic, data);
	}

	@Override
	public boolean subscribe(String topic, EventHandler eventHandler, boolean headless) {
		return _broker.subscribe(topic, null, eventHandler, headless);
	}

	@Override
	public boolean unSubscribe(EventHandler eventHandler) {
		return _broker.unsubscribe(eventHandler);
	}

	@Override
	public IEventBroker getEclipseBroker() {
		return _broker;
	}

}
