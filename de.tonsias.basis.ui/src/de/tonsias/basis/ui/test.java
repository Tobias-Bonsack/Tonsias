package de.tonsias.basis.ui;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.e4.ui.workbench.UIEvents.Part;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IInstanzService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class test {

	@Inject
	IInstanzService _instanzService;

	Map<IInstanz, TreeItem> _objectToTreeItem = new HashMap<>();

	@PostConstruct
	public void postConstruct(Composite parent) {
		parent.setLayout(new FillLayout());

		Tree tree = new Tree(parent, SWT.V_SCROLL);
		Deque<IInstanz> objectToConvert = new LinkedList<IInstanz>();
		IInstanz root = _instanzService.getRoot();

		objectToConvert.add(root);
		while (!objectToConvert.isEmpty()) {
			IInstanz pollFirst = objectToConvert.pollFirst();
			createItem(pollFirst, tree);

			_instanzService.getInstanzes(pollFirst.getChildren());
		}

		tree.pack();

		Menu menu = new Menu(tree);

		MenuItem menuItem = new MenuItem(menu, 0);
		menuItem.setText("create child");
		menuItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			TreeItem treeItem = tree.getSelection()[0];
			TreeItem treeItem2 = new TreeItem(treeItem, SWT.None);
			treeItem2.setText("child - not an instanz right now");
			tree.pack();
			parent.requestLayout();
		}));

		tree.setMenu(menu);
		parent.requestLayout();

	}

	private TreeItem createItem(IInstanz object, Tree parent) {
		TreeItem treeItem = new TreeItem(parent, SWT.None);
		treeItem.setText("key: "+object.getOwnKey());
		_objectToTreeItem.put(object, treeItem);

		Set<String> keys = object.getSingleValues(SingleValueTypes.SINGLE_STRING).keySet();
		keys.stream().forEach(k -> {
			TreeItem tI = new TreeItem(parent, SWT.None);
			tI.setText(k);
		});
		return treeItem;
	}

}
