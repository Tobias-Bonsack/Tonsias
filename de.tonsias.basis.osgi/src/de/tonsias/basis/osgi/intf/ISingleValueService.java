package de.tonsias.basis.osgi.intf;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;

/**
 * The values holding one content. Everything that is not about "one content"
 * lives in {@link IValueService}, where {@link IMultiValueService} finds it too.
 */
public interface ISingleValueService extends IValueService<ISingleValue<?>, SingleValueType> {

	/**
	 * Creates a new {@link ISingleValue}, but it will not be saved and the parent
	 * will not be informed
	 *
	 * @param <E>           created class
	 * @param clazz         identification
	 * @param parentKey     Key of the parent {@link IInstanz}
	 * @param parameterName name of the parameter that holds the {@link IInstanz}
	 * @param value         of the new created {@link ISingleValue}
	 * @return new instance of {@link ISingleValue} or null
	 */
	<E extends ISingleValue<?>> E createNew(Class<E> clazz, String parentKey, String parameterName, Object value,
			IEventBrokerBridge.Type eventType);

	/**
	 * Try to change the value of a single value
	 *
	 * @param ownKey   Key of the single value
	 * @param newValue Possible new value
	 * @return boolean if the change was successful or not
	 */
	boolean changeValue(String ownKey, Object newValue, IEventBrokerBridge.Type eventType);
}
