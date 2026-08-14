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

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IMultiValue;
import de.tonsias.basis.model.interfaces.IObject;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.model.interfaces.IValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.osgi.intf.non.service.PreferenceEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.ui.dialog.AValueDialogBase;
import de.tonsias.basis.ui.dialog.ValueDialogs;
import de.tonsias.basis.ui.handler.CreateInstanzOperation;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.node.TreeNodeWrapper;
import de.tonsias.basis.ui.util.MessagesUtil;
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
	IMultiValueService _multiService;

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
		createValueMenuItems(tree, menu, _messages.constant_singleValue, SingleValueType.values());
		createValueMenuItems(tree, menu, _messages.constant_multiValue, MultiValueType.values());
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
		_menuItems.computeIfAbsent(IValue.class, c -> new ArrayList<>()).add(menuItem);

		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// what a tree item carries is the element of the content provider, and that
				// is a node of the tree - never the object it stands for. Casting straight to
				// the value threw before it deleted anything, see
				// https://github.com/Tobias-Bonsack/Tonsias/issues/90
				IValue value = selectedValue(tree);
				if (value == null) {
					return;
				}

				Collection<String> connectedInstanzKeys = value.getConnectedInstanzKeys();
				// the value says which type it is, so there is nothing to look up
				IValueType type = value.getType();

				_broker.post(EventConstants.OPEN_OPERATION, null);
				deleteValue(value);
				_instanzService.removeValueKey(connectedInstanzKeys, type, value.getOwnKey(),
						IEventBrokerBridge.Type.POST);
				_broker.post(EventConstants.CLOSE_OPERATION, null);

				_treeViewer.refresh();
			};
		});
	}

	/**
	 * The attribute the one selected node stands for, or {@code null} when the
	 * selection is no attribute - the menu item is only enabled over one, and a
	 * selection that changed under it is nothing to act on either way.
	 */
	private IValue selectedValue(Tree tree) {
		TreeItem[] selection = tree.getSelection();
		if (selection.length == 0 || !(selection[0].getData() instanceof TreeNodeWrapper node)) {
			return null;
		}
		return node.getObject() instanceof IValue value ? value : null;
	}

	private void deleteValue(IValue value) {
		if (value.getType().isMulti()) {
			_multiService.deleteValue((IMultiValue<?>) value, IEventBrokerBridge.Type.SEND);
			return;
		}
		_singleService.deleteValue((ISingleValue<?>) value, IEventBrokerBridge.Type.SEND);
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
					newInstanzOperation.execute(_broker.getEclipseBroker(), _instanzService, _messages);
					// the nodes below are the content provider's to build, so there is nothing
					// to hang into the tree here - only to ask it again
					_treeViewer.refresh(parent);
				}
			}
		});
	}

	/**
	 * One cascading menu per kind, one item per type in it. Which dialog an item
	 * opens is {@link ValueDialogs}', and what happens afterwards is the same for
	 * all ten - so a new type is a constant in an enum and nothing here.
	 */
	private void createValueMenuItems(Tree tree, Menu menu, String label, IValueType[] types) {
		MenuItem parentItem = new MenuItem(menu, SWT.CASCADE);
		parentItem.setText(label);
		_menuItems.get(IInstanz.class).add(parentItem);

		Menu valueMenu = new Menu(menu);
		parentItem.setMenu(valueMenu);

		for (IValueType type : types) {
			MenuItem item = new MenuItem(valueMenu, SWT.None);
			item.setText(MessagesUtil.getValueTypeLabel(_messages, type));
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					openValueDialog(tree, type);
				}
			});
		}
	}

	private void openValueDialog(Tree tree, IValueType type) {
		TreeItem[] selection = tree.getSelection();
		if (selection.length != 1) {
			return;
		}

		TreeNodeWrapper parent = (TreeNodeWrapper) selection[0].getData();
		IInstanz parentObject = (IInstanz) parent.getObject();

		AValueDialogBase<?, ?> dialog = ValueDialogs.create(type, new Shell(), parentObject, _messages);
		if (dialog.open() == Window.OK) {
			_treeViewer.refresh(parent);
		}
	}

	/**
	 * The three listeners below are the tree following the model. Each of them is
	 * the event of the change <em>on the instanz</em>, never the event of the thing
	 * being created: a value is created with {@code Type.POST} and hung onto its
	 * instanz afterwards, by {@code ChangePropagationListener} on the event admin's
	 * thread. A view refreshing on the creation reads the instanz before the
	 * attribute is on it, shows nothing, and is not asked again until the next
	 * operation - which is the "one operation behind" of #89.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/89">#89</a>
	 */
	private void refreshTree() {
		_treeViewer.refresh();
	}

	@Inject
	@Optional
	private void newInstanzListener(@UIEventTopic(InstanzEventConstants.NEW) Event data) {
		refreshTree();
	}

	/** an instanz came under a parent, or went - the tree draws it as a child */
	@Inject
	@Optional
	private void childListChangeListener(@UIEventTopic(InstanzEventConstants.CHILD_LIST_CHANGE) Event data) {
		refreshTree();
	}

	/**
	 * An attribute came or went on an instanz. That is a child of it in the tree
	 * while the preference is on, and it is where the label of an instanz is read
	 * out of either way - so one listener for all ten types, because the tree draws
	 * them all alike.
	 */
	@Inject
	@Optional
	private void valueListChangeListener(@UIEventTopic(InstanzEventConstants.VALUE_LIST_CHANGE) Event data) {
		refreshTree();
	}

	/**
	 * Stays on {@code SINGLE_STRING} alone: the label of the model view is one
	 * string by definition, so a list can never be the attribute it shows.
	 */
	@Inject
	@Optional
	private void changeOfModelViewText(@UIEventTopic(SingleValueEventConstants.VALUE_CHANGE) Event data) {
		Object property = data.getProperty(IEventBroker.DATA);
		if (!(property instanceof SingleValueEventConstants.ValueChangeEvent changeEvent)
				|| changeEvent._type() != SingleValueType.SINGLE_STRING) {
			return;
		}

		IBasicPreferenceService prefService = OsgiUtil.getService(IBasicPreferenceService.class);
		String shownVariable = prefService.getValue(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), String.class)
				.orElse("");

		java.util.Optional<? extends ISingleValue<?>> sv = _singleService.resolveKey(changeEvent._type().getPath(),
				changeEvent._key(), changeEvent._type().getClazz());
		if (sv.isEmpty()) {
			return;
		}
		String ownKey = sv.get().getOwnKey();

		Collection<IInstanz> linkedInstanzen = _instanzService.resolveKeys(sv.get().getConnectedInstanzKeys());
		if (linkedInstanzen.stream()
				.filter(instanz -> instanz.getValues(changeEvent._type()).containsKey(ownKey))
				.anyMatch(instanz -> instanz.getValues(changeEvent._type()).get(ownKey)
						.equals(shownVariable))) {
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
