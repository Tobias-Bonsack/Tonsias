package de.tonsias.basis.model.interfaces;

import java.util.Collection;

/**
 * Marker interface for a variable of an {@link IInstanz}, available here as
 * single object
 * 
 */
public interface ISingleValue<T> extends IValue {
	
	T getValue();
	
	boolean setValue(T value);
	
	Collection<String> getConnectedInstanzKeys();
	
	void addConnectedInstanzKey(String key);

}
