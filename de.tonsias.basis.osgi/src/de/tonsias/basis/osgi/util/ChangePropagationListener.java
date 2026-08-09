package de.tonsias.basis.osgi.util;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
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
				_instanz.removeSubtreeInstanz(childKey, IEventBrokerBridge.Type.SEND);
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
			emptyReferencesPointingAt(i);
		});
	}

	/**
	 * Puts every relation pointing at this instanz back to pointing nowhere. The
	 * value itself is left standing: it is an attribute of somebody else, with a
	 * name that instanz gave it, and deleting the target is no reason to take an
	 * attribute off an instanz nobody asked to change.
	 * <p>
	 * Copied first, because emptying a value comes back around through
	 * {@link #changeSingleValueListener} and takes its key out of exactly this set.
	 * </p>
	 */
	private void emptyReferencesPointingAt(IInstanz target) {
		for (String valueKey : List.copyOf(target.getReferencingValueKeys())) {
			// changeValue resolves off the disk when it has to, so a relation stored in an
			// earlier session is reached as well - a key the set names and no file backs
			// answers false and changes nothing
			_singleValue.changeValue(valueKey, "", Type.SEND);
		}
	}

	/**
	 * ------------- Start cross Instanz to SingleValue Events -------------
	 */

	@Inject
	@Optional
	public void putSingleValueListener(@EventTopic(InstanzEventConstants.VALUE_LIST_CHANGE) Event event) {
		LinkedValueChangeEvent data = (LinkedValueChangeEvent) event.getProperty(IEventBroker.DATA);
		switch (data._changeType()) {
		case ADD:
			data._valueKeys()
					.forEach(svKey -> _singleValue.addToParent(data._singleValuetype(), svKey, data._key(), Type.SEND));
			break;
		case REMOVE:
			// TODO: add logic
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + data._changeType());
		}
	}

	/**
	 * ------------- Start cross SingleValue to Instanz Events -------------
	 */

	@Inject
	@Optional
	public void newSingleValueListener(@EventTopic(SingleValueEventConstants.NEW) Event event) {
		SingleValueNewEvent data = (SingleValueNewEvent) event.getProperty(IEventBroker.DATA);
		for (String ownerKey : data._ownerKeys()) {
			_instanz.putSingleValue(ownerKey, data._type(), data._key(), data._name(), Type.SEND);
		}

		// a relation is created pointing somewhere already, so the target learns about
		// it here rather than from a change that never comes
		targetOf(data).ifPresent(target -> _instanz.putReferencingValue(target, data._key(), Type.SEND));
	}

	@Inject
	@Optional
	public void removeSingleValueListener(@EventTopic(SingleValueEventConstants.DELETE) Event event) {
		SingleValueDeleteEvent data = (SingleValueDeleteEvent) event.getProperty(IEventBroker.DATA);
		_instanz.removeValueKey(data._ownerKeys(), data._type(), data._key(), Type.SEND);

		targetOf(data).ifPresent(target -> _instanz.removeReferencingValue(target, data._key(), Type.SEND));
	}

	/**
	 * The relation moved from one instanz to another, which is two changes: the old
	 * target stops being pointed at and the new one starts. Both services answer
	 * {@code false} and fire nothing when the key is not in the set, or already is,
	 * so a value that stayed where it was ends the chain here.
	 */
	@Inject
	@Optional
	public void changeSingleValueListener(@EventTopic(SingleValueEventConstants.VALUE_CHANGE) Event event) {
		ValueChangeEvent data = (ValueChangeEvent) event.getProperty(IEventBroker.DATA);
		if (data.getType() != SingleValueType.SINGLE_INSTANZ) {
			return;
		}

		if (data._oldValue() instanceof String oldTarget) {
			_instanz.removeReferencingValue(oldTarget, data._key(), Type.SEND);
		}
		if (data._newValue() instanceof String newTarget) {
			_instanz.putReferencingValue(newTarget, data._key(), Type.SEND);
		}
	}

	/**
	 * Where the relation named by this event points. The single value events carry
	 * the key and the type of a value, never its content, so the value itself has
	 * to be read - and only for the one type that has a target at all.
	 *
	 * @return the target key, empty for every other type and for a relation
	 *         pointing nowhere
	 */
	private java.util.Optional<String> targetOf(SingleValueEvent data) {
		if (data.getType() != SingleValueType.SINGLE_INSTANZ) {
			return java.util.Optional.empty();
		}
		return relationOf(data.getKey()).map(SingleInstanzValue::getValue).filter(target -> !target.isBlank());
	}

	private java.util.Optional<SingleInstanzValue> relationOf(String valueKey) {
		return _singleValue.resolveKey(SingleValueType.SINGLE_INSTANZ.getPath(), valueKey, SingleInstanzValue.class);
	}

	@Inject
	@Optional
	public void addToParentListener(@EventTopic(SingleValueEventConstants.INSTANZ_LIST_CHANGE) Event event) {
		LinkedInstanzChangeEvent data = (LinkedInstanzChangeEvent) event.getProperty(IEventBroker.DATA);
		switch (data._changeType()) {
		case ADD:
			data._instanzKeys().forEach(instanzKey -> _instanz.putSingleValue(instanzKey, data._singleValuetype(),
					data._key(), null, Type.SEND));
			break;
		case REMOVE:
			// TODO: add logic
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + data._changeType());
		}
	}

	/**
	 * ------------- Start SingleValue Events -------------
	 */

}
