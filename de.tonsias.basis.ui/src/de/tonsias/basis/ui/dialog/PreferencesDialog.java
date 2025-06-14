package de.tonsias.basis.ui.dialog;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.notifications.NotificationPopup;
import org.eclipse.jface.widgets.GroupFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.logic.dialog.PreferencesDialogLogic;
import de.tonsias.basis.logic.dialog.PreferencesDialogLogic.PreferenceFeature;

public class PreferencesDialog extends Dialog {

	private static final int SAVE_ID = IDialogConstants.CLIENT_ID + 1;

	private final PreferencesDialogLogic _logic = new PreferencesDialogLogic();

	private Composite _preferenceParent;

	private Map<String, Text> _texts = new HashMap<>();

	public PreferencesDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(300, 200);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);

		createEntryList(composite);
		_preferenceParent = new Group(composite, SWT.None);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_preferenceParent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_preferenceParent);
		refreshRightSide(_logic.getPreferenceNames().iterator().next());

		getShell().setMinimumSize(500, 300);
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button saveButton = createButton(parent, SAVE_ID, "Speichern", false);
		saveButton.setEnabled(false);

		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);

		switch (buttonId) {
		case SAVE_ID:
			saveCurrentPref(buttonId);
			break;
		}
	}

	private void saveCurrentPref(int buttonId) {
		Map<String, String> collect = _texts.entrySet().stream()
				.collect(Collectors.toMap(i -> i.getKey(), i -> i.getValue().getText()));
		_logic.savePreference(collect);
		getButton(buttonId).setEnabled(false);
	}

	private void createEntryList(Composite composite) {
		Consumer<String> refreshMethod = s -> this.refreshRightSide(s);
		Supplier<Boolean> checkForSave = this::checkForSave;

		Group parent = GroupFactory.newGroup(SWT.None).text("Basic").create(composite);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);

		List list = new List(parent, SWT.None);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(list);
		list.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] selection = list.getSelection();
				if (selection.length <= 0) {
					return;
				}
				checkForSave.get();
				refreshMethod.accept(selection[0]);
			};
		});
		_logic.getPreferenceNames().forEach(list::add);
	}

	private Boolean checkForSave() {
		if (getButton(SAVE_ID).isEnabled() && MessageDialog.openQuestion(getParentShell(), "Wirklich?",
				"Sollen die Änderungen gespeichert werden?")) {
			saveCurrentPref(SAVE_ID);
			return true;
		}
		getButton(SAVE_ID).setEnabled(false);
		return false;
	}

	private void refreshRightSide(String name) {
		Arrays.stream(_preferenceParent.getChildren()).forEach(i -> i.dispose());
		_texts.clear();
		Collection<PreferenceFeature> preferences = _logic.getPreferences(name);

		for (var pair : preferences) {
			LabelFactory.newLabel(SWT.None).data(GridDataFactory.fillDefaults().create()).text(pair.name())
					.create(_preferenceParent);
			Text text = TextFactory.newText(SWT.None)//
					.text(pair.value())//
					.layoutData(GridDataFactory.fillDefaults().grab(true, false).create())//
					.onModify(event -> {
						getButton(SAVE_ID).setEnabled(true);
					})//
					.create(_preferenceParent);
			text.setEditable(pair.editable());
			if(!pair.editable()) {
				text.addListener(SWT.MouseDown, event -> {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.getText()), null);
					NotificationPopup.forDisplay(Display.getCurrent()).delay(2_000).title("Copy Parameter", true).text(String.format("%s: \n%s",pair.name(), pair.value())).build().open();
				});
			}
			
			_texts.put(pair.name(), text);
		}

		_preferenceParent.layout();
	}
	
}
