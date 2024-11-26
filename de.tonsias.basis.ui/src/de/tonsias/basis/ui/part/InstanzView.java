package de.tonsias.basis.ui.part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class InstanzView {

	private IInstanz _shownInstanz = null;

	private Label _ownKeyLabel;

	private Collection<Group> _groups = new ArrayList<Group>();

	private Composite _parent;

	@Inject
	private MPart _part;

	@Inject
	private IInstanzService _instanzService;

	@Inject
	private ISingleValueService _singleService;

	@Inject
	private IEventBrokerBridge _broker;

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

	private void createParent() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText("Parent");
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
		parent.setText("Children");
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

	private void createInstanzInfos() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText("Instanz");
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);

		LabelFactory.newLabel(SWT.None)//
				.text("Key")//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);
		_ownKeyLabel = LabelFactory.newLabel(SWT.None)//
				.text(_shownInstanz.getOwnKey())//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);

	}

	private void createSingleValueGroup() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText("SingleValues");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		_groups.add(parent);

		SingleValueType[] values = SingleValueType.values();
		for (SingleValueType type : values) {
			Group typeGroup = new Group(parent, SWT.None);
			typeGroup.setText(type.name());
			GridDataFactory.fillDefaults().grab(true, false).applyTo(typeGroup);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(typeGroup);

			BiMap<String, String> singleValues = _shownInstanz.getSingleValues(type);
			for (Entry<String, String> attribute : singleValues.entrySet()) {
				createSingleValueNameText(typeGroup, attribute, type);

				Optional<? extends ISingleValue<?>> singleValue = _singleService.resolveKey(type.getPath(),
						attribute.getKey(), type.getClazz());
				if (singleValue.isPresent()) {
					createSinlgeValueTexts(typeGroup, singleValue);
				}
			}
		}
	}

	private void createSinlgeValueTexts(Group typeGroup, Optional<? extends ISingleValue<?>> singleValue) {
		TextFactory.newText(SWT.None)//
				.text(singleValue.get().getValue().toString())//
				.onModify(event -> {
					String text = ((Text) event.widget).getText();
					_singleService.changeValue(singleValue.get().getOwnKey(), text);
					_part.setDirty(true);
				}).layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
				.create(typeGroup);

		Label keyLabel = LabelFactory.newLabel(SWT.None)//
				.text("Key: " + singleValue.get().getOwnKey())//
				.layoutData(GridDataFactory.fillDefaults().create())//
				.create(typeGroup);

		Menu labelCM = new Menu(keyLabel);
		keyLabel.setMenu(labelCM);

		MenuItem deleteMI = new MenuItem(labelCM, SWT.PUSH);
		deleteMI.setData(singleValue.get());
		deleteMI.setText("Delete");
		deleteMI.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISingleValue<?> data = (ISingleValue<?>) e.widget.getData();
				_singleService.deleteValue(data);

				SingleValueType type = SingleValueType.getByClass(data.getClass()).get();
				_instanzService.removeValueKey(data.getConnectedInstanzKeys(), type, data.getOwnKey());
			}
		});
	}

	private void createSingleValueNameText(Group parent, Entry<String, String> attribute, SingleValueType type) {
		TextFactory.newText(SWT.None)//
				.enabled(true)//
				.layoutData(GridDataFactory.fillDefaults().create())//
				.text(attribute.getValue())//
				.onModify(event -> {
					_instanzService.putSingleValue(_shownInstanz.getOwnKey(), type, attribute.getKey(),
							((Text) event.widget).getText());
					_part.setDirty(true);
				})//
				.create(parent);
	}

	@Inject
	@org.eclipse.e4.core.di.annotations.Optional
	private void selectionEventListener(
			@UIEventTopic(InstanzEventConstants.SELECTED) InstanzEventConstants.PureInstanzData data) {
		if (data._newInstanz() == null || data._newInstanz().equals(_shownInstanz)) {
			return;
		}

		if (_part.isDirty() && MessageDialog.openQuestion(new Shell(), "Ist noch dirty hier",
				"Sollen die Änderungen gespeichert werden?")) {
			_broker.send(EventConstants.SAVE_ALL, null);
		}

		_shownInstanz = data._newInstanz();
		_part.setDirty(false);
		updateView();
	}
}
