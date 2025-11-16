package de.tonsias.basis.osgi.intf.non.service;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public interface IPreferences {
	
	public interface PreferenceKeyEnum{
		public String getKey();

		public String getInitValue();

		public boolean isEnabled();
	}

	/**
	 * 
	 * @return the node for the preference
	 */
	IEclipsePreferences getNode();
	
	PreferenceKeyEnum[] getKeys();

}
