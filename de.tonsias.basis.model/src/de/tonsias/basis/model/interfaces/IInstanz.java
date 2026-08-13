package de.tonsias.basis.model.interfaces;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;

/**
 * top interface for each displayed object
 */
public interface IInstanz extends IObject, ISavePathOwner {
	// Parent-Child section

	void setParentKey(String newParent);

	String getParentKey();

	/**
	 * Get all available {@link IInstanz} children, no values!
	 * 
	 * @return Collection of Keys
	 */
	Collection<String> getChildren();

	/**
	 * try to add children set
	 * 
	 * @param children to add
	 * @return map of true-added and false-not added
	 */
	Map<Boolean, Collection<String>> addChildKeys(String... children);

	/**
	 * try to remove children set
	 * 
	 * @param children to remove
	 * @return map of true-removed and false-not removed
	 */
	Map<Boolean, Collection<String>> removeChildKeys(String... children);

	// value section

	/**
	 * Get all {@link IValue}s of one type available of this {@link IInstanz}
	 *
	 * @param type from {@link SingleValueType} or {@link MultiValueType} to search
	 *             for
	 * @return {@link BiMap} Key is the Key of the {@link IValue}, Value is the Name
	 *         of the Parameter
	 */
	BiMap<String, String> getValues(IValueType type);

	void addValuekeys(IValueType type, Entry<String, String> keyToName);

	void deleteKeys(IValueType type, String... keys);

	void deleteParam(IValueType type, String... names);

	// relation section

	/**
	 * The other end of a {@code SINGLE_INSTANZ} or {@code MULTI_INSTANZ} relation:
	 * the keys of the values pointing <em>at</em> this instanz. A relation is stored
	 * on the value alone - it carries the key of its target - so without this set
	 * nothing could ever answer which values a given instanz is the target of, and a
	 * deleted instanz would leave every one of them holding a key that resolves to
	 * nothing.
	 * <p>
	 * A set, and each value key is in it at most once. That holds for a list of
	 * relations as well because {@code MultiInstanzValue} refuses duplicates, so one
	 * value points at one target at most once - a list that could point at the same
	 * instanz twice would need this end to count how often.
	 * </p>
	 * <p>
	 * Kept in sync by {@code ChangePropagationListener}, like both ends of every
	 * other relation. Do not fill it directly: a reference set past
	 * {@code IInstanzService} writes no delta and would be lost on the next save.
	 * </p>
	 *
	 * @return the value keys, live
	 */
	Collection<String> getReferencingValueKeys();

	/**
	 * @param valueKey of the {@code SingleInstanzValue} now pointing here
	 * @return true if it was not recorded before
	 */
	boolean addReferencingValueKey(String valueKey);

	/**
	 * @param valueKey of the {@code SingleInstanzValue} no longer pointing here
	 * @return true if it was recorded before
	 */
	boolean removeReferencingValueKey(String valueKey);
}
