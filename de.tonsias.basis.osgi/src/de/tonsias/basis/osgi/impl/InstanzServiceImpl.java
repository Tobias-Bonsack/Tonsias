package de.tonsias.basis.osgi.impl;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import jakarta.inject.Inject;

@Component
public class InstanzServiceImpl implements IInstanzService {

	@Inject
	IKeyService _keyService;

	@Inject
	LoadService _loadService;

	@Inject
	SaveService _saveService;

	@Override
	public Optional<IInstanz> resolveKey(String key) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public IInstanz getRoot() {
		String path = "instanz/" + String.valueOf(Character.MIN_VALUE + 1);
		Instanz root = _loadService.loadFromGson(path, Instanz.class);

		if (root != null) {
			return root;
		}

		String key = _keyService.generateKey();
		root = new Instanz(key);
		_saveService.safeAsGson(root, root.getClass());
		return root;
	}

}
