package de.tonsias.basis.model.interfaces;

import java.util.Collection;

/**
 * top interface for each displayed object
 */
public interface IInstanz{
	
	String getOwnKey();
	
	void setParentKey(String newParent);
	
	String getParentKey();

	void addChildKeys(String... children);
	
	void addChildKeys(Collection<String> children);
	
	void removeChildKeys(String... children);
	
	void removeChildKeys(Collection<String> children);
}
