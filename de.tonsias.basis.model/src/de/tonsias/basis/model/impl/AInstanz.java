/**
 * 
 */
package de.tonsias.basis.model.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;

/**
 * 
 */
public abstract class AInstanz implements IInstanz {

	private String _parentKey = null;

	private String _ownKey = null;

	private Set<String> _childKeys = new HashSet<String>();

	private BiMap<String, String> _singleStringKeyValueMap = HashBiMap.create();

	public AInstanz(String key) {
		this._ownKey = key;
	}

	public AInstanz(String key, String parent) {
		this._ownKey = key;
		this._parentKey = parent;
	}

	public AInstanz(String _parentKey, String _ownKey, Set<String> _childKeys,
			BiMap<String, String> _singleStringKeyValueMap) {
		this._parentKey = _parentKey;
		this._ownKey = _ownKey;
		this._childKeys = _childKeys;
		this._singleStringKeyValueMap = _singleStringKeyValueMap;
	}

	@Override
	public void setParentKey(String newParent) {
		this._parentKey = newParent;
	}

	@Override
	public void addChildKeys(Collection<String> children) {
		_childKeys.addAll(children);
	}

	@Override
	public void addChildKeys(String... children) {
		Stream.of(children).forEach(child -> _childKeys.add(child));
	}

	@Override
	public void removeChildKeys(Collection<String> children) {
		_childKeys.removeAll(children);
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

	@Override
	public void addValuekeys(SingleValueTypes type, BiMap<String, String> keyToName) {
		switch (type) {
		case SINGLE_STRING: {
			_singleStringKeyValueMap.putAll(keyToName);
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}

	}

	@Override
	public void deleteKeys(SingleValueTypes type, String... keys) {
		switch (type) {
		case SINGLE_STRING: {
			Arrays.stream(keys).forEach(key -> _singleStringKeyValueMap.remove(key));
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}

	}

	@Override
	public void deleteParam(SingleValueTypes type, String... names) {
		switch (type) {
		case SINGLE_STRING: {
			BiMap<String, String> inverse = _singleStringKeyValueMap.inverse();
			Arrays.stream(names).forEach(name -> inverse.remove(name));
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}

	}

	@Override
	public BiMap<String, String> getSingleValues(SingleValueTypes type) {
		switch (type) {
		case SINGLE_STRING: {
			return _singleStringKeyValueMap;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}
	}

	@Override
	public Collection<String> getChildren() {
		return _childKeys;
	}

}
