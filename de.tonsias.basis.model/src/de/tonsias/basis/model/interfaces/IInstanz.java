package de.tonsias.basis.model.interfaces;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.SingleValueTypes;

/**
 * top interface for each displayed object
 */
public interface IInstanz {

	String getOwnKey();

	// Parent-Child section

	void setParentKey(String newParent);

	String getParentKey();

	void addChildKeys(String... children);

	void addChildKeys(Collection<String> children);

	void removeChildKeys(String... children);

	void removeChildKeys(Collection<String> children);

	// single value section
	
	BiMap<String, String> getSingleValues(SingleValueTypes type);
	
	void addValuekeys(SingleValueTypes type, BiMap<String, String> nameToKey);

	void deleteKeys(SingleValueTypes type, String... keys);

	void deleteParam(SingleValueTypes type, String... names);
}
