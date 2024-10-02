package de.tonsias.basis.osgi.intf.non.service;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public interface IPreferences {

	/**
	 * 
	 * @return the node for the preference
	 */
	IEclipsePreferences getNode();

}
