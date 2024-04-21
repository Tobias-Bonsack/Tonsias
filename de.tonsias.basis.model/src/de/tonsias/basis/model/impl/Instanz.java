package de.tonsias.basis.model.impl;

import java.io.Serializable;
import de.tonsias.basis.model.interfaces.ISavePathOwner;

public class Instanz extends AInstanz implements Serializable, Cloneable, ISavePathOwner {

	private static final String PATH = "instanz/";

	public Instanz(String key) {
		super(key);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getPath() {
		return PATH + getOwnKey();
	}
}
