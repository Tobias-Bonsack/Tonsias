package de.tonsias.basis.osgi.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;

@Component
public class InstanzServiceImpl implements IInstanzService {

	@Reference
	IKeyService _keyService;

	@Reference
	LoadService _loadService;

	@Reference
	SaveService _saveService;

	@Reference
	IEventBrokerBridge _broker;

	private final Map<String, IInstanz> _cache = new HashMap<>();

	@Override
	public Optional<IInstanz> resolveKey(String key) {
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
	public Collection<IInstanz> getInstanzes(Collection<String> keys) {
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
	public IInstanz createInstanz(IInstanz parent) {
		String key = _keyService.generateKey();
		Instanz instanz = new Instanz(key);
		instanz.setParentKey(parent.getOwnKey());
		parent.addChildKeys(instanz.getOwnKey());
		_cache.put(key, instanz);

		Map<String, Object> data = new HashMap<String, Object>();
		data.put(InstanzEventConstants.PureInstanzData.class.getName(),
				new InstanzEventConstants.PureInstanzData(instanz));
		_broker.post(InstanzEventConstants.NEW, data);

		return instanz;
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
	public void changeAttributeName(String instanzKey, SingleValueType type, String key, String newName) {
		Optional<IInstanz> instanz = resolveKey(instanzKey);
		if (instanz.isEmpty() || type == null || key.isBlank() || newName.isBlank()) {
			return;
		}

		instanz.get().getSingleValues(type).put(key, newName);
	}

	@Override
	public boolean removeValueKey(Collection<String> instanzKeys, SingleValueType type, String valueKeyToRemove) {
		Collection<IInstanz> instanzes = getInstanzes(instanzKeys);
		for (IInstanz instanz : instanzes) {
			instanz.getSingleValues(type).remove(valueKeyToRemove);
			_broker.send(InstanzEventConstants.NEW,
					new InstanzEventConstants.AttributeChangeData(instanz.getOwnKey(), valueKeyToRemove, null));
		}
		return false;
	}
}
