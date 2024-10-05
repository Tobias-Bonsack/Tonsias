package de.tonsias.basis.osgi.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBride;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.PreferenceEventConstants;

@Component
public class BasicPreferenceServiceImpl implements IBasicPreferenceService {

	@Reference
	IEventBrokerBride _broker;

	@Override
	public <T> List<T> getAsList(String key, Class<T> type) {
		String value = getNode().get(key, "");
		String[] split = value.split(REGEX);
		List<T> result = Arrays.stream(split).map(i -> castToType(i, type)).collect(Collectors.toList());
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> T castToType(String i, Class<T> type) {
		if (type == String.class) {
			return (T) i;
		} else if (type == Integer.class) {
			return (T) Integer.valueOf(i);
		} else if (type == Boolean.class) {
			return (T) Boolean.valueOf(i);
		}
		throw new UnsupportedOperationException("Class " + type.toGenericString() + " not supported");
	}

	@Override
	public void saveAsList(String key, List<?> list) throws BackingStoreException {
		String oldValue = getAsList(key, String.class).stream().map(i -> i.toString())
				.collect(Collectors.joining(REGEX));
		String value = list.stream().map(i -> i.toString()).collect(Collectors.joining(REGEX));

		if (oldValue.equals(value)) {
			return;
		}

		IEclipsePreferences node = getNode();

		node.put(key, value);
		node.flush();

		Object eventObject = getEventObject(oldValue, value.isBlank() ? null : list);
		_broker.post(PreferenceEventConstants.getForKey(key), eventObject);
	}

	@Override
	public <T> Optional<T> getValue(String key, Class<T> type) {
		String value = getNode().get(key, null);
		return value == null ? Optional.empty() : Optional.of(castToType(value, type));

	}

	@Override
	public void saveAsToString(String key, Object toSave) throws BackingStoreException {
		IEclipsePreferences node = getNode();
		Optional<String> oldValue = getValue(key, String.class);
		if (oldValue.orElse("").equals(toSave.toString())) {
			return;
		}

		node.put(key, toSave.toString());
		node.flush();

		Object eventObject = getEventObject(oldValue.orElse(""), toSave);
		_broker.post(PreferenceEventConstants.getForKey(key), eventObject);
	}

	@Override
	public IEclipsePreferences getNode() {
		return InstanceScope.INSTANCE.getNode("BasicPreference");
	}

	private Object getEventObject(String oldValue, Object value) {
		Map<String, Object> hashMap = new HashMap<>();
		if (oldValue.isBlank()) {
			hashMap.put(EventConstants.EVENT_TYPE, EventConstants.EventType.NEW);
		} else if (value == null) {
			hashMap.put(EventConstants.EVENT_TYPE, EventConstants.EventType.DELETE);
		} else {
			hashMap.put(EventConstants.EVENT_TYPE, EventConstants.EventType.CHANGE);
		}
		hashMap.put(EventConstants.OLD_VALUE, oldValue);
		hashMap.put(EventConstants.NEW_VAlUE, value);
		return hashMap;
	}
}
