package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.ui.dialog.PreferencesDialog;
import de.tonsias.basis.ui.i18n.Messages;

public class PreferencesHandler {

	@Execute
	public void execute(@Translation Messages messages) {
		Shell parentShell = new Shell(SWT.DIALOG_TRIM | SWT.RESIZE);
		parentShell.setSize(500, 500);
		PreferencesDialog dialog = new PreferencesDialog(parentShell, messages);
		dialog.open();
	}
}
