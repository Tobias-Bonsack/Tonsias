package de.tonsias.basis.osgi.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleBooleanValue;
import de.tonsias.basis.model.impl.value.SingleFloatValue;
import de.tonsias.basis.model.impl.value.SingleInstanzValue;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.LinkedInstanzChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.SingleValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.ValueEventConstants.ChangeType;

/**
 * What is left once {@link AValueServiceImpl} has taken the cache, the folder
 * scans and the save and delete: which class a type builds, what "the value
 * changed" means for one content, and which event records this family fires.
 */
@Component
public class SingleValueServiceImpl extends AValueServiceImpl<ISingleValue<?>, SingleValueType>
		implements ISingleValueService {

	@Reference
	SaveService _saveService;

	@Reference
	LoadService _loadService;

	@Reference
	DeleteService _deleteService;

	@Reference
	IKeyService _keyService;

	@Reference
	IEventBrokerBridge _broker;

	@Override
	protected SaveService saveService() {
		return _saveService;
	}

	@Override
	protected LoadService loadService() {
		return _loadService;
	}

	@Override
	protected DeleteService deleteService() {
		return _deleteService;
	}

	@Override
	protected IKeyService keyService() {
		return _keyService;
	}

	@Override
	protected IEventBrokerBridge broker() {
		return _broker;
	}

	@Override
	protected SingleValueType[] types() {
		return SingleValueType.values();
	}

	@Override
	protected SingleValueType typeOf(ISingleValue<?> value) {
		return value.getType();
	}

	@Override
	protected ISingleValue<?> newInstance(SingleValueType type, String key) {
		return switch (type) {
		case SINGLE_STRING -> new SingleStringValue(key);
		case SINGLE_INTEGER -> new SingleIntegerValue(key);
		case SINGLE_BOOLEAN -> new SingleBooleanValue(key);
		case SINGLE_FLOAT -> new SingleFloatValue(key);
		case SINGLE_INSTANZ -> new SingleInstanzValue(key);
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	@Override
	protected void fireDelete(SingleValueType type, ISingleValue<?> value, Collection<String> ownerKeys,
			Type eventType) {
		fireEvent(eventType, SingleValueEventConstants.DELETE,
				new SingleValueDeleteEvent(type, value.getOwnKey(), ownerKeys));
	}

	@Override
	protected void fireInstanzListChange(SingleValueType type, String valueKey, ChangeType change,
			Collection<String> instanzKeys, Type eventType) {
		fireEvent(eventType, SingleValueEventConstants.INSTANZ_LIST_CHANGE,
				new LinkedInstanzChangeEvent(valueKey, type, change, instanzKeys));
	}

	@SuppressWarnings("unchecked") // the type was looked up from clazz, so create builds exactly that class
	@Override
	public <E extends ISingleValue<?>> E createNew(Class<E> clazz, String parentKey, String parameterName, Object value,
			IEventBrokerBridge.Type eventType) {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(parentKey);
		Objects.requireNonNull(parameterName);
		Objects.requireNonNull(value);

		Optional<SingleValueType> type = SingleValueType.getByClass(clazz);
		if (type.isEmpty()) {
			return null;
		}

		ISingleValue<?> singleValue = create(type.get());
		singleValue.addConnectedInstanzKey(parentKey);
		singleValue.tryToSetValue(value);
		cache(singleValue);

		var data = new SingleValueEventConstants.SingleValueNewEvent(type.get(), singleValue.getOwnKey(), parameterName,
				List.of(parentKey));
		fireEvent(eventType, SingleValueEventConstants.NEW, data);

		return (E) singleValue;
	}

	@Override
	public boolean changeValue(String ownKey, Object newValue, IEventBrokerBridge.Type eventType) {
		// off the cache alone this used to throw a NullPointerException for every value
		// that had not been touched in this session - which after a restart is every
		// value there is, see
		// https://github.com/Tobias-Bonsack/Tonsias/issues/78
		Optional<ISingleValue<?>> resolved = resolveAnyKey(ownKey);
		if (resolved.isEmpty()) {
			return false;
		}

		ISingleValue<?> value = resolved.get();
		Object oldValue = value.getValue();
		boolean isChanged = value.tryToSetValue(newValue);
		if (isChanged) {
			var data = new SingleValueEventConstants.ValueChangeEvent(ownKey, value.getType(), oldValue, newValue);
			fireEvent(eventType, SingleValueEventConstants.VALUE_CHANGE, data);
		}
		return isChanged;
	}
}
