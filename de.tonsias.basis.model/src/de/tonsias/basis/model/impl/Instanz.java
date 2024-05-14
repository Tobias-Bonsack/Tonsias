package de.tonsias.basis.model.impl;

import java.io.Serializable;

import de.tonsias.basis.model.interfaces.IObject;

public class Instanz extends AInstanz implements Serializable, Cloneable {

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
		return PATH;
	}

	@Override
	public int hashCode() {
		return getOwnKey().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof IObject)) {
			return false;
		}
		IObject object = (IObject) obj;
		return this.getOwnKey().equals(object.getOwnKey());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getOwnKey()).append(" ");
		String[] string = this.getClass().toString().split("\\.");
		builder.append(": ").append(string[string.length - 1]);
		return builder.toString();
	}
}
