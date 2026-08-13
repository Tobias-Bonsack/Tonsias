package de.tonsias.basis.osgi.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.*;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.ElementsChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueNewEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.*;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Keeps both ends of every relation in sync by re-entering the services with
 * {@code Type.SEND}.
 * <p>
 * That only terminates because every service method here answers {@code false}
 * and fires <em>nothing</em> when it is asked for a state that already holds.
 * That is a rule, not a courtesy: an attribute hangs on an instanz at both ends -
 * the instanz names the value key, the value names the instanz key - and the two
 * handlers that keep those in step call each other. Taking a value off an instanz
 * runs
 * {@code removeValueKey -> valueChange REMOVE -> removeFromParent -> linkedInstanzChange
 * REMOVE -> removeValueKey}, and the second time round the key is already gone,
 * so nothing is fired and the chain stops. A service that fired for a removal
 * that removed nothing would never stop.
 * </p>
 * <p>
 * The one edge that would close a cycle by itself is
 * {@link InstanzEventConstants#REFERENCE_LIST_CHANGE}: it is the last hop of the
 * relation chains below, and nothing but {@code DeltaServiceImpl} listens to it.
 * <b>No handler in this class may subscribe to it.</b>
 * </p>
 */
@Creatable
@Singleton
public class ChangePropagationListener {

	IInstanzService _instanz;

	ISingleValueService _singleValue;

	IMultiValueService _multiValue;

	@PostConstruct
	public void loadServices() {
		OsgiUtil.lazyLoading(IInstanzService.class, this::initInstanz);
		OsgiUtil.lazyLoading(ISingleValueService.class, this::initSingleValue);
		OsgiUtil.lazyLoading(IMultiValueService.class, this::initMultiValue);
	}

	private void initInstanz(IInstanzService service) {
		_instanz = service;
	}

	private void initSingleValue(ISingleValueService service) {
		_singleValue = service;
	}

	private void initMultiValue(IMultiValueService service) {
		_multiValue = service;
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
	 * Takes this instanz out of every relation pointing at it. The value itself is
	 * left standing: it is an attribute of somebody else, with a name that instanz
	 * gave it, and deleting the target is no reason to take an attribute off an
	 * instanz nobody asked to change.
	 * <p>
	 * A single relation ends up pointing nowhere, which is the state it has for
	 * exactly this. A list keeps pointing at everything else it points at - only the
	 * one element goes. Emptying it would take relations off targets nobody touched,
	 * for the same reason the value is not deleted.
	 * </p>
	 * <p>
	 * The referencing set holds bare keys and does not say which family one belongs
	 * to, so the multi service is asked first - {@code resolveAnyKey} reaches a value
	 * stored in an earlier session as well, and answers empty for a key it holds no
	 * file for. Copied first, because both calls come back around and take the key
	 * out of exactly this set.
	 * </p>
	 */
	private void emptyReferencesPointingAt(IInstanz target) {
		for (String valueKey : List.copyOf(target.getReferencingValueKeys())) {
			if (_multiValue.resolveAnyKey(valueKey).isPresent()) {
				_multiValue.removeElement(valueKey, target.getOwnKey(), Type.SEND);
				continue;
			}
			// changeValue resolves off the disk when it has to, so a relation stored in an
			// earlier session is reached as well - a key the set names and no file backs
			// answers false and changes nothing
			_singleValue.changeValue(valueKey, "", Type.SEND);
		}
	}

	/**
	 * ------------- Start cross Instanz to Value Events -------------
	 */

	@Inject
	@Optional
	public void putValueListener(@EventTopic(InstanzEventConstants.VALUE_LIST_CHANGE) Event event) {
		LinkedValueChangeEvent data = (LinkedValueChangeEvent) event.getProperty(IEventBroker.DATA);
		switch (data._changeType()) {
		case ADD:
			data._valueKeys().forEach(valueKey -> addToParent(data._valueType(), valueKey, data._key()));
			break;
		case REMOVE:
			data._valueKeys().forEach(valueKey -> removeFromParent(data._valueType(), valueKey, data._key()));
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + data._changeType());
		}
	}

	private void addToParent(IValueType type, String valueKey, String instanzKey) {
		if (type instanceof MultiValueType multi) {
			_multiValue.addToParent(multi, valueKey, instanzKey, Type.SEND);
			return;
		}
		_singleValue.addToParent((SingleValueType) type, valueKey, instanzKey, Type.SEND);
	}

	private void removeFromParent(IValueType type, String valueKey, String instanzKey) {
		if (type instanceof MultiValueType multi) {
			_multiValue.removeFromParent(multi, valueKey, instanzKey, Type.SEND);
			return;
		}
		_singleValue.removeFromParent((SingleValueType) type, valueKey, instanzKey, Type.SEND);
	}

	/**
	 * ------------- Start cross SingleValue to Instanz Events -------------
	 */

	@Inject
	@Optional
	public void newSingleValueListener(@EventTopic(SingleValueEventConstants.NEW) Event event) {
		SingleValueNewEvent data = (SingleValueNewEvent) event.getProperty(IEventBroker.DATA);
		for (String ownerKey : data._ownerKeys()) {
			_instanz.putValue(ownerKey, data._type(), data._key(), data._name(), Type.SEND);
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
	 * the key and the type of a value, never its content, so the value itself has to
	 * be read - and only for the one type that has a target at all.
	 *
	 * @return the target key, empty for every other type and for a relation pointing
	 *         nowhere
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
			data._instanzKeys().forEach(
					instanzKey -> _instanz.putValue(instanzKey, data._singleValuetype(), data._key(), null, Type.SEND));
			break;
		case REMOVE:
			_instanz.removeValueKey(data._instanzKeys(), data._singleValuetype(), data._key(), Type.SEND);
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + data._changeType());
		}
	}

	/**
	 * ------------- Start cross MultiValue to Instanz Events -------------
	 */

	@Inject
	@Optional
	public void newMultiValueListener(@EventTopic(MultiValueEventConstants.NEW) Event event) {
		MultiValueNewEvent data = (MultiValueNewEvent) event.getProperty(IEventBroker.DATA);
		for (String ownerKey : data._ownerKeys()) {
			_instanz.putValue(ownerKey, data._type(), data._key(), data._name(), Type.SEND);
		}

		// a list of relations is created pointing somewhere already, so every target
		// learns about it here rather than from a change that never comes
		targetsOf(data._type(), data._elements())
				.forEach(target -> _instanz.putReferencingValue(target, data._key(), Type.SEND));
	}

	@Inject
	@Optional
	public void removeMultiValueListener(@EventTopic(MultiValueEventConstants.DELETE) Event event) {
		MultiValueDeleteEvent data = (MultiValueDeleteEvent) event.getProperty(IEventBroker.DATA);
		_instanz.removeValueKey(data._ownerKeys(), data._type(), data._key(), Type.SEND);

		// the elements travel with the event, because by now the value may be out of
		// the cache and its file already scheduled for deletion
		targetsOf(data._type(), data._elements())
				.forEach(target -> _instanz.removeReferencingValue(target, data._key(), Type.SEND));
	}

	/**
	 * The list moved: some targets stopped being pointed at and some started. Both
	 * services answer {@code false} and fire nothing when the key is not in the set,
	 * or already is, so a list that was only reordered ends the chain here - it
	 * arrives with both collections empty.
	 */
	@Inject
	@Optional
	public void changeMultiValueListener(@EventTopic(MultiValueEventConstants.VALUES_CHANGE) Event event) {
		ElementsChangeEvent data = (ElementsChangeEvent) event.getProperty(IEventBroker.DATA);

		targetsOf(data._type(), data._removedElements())
				.forEach(target -> _instanz.removeReferencingValue(target, data._key(), Type.SEND));
		targetsOf(data._type(), data._addedElements())
				.forEach(target -> _instanz.putReferencingValue(target, data._key(), Type.SEND));
	}

	@Inject
	@Optional
	public void addToParentMultiListener(@EventTopic(MultiValueEventConstants.INSTANZ_LIST_CHANGE) Event event) {
		MultiValueEventConstants.LinkedInstanzChangeEvent data = (MultiValueEventConstants.LinkedInstanzChangeEvent) event
				.getProperty(IEventBroker.DATA);
		switch (data._changeType()) {
		case ADD:
			data._instanzKeys().forEach(
					instanzKey -> _instanz.putValue(instanzKey, data._multiValuetype(), data._key(), null, Type.SEND));
			break;
		case REMOVE:
			_instanz.removeValueKey(data._instanzKeys(), data._multiValuetype(), data._key(), Type.SEND);
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + data._changeType());
		}
	}

	/**
	 * The instanzes a list of relations points at. Unlike the single side this needs
	 * no lookup - the multi events carry their elements, which is what they are for.
	 *
	 * @return the target keys, empty for every content type that has no target
	 */
	private Stream<String> targetsOf(IValueType type, Collection<?> elements) {
		if (type.getContentType() != ValueContentType.INSTANZ) {
			return Stream.empty();
		}
		return elements.stream()//
				.filter(String.class::isInstance)//
				.map(String.class::cast)//
				.filter(key -> !key.isBlank());
	}
}
