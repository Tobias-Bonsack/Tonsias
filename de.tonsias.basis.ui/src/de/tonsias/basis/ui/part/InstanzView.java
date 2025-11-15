package de.tonsias.basis.ui.part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.BiMap;

import de.tonsias.basis.logic.part.InstanzViewLogic;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.ui.i18n.Messages;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class InstanzView {

	@Inject
	@Translation
	Messages _messages;

	@Inject
	private MPart _part;

	@Inject
	private IInstanzService _instanzService;

	@Inject
	private ISingleValueService _singleService;

	@Inject
	private IEventBrokerBridge _broker;

	private InstanzViewLogic _logic;

	private IInstanz _shownInstanz = null;

	private Label _ownKeyLabel;

	private Collection<Group> _groups = new ArrayList<Group>();

	private Composite _parent;

	@PostConstruct
	public void postConstruct(Composite parent) {
		_logic = new InstanzViewLogic(_instanzService, _singleService);

		GridLayoutFactory.fillDefaults().applyTo(parent);
		_parent = parent;

		if (_shownInstanz == null) {
			return;
		}
		createInstanzInfos();
		createSingleValueGroup();
		createChildren();
		createParent();
		
		parent.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					updateView();
				}
			}
		});
	}

	private void createInstanzInfos() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText(_messages.constant_instanz);
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);

		LabelFactory.newLabel(SWT.None)//
				.text(_messages.constant_key)//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);
		_ownKeyLabel = LabelFactory.newLabel(SWT.None)//
				.text(_shownInstanz.getOwnKey())//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);

	}

	public void updateView() {
		if (_ownKeyLabel == null) {
			createInstanzInfos();
		}
		_ownKeyLabel.setText(_shownInstanz.getOwnKey());

		_groups.forEach(group -> group.dispose());
		createSingleValueGroup();
		createParent();
		createChildren();

		_parent.layout();
	}

	private void createParent() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText(_messages.constant_parent);
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		_groups.add(parent);

		LabelFactory.newLabel(SWT.None)//
				.text(_shownInstanz.getParentKey())//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);
	}

	private void createChildren() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText(_messages.constant_children);
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		_groups.add(parent);

		Collection<String> children = _shownInstanz.getChildren();
		for (String key : children) {
			LabelFactory.newLabel(SWT.None)//
					.text(key)//
					.data(GridDataFactory.fillDefaults().create())//
					.create(parent);
		}
	}

	private void createSingleValueGroup() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText(_messages.constant_singleValue);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		_groups.add(parent);

		SingleValueType[] values = SingleValueType.values();
		for (SingleValueType type : values) {
			Group typeGroup = new Group(parent, SWT.None);
			typeGroup.setText(type.name()); // TODO: translation for type names
			GridDataFactory.fillDefaults().grab(true, false).applyTo(typeGroup);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(typeGroup);

			BiMap<String, String> singleValues = _shownInstanz.getSingleValues(type);
			for (Entry<String, String> svKeyToName : singleValues.entrySet()) {
				Optional<? extends ISingleValue<?>> singleValue = _singleService.resolveKey(type.getPath(),
						svKeyToName.getKey(), type.getClazz());
				if (singleValue.isPresent()) {
					createSingleValueNameText(typeGroup, singleValue.get(), svKeyToName.getValue(), type);
					createSinlgeValueTexts(typeGroup, singleValue.get());
				} // TODO: is there always a resolvable single value?
			}
		}
	}

	private void createSinlgeValueTexts(Group typeGroup, ISingleValue<?> singleValue) {
		Control control = null;
		switch (SingleValueType.getByClass(singleValue.getClass()).get()) {
		case SINGLE_INTEGER:
			control = TextFactory.newText(SWT.None)//
					.text(singleValue.getValue().toString())//
					.onModify(event -> onSingleValueModify(singleValue, event))
					.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
					.create(typeGroup);
			break;
		case SINGLE_STRING:
			control = TextFactory.newText(SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL)//
					.text(singleValue.getValue().toString())//
					.onModify(event -> onSingleValueModify(singleValue, event))//
					.layoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 100).create())//
					.create(typeGroup);
			break;
		}
		createSaveKeyListener(control);

		Label keyLabel = LabelFactory.newLabel(SWT.None)//
				.text(_messages.constant_key + ": " + singleValue.getOwnKey())//
				.layoutData(GridDataFactory.fillDefaults().create())//
				.create(typeGroup);

		Menu labelCM = new Menu(keyLabel);
		labelCM.setData(keyLabel);
		keyLabel.setMenu(labelCM);

		MenuItem deleteMI = new MenuItem(labelCM, SWT.PUSH);
		deleteMI.setData(singleValue);
		deleteMI.setText(_messages.mi_delete);
		deleteMI.addSelectionListener(deleteSingleValueSelectionListener());
	}

	private void createSaveKeyListener(Control control) {
		control.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.stateMask & SWT.CTRL) != 0 && e.keyCode == 's') {
					performSafeAction(0);
				}
			}
		});
	}

	private SelectionAdapter deleteSingleValueSelectionListener() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISingleValue<?> singleValue = (ISingleValue<?>) e.widget.getData();
				if (e.getSource() instanceof MenuItem mi) {
					Control parent = (Control) mi.getParent().getData();
					parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
				}
				_logic.createDeleteSvJob(singleValue);
				_part.setDirty(true);
			}
		};
	}

	private void onSingleValueModify(ISingleValue<?> singleValue, ModifyEvent event) {
		if (_logic.isInDelete(singleValue)) {
			return;
		}

		Text text = (Text) event.widget;
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		_logic.createModifySvJob(singleValue.getOwnKey(), text.getText());
		_part.setDirty(true);
	}

	private void createSingleValueNameText(Group parent, ISingleValue<?> singleValue, String parameterName,
			SingleValueType type) {
		TextFactory.newText(SWT.None)//
				.enabled(true)//
				.layoutData(GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create())//
				.text(parameterName)//
				.onModify(event -> onSingleValueNameModify(singleValue, type, event))//
				.create(parent);
	}

	private void onSingleValueNameModify(ISingleValue<?> singleValue, SingleValueType type, ModifyEvent event) {
		Text text = (Text) event.widget;
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		_logic.createSvNameModifyJob(_shownInstanz.getOwnKey(), ((Text) event.widget).getText(), singleValue);
		_part.setDirty(true);
	}

	@Inject
	@org.eclipse.e4.core.di.annotations.Optional
	private void changeListener(
			@UIEventTopic(InstanzEventConstants.NAME_CHANGE) InstanzEventConstants.ValueRenameEvent data) {
		if (_shownInstanz == null || !data._key().equals(_shownInstanz.getOwnKey())) {
			return;
		}
		updateView();
	}

	@Inject
	@org.eclipse.e4.core.di.annotations.Optional
	private void changeListener(
			@UIEventTopic(SingleValueEventConstants.VALUE_CHANGE) SingleValueEventConstants.ValueChangeEvent data) {
		Optional<? extends ISingleValue<?>> sValue = _singleService//
				.resolveKey(data._type().getPath(), data._key(), data._type().getClazz());

		if (_shownInstanz != null && sValue.isPresent()
				&& sValue.get().getConnectedInstanzKeys().contains(_shownInstanz.getOwnKey())) {
			return;
		}

		updateView();
	}

	@Inject
	@org.eclipse.e4.core.di.annotations.Optional
	private void selectionEventListener(
			@UIEventTopic(InstanzEventConstants.SELECTED) InstanzEventConstants.InstanzEvent data) {
		if (data._key() == null || (_shownInstanz != null && data._key().equals(_shownInstanz.getOwnKey()))) {
			return;
		}

		if (_part.isDirty()) {
			int index = MessageDialog.open(MessageDialog.QUESTION, new Shell(), _messages.dialog_save_title,
					_messages.dialog_save_text, SWT.None, _messages.constant_yes, _messages.constant_no,
					_messages.constant_cancel);
			performSafeAction(index);
			if (index == 2) {
				return;
			}
		}

		_shownInstanz = _instanzService.resolveKey(data._key()).orElseGet(() -> null);
		_part.setDirty(false);
		updateView();
	}

	public void performSafeAction(int index) {
		_logic.executeChanges(index, _broker, _shownInstanz);
		_part.setDirty(false);
		updateView();
	}
	
	
}
