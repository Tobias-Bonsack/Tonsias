package de.tonsias.basis.ui.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.GroupFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.logic.dialog.PreferencesDialogLogic;

public class PreferencesDialog extends Dialog {

	private final PreferencesDialogLogic _logic = new PreferencesDialogLogic();

	private Composite _preferenceParent;

	private Collection<Text> _texts = new ArrayList<Text>();

	public PreferencesDialog(Shell parentShell) {
		super(parentShell);
		System.out.println(this.isResizable());
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
		return composite;
	}

	private void createEntryList(Composite composite) {
		Consumer<String> refreshMethod = s -> this.refreshRightSide(s);
		Group parent = GroupFactory.newGroup(SWT.None).text("Basic").create(composite);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);

		List list = new List(parent, SWT.None);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(list);
		list.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				String[] selection = list.getSelection();
				if (selection.length <= 0) {
					return;
				}
				refreshMethod.accept(selection[0]);

			}
		});
		_logic.getPreferenceNames().forEach(list::add);
	}

	private void refreshRightSide(String name) {
		Arrays.stream(_preferenceParent.getChildren()).forEach(i -> i.dispose());
		_texts.clear();
		Map<String, String> preferences = _logic.getPreferences(name);

		for (var pair : preferences.entrySet()) {
			LabelFactory.newLabel(SWT.None).data(GridDataFactory.fillDefaults().create()).text(pair.getKey())
					.create(_preferenceParent);
			Text text = TextFactory.newText(SWT.None).text(pair.getValue()).create(_preferenceParent);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
			_texts.add(text);
		}

		_preferenceParent.layout();
	}

}
