package de.tonsias.basis.ui.handler;

import org.eclipse.e4.core.di.annotations.Execute;

import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.util.OsgiUtil;

public class SaveHandler {

	@Execute
	public void execute() {
		IEventBrokerBridge service = OsgiUtil.getService(IEventBrokerBridge.class);
		service.post(EventConstants.SAVE_ALL, null);
	}
}
