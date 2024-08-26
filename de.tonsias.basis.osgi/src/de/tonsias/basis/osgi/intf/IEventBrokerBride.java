package de.tonsias.basis.osgi.intf;

import java.util.Dictionary;
import java.util.Map;

public interface IEventBrokerBride {

	/**
	 * Publish event synchronously (the method does not return until the event is
	 * processed).
	 * <p>
	 * If data is a {@link Map} or a {@link Dictionary}, it is passed as is.
	 * Otherwise, a new Map is constructed and its {@link #DATA} attribute is
	 * populated with this value.
	 * </p>
	 *
	 * @param topic topic of the event to be published
	 * @param data  data to be published with the event
	 * @return <code>true</code> if this operation was performed successfully;
	 *         <code>false</code> otherwise
	 */
	boolean send(String topic, Object data);

	/**
	 * Publish event asynchronously (this method returns immediately).
	 * <p>
	 * If data is a {@link Map} or a {@link Dictionary}, it is passed as is.
	 * Otherwise, a new Map is constructed and its {@link #DATA} attribute is
	 * populated with this value.
	 * </p>
	 *
	 * @param topic topic of the event to be published
	 * @param data  data to be published with the event
	 * @return <code>true</code> if this operation was performed successfully;
	 *         <code>false</code> otherwise
	 */
	boolean post(String topic, Object data);
}
