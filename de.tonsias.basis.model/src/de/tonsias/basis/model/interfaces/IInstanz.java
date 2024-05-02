package de.tonsias.basis.model.interfaces;

import java.util.Collection;

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.SingleValueTypes;

/**
 * top interface for each displayed object
 */
public interface IInstanz extends IObject, ISavePathOwner {
	// Parent-Child section

	void setParentKey(String newParent);

	String getParentKey();
	
	Collection<String> getChildren();

	void addChildKeys(String... children);

	void addChildKeys(Collection<String> children);

	void removeChildKeys(String... children);

	void removeChildKeys(Collection<String> children);

	// single value section
	
	BiMap<String, String> getSingleValues(SingleValueTypes type);
	
	void addValuekeys(SingleValueTypes type, BiMap<String, String> keyToName);

	void deleteKeys(SingleValueTypes type, String... keys);

	void deleteParam(SingleValueTypes type, String... names);
}
