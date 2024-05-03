package de.tonsias.basis.osgi.impl;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.ISingleValueService;

@Component
public class SingleValueServiceImpl implements ISingleValueService {

	@Reference
	SaveService _saveService;

	@Reference
	LoadService _loadService;

	@Reference
	IKeyService _keyService;

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
	public <E extends ISingleValue<?>> E createNew(Class<E> clazz, IInstanz parent, String name) {
		Optional<SingleValueTypes> type = SingleValueTypes.getByClass(clazz);
		if (type.isEmpty()) {
			return null;
		}

		E singleValue = create(clazz, type.get());
		SimpleEntry<String, Object> keyToName = new AbstractMap.SimpleEntry<String, Object>(singleValue.getOwnKey(),
				name);
		parent.addValuekeys(type.get(), keyToName);
		_cache.put(singleValue.getOwnKey(), singleValue);
		return singleValue;
	}

	@SuppressWarnings("unchecked") // is checked in reality
	private <E extends ISingleValue<?>> E create(Class<E> clazz, SingleValueTypes type) {
		String key = _keyService.generateKey();
		switch (type) {
		case SINGLE_STRING: {
			return (E) new SingleStringValue(key);
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}
	}

	@Override
	public void saveAll() {
		_cache.values().stream().forEach(i -> _saveService.safeAsGson(i, i.getClass()));
	}

}
