package de.tonsias.basis.data.access.osgi.intf;

import java.util.Collection;

import de.tonsias.basis.model.interfaces.IInstanz;

public interface SaveService {

	<E> void safeAsGson(Collection<IInstanz> list, Class<E> objectType);

	<E> void safeAsGson(IInstanz object, Class<E> objectType);

}
