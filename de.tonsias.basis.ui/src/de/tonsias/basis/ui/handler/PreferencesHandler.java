package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.ui.dialog.PreferencesDialog;

public class PreferencesHandler {

	@Execute
	public void execute() {
		Shell parentShell = new Shell(SWT.DIALOG_TRIM | SWT.RESIZE);
		parentShell.setSize(500, 500);
		PreferencesDialog dialog = new PreferencesDialog(parentShell);
		dialog.open();
	}
}
