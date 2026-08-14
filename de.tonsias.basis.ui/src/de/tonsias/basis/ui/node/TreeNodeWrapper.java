package de.tonsias.basis.ui.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.model.interfaces.IValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import jakarta.inject.Inject;

public class TreeNodeWrapper {

	static final IInstanzService _instanzService = OsgiUtil.getService(IInstanzService.class);

	static final ISingleValueService _singleService = OsgiUtil.getService(ISingleValueService.class);

	static final IMultiValueService _multiService = OsgiUtil.getService(IMultiValueService.class);

	static final IBasicPreferenceService _prefService = OsgiUtil.getService(IBasicPreferenceService.class);

	private final IObject _object;

	private final TreeNodeWrapper _parent;

	@Inject
	public TreeNodeWrapper(IObject object, TreeNodeWrapper parent) {
		_object = object;
		_parent = parent;
	}

	public int getChildCount() {
		return children().size();
	}

	public TreeNodeWrapper getChildAt(int index) {
		List<Child> children = children();
		if (index < 0 || index >= children.size()) {
			return null;
		}

		Child child = children.get(index);
		Optional<? extends IObject> object = child._type() == null //
				? _instanzService.resolveKey(child._key())
				: resolve(child._type(), child._key());
		return object.map(resolved -> new TreeNodeWrapper(resolved, this)).orElse(null);
	}

	/**
	 * What this node draws below itself: the child instanzen, and behind them the
	 * attributes - those only while the preference says to show them. The one place
	 * that says what a child is, so how many there are and which one sits at an
	 * index cannot answer differently.
	 * <p>
	 * Keys rather than objects: the tree asks for the count far more often than for
	 * an element, and resolving one may go to the disk - so only the element that
	 * was really asked for is resolved, in {@link #getChildAt(int)}.
	 * </p>
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/91">#91</a>
	 */
	private List<Child> children() {
		if (!(_object instanceof IInstanz instanz)) {
			return List.of();
		}

		List<Child> children = new ArrayList<>();
		instanz.getChildren().forEach(key -> children.add(new Child(null, key)));

		if (_prefService.getValue(IBasicPreferenceService.Key.SHOW_VALUES.getKey(), Boolean.class).orElse(false)) {
			for (IValueType type : ValueTypes.valuesList()) {
				instanz.getValues(type).keySet().forEach(key -> children.add(new Child(type, key)));
			}
		}
		return children;
	}

	/**
	 * A child by key, before anybody looked for the object behind it.
	 *
	 * @param _type of the attribute, and {@code null} for a child instanz - which is
	 *              the one kind of child that has no value type
	 */
	private record Child(IValueType _type, String _key) {
	}

	private Optional<? extends IValue> resolve(IValueType type, String valueKey) {
		if (type instanceof MultiValueType multi) {
			return _multiService.resolveKey(multi.getPath(), valueKey, multi.getClazz());
		}
		return _singleService.resolveKey(type.getPath(), valueKey, ((SingleValueType) type).getClazz());
	}

	public TreeNodeWrapper getParent() {
		return _parent;
	}

	@Override
	public String toString() {
		return _object.toString();
	}

	public Class<? extends IObject> getObjectClass() {
		return _object.getClass();
	}

	public IObject getObject() {
		return _object;
	}

}
