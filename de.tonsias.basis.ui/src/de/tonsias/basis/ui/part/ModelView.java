package de.tonsias.basis.ui.part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.tonsias.basis.model.interfaces.IInstanz;
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

	private final Map<Class<? extends IObject>, Collection<MenuItem>> _menuItems = new HashMap<>();

	private

	@PostConstruct void postConstruct(Composite parent) {
		parent.setLayout(new FillLayout());

		Tree tree = new Tree(parent, SWT.BORDER | SWT.VIRTUAL);
		TreeViewer treeViewer = new TreeViewer(tree);

		treeViewer.setContentProvider(new TreeContentProvider(treeViewer));
		treeViewer.setLabelProvider(new TreeLabelProvider());
		treeViewer.setUseHashlookup(true);

		TreeNodeWrapper root = new TreeNodeWrapper(_instanzService.getRoot(), null);
		treeViewer.setInput(root);
		treeViewer.setChildCount(root, root.getChildCount());

		createMenu(tree);
	}

	private void createMenu(Tree tree) {
		Menu menu = new Menu(tree);
		createInstanzMenuItem(tree, menu);

		tree.setMenu(menu);

		tree.addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(MenuDetectEvent e) {
				TreeItem[] selection = ((Tree) e.widget).getSelection();
				if (selection.length == 1) {
					TreeNodeWrapper selectedItem = (TreeNodeWrapper) selection[0].getData();
					Class<? extends IObject> objectClass = selectedItem.getObjectClass();
					_menuItems.values().stream().flatMap(i -> i.stream()).forEach(i -> i.set(false));
					_menuItems.getOrDefault(objectClass, Collections.emptyList()).stream()
							.forEach(i -> i.setEnabled(true));
				} else {
					_menuItems.values().stream().flatMap(i -> i.stream()).forEach(i -> i.setEnabled(false));
				}
			}
		});
	}

	private void createInstanzMenuItem(Tree tree, Menu menu) {
		MenuItem menuItem = new MenuItem(menu, SWT.NONE);
		menuItem.setText("Erstelle Instanz");
		_menuItems.putIfAbsent(IInstanz.class, new ArrayList<>());
		_menuItems.get(IInstanz.class).add(menuItem);

		menuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selection = tree.getSelection();
				if (selection.length > 0) {
					TreeNodeWrapper selectedItem = (TreeNodeWrapper) selection[0].getData();
				}
			}
		});
	}
}
