package de.tonsias.basis.model.impl.value;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.tonsias.basis.model.interfaces.IValue;

/**
 * What every value carries whatever it holds: its own key and the instanzes
 * holding it.
 * <p>
 * The field names are the names Gson writes, and they were {@code ASingleValue}'s
 * before they were this class's. Moving a field one level up changes neither its
 * name nor its place in the flat json object, so a file written before this class
 * existed still loads - as long as the fields are <em>moved</em> rather than
 * copied: a second declaration of the same name anywhere in the hierarchy makes
 * Gson throw "declares multiple JSON fields".
 * </p>
 */
public abstract class AValue implements IValue {

	private final String _ownKey;

	// no field initializer: Gson allocates this class without running a constructor,
	// so an initializer never runs and a json that does not name the field leaves it
	// null - which used to make getConnectedInstanzKeys throw, see
	// https://github.com/Tobias-Bonsack/Tonsias/issues/83
	// connections() is therefore the single place that creates the set, the same
	// rule AInstanz follows for its maps, see
	// https://github.com/Tobias-Bonsack/Tonsias/issues/61
	private Set<String> _connectedInstanzes;

	protected AValue(String key) {
		_ownKey = key;
	}

	protected AValue(String key, Set<String> connectedInstanzes) {
		_ownKey = key;
		_connectedInstanzes = connectedInstanzes;
	}

	@Override
	public String getOwnKey() {
		return _ownKey;
	}

	/**
	 * The folder is the type's, always - a value that answered anything else would
	 * be written where nothing looks for it.
	 */
	@Override
	public final String getPath() {
		return getType().getPath();
	}

	private Set<String> connections() {
		if (_connectedInstanzes == null) {
			_connectedInstanzes = new HashSet<>();
		}
		return _connectedInstanzes;
	}

	@Override
	public Collection<String> getConnectedInstanzKeys() {
		return Collections.unmodifiableSet(connections());
	}

	@Override
	public boolean addConnectedInstanzKey(String key) {
		return connections().add(key);
	}

	@Override
	public boolean removeConnection(Collection<String> connectedInstanzKeys) {
		return connections().removeAll(connectedInstanzKeys);
	}

	/** what stands between the key and the class name in {@link #toString()} */
	protected abstract Object getContent();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getOwnKey()).append(" ");
		builder.append(this.getContent()).append(" ");
		String[] string = this.getClass().toString().split("\\.");
		builder.append(": ").append(string[string.length - 1]);
		return builder.toString();
	}
}
