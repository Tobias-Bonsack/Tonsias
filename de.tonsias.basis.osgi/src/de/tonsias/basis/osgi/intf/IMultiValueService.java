package de.tonsias.basis.osgi.intf;

import java.util.Collection;

import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IMultiValue;

/**
 * The values holding a list. Everything that is not about the list lives in
 * {@link IValueService}, where {@link ISingleValueService} finds it too.
 * <p>
 * The vocabulary is deliberately "element" and not "value": taking an element out
 * of a list has nothing to do with
 * {@link IValueService#deleteValue(de.tonsias.basis.model.interfaces.IValue, IEventBrokerBridge.Type)},
 * which drops the whole attribute.
 * </p>
 */
public interface IMultiValueService extends IValueService<IMultiValue<?>, MultiValueType> {

	/**
	 * Creates a new {@link IMultiValue}, but it will not be saved and the parent
	 * will not be informed
	 *
	 * @param <E>           created class
	 * @param clazz         identification
	 * @param parentKey     Key of the parent {@link IInstanz}
	 * @param parameterName name of the parameter that holds the {@link IInstanz}
	 * @param elements      the list to start with - may be empty, which is what a
	 *                      list says instead of a default value
	 * @return new instance of {@link IMultiValue} or null
	 */
	<E extends IMultiValue<?>> E createNew(Class<E> clazz, String parentKey, String parameterName,
			Collection<?> elements, IEventBrokerBridge.Type eventType);

	/**
	 * @return false when the list already holds it, or the type will not read it -
	 *         and then no event, so a propagation chain ends here
	 */
	boolean addElement(String ownKey, Object element, IEventBrokerBridge.Type eventType);

	/**
	 * @return false when it was not in the list
	 */
	boolean removeElement(String ownKey, Object element, IEventBrokerBridge.Type eventType);

	/**
	 * Replaces the whole list and fires one event carrying both directions.
	 *
	 * @return false when nothing moved, or when one of the elements is something the
	 *         type will not read - the list is then left as it was
	 */
	boolean changeElements(String ownKey, Collection<?> newElements, IEventBrokerBridge.Type eventType);
}
