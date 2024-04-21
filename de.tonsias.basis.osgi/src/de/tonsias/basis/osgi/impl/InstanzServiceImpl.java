package de.tonsias.basis.osgi.impl;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import jakarta.inject.Inject;

@Component
public class InstanzServiceImpl implements IInstanzService {

	@Reference
	IKeyService _keyService;

	@Reference
	LoadService _loadService;

	@Reference
	SaveService _saveService;

	@Override
	public Optional<IInstanz> resolveKey(String key) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public IInstanz getRoot() {
		String path = "instanz/" + String.valueOf(KeyServiceImpl.KEYCHARS[0]);
		Instanz root = _loadService.loadFromGson(path, Instanz.class);

		if (root != null) {
			return root;
		}

		String key = _keyService.initKey();
		root = new Instanz(key);
		_saveService.safeAsGson(root, root.getClass());
		return root;
	}

}
