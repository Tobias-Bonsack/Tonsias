package de.tonsias.basis.model.interfaces;

import java.util.Collection;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.SingleValueType;

/**
 * top interface for each displayed object
 */
public interface IInstanz extends IObject, ISavePathOwner {
	// Parent-Child section

	void setParentKey(String newParent);

	String getParentKey();

	/**
	 * Get all available {@link IInstanz} children, no values!
	 * 
	 * @return Collection of Keys
	 */
	Collection<String> getChildren();

	void addChildKeys(String... children);

	void addChildKeys(Collection<String> children);

	void removeChildKeys(String... children);

	void removeChildKeys(Collection<String> children);

	// single value section

	/**
	 * Get all {@link ISingleValue} available of this {@link IInstanz}
	 * 
	 * @param type from {@link SingleValueType} to search for
	 * @return {@link BiMap} Key is the Key of the {@link ISingleValue}, Value is
	 *         the Name of the Parameter
	 */
	BiMap<String, String> getSingleValues(SingleValueType type);

	void addValuekeys(SingleValueType type, Entry<String, String> keyToName);

	void deleteKeys(SingleValueType type, String... keys);

	void deleteParam(SingleValueType type, String... names);
}
