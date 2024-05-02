package de.tonsias.basis.ui;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class test {

	@Inject
	IInstanzService _instanzService;

	@Inject
	ISingleValueService _singleService;

	BiMap<IObject, TreeItem> _objectToTreeItem = HashBiMap.create();

	@PostConstruct
	public void postConstruct(Composite parent) {
		parent.setLayout(new FillLayout());

		Tree tree = new Tree(parent, SWT.V_SCROLL);
		Deque<IInstanz> objectToConvert = new LinkedList<IInstanz>();
		IInstanz root = _instanzService.getRoot();

		objectToConvert.add(root);
		TreeItem oldItem = null;
		while (!objectToConvert.isEmpty()) {
			IInstanz pollFirst = objectToConvert.pollFirst();

			TreeItem item = null;
			if (oldItem == null) {
				item = createItem(pollFirst, tree);
			} else {
				item = createItem(pollFirst, oldItem);
			}

			Collection<IInstanz> instanzes = _instanzService.getInstanzes(pollFirst.getChildren());
			objectToConvert.addAll(instanzes);

			oldItem = item;
		}

		tree.pack();

		Menu menu = new Menu(tree);

		createContextMenu(parent, tree, menu);

		tree.setMenu(menu);
		parent.requestLayout();

	}

	private void createContextMenu(Composite parent, Tree tree, Menu menu) {
		MenuItem menuItem = new MenuItem(menu, 0);
		menuItem.setText("create child");
		menuItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			TreeItem treeItem = tree.getSelection()[0];
			IInstanz parentI = (IInstanz) _objectToTreeItem.inverse().get(treeItem);
			IInstanz instanz = _instanzService.createInstanz(parentI);
			parentI.addChildKeys(instanz.getOwnKey());
			instanz.setParentKey(parentI.getOwnKey());

			TreeItem treeItem2 = new TreeItem(treeItem, SWT.None);
			treeItem2.setText("Instanzkey:" + instanz.getOwnKey());
			_objectToTreeItem.put(instanz, treeItem2);
			tree.pack();
			parent.requestLayout();
		}));

		MenuItem createStringValue = new MenuItem(menu, SWT.None);
		createStringValue.setText("CreateStringValue");
		createStringValue.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			TreeItem treeItem = tree.getSelection()[0];
			IInstanz parentI = (IInstanz) _objectToTreeItem.inverse().get(treeItem);

			ISingleValue<String> singleValue = (ISingleValue<String>) _singleService.createNew(SingleStringValue.class,
					parentI, "Name");
			singleValue.setValue("Value: " + singleValue.getOwnKey());
			TreeItem treeItem2 = new TreeItem(treeItem, SWT.None);
			treeItem2.setText(singleValue.getOwnKey() + "-" + singleValue.getValue());

			_objectToTreeItem.put(singleValue, treeItem2);
			SimpleEntry<String, Object> keyToName = new AbstractMap.SimpleEntry<String, Object>(singleValue.getOwnKey(),
					"Name: " + singleValue.getValue());
			parentI.addValuekeys(SingleValueTypes.SINGLE_STRING, keyToName);
			singleValue.addConnectedInstanzKey(parentI.getOwnKey());

			tree.pack();
			parent.requestLayout();
		}));

		MenuItem save = new MenuItem(menu, SWT.None);
		save.setText("Save all");
		save.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			_instanzService.saveAll();
			_singleService.saveAll();
		}));
	}

	private TreeItem createItem(IInstanz object, Tree parent) {
		TreeItem treeItem = new TreeItem(parent, SWT.None);
		treeItem.setText("key: " + object.getOwnKey());
		_objectToTreeItem.put(object, treeItem);

		BiMap<String, String> keysToName = object.getSingleValues(SingleValueTypes.SINGLE_STRING);
		keysToName.entrySet().stream().forEach(k -> {
			Optional<SingleStringValue> value = _singleService.resolveKey(object.getPath(), k.getKey(),
					SingleStringValue.class);
			if (value.isPresent()) {
				TreeItem tI = new TreeItem(treeItem, SWT.None);
				tI.setText(value.get().getOwnKey() + "-" + value.get().getValue());
				_objectToTreeItem.put(value.get(), tI);
			}
		});
		return treeItem;
	}

	private TreeItem createItem(IInstanz object, TreeItem parent) {
		TreeItem treeItem = new TreeItem(parent, SWT.None);
		treeItem.setText("key: " + object.getOwnKey());
		_objectToTreeItem.put(object, treeItem);

		BiMap<String, String> keysToName = object.getSingleValues(SingleValueTypes.SINGLE_STRING);
		keysToName.entrySet().stream().forEach(k -> {
			Optional<SingleStringValue> value = _singleService.resolveKey(object.getPath(), k.getKey(),
					SingleStringValue.class);
			if (value.isPresent()) {
				TreeItem tI = new TreeItem(treeItem, SWT.None);
				tI.setText(value.get().getOwnKey() + "-" + value.get().getValue());
				_objectToTreeItem.put(value.get(), tI);
			}
		});
		return treeItem;
	}

}
