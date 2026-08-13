package de.tonsias.basis.logic.part;

import java.util.*;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.intf.non.service.ValueEventConstants.ValueEvent;

public class InstanzViewLogic {

	private final Map<String, Job> _modifyValueMap = new HashMap<>();

	private final Map<String, Job> _modifyValueNameMap = new HashMap<>();

	private final Map<String, Job> _deleteValueMap = new HashMap<>();

	private final JobGroup _jobGroup;

	private ISingleValueService _svService;

	private IMultiValueService _mvService;

	private IInstanzService _inService;

	public InstanzViewLogic(IInstanzService inService, ISingleValueService svService, IMultiValueService mvService) {
		_inService = inService;
		_svService = svService;
		_mvService = mvService;
		_jobGroup = new JobGroup("InstanzViewLogic JobGroup", 1, 0);
	}

	public void createModifyValueJob(String valueKey, Object newValue) {
		if (_deleteValueMap.containsKey(valueKey)) {
			return;
		}

		Job job = job("Change value: " + valueKey, () -> _svService.changeValue(valueKey, newValue, Type.SEND));
		_modifyValueMap.put(valueKey, job);
	}

	/**
	 * The list is handed over whole rather than as single elements: the dialog that
	 * edits one shows the whole list and hands back what it should read afterwards,
	 * and the service turns that into what came and what went.
	 */
	public void createModifyElementsJob(String valueKey, Collection<?> newElements) {
		if (_deleteValueMap.containsKey(valueKey)) {
			return;
		}

		Job job = job("Change elements: " + valueKey,
				() -> _mvService.changeElements(valueKey, newElements, Type.SEND));
		_modifyValueMap.put(valueKey, job);
	}

	public boolean isInDelete(IValue value) {
		return _deleteValueMap.containsKey(value.getOwnKey());
	}

	public void createDeleteValueJob(IValue value) {
		String ownKey = value.getOwnKey();
		Job job = job("Delete value: " + ownKey, () -> deleteValue(value));

		_modifyValueMap.remove(ownKey);
		_modifyValueNameMap.remove(ownKey);
		_deleteValueMap.put(ownKey, job);
	}

	private void deleteValue(IValue value) {
		if (value.getType().isMulti()) {
			_mvService.deleteValue((de.tonsias.basis.model.interfaces.IMultiValue<?>) value, Type.SEND);
			return;
		}
		_svService.deleteValue((de.tonsias.basis.model.interfaces.ISingleValue<?>) value, Type.SEND);
	}

	public void createValueNameModifyJob(String instanzKey, String newName, IValue value) {
		if (_deleteValueMap.containsKey(value.getOwnKey())) {
			return;
		}

		// the value says which type it is, so there is nothing to look up and nothing
		// that could answer empty
		Job job = job("Change value name: ",
				() -> _inService.changeValueName(instanzKey, value.getType(), value.getOwnKey(), newName, Type.SEND));
		_modifyValueNameMap.put(value.getOwnKey(), job);
	}

	private Job job(String name, Runnable work) {
		Job job = new Job(name) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				work.run();
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}
		};
		job.setJobGroup(_jobGroup);
		return job;
	}

	/**
	 *
	 * @param dialogReturn 0=apply, 1=cancel, 2=apply and reselect
	 * @param broker
	 * @param shownInstanz
	 */
	public void executeChanges(int dialogReturn, IEventBrokerBridge broker, IInstanz shownInstanz) {
		switch (dialogReturn) {
		case 0:
			List<Job> changeJobs = new ArrayList<>();
			changeJobs.addAll(_modifyValueMap.values());
			changeJobs.addAll(_modifyValueNameMap.values());
			changeJobs.addAll(_deleteValueMap.values());

			addConsumerOperation(broker, EventConstants.OPEN_OPERATION, changeJobs::addFirst);
			addConsumerOperation(broker, EventConstants.CLOSE_OPERATION, changeJobs::addLast);

			changeJobs.forEach(j -> j.schedule());
			clear();
			return;
		case 1:
			clear();
			return;
		case 2:
			clear();
			InstanzEvent data = new InstanzEventConstants.InstanzEvent(shownInstanz.getOwnKey(), null);
			broker.send(InstanzEventConstants.SELECTED, Map.of(IEventBroker.DATA, data));
			return;
		}
	}

	/**
	 * Whether a value delta concerns the instanz currently shown, whichever family
	 * it came from. As long as nothing is selected there is no key to compare
	 * against, so no delta concerns the view.
	 */
	public boolean affectsShownInstanz(IInstanz shownInstanz, ValueEvent event) {
		if (shownInstanz == null) {
			return false;
		}

		return resolve(event.getType(), event.getKey())//
				.map(value -> value.getConnectedInstanzKeys().contains(shownInstanz.getOwnKey()))//
				.orElse(false);
	}

	private Optional<? extends IValue> resolve(IValueType type, String key) {
		if (type == null) {
			return Optional.empty();
		}
		if (type instanceof MultiValueType multi) {
			return _mvService.resolveKey(multi.getPath(), key, multi.getClazz());
		}
		return _svService.resolveKey(type.getPath(), key, ((SingleValueType) type).getClazz());
	}

	private void addConsumerOperation(IEventBrokerBridge broker, String openOperation, Consumer<Job> consumer) {
		Job job = job(openOperation, () -> broker.send(openOperation, null));
		consumer.accept(job);
	}

	private void clear() {
		_modifyValueMap.clear();
		_modifyValueNameMap.clear();
		_deleteValueMap.clear();
	}
}
