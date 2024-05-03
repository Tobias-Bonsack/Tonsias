package de.tonsias.basis.ui.part;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.ui.node.TreeNodeWrapper;
import de.tonsias.basis.ui.provider.TreeContentProvider;
import de.tonsias.basis.ui.provider.TreeLabelProvider;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class ModelView {

	@Inject
	IInstanzService _instanzService;

	@Inject
	ISingleValueService _singleService;

	BiMap<IObject, TreeItem> _objectToTreeItem = HashBiMap.create();

	@PostConstruct
	void postConstruct(Composite parent) {
		parent.setLayout(new FillLayout());

		Tree tree = new Tree(parent, SWT.BORDER | SWT.VIRTUAL);
		TreeViewer treeViewer = new TreeViewer(tree);

		treeViewer.setContentProvider(new TreeContentProvider(treeViewer));
		treeViewer.setLabelProvider(new TreeLabelProvider());
		treeViewer.setUseHashlookup(true);

		TreeNodeWrapper root = new TreeNodeWrapper(_instanzService.getRoot(), null);
		treeViewer.setInput(root);
		treeViewer.setChildCount(root, root.getChildCount());

	}

}
