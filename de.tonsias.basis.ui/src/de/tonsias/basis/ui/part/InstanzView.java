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
import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.BiMap;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.logic.part.InstanzViewLogic;
import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IMultiValue;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.model.interfaces.IValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.ValueEventConstants;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.dialog.AMultiValueDialog;
import de.tonsias.basis.ui.dialog.ValueDialogs;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.util.MessagesUtil;
import de.tonsias.basis.ui.widget.InstanzSelectionDialog;
import java.util.stream.Collectors;
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
	private IMultiValueService _multiService;

	@Inject
	private IEventBrokerBridge _broker;

	private InstanzViewLogic _logic;

	private IInstanz _shownInstanz = null;

	private Label _ownKeyLabel;

	private Collection<Group> _groups = new ArrayList<Group>();

	private Composite _parent;

	@PostConstruct
	public void postConstruct(Composite parent) {
		_logic = new InstanzViewLogic(_instanzService, _singleService, _multiService);

		GridLayoutFactory.fillDefaults().applyTo(parent);
		_parent = parent;

		if (_shownInstanz == null) {
			return;
		}
		createInstanzInfos();
		createValueGroups();
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
		createValueGroups();
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

	private void createValueGroups() {
		createValueGroup(_messages.constant_singleValue, SingleValueType.values());
		createValueGroup(_messages.constant_multiValue, MultiValueType.values());
	}

	private void createValueGroup(String label, IValueType[] types) {
		Group parent = new Group(_parent, SWT.None);
		parent.setText(label);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		_groups.add(parent);

		for (IValueType type : types) {
			Group typeGroup = new Group(parent, SWT.None);
			typeGroup.setText(MessagesUtil.getValueTypeLabel(_messages, type));
			GridDataFactory.fillDefaults().grab(true, false).applyTo(typeGroup);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(typeGroup);

			BiMap<String, String> values = _shownInstanz.getValues(type);
			for (Entry<String, String> keyToName : values.entrySet()) {
				Optional<? extends IValue> value = resolve(type, keyToName.getKey());
				if (value.isPresent()) {
					createValueNameText(typeGroup, value.get(), keyToName.getValue());
					createValueControls(typeGroup, value.get());
				} // TODO: is there always a resolvable value? See
					// https://github.com/Tobias-Bonsack/Tonsias/issues/86
			}
		}
	}

	private Optional<? extends IValue> resolve(IValueType type, String valueKey) {
		if (type instanceof MultiValueType multi) {
			return _multiService.resolveKey(multi.getPath(), valueKey, multi.getClazz());
		}
		return _singleService.resolveKey(type.getPath(), valueKey, ((SingleValueType) type).getClazz());
	}

	/**
	 * The widget an attribute is edited with, chosen by what it holds rather than by
	 * which of the ten types it is - five contents, plus the one branch that really
	 * differs.
	 */
	private void createValueControls(Group typeGroup, IValue value) {
		Control control = value.getType().isMulti() ? createElementsButton(typeGroup, (IMultiValue<?>) value)
				: createSingleValueControl(typeGroup, (ISingleValue<?>) value);
		createSaveKeyListener(control);

		Label keyLabel = LabelFactory.newLabel(SWT.None)//
				.text(_messages.constant_key + ": " + value.getOwnKey())//
				.layoutData(GridDataFactory.fillDefaults().create())//
				.create(typeGroup);

		Menu labelCM = new Menu(keyLabel);
		labelCM.setData(keyLabel);
		keyLabel.setMenu(labelCM);

		MenuItem deleteMI = new MenuItem(labelCM, SWT.PUSH);
		deleteMI.setData(value);
		deleteMI.setText(_messages.mi_delete);
		deleteMI.addSelectionListener(deleteValueSelectionListener());
	}

	/**
	 * A list is edited in the dialog that shows the whole of it. The view has one
	 * line per attribute, which is no room for a table - so the button says what the
	 * list holds and opens the dialog, the way a relation's button opens the
	 * chooser.
	 */
	private Control createElementsButton(Group typeGroup, IMultiValue<?> multiValue) {
		Button edit = ButtonFactory.newButton(SWT.PUSH)//
				.text(elementsLabel(multiValue))//
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
				.create(typeGroup);
		edit.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> editElements(multiValue, edit)));
		return edit;
	}

	private String elementsLabel(IMultiValue<?> multiValue) {
		if (multiValue.getValues().isEmpty()) {
			return _messages.constant_edit;
		}
		if (multiValue.getType().getContentType() == ValueContentType.INSTANZ) {
			return multiValue.getValues().stream().map(element -> targetLabel(String.valueOf(element)))
					.collect(Collectors.joining(", "));
		}
		return multiValue.getValues().stream().map(String::valueOf).collect(Collectors.joining(", "));
	}

	/**
	 * Opens the list dialog on the value as it stands and queues what comes back.
	 * Cancelling leaves the list as it is - the button is not a change in itself.
	 */
	private void editElements(IMultiValue<?> multiValue, Button button) {
		if (_logic.isInDelete(multiValue)) {
			return;
		}

		AMultiValueDialog<?> dialog = ValueDialogs.edit(multiValue, button.getShell(), _shownInstanz, _messages);
		// the view queues what its widgets did and applies it all on save, so the
		// dialog must not write on its own - the same rule the text fields follow
		dialog.setWriteOnOk(false);
		if (dialog.open() != Window.OK) {
			return;
		}

		button.setBackground(button.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		_logic.createModifyElementsJob(multiValue.getOwnKey(), dialog.getEnteredElements());
		_part.setDirty(true);
	}

	private Control createSingleValueControl(Group typeGroup, ISingleValue<?> singleValue) {
		switch (singleValue.getType().getContentType()) {
		// both numbers are entered in one line and travel on as text, which
		// tryToSetValue parses - the only difference is what it accepts there
		case INTEGER:
		case FLOAT:
			return TextFactory.newText(SWT.None)//
					.text(singleValue.getValue().toString())//
					.onModify(event -> onSingleValueModify(singleValue, event))
					.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
					.create(typeGroup);
		case STRING:
			return TextFactory.newText(SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL)//
					.text(singleValue.getValue().toString())//
					.onModify(event -> onSingleValueModify(singleValue, event))//
					.layoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 100).create())//
					.create(typeGroup);
		case BOOLEAN:
			Button check = ButtonFactory.newButton(SWT.CHECK)//
					.text("")//
					.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
					.create(typeGroup);
			check.setSelection(Boolean.TRUE.equals(singleValue.getValue()));
			check.addSelectionListener(SelectionListener
					.widgetSelectedAdapter(event -> onSingleValueSelect(singleValue, check.getSelection(), check)));
			return check;
		case INSTANZ:
			// a relation is chosen, not typed. The view has one line per attribute, which
			// is no room for a tree, so the button says where the relation points and
			// opens the chooser
			Button choose = ButtonFactory.newButton(SWT.PUSH)//
					.text(targetLabel(String.valueOf(singleValue.getValue())))//
					.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
					.create(typeGroup);
			choose.addSelectionListener(
					SelectionListener.widgetSelectedAdapter(event -> chooseTarget(singleValue, choose)));
			return choose;
		default:
			// without a widget the caller would go on with null - a new content type has
			// to bring its own case rather than break the whole view
			throw new IllegalArgumentException("Unexpected value: " + singleValue.getType());
		}
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

	private SelectionAdapter deleteValueSelectionListener() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IValue singleValue = (IValue) e.widget.getData();
				if (e.getSource() instanceof MenuItem mi) {
					Control parent = (Control) mi.getParent().getData();
					parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
				}
				_logic.createDeleteValueJob(singleValue);
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
		_logic.createModifyValueJob(singleValue.getOwnKey(), text.getText());
		_part.setDirty(true);
	}

	/**
	 * The counterpart of {@link #onSingleValueModify} for the controls a value is
	 * chosen in rather than typed into: the check box and the combo box of a
	 * relation. What they hold sits on the widget instead of in the event, so the
	 * caller reads it out and hands it on - a {@link Boolean} for the one, the key
	 * of the chosen instanz for the other.
	 */
	private void onSingleValueSelect(ISingleValue<?> singleValue, Object newValue, Control control) {
		if (_logic.isInDelete(singleValue)) {
			return;
		}

		control.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		_logic.createModifyValueJob(singleValue.getOwnKey(), newValue);
		_part.setDirty(true);
	}

	/**
	 * Opens the chooser on the tree as it is right now and hands the chosen key on
	 * the way the check box hands on its state. Cancelling leaves the relation
	 * where it points - the button is not a change in itself.
	 */
	private void chooseTarget(ISingleValue<?> singleValue, Button button) {
		InstanzSelectionDialog dialog = new InstanzSelectionDialog(button.getShell(), instanzChoices().tree(),
				_messages, String.valueOf(singleValue.getValue()));
		if (dialog.open() != Window.OK) {
			return;
		}

		button.setText(dialog.getSelectedLabel());
		onSingleValueSelect(singleValue, dialog.getSelectedKey(), button);
	}

	/**
	 * How the instanz a relation points at reads. A relation pointing nowhere says
	 * so in so many words rather than sitting there as an empty button - and so
	 * does one whose target the walk did not reach.
	 */
	private String targetLabel(String targetKey) {
		return instanzChoices().labelOf(targetKey).orElse(_messages.constant_noInstanz);
	}

	/**
	 * The instanzen a relation can point at. Built per call rather than kept: the
	 * model changes under the view, and a stale tree would offer instanzen that are
	 * gone and hide the ones just made.
	 */
	private InstanzChoices instanzChoices() {
		return new InstanzChoices(_instanzService, _singleService,
				OsgiUtil.getService(IBasicPreferenceService.class));
	}

	private void createValueNameText(Group parent, IValue value, String parameterName) {
		TextFactory.newText(SWT.None)//
				.enabled(true)//
				.layoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).create())//
				.text(parameterName)//
				.onModify(event -> onValueNameModify(value, event))//
				.create(parent);
	}

	private void onValueNameModify(IValue value, ModifyEvent event) {
		Text text = (Text) event.widget;
		text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_GREEN));
		_logic.createValueNameModifyJob(_shownInstanz.getOwnKey(), ((Text) event.widget).getText(), value);
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

	@Inject
	@org.eclipse.e4.core.di.annotations.Optional
	private void instanzDeltaEventListener(@UIEventTopic(InstanzEventConstants.ALL_DELTA_TOPIC) InstanzEventConstants.KeyEvent event) {
		if (_shownInstanz == null || !event.getKey().equals(_shownInstanz.getOwnKey())) {
			return;
		}
		updateView();
	}
	@Inject
	@org.eclipse.e4.core.di.annotations.Optional
	private void singlevalueDeltaEventListener(
			@UIEventTopic(SingleValueEventConstants.ALL_DELTA_TOPIC) SingleValueEventConstants.SingleValueEvent event) {
		valueDeltaEvent(event);
	}

	@Inject
	@org.eclipse.e4.core.di.annotations.Optional
	private void multivalueDeltaEventListener(
			@UIEventTopic(MultiValueEventConstants.ALL_DELTA_TOPIC) MultiValueEventConstants.MultiValueEvent event) {
		valueDeltaEvent(event);
	}

	/** both families ask the same question, so they answer it in the same place */
	private void valueDeltaEvent(ValueEventConstants.ValueEvent event) {
		if (_logic.affectsShownInstanz(_shownInstanz, event)) {
			updateView();
		}
	}
}
