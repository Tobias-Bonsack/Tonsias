package de.tonsias.basis.ui.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.viewers.ITreeContentProvider;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
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
			// TODO: singleservice needs resolve keys...
		});
		return null;
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		// TODO Auto-generated method stub
		return false;
	}

}
