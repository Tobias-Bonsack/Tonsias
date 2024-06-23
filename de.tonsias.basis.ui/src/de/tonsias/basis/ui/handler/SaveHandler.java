package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;

import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;

public class SaveHandler {

	@Execute
	public void execute() {
		OsgiUtil.getService(IInstanzService.class).saveAll();
		OsgiUtil.getService(ISingleValueService.class).saveAll();
	}
}
