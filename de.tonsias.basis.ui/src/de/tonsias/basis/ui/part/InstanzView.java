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

	private final InstanzViewLogic _logic = new InstanzViewLogic();

	private IInstanz _shownInstanz = null;

	private Label _ownKeyLabel;

	private Collection<Group> _groups = new ArrayList<Group>();

	private Composite _parent;

	@PostConstruct
	public void postConstruct(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		_parent = parent;

		if (_shownInstanz == null) {
			return;
		}
		createInstanzInfos();
		createSingleValueGroup();
		createChildren();
		createParent();
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

	private void updateView() {
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
			for (Entry<String, String> attribute : singleValues.entrySet()) {
				createSingleValueNameText(typeGroup, attribute, type);

				Optional<? extends ISingleValue<?>> singleValue = _singleService.resolveKey(type.getPath(),
						attribute.getKey(), type.getClazz());
				if (singleValue.isPresent()) {
					createSinlgeValueTexts(typeGroup, singleValue);
				} // TODO: is there always a resolvable single value?
			}
		}
	}

	private void createSinlgeValueTexts(Group typeGroup, Optional<? extends ISingleValue<?>> singleValue) {
		TextFactory.newText(SWT.None)//
				.text(singleValue.get().getValue().toString())//
				.onModify(event -> onSingleValueModify(singleValue, event))
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
				.create(typeGroup);

		Label keyLabel = LabelFactory.newLabel(SWT.None)//
				.text(_messages.constant_key + ": " + singleValue.get().getOwnKey())//
				.layoutData(GridDataFactory.fillDefaults().create())//
				.create(typeGroup);

		Menu labelCM = new Menu(keyLabel);
		labelCM.setData(keyLabel);
		keyLabel.setMenu(labelCM);

		MenuItem deleteMI = new MenuItem(labelCM, SWT.PUSH);
		deleteMI.setData(singleValue.get());
		deleteMI.setText(_messages.mi_delete);
		deleteMI.addSelectionListener(deleteSingleValueSelectionListener());
	}

	private SelectionAdapter deleteSingleValueSelectionListener() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISingleValue<?> data = (ISingleValue<?>) e.widget.getData();
				if (e.getSource() instanceof MenuItem mi) {
					Control parent = (Control) mi.getParent().getData();
					parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
				}

				_logic.createOneAndQuadFunctionJob(_singleService::removeValue, data, _instanzService::removeValueKey);
				_part.setDirty(true);

			}
		};
	}

	private void onSingleValueModify(Optional<? extends ISingleValue<?>> singleValue, ModifyEvent event) {
		Text text = (Text) event.widget;
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		_logic.createBiFunctionJob(_singleService::changeValue, singleValue.get().getOwnKey(), text.getText());
		_part.setDirty(true);
	}

	private void onSingleValueNameModify(Entry<String, String> attribute, SingleValueType type, ModifyEvent event) {
		Text text = (Text) event.widget;
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		_logic.createPentaConsumerJob(_instanzService::putSingleValue, _shownInstanz.getOwnKey(), type,
				attribute.getKey(), ((Text) event.widget).getText());
		_part.setDirty(true);
	}

	private void createSingleValueNameText(Group parent, Entry<String, String> attribute, SingleValueType type) {
		TextFactory.newText(SWT.None)//
				.enabled(true)//
				.layoutData(GridDataFactory.fillDefaults().create())//
				.text(attribute.getValue())//
				.onModify(event -> onSingleValueNameModify(attribute, type, event))//
				.create(parent);
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
			_logic.executeChanges(index, _broker, _shownInstanz);
			if (index == 2) {
				return;
			}
		}

		_shownInstanz = _instanzService.resolveKey(data._key()).orElseGet(() -> null);
		_part.setDirty(false);
		updateView();
	}
}
