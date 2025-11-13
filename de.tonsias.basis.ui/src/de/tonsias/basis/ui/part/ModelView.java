package de.tonsias.basis.ui.part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.osgi.service.event.Event;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleIntegerValue;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.osgi.intf.non.service.PreferenceEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.ui.dialog.IntegerValueDialog;
import de.tonsias.basis.ui.dialog.StringValueDialog;
import de.tonsias.basis.ui.handler.CreateInstanzOperation;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.node.TreeNodeWrapper;
import de.tonsias.basis.ui.provider.TreeContentProvider;
import de.tonsias.basis.ui.provider.TreeLabelProvider;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class ModelView {

	@Inject
	IEventBrokerBridge _broker;

	@Inject
	IInstanzService _instanzService;

	@Inject
	ISingleValueService _singleService;

	@Inject
	IDeltaService _deltaService;

	@Inject
	@Translation
	Messages _messages;

	private final Map<Class<? extends IObject>, Collection<MenuItem>> _menuItems = new HashMap<>();

	private TreeViewer _treeViewer;

	private

	@PostConstruct void postConstruct(Composite parent) {
		parent.setLayout(new FillLayout());

		Tree tree = new Tree(parent, SWT.BORDER | SWT.VIRTUAL);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				TreeItem[] selection = ((Tree) e.widget).getSelection();
				if (selection.length <= 0) {
					return;
				}
				TreeNodeWrapper node = (TreeNodeWrapper) selection[0].getData();
				if (node.getObject() instanceof IInstanz instanz) {
					InstanzEvent data = new InstanzEventConstants.InstanzEvent(instanz.getOwnKey(), null);
					_broker.send(InstanzEventConstants.SELECTED, Map.of(IEventBroker.DATA, data));
				}
			}
		});

		_treeViewer = new TreeViewer(tree);

		_treeViewer.setContentProvider(new TreeContentProvider(_treeViewer));
		_treeViewer.setLabelProvider(new TreeLabelProvider());
		_treeViewer.setUseHashlookup(true);

		TreeNodeWrapper root = new TreeNodeWrapper(_instanzService.getRoot(), null);
		_treeViewer.setInput(root);
		_treeViewer.setChildCount(root, root.getChildCount());

		createMenu(tree);
	}

	private void createMenu(Tree tree) {
		Menu menu = new Menu(tree);

		// Instanz Menu Items
		createInstanzMenuItem(tree, menu);

		// Value Menu Items
		createSingleValueMenuItems(tree, menu);
		createMenuItemsForvalues(tree, menu);

		tree.setMenu(menu);

		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 'n' && e.stateMask == SWT.CONTROL) {
					var event = new org.eclipse.swt.widgets.Event();
					event.widget = tree;
					tree.notifyListeners(SWT.MenuDetect, event);
					menu.setVisible(true);
					e.doit = false;
				}
			}
		});

		tree.addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(MenuDetectEvent e) {
				TreeItem[] selection = ((Tree) e.widget).getSelection();
				if (selection.length == 1) {
					TreeNodeWrapper selectedItem = (TreeNodeWrapper) selection[0].getData();
					_menuItems.values().stream().flatMap(i -> i.stream()).forEach(i -> i.setEnabled(false));
					Class<? extends IObject> objectClass = selectedItem.getObjectClass();
					for (var declaredClass : _menuItems.keySet()) {
						if (declaredClass.isAssignableFrom(objectClass)) {
							_menuItems.get(declaredClass).stream().forEach(i -> i.setEnabled(true));
						}
					}
				} else {
					_menuItems.values().stream().flatMap(i -> i.stream()).forEach(i -> i.setEnabled(false));
				}
			}
		});
	}

	private void createMenuItemsForvalues(Tree tree, Menu menu) {
		MenuItem menuItem = new MenuItem(menu, SWT.NONE);
		menuItem.setText(_messages.mi_delete);
		_menuItems.computeIfAbsent(ISingleValue.class, c -> new ArrayList<>()).add(menuItem);

		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selection = tree.getSelection();
				if (selection.length == 0) {
					return;
				}

				ISingleValue<?> value = (ISingleValue<?>) selection[0].getData();
				Collection<String> connectedInstanzKeys = value.getConnectedInstanzKeys();
				SingleValueType type = SingleValueType.getByClass(value.getClass()).get();

				_broker.post(EventConstants.OPEN_OPERATION, null);
				_singleService.removeValue(value, IEventBrokerBridge.Type.SEND);
				_instanzService.removeValueKey(connectedInstanzKeys, type, value.getOwnKey(),
						IEventBrokerBridge.Type.POST);
				_broker.post(EventConstants.CLOSE_OPERATION, null);

				_treeViewer.refresh();
			};
		});
	}

	private void createInstanzMenuItem(Tree tree, Menu menu) {
		MenuItem menuItem = new MenuItem(menu, SWT.NONE);
		menuItem.setText(_messages.mi_createInstanz);
		_menuItems.putIfAbsent(IInstanz.class, new ArrayList<>());
		_menuItems.get(IInstanz.class).add(menuItem);

		menuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selection = tree.getSelection();
				if (selection.length > 0) {
					TreeNodeWrapper parent = (TreeNodeWrapper) selection[0].getData();
					IInstanz parentObject = (IInstanz) parent.getObject();
					CreateInstanzOperation newInstanzOperation = new CreateInstanzOperation(parentObject);
					newInstanzOperation.execute(_broker.getEclipseBroker(), _instanzService);
					IInstanz createdInstanz = newInstanzOperation.get_createdInstanz();
					new TreeNodeWrapper(createdInstanz, parent);
					_treeViewer.refresh(parent);
				}
			}
		});
	}

	private void createSingleValueMenuItems(Tree tree, Menu menu) {
		MenuItem parentItem = new MenuItem(menu, SWT.CASCADE);
		parentItem.setText(_messages.constant_singleValue);
		_menuItems.get(IInstanz.class).add(parentItem);

		Menu singleValueMenu = new Menu(menu);
		parentItem.setMenu(singleValueMenu);

		MenuItem createStringSingleValueItem = new MenuItem(singleValueMenu, SWT.None);
		createStringSingleValueItem.setText(_messages.mi_string);

		createStringSingleValueItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selection = tree.getSelection();
				if (selection.length != 1) {
					return;
				}

				TreeNodeWrapper parent = (TreeNodeWrapper) selection[0].getData();
				IInstanz parentObject = (IInstanz) parent.getObject();

				StringValueDialog dialog = new StringValueDialog(new Shell(), parentObject, _messages);
				int open = dialog.open();
				if (open == Window.OK) {
					SingleStringValue singleValue = dialog.getSingleValue();
					new TreeNodeWrapper(singleValue, parent);
					_treeViewer.refresh(parent);
				}
			}
		});

		MenuItem createStringIntegerValueItem = new MenuItem(singleValueMenu, SWT.None);
		createStringIntegerValueItem.setText(_messages.mi_integer);

		createStringIntegerValueItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] selection = tree.getSelection();
				if (selection.length != 1) {
					return;
				}

				TreeNodeWrapper parent = (TreeNodeWrapper) selection[0].getData();
				IInstanz parentObject = (IInstanz) parent.getObject();

				IntegerValueDialog dialog = new IntegerValueDialog(new Shell(), parentObject, _messages);
				int open = dialog.open();
				if (open == Window.OK) {
					SingleIntegerValue singleValue = dialog.getSingleValue();
					new TreeNodeWrapper(singleValue, parent);
					_treeViewer.refresh(parent);
				}

			}
		});
	}

	@Inject
	@Optional
	private void newInstanzListener(@UIEventTopic(InstanzEventConstants.NEW) Event data) {
		_treeViewer.refresh();
	}

	@Inject
	@Optional
	private void newSvListener(@UIEventTopic(SingleValueEventConstants.NEW) Event data) {
		IBasicPreferenceService _prefService = OsgiUtil.getService(IBasicPreferenceService.class);
		String shownVariable = _prefService.getValue(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), String.class)
				.orElse("");
		Object property = data.getProperty(IEventBroker.DATA);
		if (property instanceof SingleValueEventConstants.SingleValueNewEvent newData
				&& newData._name().equalsIgnoreCase(shownVariable)) {
			_treeViewer.refresh();
		}
	}

	@Inject
	@Optional
	private void basicShowValueListener(@UIEventTopic(PreferenceEventConstants.SHOW_VALUE_TOPIC) Event event) {
		_treeViewer.refresh();
	}

	@Inject
	@Optional
	private void basicLabelListener(@UIEventTopic(PreferenceEventConstants.MODEL_VIEW_TEXT_TOPIC) Event instanz) {
		_treeViewer.refresh();
	}
}
