package de.tonsias.basis.osgi.intf.non.service;

/**
 * Which direction a link moved: it came, or it went.
 * <p>
 * The same question for a child of an instanz, for a value hanging on one and
 * for an instanz a relation points at - so it is asked once, from a type neither
 * the instanz side nor the value side owns. It used to be two identical enums
 * nested in two constants interfaces, which meant every {@code switch} over one
 * of them read alike and only the import said which.
 * </p>
 *
 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/84">#84</a>
 */
public enum ChangeType {
	ADD, REMOVE;
}
