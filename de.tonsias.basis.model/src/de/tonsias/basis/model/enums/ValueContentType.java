package de.tonsias.basis.model.enums;

/**
 * What a value holds, independent of whether it holds one of them or a list.
 * <p>
 * The ten value types are five contents times two kinds, and that is what keeps
 * them from becoming ten copies of everything: the parsing rules
 * ({@code ValueContentRules}), the cell editors and the controls in the instanz
 * view all switch over these five, and only the two places that really differ -
 * one value against a list - ask {@link IValueType#isMulti()}.
 * </p>
 */
public enum ValueContentType {

	STRING,

	INTEGER,

	BOOLEAN,

	FLOAT,

	/**
	 * a relation rather than a literal: the content is the key of another instanz.
	 * The one content type whose values have a second end, kept in sync by
	 * {@code ChangePropagationListener}.
	 */
	INSTANZ;
}
