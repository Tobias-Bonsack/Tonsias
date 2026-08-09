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

	// no field initializers: Gson constructs an AInstanz without a constructor and
	// would leave a map absent from the json at null. getSingleValues is therefore
	// the single place that creates one, for every type alike - and there is
	// deliberately no constructor taking the maps, which would both bypass that and
	// have to grow a parameter with every new SingleValueType. Fill an instanz
	// through addValuekeys instead. See
	// https://github.com/Tobias-Bonsack/Tonsias/issues/61
	private BiMap<String, String> _singleStringKeyValueMap;

	private BiMap<String, String> _singleIntegerKeyValueMap;

	private BiMap<String, String> _singleBooleanKeyValueMap;

	private BiMap<String, String> _singleFloatKeyValueMap;

	private BiMap<String, String> _singleInstanzKeyValueMap;

	// the backward direction of a SINGLE_INSTANZ relation - see
	// IInstanz.getReferencingValueKeys. No field initializer for the same reason as
	// the maps above: Gson leaves what the json does not name at null, and an
	// instanz written before this field existed names it nowhere
	private Set<String> _referencingValueKeys;

	// the only constructor, and it takes the key alone: a parent set here would be
	// set past IInstanzService, so neither would ChangePropagationListener pull the
	// other side along nor would a delta be written. Use
	// IInstanzService.createInstanz(parentKey, Type), or setParentKey below.
	// See https://github.com/Tobias-Bonsack/Tonsias/issues/65
	public AInstanz(String key) {
		this._ownKey = key;
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
			if (_singleStringKeyValueMap == null) {
				_singleStringKeyValueMap = HashBiMap.create();
			}
			return _singleStringKeyValueMap;
		case SINGLE_INTEGER:
			if (_singleIntegerKeyValueMap == null) {
				_singleIntegerKeyValueMap = HashBiMap.create();
			}
			return _singleIntegerKeyValueMap;
		case SINGLE_BOOLEAN:
			if (_singleBooleanKeyValueMap == null) {
				_singleBooleanKeyValueMap = HashBiMap.create();
			}
			return _singleBooleanKeyValueMap;
		case SINGLE_FLOAT:
			if (_singleFloatKeyValueMap == null) {
				_singleFloatKeyValueMap = HashBiMap.create();
			}
			return _singleFloatKeyValueMap;
		case SINGLE_INSTANZ:
			if (_singleInstanzKeyValueMap == null) {
				_singleInstanzKeyValueMap = HashBiMap.create();
			}
			return _singleInstanzKeyValueMap;
		default:
			throw new IllegalArgumentException("Unexpected value: " + type);
		}
	}

	@Override
	public Collection<String> getChildren() {
		return _childKeys;
	}

	@Override
	public Collection<String> getReferencingValueKeys() {
		if (_referencingValueKeys == null) {
			_referencingValueKeys = Collections.synchronizedSet(new HashSet<String>());
		}
		return _referencingValueKeys;
	}

	@Override
	public boolean addReferencingValueKey(String valueKey) {
		if (valueKey == null || valueKey.isBlank()) {
			return false;
		}
		return getReferencingValueKeys().add(valueKey);
	}

	@Override
	public boolean removeReferencingValueKey(String valueKey) {
		return getReferencingValueKeys().remove(valueKey);
	}
}
