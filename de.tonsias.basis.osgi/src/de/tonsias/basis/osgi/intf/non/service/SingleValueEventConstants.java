package de.tonsias.basis.osgi.intf.non.service;

import de.tonsias.basis.model.interfaces.ISingleValue;

public interface SingleValueEventConstants {

	// topic identifier
	String SINGLE_VALUE = "singleValue";

	String ALL_DELTA_TOPIC = SINGLE_VALUE + "/delta/*";

	String NEW = SINGLE_VALUE + "/delta/new";

	String CHANGE = SINGLE_VALUE + "/delta/change";

	String DELETE = SINGLE_VALUE + "/delta/delete";

	static record PureSingleValueData(ISingleValue<?> _newSingleValue) {
	}

	static record AttributeChangeData(String _key, String _oldValue, String _newValue) {
	}
}
