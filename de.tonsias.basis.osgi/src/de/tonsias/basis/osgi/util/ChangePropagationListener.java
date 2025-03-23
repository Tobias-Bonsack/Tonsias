package de.tonsias.basis.osgi.util;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedChildChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ParentChange;
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
	private void newInstanzListener(@EventTopic(InstanzEventConstants.NEW) Event event) {
		InstanzEvent data = (InstanzEvent) event.getProperty(IEventBroker.DATA);
		_instanz.resolveKey(data._key()).ifPresent(child -> _instanz.putChild(child.getParentKey(), child.getOwnKey()));
	}

	@Inject
	@Optional
	private void changeChildCollectionListener(@EventTopic(InstanzEventConstants.CHILD_LIST_CHANGE) Event event) {
		LinkedChildChangeEvent data = (LinkedChildChangeEvent) event.getProperty(IEventBroker.DATA);

		switch (data._changeType()) {
		case ADD:
			// TODO: change parent service
			data._instanzKeys().forEach(key -> _instanz.changeParent(key, data._key()));
			break;
		case REMOVE:
			// TODO: removeparent? wird inkonsistent damit... remove == remove from cache?
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + data._changeType());
		}
	}

	@Inject
	@Optional
	private void changeParentListener(@EventTopic(InstanzEventConstants.PARENT_CHANGE) Event event) {
		ParentChange data = (ParentChange) event.getProperty(IEventBroker.DATA);

	}

}
