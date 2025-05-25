package de.tonsias.basis.osgi.util;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.*;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.*;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Creatable
@Singleton
public class ChangePropagationListener {

	IInstanzService _instanz;

	ISingleValueService _singleValue;

	@PostConstruct
	public void loadServices() {
		OsgiUtil.lazyLoading(IInstanzService.class, this::initInstanz);
		OsgiUtil.lazyLoading(ISingleValueService.class, this::initSingleValue);
	}

	private void initInstanz(IInstanzService service) {
		_instanz = service;
	}

	private void initSingleValue(ISingleValueService service) {
		_singleValue = service;
	}

	@Inject
	@Optional
	public void newInstanzListener(@EventTopic(InstanzEventConstants.NEW) Event event) {
		InstanzEvent data = (InstanzEvent) event.getProperty(IEventBroker.DATA);
		_instanz.putChild(data._parentKey(), data._key(), IEventBrokerBridge.Type.SEND);
	}

	@Inject
	@Optional
	public void changeChildCollectionListener(@EventTopic(InstanzEventConstants.CHILD_LIST_CHANGE) Event event) {
		LinkedChildChangeEvent data = (LinkedChildChangeEvent) event.getProperty(IEventBroker.DATA);

		switch (data._changeType()) {
		case ADD:
			data._instanzKeys().forEach(key -> _instanz.changeParent(key, data._key(), IEventBrokerBridge.Type.SEND));
			break;
		case REMOVE:
			for (String childKey : data._instanzKeys()) {
				java.util.Optional<IInstanz> child = _instanz.resolveKey(childKey);
				if (child.isEmpty() // if parent is there and it is not the same
						|| (child.get().getParentKey() != null && !data._key().equals(child.get().getParentKey()))) {
					continue;
				}
				_instanz.deleteInstanz(childKey, IEventBrokerBridge.Type.SEND);
			}
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + data._changeType());
		}
	}

	@Inject
	@Optional
	public void changeParentListener(@EventTopic(InstanzEventConstants.PARENT_CHANGE) Event event) {
		ParentChange data = (ParentChange) event.getProperty(IEventBroker.DATA);
		_instanz.putChild(data._newParentKey(), data._key(), IEventBrokerBridge.Type.SEND);
		_instanz.removeChild(data._oldParentKey(), data._key(), IEventBrokerBridge.Type.SEND);
	}

	@Inject
	@Optional
	public void deleteInstanzListener(@EventTopic(InstanzEventConstants.DELETE) Event event) {
		InstanzEvent data = (InstanzEvent) event.getProperty(IEventBroker.DATA);
		java.util.Optional<IInstanz> instanz = _instanz.resolveKey(data._key());
		instanz.ifPresent(i -> {
			i.getChildren().forEach(child -> _instanz.markInstanzAsDelete(child, Type.SEND));
		});
	}

	/**
	 * ------------- Start SingleValue Events -------------
	 */

	@Inject
	@Optional
	public void newSingleValueListener(@EventTopic(SingleValueEventConstants.NEW) Event event) {
		SingleValueEvent data = (SingleValueEvent) event.getProperty(IEventBroker.DATA);
		data.
	}

}
