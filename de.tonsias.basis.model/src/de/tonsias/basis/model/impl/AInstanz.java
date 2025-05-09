/**
 * 
 */
package de.tonsias.basis.model.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;

/**
 * 
 */
public abstract class AInstanz implements IInstanz {

	private String _parentKey = null;

	private String _ownKey = null;

	private Set<String> _childKeys = Collections.synchronizedSet(new HashSet<String>());

	private BiMap<String, String> _singleStringKeyValueMap = HashBiMap.create();

	private BiMap<String, String> _singleIntegerKeyValueMap = HashBiMap.create();

	public AInstanz(String key) {
		this._ownKey = key;
	}

	public AInstanz(String key, String parent) {
		this._ownKey = key;
		this._parentKey = parent;
	}

	public AInstanz(String _parentKey, String _ownKey, Set<String> _childKeys,
			BiMap<String, String> _singleStringKeyValueMap, BiMap<String, String> _singleIntegerKeyValueMap) {
		this._parentKey = _parentKey;
		this._ownKey = _ownKey;
		this._childKeys = _childKeys;
		this._singleStringKeyValueMap = _singleStringKeyValueMap;
		this._singleIntegerKeyValueMap = _singleIntegerKeyValueMap;
	}

	@Override
	public void setParentKey(String newParent) {
		this._parentKey = newParent;
	}

	@Override
	public Map<Boolean, Collection<String>> addChildKeys(String... children) {
		Map<Boolean, Collection<String>> result = Map.of(Boolean.TRUE, new LinkedList<String>(), Boolean.FALSE,
				new LinkedList<String>());
		Stream.of(children).forEach(i -> result.get(_childKeys.add(i)).add(i));
		return result;
	}

	@Override
	public Map<Boolean, Collection<String>> removeChildKeys(String... children) {
		Map<Boolean, Collection<String>> result = Map.of(Boolean.TRUE, new LinkedList<String>(), Boolean.FALSE,
				new LinkedList<String>());
		Stream.of(children).forEach(i -> result.get(_childKeys.remove(i)).add(i));
		return result;
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
	public void addValuekeys(SingleValueType type, Entry<String, String> keyToName) {
		getSingleValues(type).put(keyToName.getKey(), keyToName.getValue());
	}

	@Override
	public void deleteKeys(SingleValueType type, String... keys) {
		BiMap<String, String> singleValues = getSingleValues(type);
		Arrays.stream(keys).forEach(key -> singleValues.remove(key));
	}

	@Override
	public void deleteParam(SingleValueType type, String... names) {
		BiMap<String, String> singleValues = getSingleValues(type).inverse();
		Arrays.stream(names).forEach(name -> singleValues.remove(name));
	}

	@Override
	public BiMap<String, String> getSingleValues(SingleValueType type) {
		switch (type) {
		case SINGLE_STRING:
			return _singleStringKeyValueMap;
		case SINGLE_INTEGER:
			return _singleIntegerKeyValueMap;
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}
	}

	@Override
	public Collection<String> getChildren() {
		return _childKeys;
	}
}
