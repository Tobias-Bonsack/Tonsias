package de.tonsias.basis.ui.node;

import java.util.Arrays;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import jakarta.inject.Inject;

public class TreeNodeWrapper {

	@Inject
	static IInstanzService _instanzService;

	@Inject
	static ISingleValueService _singleService;

	private final IObject _object;

	public TreeNodeWrapper(IObject object) {
		_object = object;
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

}
