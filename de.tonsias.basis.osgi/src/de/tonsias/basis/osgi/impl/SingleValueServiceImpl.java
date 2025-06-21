package de.tonsias.basis.osgi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.*;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.LinkedInstanzChangeEvent.ChangeType;;

@Component
public class SingleValueServiceImpl implements ISingleValueService {

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

	private final Map<String, ISingleValue<?>> _cache = new HashMap<>();

	@Override
	public <E extends ISingleValue<?>> Optional<E> resolveKey(String path, String key, Class<E> clazz) {
		if (key == null || key.isBlank()) {
			return Optional.empty();
		}

		if (_cache.containsKey(key)) {
			ISingleValue<?> value = _cache.get(key);
			return Optional.ofNullable(clazz.isInstance(value) ? clazz.cast(value) : null);
		}

		if (path == null || path.isBlank()) {
			return Optional.empty();
		}
		E singleValue = _loadService.loadFromGson(path + key, clazz);
		_cache.put(key, singleValue);
		return Optional.ofNullable(singleValue);
	}

	@Override
	public <E extends ISingleValue<?>> Collection<E> resolveKeys(Class<E> clazz, String path, Collection<String> keys) {
		Collection<E> result = new ArrayList<E>();
		keys.stream()//
				.map(key -> resolveKey(path, key, clazz))//
				.filter(o -> o.isPresent())//
				.map(o -> o.get())//
				.forEach(ssv -> result.add(ssv));
		return result;
	}

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

		E singleValue = create(type.get());
		singleValue.addConnectedInstanzKey(parentKey);
		singleValue.tryToSetValue(value);
		_cache.put(singleValue.getOwnKey(), singleValue);

		var data = new SingleValueEventConstants.SingleValueNewEvent(type.get(), singleValue.getOwnKey(), parameterName,
				List.of(parentKey));
		fireEvent(eventType, SingleValueEventConstants.NEW, data);

		return singleValue;
	}

	@SuppressWarnings("unchecked") // is checked in reality
	private <E extends ISingleValue<?>> E create(SingleValueType type) {
		String key = _keyService.generateKey();
		switch (type) {
		case SINGLE_STRING:
			return (E) new SingleStringValue(key);
		case SINGLE_INTEGER:
			return (E) new SingleIntegerValue(key);
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}
	}

	@Override
	public void saveAll() {
		_cache.values().stream().forEach(i -> _saveService.safeAsGson(i, i.getClass()));
	}

	@Override
	public boolean saveAll(Set<String> singlevalueKeysToSave) {
		var singleValuesToSave = singlevalueKeysToSave.stream()//
				.map(key -> resolveKey(null, key, ISingleValue.class))//
				.filter(opt -> opt.isPresent()).map(opt -> opt.get())//
				.collect(Collectors.toUnmodifiableList());
		singleValuesToSave.forEach(i -> _saveService.safeAsGson(i, i.getClass()));
		return true;
	}

	@Override
	public boolean removeFromCache(String... keys) {
		return !Stream.of(keys).map(key -> _cache.remove(key)).anyMatch(removedValue -> removedValue == null);
	}

	@Override
	public boolean changeValue(String ownKey, Object newValue, IEventBrokerBridge.Type eventType) {
		ISingleValue<?> value = _cache.get(ownKey);
		Object oldValue = value.getValue();
		boolean isChanged = value.tryToSetValue(newValue);
		if (isChanged) {
			var type = SingleValueType.getByClass(value.getClass()).orElseGet(() -> null);
			var data = new SingleValueEventConstants.ValueChangeEvent(ownKey, type, oldValue, newValue);
			fireEvent(eventType, SingleValueEventConstants.VALUE_CHANGE, data);
		}
		return isChanged;
	}

	@Override
	public boolean removeValue(ISingleValue<?> valueToDelete, IEventBrokerBridge.Type eventType) {
		SingleValueType.getByClass(valueToDelete.getClass()).ifPresent(type -> {
			markSingleValueAsDelete(valueToDelete.getOwnKey(), eventType);
		});
		return true;
	}

	@Override
	public void markSingleValueAsDelete(String singleValueKeyToMark, Type eventType) {
		var value = _cache.get(singleValueKeyToMark);
		Optional<SingleValueType> optional = SingleValueType.getByClass(value.getClass());
		optional.ifPresent(type -> {
			Collection<String> connectedInstanzKeys = value.getConnectedInstanzKeys();
			value.removeConnection(connectedInstanzKeys);
			var data = new SingleValueEventConstants.SingleValueDeleteEvent(type, singleValueKeyToMark, connectedInstanzKeys);
			fireEvent(eventType, SingleValueEventConstants.DELETE, data);
		});
	}

	@Override
	public boolean deleteAll(Set<String> singlevalueKeysToDelete) throws CompletionException {
		CompletionException ex = new CompletionException(null);
		for (String key : singlevalueKeysToDelete) {
			try {
				_deleteService.deleteFile(key);
			} catch (IOException e) {
				ex.addSuppressed(ex);
			}
		}

		if (ex.getSuppressed().length > 0) {
			throw ex;
		}

		return true;
	}

	private void fireEvent(Type eventType, String eventName, Object data) {
		switch (eventType) {
		case POST -> _broker.post(eventName, Map.of(IEventBroker.DATA, data));
		case SEND -> _broker.send(eventName, Map.of(IEventBroker.DATA, data));
		default -> throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean addToParent(SingleValueType type, String valueKey, String parentKey, Type eventType) {
		Optional<? extends ISingleValue<?>> sv = resolveKey(type.getPath(), valueKey, type.getClazz());
		if (sv.isEmpty()) {
			return false;
		}

		boolean isAdded = sv.get().addConnectedInstanzKey(parentKey);
		if (isAdded) {
			LinkedInstanzChangeEvent data = new LinkedInstanzChangeEvent(valueKey, type, ChangeType.ADD,
					Collections.singleton(parentKey));
			fireEvent(eventType, SingleValueEventConstants.INSTANZ_LIST_CHANGE, data);
		}
		return isAdded;
	}
}
