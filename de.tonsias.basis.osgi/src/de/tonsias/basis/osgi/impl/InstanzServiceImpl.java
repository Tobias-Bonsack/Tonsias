package de.tonsias.basis.osgi.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ChangeType;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedChildChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.LinkedValueChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ParentChange;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.ValueRenameEvent;

@Component
public class InstanzServiceImpl implements IInstanzService {

	@Reference
	IKeyService _keyService;

	@Reference
	LoadService _loadService;

	@Reference
	SaveService _saveService;

	@Reference
	DeleteService _deleteService;

	@Reference
	IEventBrokerBridge _broker;

	private final Map<String, IInstanz> _cache = new HashMap<>();

	@Override
	public IInstanz getRoot() {
		String rootID = String.valueOf(KeyServiceImpl.KEYCHARS[0]);
		String path = "instanz/" + rootID;

		if (_cache.containsKey(rootID)) {
			return _cache.get(rootID);
		}

		Instanz root = _loadService.loadFromGson(path, Instanz.class);

		if (root != null) {
			_cache.put(root.getOwnKey(), root);
			return root;
		}

		String key = _keyService.initKey();
		root = new Instanz(key);
		_cache.put(root.getOwnKey(), root);
		_saveService.safeAsGson(root, root.getClass());
		return root;
	}

	@Override
	public synchronized Optional<IInstanz> resolveKey(String key) {
		if (key == null) {
			return Optional.empty();
		}

		if (_cache.containsKey(key)) {
			return Optional.of(_cache.get(key));
		}

		String path = "instanz/" + key;
		Instanz instanz = _loadService.loadFromGson(path, Instanz.class);

		if (instanz != null) {
			_cache.put(key, instanz);
			return Optional.of(instanz);
		}

		return Optional.empty();
	}

	@Override
	public synchronized Collection<IInstanz> resolveKeys(Collection<String> keys) {
		List<IInstanz> result = new ArrayList<>();
		for (String key : keys) {
			if (_cache.containsKey(key)) {
				result.add(_cache.get(key));
				continue;
			}

			Optional<IInstanz> resolved = resolveKey(key);
			if (resolved.isPresent()) {
				IInstanz instanz = resolved.get();
				result.add(instanz);
			}
		}
		return result;

	}

	@Override
	public IInstanz createInstanz(String parentKey, IEventBrokerBridge.Type eventType) {
		if (parentKey == null || parentKey.isBlank()) {
			return null;
		}

		String key = _keyService.generateKey();
		Instanz instanz = new Instanz(key);
		instanz.setParentKey(parentKey);
		_cache.put(key, instanz);

		var data = new InstanzEvent(instanz.getOwnKey(), parentKey);
		fireEvent(eventType, InstanzEventConstants.NEW, data);
		return instanz;
	}

	@Override
	public boolean removeSubtreeInstanz(String instanzKey, IEventBrokerBridge.Type eventType) {
		// Remove parent relation
		Optional<IInstanz> instanz = resolveKey(instanzKey);
		if (instanz.isEmpty()) {
			return false;
		}

		// check if already parent removed
		String parentKey = instanz.get().getParentKey();
		if (parentKey == null) {
			return false;
		}

		// remove parent and tell parent to remove child
		instanz.get().setParentKey(null);
		resolveKey(parentKey).ifPresent(parent -> {
			removeChild(parent.getOwnKey(), instanzKey, eventType);
		});

		markInstanzAsDelete(instanzKey, eventType);
		return true;
	}

	@Override
	public void markInstanzAsDelete(String instanzKey, Type eventType) {
		var event = new InstanzEvent(instanzKey, null);
		fireEvent(eventType, InstanzEventConstants.DELETE, event);
	}

	@Override
	public void saveAll() {
		_cache.values().forEach(t -> _saveService.safeAsGson(t, t.getClass()));
	}

	@Override
	public boolean saveAll(Set<String> instanzKeysToSave) {
		var instanzsToSave = instanzKeysToSave.stream()//
				.map(key -> resolveKey(key))//
				.filter(opt -> opt.isPresent()).map(opt -> opt.get())//
				.collect(Collectors.toUnmodifiableList());
		instanzsToSave.forEach(i -> _saveService.safeAsGson(i, i.getClass()));
		return true;
	}

	@Override
	public boolean deleteAll(Set<String> instanzKeysToDelete) throws CompletionException {
		CompletionException ex = new CompletionException(null);
		for (String key : instanzKeysToDelete) {
			try {
				_deleteService.deleteFile(key);
			} catch (IOException e) {
				ex.addSuppressed(e);
			}
		}

		if (ex.getSuppressed().length > 0) {
			throw ex;
		}

		return true;
	}

	@Override
	public void putSingleValue(String instanzKey, SingleValueType type, String key, String name,
			IEventBrokerBridge.Type eventType) {
		Optional<IInstanz> instanz = resolveKey(instanzKey);
		if (instanz.isEmpty() || type == null || key.isBlank()) {
			return;
		}

		String paramName = name == null || name.isBlank() ? key : name;
		boolean isAdded = instanz.get().getSingleValues(type).putIfAbsent(key, paramName) == null;
		if(isAdded) {
			var changeData = new LinkedValueChangeEvent(instanzKey, type, ChangeType.ADD, List.of(key));
			fireEvent(eventType, InstanzEventConstants.VALUE_LIST_CHANGE, changeData);
		}
	}

	@Override
	public void changeSingleValueName(String instanzKey, SingleValueType type, String key, String newName,
			IEventBrokerBridge.Type eventType) {
		Optional<IInstanz> instanz = resolveKey(instanzKey);
		if (instanz.isEmpty() || type == null || key.isBlank() || newName.isBlank()) {
			return;
		}

		String oldName = instanz.get().getSingleValues(type).get(key);
		instanz.get().getSingleValues(type).put(key, newName);

		var changeData = new ValueRenameEvent(instanzKey, type, key, oldName, newName);
		fireEvent(eventType, InstanzEventConstants.NAME_CHANGE, changeData);
	}

	@Override
	public boolean removeValueKey(Collection<String> instanzKeys, SingleValueType type, String valueKeyToRemove,
			IEventBrokerBridge.Type eventType) {
		Collection<IInstanz> instanzes = resolveKeys(instanzKeys);
		for (IInstanz instanz : instanzes) {
			instanz.getSingleValues(type).remove(valueKeyToRemove);
			var data = new LinkedValueChangeEvent(instanz.getOwnKey(), type, ChangeType.REMOVE,
					List.of(valueKeyToRemove));
			fireEvent(eventType, InstanzEventConstants.VALUE_LIST_CHANGE, data);
		}
		return true;
	}

	@Override
	public boolean changeParent(String childKey, String parentKey, IEventBrokerBridge.Type eventType) {
		Optional<IInstanz> child = resolveKey(childKey);
		Optional<IInstanz> parent = resolveKey(parentKey);
		if (child.isEmpty() || parent.isEmpty() || child.get().getParentKey().equals(parentKey)) {
			return false;
		}

		var data = new ParentChange(childKey, parentKey, child.get().getParentKey());
		child.get().setParentKey(parentKey);
		fireEvent(eventType, InstanzEventConstants.PARENT_CHANGE, data);
		return true;
	}

	@Override
	public boolean putChild(String parentKey, String childKey, IEventBrokerBridge.Type eventType) {
		Optional<IInstanz> parent = resolveKey(parentKey);
		if (parent.isEmpty() || childKey == null || childKey.isBlank()) {
			return false;
		}

		Map<Boolean, Collection<String>> addedNotAddedMap = parent.get().addChildKeys(childKey);
		if (addedNotAddedMap.get(true).isEmpty()) {
			return false;
		}

		var data = new LinkedChildChangeEvent(parentKey, ChangeType.ADD, addedNotAddedMap.get(true));
		fireEvent(eventType, InstanzEventConstants.CHILD_LIST_CHANGE, data);
		return true;
	}

	@Override
	public boolean removeChild(String parentKey, String childKey, Type eventType) {
		Optional<IInstanz> parent = resolveKey(parentKey);
		if (parent.isEmpty() || childKey == null || childKey.isBlank()) {
			return false;
		}

		Map<Boolean, Collection<String>> removeNotRemovedMap = parent.get().removeChildKeys(childKey);
		if (removeNotRemovedMap.get(true).isEmpty()) {
			return false;
		}

		var data = new LinkedChildChangeEvent(parentKey, ChangeType.REMOVE, removeNotRemovedMap.get(true));
		fireEvent(eventType, InstanzEventConstants.CHILD_LIST_CHANGE, data);
		return true;
	}

	private void fireEvent(Type eventType, String eventName, Object data) {
		switch (eventType) {
		case POST -> _broker.post(eventName, Map.of(IEventBroker.DATA, data));
		case SEND -> _broker.send(eventName, Map.of(IEventBroker.DATA, data));
		default -> throw new IllegalArgumentException();
		}
	}
}
