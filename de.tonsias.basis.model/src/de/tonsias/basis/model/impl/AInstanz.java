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
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;

/**
 * 
 */
public abstract class AInstanz implements IInstanz {

	private String _parentKey = null;

	private String _ownKey = null;

	private Set<String> _childKeys = Collections.synchronizedSet(new HashSet<String>());

	// One map per value type, and one named field per map rather than a map of
	// maps: every instanz json ever written names these fields, and a map keyed by
	// type name would make Gson drop all of them - every instanz in every workspace
	// would silently lose its attributes.
	//
	// No field initializers: Gson constructs an AInstanz without a constructor and
	// would leave a map absent from the json at null. getValues is therefore the
	// single place that creates one, for every type alike - and there is
	// deliberately no constructor taking the maps, which would both bypass that and
	// have to grow a parameter with every new value type. Fill an instanz through
	// addValuekeys instead. See
	// https://github.com/Tobias-Bonsack/Tonsias/issues/61
	private BiMap<String, String> _singleStringKeyValueMap;

	private BiMap<String, String> _singleIntegerKeyValueMap;

	private BiMap<String, String> _singleBooleanKeyValueMap;

	private BiMap<String, String> _singleFloatKeyValueMap;

	private BiMap<String, String> _singleInstanzKeyValueMap;

	private BiMap<String, String> _multiStringKeyValueMap;

	private BiMap<String, String> _multiIntegerKeyValueMap;

	private BiMap<String, String> _multiBooleanKeyValueMap;

	private BiMap<String, String> _multiFloatKeyValueMap;

	private BiMap<String, String> _multiInstanzKeyValueMap;

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
	public void addValuekeys(IValueType type, Entry<String, String> keyToName) {
		getValues(type).put(keyToName.getKey(), keyToName.getValue());
	}

	@Override
	public void deleteKeys(IValueType type, String... keys) {
		BiMap<String, String> values = getValues(type);
		Arrays.stream(keys).forEach(key -> values.remove(key));
	}

	@Override
	public void deleteParam(IValueType type, String... names) {
		BiMap<String, String> values = getValues(type).inverse();
		Arrays.stream(names).forEach(name -> values.remove(name));
	}

	@Override
	public BiMap<String, String> getValues(IValueType type) {
		if (type instanceof SingleValueType single) {
			return switch (single) {
			case SINGLE_STRING -> orCreate(_singleStringKeyValueMap, map -> _singleStringKeyValueMap = map);
			case SINGLE_INTEGER -> orCreate(_singleIntegerKeyValueMap, map -> _singleIntegerKeyValueMap = map);
			case SINGLE_BOOLEAN -> orCreate(_singleBooleanKeyValueMap, map -> _singleBooleanKeyValueMap = map);
			case SINGLE_FLOAT -> orCreate(_singleFloatKeyValueMap, map -> _singleFloatKeyValueMap = map);
			case SINGLE_INSTANZ -> orCreate(_singleInstanzKeyValueMap, map -> _singleInstanzKeyValueMap = map);
			default -> throw new IllegalArgumentException("Unexpected value: " + type);
			};
		}
		if (type instanceof MultiValueType multi) {
			return switch (multi) {
			case MULTI_STRING -> orCreate(_multiStringKeyValueMap, map -> _multiStringKeyValueMap = map);
			case MULTI_INTEGER -> orCreate(_multiIntegerKeyValueMap, map -> _multiIntegerKeyValueMap = map);
			case MULTI_BOOLEAN -> orCreate(_multiBooleanKeyValueMap, map -> _multiBooleanKeyValueMap = map);
			case MULTI_FLOAT -> orCreate(_multiFloatKeyValueMap, map -> _multiFloatKeyValueMap = map);
			case MULTI_INSTANZ -> orCreate(_multiInstanzKeyValueMap, map -> _multiInstanzKeyValueMap = map);
			default -> throw new IllegalArgumentException("Unexpected value: " + type);
			};
		}
		throw new IllegalArgumentException("Unexpected value: " + type);
	}

	/**
	 * @param map     the field as it stands, null when the json did not name it
	 * @param setter  writes the created map back into that field - the one thing a
	 *                switch arm cannot do to a field it only reads
	 */
	private BiMap<String, String> orCreate(BiMap<String, String> map, Consumer<BiMap<String, String>> setter) {
		if (map != null) {
			return map;
		}
		BiMap<String, String> created = HashBiMap.create();
		setter.accept(created);
		return created;
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
