/**
 * 
 */
package de.tonsias.basis.model.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import de.tonsias.basis.model.interfaces.IInstanz;

/**
 * 
 */
public abstract class AInstanz implements IInstanz {

	private String _parentKey = null;

	private String _ownKey = null;

	private Set<String> _childKeys = new HashSet<String>();

	public AInstanz(String key) {
		this._ownKey = key;
	}

	public AInstanz(String key, String parent) {
		this._ownKey = key;
		this._parentKey = parent;
	}
	
	@Override
	public void setParentKey(String newParent) {
		this._parentKey = newParent;
	}
	
	@Override
	public void addChildKeys(Collection<String> children) {
		children.stream().forEach(child -> _childKeys.add(child));
	}
	
	
	@Override
	public void addChildKeys(String... children) {
		Stream.of(children).forEach(child -> _childKeys.add(child));
	}

	@Override
	public void removeChildKeys(Collection<String> children) {
		children.stream().forEach(child -> _childKeys.remove(child));
	}

	@Override
	public void removeChildKeys(String... children) {
		Stream.of(children).forEach(child -> _childKeys.remove(child));

	}
	
	@Override
	public String getOwnKey() {
		return _ownKey;
	}

	@Override
	public String getParentKey() {
		return _parentKey;
	}
}
