package de.tonsias.basis.osgi.impl;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.osgi.intf.IEventBrokerBride;
import jakarta.inject.Inject;

public class EventBrokerBridgeImpl implements IEventBrokerBride {

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

}
