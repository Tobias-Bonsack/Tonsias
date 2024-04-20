package de.tonsias.basis.osgi.impl;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IInstanzService;

@Component
public class InstanzServiceImpl implements IInstanzService {

	@Override
	public Optional<IInstanz> resolveKey(String key) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public IInstanz getRoot() {
		// TODO Auto-generated method stub
		return null;
	}

}
