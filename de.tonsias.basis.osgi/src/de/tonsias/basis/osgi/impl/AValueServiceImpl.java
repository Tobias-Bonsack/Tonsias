package de.tonsias.basis.osgi.impl;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.interfaces.IValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.IValueService;
import de.tonsias.basis.osgi.intf.non.service.ValueEventConstants.ChangeType;

/**
 * Everything a value service does that does not depend on what a value holds: the
 * cache in front of the files, the way from a key back to an object, and the save
 * and delete the delta service drives.
 * <p>
 * The five collaborators stay {@code @Reference} fields of the concrete component
 * classes and are reached here through accessors. Whether Felix SCR resolves
 * field injection into an <em>inherited</em> field is not something this project
 * should find out at runtime, with a component that then silently never
 * activates - the descriptors are written by hand and name the field they inject.
 * </p>
 *
 * @param <V> the values this service holds
 * @param <T> the types those values have
 */
public abstract class AValueServiceImpl<V extends IValue, T extends IValueType> implements IValueService<V, T> {

	private final Map<String, V> _cache = new HashMap<>();

	protected abstract SaveService saveService();

	protected abstract LoadService loadService();

	protected abstract DeleteService deleteService();

	protected abstract IKeyService keyService();

	protected abstract IEventBrokerBridge broker();

	/** every type this service holds - the folder scans walk it */
	protected abstract T[] types();

	/** a fresh value of that type under that key */
	protected abstract V newInstance(T type, String key);

	/**
	 * @return the type of a value this service holds, or null for a value no type
	 *         maps to - which cannot happen for anything the model brings along, but
	 *         a caller can hand over its own implementation
	 */
	protected abstract T typeOf(V value);

	/** the delete event of this family, with the owners it has just been cut from */
	protected abstract void fireDelete(T type, V value, Collection<String> ownerKeys, Type eventType);

	/** the "an owner was added" event of this family */
	protected abstract void fireInstanzListChange(T type, String valueKey, ChangeType change,
			Collection<String> instanzKeys, Type eventType);

	@Override
	public <E extends V> Optional<E> resolveKey(String path, String key, Class<E> clazz) {
		if (key == null || key.isBlank()) {
			return Optional.empty();
		}

		if (_cache.containsKey(key)) {
			V value = _cache.get(key);
			return Optional.ofNullable(clazz.isInstance(value) ? clazz.cast(value) : null);
		}

		if (path == null || path.isBlank()) {
			return Optional.empty();
		}

		E value = loadService().loadFromGson(path + key, clazz);
		if (value == null) {
			// no file is no answer, and it is no answer worth remembering either: a null
			// left in the cache counts as "asked and answered" from then on, so the key
			// would never resolve again in this session - not even once its file is
			// there, and not even when the next caller looks in the right folder. See
			// https://github.com/Tobias-Bonsack/Tonsias/issues/79
			return Optional.empty();
		}

		_cache.put(key, value);
		return Optional.of(value);
	}

	@Override
	public <E extends V> Collection<E> resolveKeys(Class<E> clazz, String path, Collection<String> keys) {
		Collection<E> result = new ArrayList<E>();
		keys.stream()//
				.map(key -> resolveKey(path, key, clazz))//
				.filter(o -> o.isPresent())//
				.map(o -> o.get())//
				.forEach(value -> result.add(value));
		return result;
	}

	@Override
	public Optional<V> resolveAnyKey(String key) {
		if (key == null || key.isBlank()) {
			return Optional.empty();
		}

		V cached = _cache.get(key);
		if (cached != null) {
			return Optional.of(cached);
		}

		for (T type : types()) {
			Optional<? extends V> loaded = resolve(type, key);
			if (loaded.isPresent()) {
				return Optional.of(loaded.get());
			}
		}
		return Optional.empty();
	}

	/** the value of a known type, off the cache or out of that type's folder */
	protected final Optional<? extends V> resolve(T type, String key) {
		return resolveKey(type.getPath(), key, clazzOf(type));
	}

	@SuppressWarnings("unchecked") // a type of this service names a class of this service, by construction
	private Class<? extends V> clazzOf(T type) {
		return (Class<? extends V>) type.getClazz();
	}

	@Override
	public void saveAll() {
		_cache.values().stream().forEach(i -> saveService().safeAsGson(i, i.getClass()));
	}

	@Override
	public boolean saveAll(Set<String> keysToSave) {
		var valuesToSave = keysToSave.stream()//
				.map(_cache::get)//
				.filter(Objects::nonNull)//
				.collect(Collectors.toUnmodifiableList());
		valuesToSave.forEach(i -> saveService().safeAsGson(i, i.getClass()));
		return true;
	}

	@Override
	public boolean removeFromCache(String... keys) {
		return !Stream.of(keys).map(key -> _cache.remove(key)).anyMatch(removedValue -> removedValue == null);
	}

	@Override
	public boolean deleteValue(V valueToDelete, Type eventType) {
		if (typeOf(valueToDelete) != null) {
			markValueAsDelete(valueToDelete.getOwnKey(), eventType);
		}
		return true;
	}

	@Override
	public void markValueAsDelete(String keyToMark, Type eventType) {
		// off the cache alone this used to throw for every value that had not been
		// touched in this session - which after a restart is every value there is, see
		// https://github.com/Tobias-Bonsack/Tonsias/issues/78
		Optional<V> resolved = resolveAnyKey(keyToMark);
		if (resolved.isEmpty()) {
			return;
		}

		V value = resolved.get();
		T type = typeOf(value);
		if (type == null) {
			return;
		}

		// a live view of the connections, so it has to be copied before they are cut -
		// the event tells the owners to drop the value key and would carry nobody
		Collection<String> connectedInstanzKeys = List.copyOf(value.getConnectedInstanzKeys());
		value.removeConnection(connectedInstanzKeys);
		fireDelete(type, value, connectedInstanzKeys, eventType);
	}

	@Override
	public boolean deleteAll(Set<String> keysToDelete) throws CompletionException {
		CompletionException ex = new CompletionException(null);
		for (String key : keysToDelete) {
			try {
				deleteFile(key);
			} catch (IOException e) {
				ex.addSuppressed(e);
			}
		}

		if (ex.getSuppressed().length > 0) {
			throw ex;
		}

		return true;
	}

	/**
	 * The delta bookkeeping only knows keys, the delete service works on files - and
	 * which folder a value lives in depends on its type. A value that is no longer
	 * cached no longer tells its type, so every folder is tried until one of them
	 * holds the file. Finding it in none of them is no failure either: a value
	 * created and deleted before any save never got written, and no file is what the
	 * delete was after, see
	 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/53">#53</a>.
	 */
	private void deleteFile(String key) throws IOException {
		V cached = _cache.get(key);
		if (cached != null) {
			deleteIfPresent(cached.getPath() + key + ".json");
			return;
		}

		for (T type : types()) {
			if (deleteIfPresent(type.getPath() + key + ".json")) {
				return;
			}
		}
	}

	/**
	 * @return whether there was a file at that path - which is the only way the
	 *         folder search can tell it has found the right folder
	 */
	private boolean deleteIfPresent(String path) throws IOException {
		try {
			return deleteService().deleteFile(path);
		} catch (NoSuchFileException e) {
			return false;
		}
	}

	@Override
	public boolean addToParent(T type, String valueKey, String parentKey, Type eventType) {
		Optional<? extends V> value = resolve(type, valueKey);
		if (value.isEmpty()) {
			return false;
		}

		boolean isAdded = value.get().addConnectedInstanzKey(parentKey);
		if (isAdded) {
			fireInstanzListChange(type, valueKey, ChangeType.ADD, Collections.singleton(parentKey), eventType);
		}
		return isAdded;
	}

	/** a new value of that type under a freshly generated key, not yet cached */
	protected final V create(T type) {
		return newInstance(type, keyService().generateKey());
	}

	protected final void cache(V value) {
		_cache.put(value.getOwnKey(), value);
	}

	protected final void fireEvent(Type eventType, String eventName, Object data) {
		switch (eventType) {
		case POST -> broker().post(eventName, Map.of(IEventBroker.DATA, data));
		case SEND -> broker().send(eventName, Map.of(IEventBroker.DATA, data));
		default -> throw new IllegalArgumentException();
		}
	}
}
