package de.tonsias.basis.osgi.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.prefs.BackingStoreException;

import de.tonsias.basis.osgi.intf.IBasicPreferenceService;

@Component
public class BasicPreferenceServiceImpl implements IBasicPreferenceService {

	@Override
	public <T> List<T> getAsList(String key, Class<T> type) {
		String value = getNode().get(key, "");

		List<T> result = Arrays.stream(value.split(",")).map(type::cast).collect(Collectors.toList());
		return result;
	}

	@Override
	public void saveAsList(String key, List<?> list) throws BackingStoreException {
		String value = list.stream().map(i -> i.toString()).collect(Collectors.joining(","));
		IEclipsePreferences node = getNode();
		node.put(key, value);
		node.flush();
	}

	@Override
	public <T> Optional<T> getValue(String key, Class<T> type) {
		String value = getNode().get(key, null);
		return value == null ? Optional.empty() : Optional.of(type.cast(value));

	}

	@Override
	public void saveAsToString(String key, Object toSave) throws BackingStoreException {
		IEclipsePreferences node = getNode();
		node.put(key, toSave.toString());
		node.flush();
	}

	protected IEclipsePreferences getNode() {
		return InstanceScope.INSTANCE.getNode("BasicPreference");
	}
}
