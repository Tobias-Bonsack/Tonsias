package de.tonsias.basis.data.access.osgi.intf;

import java.util.Collection;

import de.tonsias.basis.model.interfaces.ISavePathOwner;

public interface SaveService {

	<E> void safeAsGson(Collection<ISavePathOwner> list, Class<E> objectType);

	<E> void safeAsGson(ISavePathOwner object, Class<E> objectType);

}
