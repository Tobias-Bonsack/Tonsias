package de.tonsias.basis.model.enums;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IValue;

/**
 * What both {@link SingleValueType} and {@link MultiValueType} answer, so that
 * everything which only needs "where does this live and what class is it" can be
 * written once. {@code IInstanz.getValues}, the folder scans in the services and
 * the type combo in the create dialog all take this rather than one of the enums.
 * <p>
 * It lives beside the two enums rather than in {@code interfaces} because
 * {@link IInstanz} already imports this package, so no new edge between the two
 * packages appears.
 * </p>
 * <p>
 * Implemented only by the two enums. {@link #name()} is what an enum brings
 * along; it is declared here so a log or an i18n fallback can name a type without
 * knowing which of the two it holds.
 * </p>
 */
public interface IValueType {

	/** @see Enum#name() */
	String name();

	/**
	 * Folder this type's values are saved in, below the instance location, with a
	 * trailing slash. Unique across <em>both</em> enums - the services find a value
	 * whose type they no longer know by trying one folder after the other.
	 */
	String getPath();

	/**
	 * The concrete class to hand to the load service, which is what tells Gson which
	 * fields to read. Narrowed to {@code ISingleValue} resp. {@code IMultiValue} by
	 * the two enums.
	 */
	Class<? extends IValue> getClazz();

	/** what the values of this type hold */
	ValueContentType getContentType();

	/** whether a value of this type holds a list rather than a single content */
	boolean isMulti();
}
