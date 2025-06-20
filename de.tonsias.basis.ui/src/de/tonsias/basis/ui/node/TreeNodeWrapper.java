package de.tonsias.basis.ui.node;

import java.util.Arrays;
import java.util.Optional;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import jakarta.inject.Inject;

public class TreeNodeWrapper {

	static final IInstanzService _instanzService = OsgiUtil.getService(IInstanzService.class);

	static final ISingleValueService _singleService = OsgiUtil.getService(ISingleValueService.class);

	static final IBasicPreferenceService _prefService = OsgiUtil.getService(IBasicPreferenceService.class);

	private final IObject _object;

	private final TreeNodeWrapper _parent;

	@Inject
	public TreeNodeWrapper(IObject object, TreeNodeWrapper parent) {
		_object = object;
		_parent = parent;
	}

	public int getChildCount() {
		if (_object instanceof IInstanz) {
			return getChildCount((IInstanz) _object);
		}
		return 0;
	}

	private int getChildCount(IInstanz instanz) {
		int result = 0;
		result += instanz.getChildren().size();

		if (_prefService.getValue(IBasicPreferenceService.Key.SHOW_VALUES.getKey(), Boolean.class).orElse(false)) {
			result += Arrays.stream(SingleValueType.values()).mapToInt(s -> instanz.getSingleValues(s).size()).sum();
		}
		return result;
	}

	public TreeNodeWrapper getChildAt(int index) {
		if (!(_object instanceof IInstanz)) {
			return null;
		}
		int cIndex = 0;
		IInstanz instanz = (IInstanz) _object;
		for (String instanzKey : instanz.getChildren()) {
			if (cIndex == index) {
				Optional<IInstanz> child = _instanzService.resolveKey(instanzKey);
				if (child.isPresent()) {
					return new TreeNodeWrapper(child.get(), this);
				}
			}
			cIndex++;
		}

		for (SingleValueType type : SingleValueType.values()) {
			for (String singleValueKey : instanz.getSingleValues(type).keySet()) {
				if (cIndex == index) {
					Optional<? extends ISingleValue<?>> singleValue = _singleService.resolveKey(type.getPath(),
							singleValueKey, type.getClazz());
					if (singleValue.isPresent()) {
						return new TreeNodeWrapper(singleValue.get(), this);
					}
				}
				cIndex++;
			}

		}
		return null;
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
