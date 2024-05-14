package de.tonsias.basis.ui.node;

import java.util.Arrays;
import java.util.Optional;

import org.osgi.framework.FrameworkUtil;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import jakarta.inject.Inject;

public class TreeNodeWrapper {

	static final IInstanzService _instanzService = FrameworkUtil.getBundle(TreeNodeWrapper.class).getBundleContext()
			.getService(FrameworkUtil.getBundle(TreeNodeWrapper.class).getBundleContext()
					.getServiceReference(IInstanzService.class));

	static final ISingleValueService _singleService = FrameworkUtil.getBundle(TreeNodeWrapper.class).getBundleContext()
			.getService(FrameworkUtil.getBundle(TreeNodeWrapper.class).getBundleContext()
					.getServiceReference(ISingleValueService.class));;

	// TODO ueberleg ob nicht der key hier besser waere
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
		result += Arrays.stream(SingleValueTypes.values()).mapToInt(s -> instanz.getSingleValues(s).size()).sum();
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

		for (SingleValueTypes type : SingleValueTypes.values()) {
			for (String singleValueKey : instanz.getSingleValues(type).keySet()) {
				if (cIndex == index) {
					Optional<? extends ISingleValue<?>> singleValue = _singleService.resolveKey(instanz.getPath(),
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
		if (_object instanceof IInstanz) {
			return IInstanz.class;
		}
		return IObject.class;
	}

}
