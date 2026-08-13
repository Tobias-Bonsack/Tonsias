package de.tonsias.basis.osgi.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.impl.value.MultiBooleanValue;
import de.tonsias.basis.model.impl.value.MultiFloatValue;
import de.tonsias.basis.model.impl.value.MultiInstanzValue;
import de.tonsias.basis.model.impl.value.MultiIntegerValue;
import de.tonsias.basis.model.impl.value.MultiStringValue;
import de.tonsias.basis.model.interfaces.IMultiValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.ElementsChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.LinkedInstanzChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueNewEvent;
import de.tonsias.basis.osgi.intf.non.service.ChangeType;

/**
 * The mirror of {@link SingleValueServiceImpl} for the lists: which class a type
 * builds, what "the value changed" means for a list, and which event records this
 * family fires. Everything else is {@link AValueServiceImpl}'s.
 */
@Component
public class MultiValueServiceImpl extends AValueServiceImpl<IMultiValue<?>, MultiValueType>
		implements IMultiValueService {

	@Reference
	SaveService _saveService;

	@Reference
	LoadService _loadService;

	@Reference
	DeleteService _deleteService;

	@Reference
	IKeyService _keyService;

	@Reference
	IEventBrokerBridge _broker;

	@Override
	protected SaveService saveService() {
		return _saveService;
	}

	@Override
	protected LoadService loadService() {
		return _loadService;
	}

	@Override
	protected DeleteService deleteService() {
		return _deleteService;
	}

	@Override
	protected IKeyService keyService() {
		return _keyService;
	}

	@Override
	protected IEventBrokerBridge broker() {
		return _broker;
	}

	@Override
	protected MultiValueType[] types() {
		return MultiValueType.values();
	}

	@Override
	protected MultiValueType typeOf(IMultiValue<?> value) {
		return value.getType();
	}

	@Override
	protected IMultiValue<?> newInstance(MultiValueType type, String key) {
		return switch (type) {
		case MULTI_STRING -> new MultiStringValue(key);
		case MULTI_INTEGER -> new MultiIntegerValue(key);
		case MULTI_BOOLEAN -> new MultiBooleanValue(key);
		case MULTI_FLOAT -> new MultiFloatValue(key);
		case MULTI_INSTANZ -> new MultiInstanzValue(key);
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	@Override
	protected void fireDelete(MultiValueType type, IMultiValue<?> value, Collection<String> ownerKeys,
			Type eventType) {
		// the elements travel with the event: by the time a listener runs, the value
		// may be out of the cache and its file already scheduled for deletion, and the
		// targets of a relation still have to learn that nothing points at them anymore
		fireEvent(eventType, MultiValueEventConstants.DELETE,
				new MultiValueDeleteEvent(type, value.getOwnKey(), ownerKeys, List.copyOf(value.getValues())));
	}

	@Override
	protected void fireInstanzListChange(MultiValueType type, String valueKey, ChangeType change,
			Collection<String> instanzKeys, Type eventType) {
		fireEvent(eventType, MultiValueEventConstants.INSTANZ_LIST_CHANGE,
				new LinkedInstanzChangeEvent(valueKey, type, change, instanzKeys));
	}

	@SuppressWarnings("unchecked") // the type was looked up from clazz, so create builds exactly that class
	@Override
	public <E extends IMultiValue<?>> E createNew(Class<E> clazz, String parentKey, String parameterName,
			Collection<?> elements, IEventBrokerBridge.Type eventType) {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(parentKey);
		Objects.requireNonNull(parameterName);
		Objects.requireNonNull(elements);

		Optional<MultiValueType> type = MultiValueType.getByClass(clazz);
		if (type.isEmpty()) {
			return null;
		}

		IMultiValue<?> multiValue = create(type.get());
		multiValue.addConnectedInstanzKey(parentKey);
		multiValue.tryToSetValues(elements);
		cache(multiValue);

		var data = new MultiValueNewEvent(type.get(), multiValue.getOwnKey(), parameterName, List.of(parentKey),
				List.copyOf(multiValue.getValues()));
		fireEvent(eventType, MultiValueEventConstants.NEW, data);

		return (E) multiValue;
	}

	@Override
	public boolean addElement(String ownKey, Object element, Type eventType) {
		return mutate(ownKey, value -> value.tryToAddValue(element), eventType);
	}

	@Override
	public boolean removeElement(String ownKey, Object element, Type eventType) {
		return mutate(ownKey, value -> value.tryToRemoveValue(element), eventType);
	}

	@Override
	public boolean changeElements(String ownKey, Collection<?> newElements, Type eventType) {
		return mutate(ownKey, value -> value.tryToSetValues(newElements), eventType);
	}

	/**
	 * The one way a list is changed, so the event always says what actually
	 * happened. The elements are diffed off the list rather than taken from the
	 * caller: what goes in is text as often as not, and an event carrying "42" for a
	 * list that now holds {@code 42} would send a listener looking for something
	 * that is not there.
	 * <p>
	 * A change that changes nothing answers {@code false} here and fires nothing at
	 * all, which is what ends a propagation chain.
	 * </p>
	 */
	private boolean mutate(String ownKey, Predicate<IMultiValue<?>> change, Type eventType) {
		// the same reason changeValue resolves rather than reading the cache, see
		// https://github.com/Tobias-Bonsack/Tonsias/issues/78
		Optional<IMultiValue<?>> resolved = resolveAnyKey(ownKey);
		if (resolved.isEmpty()) {
			return false;
		}

		IMultiValue<?> value = resolved.get();
		List<?> before = List.copyOf(value.getValues());
		if (!change.test(value)) {
			return false;
		}

		List<?> after = List.copyOf(value.getValues());
		List<?> added = after.stream().filter(element -> !before.contains(element)).toList();
		List<?> removed = before.stream().filter(element -> !after.contains(element)).toList();

		fireEvent(eventType, MultiValueEventConstants.VALUES_CHANGE,
				new ElementsChangeEvent(ownKey, value.getType(), added, removed));
		return true;
	}
}
