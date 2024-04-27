package de.tonsias.basis.data.access.osgi.intf;

import java.util.Collection;

import de.tonsias.basis.model.interfaces.ISavePathOwner;

public interface SaveService {

	/**
	 * saves the entire list into a json
	 * @param <E> original class to save
	 * @param list of objects to save
	 * @param objectType for class identification
	 */
	<E> void safeAsGson(Collection<ISavePathOwner> list, Class<E> objectType);

	/**
	 * saves the object into a json
	 * @param <E> original class to save
	 * @param object to save
	 * @param objectType for class identification
	 */
	<E> void safeAsGson(ISavePathOwner object, Class<E> objectType);

}
