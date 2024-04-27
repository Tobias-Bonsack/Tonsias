package de.tonsias.basis.osgi.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;

@Component
public class InstanzServiceImpl implements IInstanzService {

	@Reference
	IKeyService _keyService;

	@Reference
	LoadService _loadService;

	@Reference
	SaveService _saveService;

	private Map<String, IInstanz> _cache = new HashMap<>();

	@Override
	public Optional<IInstanz> resolveKey(String key) {
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
		String path = "instanz/" + String.valueOf(KeyServiceImpl.KEYCHARS[0]);
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

}
