package de.tonsias.basis.osgi.intf;

import java.util.Optional;

import de.tonsias.basis.model.interfaces.ISingleValue;

public interface ISingleValueService {

	<E extends ISingleValue<?>> Optional<E> resolveKey(String path, String key, Class<E> clazz);

}
