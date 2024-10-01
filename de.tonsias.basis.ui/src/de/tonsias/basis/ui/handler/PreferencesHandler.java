package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.ui.dialog.PreferencesDialog;

public class PreferencesHandler {

	@Execute
	public void execute() {
		PreferencesDialog dialog = new PreferencesDialog(new Shell());
		dialog.open();
	}
}
