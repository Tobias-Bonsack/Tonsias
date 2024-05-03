package de.tonsias.basis.ui.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import jakarta.inject.Inject;

public class TreeContentProvider implements ITreeContentProvider {

	@Inject
	IInstanzService _instanzService;

	@Inject
	ISingleValueService _singleService;

	@Override
	public Object[] getElements(Object inputElement) {
		IInstanz root = _instanzService.getRoot();
		return new Object[] { root };
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof IInstanz)) {
			return new Object[0];
		}
		IInstanz iInstanz = (IInstanz) parentElement;

		Collection<Object> children = new ArrayList<>();
		children.addAll(_instanzService.getInstanzes(iInstanz.getChildren()));
		Arrays.stream(SingleValueTypes.values()).forEach(s -> {
			Set<String> valueKeys = iInstanz.getSingleValues(s).keySet();
			Collection<? extends ISingleValue<?>> resolvedValues = _singleService.resolveKeys(s.getClazz(),
					iInstanz.getPath(), valueKeys);

			children.addAll(resolvedValues);
		});
		return children.toArray();
	}

	@Override
	public Object getParent(Object element) {
		if (!(element instanceof IInstanz) || !(element instanceof ISingleValue<?>)) {
			return new Object[0];
		}
		// TODO rework model (getparentkey into own interface)
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		// TODO Auto-generated method stub
		return false;
	}

}
