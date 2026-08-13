package de.tonsias.basis.osgi.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.*;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.ElementsChangeEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueDeleteEvent;
import de.tonsias.basis.osgi.intf.non.service.MultiValueEventConstants.MultiValueNewEvent;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants;
import de.tonsias.basis.osgi.intf.non.service.SingleValueEventConstants.*;
import jakarta.inject.Inject;

/**
 * contains all emerging model-changed events, in order to have a bracket, the
 * operation is still listened to.
 * 
 * 11/23/2024: operations are not summarized, that doesn't seem to be relevant
 * to me at the moment, since even with undo/redo you can find out the brackets
 * here
 */
public class DeltaServiceImpl implements IDeltaService {

	@Inject
	protected IEventBrokerBridge _eventBridge;

	@Inject
	protected IInstanzService _instanzService;

	@Inject
	protected ISingleValueService _singleValueService;

	@Inject
	protected IMultiValueService _multiValueService;

	protected Collection<Event> _notSavedEvents = new LinkedList<Event>();

	public void postConstruct() {
		_notSavedEvents.add(START_EVENT);

		_eventBridge.subscribe(InstanzEventConstants.ALL_DELTA_TOPIC, this, true);
		_eventBridge.subscribe(SingleValueEventConstants.ALL_DELTA_TOPIC, this, true);
		_eventBridge.subscribe(MultiValueEventConstants.ALL_DELTA_TOPIC, this, true);
		_eventBridge.subscribe(EventConstants.OPEN_OPERATION, this, true);
		_eventBridge.subscribe(EventConstants.CLOSE_OPERATION, this, true);
		_eventBridge.subscribe(EventConstants.SAVE_ALL, event -> saveDeltas(), true);
	}

	@Override
	public void handleEvent(Event event) {
		_notSavedEvents.add(event);
	}

	@Override
	public void saveDeltas() {
		Set<String> instanzKeysToSave = new HashSet<String>();
		Set<String> singlevalueKeysToSave = new HashSet<String>();
		Set<String> multivalueKeysToSave = new HashSet<String>();
		Set<String> instanzKeysToDelete = new HashSet<String>();
		Set<String> singlevalueKeysToDelete = new HashSet<String>();
		Set<String> multivalueKeysToDelete = new HashSet<String>();

		for (Event event : _notSavedEvents) {
			if (InstanzEventConstants.KNOWN_DELTA.contains(event.getTopic())) {
				handleInstanzEvents(event, instanzKeysToSave, instanzKeysToDelete);
			}
			if (SingleValueEventConstants.KNOWN_DELTA.contains(event.getTopic())) {
				handleSingleValueEvents(event, singlevalueKeysToSave, singlevalueKeysToDelete);
			}
			if (MultiValueEventConstants.KNOWN_DELTA.contains(event.getTopic())) {
				handleMultiValueEvents(event, multivalueKeysToSave, multivalueKeysToDelete);
			}
		}

		// a value that is created and dropped again within one log is in both sets:
		// it is written first and removed afterwards, so nothing is left behind
		CompletionException failures = new CompletionException("saving the deltas failed", null);
		try {
			attempt(failures, () -> _instanzService.saveAll(instanzKeysToSave));
			attempt(failures, () -> _instanzService.deleteAll(instanzKeysToDelete));
			attempt(failures, () -> _singleValueService.saveAll(singlevalueKeysToSave));
			attempt(failures, () -> _singleValueService.deleteAll(singlevalueKeysToDelete));
			attempt(failures, () -> _multiValueService.saveAll(multivalueKeysToSave));
			attempt(failures, () -> _multiValueService.deleteAll(multivalueKeysToDelete));
		} finally {
			// The log is the difference against the disk, and every one of the six steps
			// was given its chance - so it is spent, whatever came of it. Keeping it would
			// fold the same set again on the next save, fail on the same step again, and
			// from then on nothing would ever be written, see
			// https://github.com/Tobias-Bonsack/Tonsias/issues/53
			_notSavedEvents.clear();
			_notSavedEvents.add(START_EVENT);
		}

		if (failures.getSuppressed().length > 0) {
			throw failures;
		}
	}

	/**
	 * Runs one step of the save and keeps what it threw instead of letting it end
	 * the save - the other five steps have files of their own to write, and they are
	 * not lost because one of them could not.
	 */
	private void attempt(CompletionException failures, Runnable step) {
		try {
			step.run();
		} catch (RuntimeException e) {
			failures.addSuppressed(e);
		}
	}

	private void handleSingleValueEvents(Event event, Set<String> singlevalueKeysToSave,
			Set<String> singlevalueKeysToDelete) {
		switch (event.getTopic()) {
		case SingleValueEventConstants.NEW:
			var value = SingleValueNewEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToSave.add(value._key());
			break;
		case SingleValueEventConstants.VALUE_CHANGE:
			var value2 = ValueChangeEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToSave.add(value2._key());
			break;
		case SingleValueEventConstants.INSTANZ_LIST_CHANGE:
			var value3 = LinkedInstanzChangeEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToSave.add(value3._key());
			break;
		case SingleValueEventConstants.DELETE:
			var value4 = SingleValueDeleteEvent.class.cast(event.getProperty(IEventBroker.DATA));
			singlevalueKeysToDelete.add(value4._key());
			break;
		default:
			throw new IllegalArgumentException("Enum value unknown: " + event.getTopic());
		}
	}

	/**
	 * The four cases have to match {@link MultiValueEventConstants#KNOWN_DELTA}
	 * exactly: a topic in that list without a branch here throws, and the log is
	 * cleared in the {@code finally} above - so the whole batch would be gone
	 * without a trace.
	 */
	private void handleMultiValueEvents(Event event, Set<String> multivalueKeysToSave,
			Set<String> multivalueKeysToDelete) {
		Object property = event.getProperty(IEventBroker.DATA);
		switch (event.getTopic()) {
		case MultiValueEventConstants.NEW:
			multivalueKeysToSave.add(MultiValueNewEvent.class.cast(property)._key());
			break;
		case MultiValueEventConstants.VALUES_CHANGE:
			// an event with nothing added and nothing removed is a reorder, and the order
			// is part of the value - the file still has to be rewritten
			multivalueKeysToSave.add(ElementsChangeEvent.class.cast(property)._key());
			break;
		case MultiValueEventConstants.INSTANZ_LIST_CHANGE:
			multivalueKeysToSave.add(MultiValueEventConstants.LinkedInstanzChangeEvent.class.cast(property)._key());
			break;
		case MultiValueEventConstants.DELETE:
			multivalueKeysToDelete.add(MultiValueDeleteEvent.class.cast(property)._key());
			break;
		default:
			throw new IllegalArgumentException("Enum value unknown: " + event.getTopic());
		}
	}

	private void handleInstanzEvents(Event event, Set<String> instanzKeysToSave, Set<String> instanzKeysToDelete) {
		Object property = event.getProperty(IEventBroker.DATA);
		switch (event.getTopic()) {
		case InstanzEventConstants.NEW:
			var value = InstanzEvent.class.cast(property);
			instanzKeysToSave.add(value._key());
			break;
		case InstanzEventConstants.PARENT_CHANGE:
			var value6 = ParentChange.class.cast(property);
			instanzKeysToSave.add(value6._key());
			break;
		case InstanzEventConstants.CHILD_LIST_CHANGE:
			var value5 = LinkedChildChangeEvent.class.cast(property);
			instanzKeysToSave.add(value5._key());
			break;
		case InstanzEventConstants.NAME_CHANGE:
			var value2 = ValueRenameEvent.class.cast(property);
			instanzKeysToSave.add(value2._key());
			break;
		case InstanzEventConstants.VALUE_LIST_CHANGE:
			var value3 = LinkedValueChangeEvent.class.cast(property);
			instanzKeysToSave.add(value3._key());
			break;
		case InstanzEventConstants.REFERENCE_LIST_CHANGE:
			// the set of values pointing at an instanz is stored on the instanz, so the
			// target is what has to be written - the value itself did not change here
			var value7 = LinkedReferenceChangeEvent.class.cast(property);
			instanzKeysToSave.add(value7._key());
			break;
		case InstanzEventConstants.DELETE:
			var value4 = InstanzEvent.class.cast(property);
			instanzKeysToDelete.add(value4._key());
			_instanzService.resolveKey(value4._key()).ifPresent(i -> instanzKeysToSave.add(i.getParentKey()));
			break;
		default:
			throw new IllegalArgumentException("Enum value unknown: " + event.getTopic());
		}
	}

	@Override
	public Collection<Event> getDeltas() {
		return _notSavedEvents;
	}
}
