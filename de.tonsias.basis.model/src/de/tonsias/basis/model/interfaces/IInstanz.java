package de.tonsias.basis.model.interfaces;

import java.util.Collection;

/**
 * top interface for each displayed object
 */
public interface IInstanz {
	Collection<String> getAllKeys();

	String getParentKey();

	Collection<String> getChildrenKeys();

	Collection<String> getAllSingleValueKeys();

	Collection<String> getAllCollectionValueKeys();
}
