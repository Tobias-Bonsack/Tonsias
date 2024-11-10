package de.tonsias.basis.osgi.impl;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import jakarta.inject.Inject;

public class DeltaServiceImpl implements IDeltaService {

	@Inject
	IEventBrokerBridge _eventBride;

	public void postConstruct() {
	}
}
