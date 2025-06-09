package de.tonsias.basis.logic.part;

import java.util.*;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;

public class InstanzViewLogic {

	private final Map<String, Job> _modifySvMap = new HashMap<>();

	private final Map<String, Job> _modifySvNameMap = new HashMap<>();

	private final Map<String, Job> _deleteSvMap = new HashMap<>();

	private final JobGroup _jobGroup;

	private ISingleValueService _svService;

	private IInstanzService _inService;

	public InstanzViewLogic(IInstanzService inService, ISingleValueService svService) {
		_inService = inService;
		_svService = svService;
		_jobGroup = new JobGroup("InstanzViewLogic JobGroup", 1, 0);
	}

	public void createModifySvJob(String valueKey, Object newValue) {
		if (_deleteSvMap.containsKey(valueKey)) {
			return;
		}

		Job job = new Job("Change SV-Value: "+ valueKey) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				_svService.changeValue(valueKey, newValue, Type.SEND);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}
		};
		job.setJobGroup(_jobGroup);
		_modifySvMap.put(valueKey, job);
	}

	public boolean isInDelete(ISingleValue<?> singleValue) {
		return _modifySvMap.containsKey(singleValue.getOwnKey());
	}

	public void createDeleteSvJob(ISingleValue<?> singleValue) {
		String ownKey = singleValue.getOwnKey();
		Job job = new Job("Delete SV: " + ownKey) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				_svService.removeValue(singleValue, Type.SEND);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}
		};
		job.setJobGroup(_jobGroup);
		_modifySvMap.remove(ownKey);
		_modifySvNameMap.remove(ownKey);
		_deleteSvMap.put(ownKey, job);

	}

	public void createSvNameModifyJob(String instanzKey, String newName, ISingleValue<?> sv) {
		if (_deleteSvMap.containsKey(sv.getOwnKey())) {
			return;
		}
		
		Job job = new Job("Change SV-Name: ") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Optional<SingleValueType> svType = SingleValueType.getByClass(sv.getClass());
				_inService.changeSingleValueName(instanzKey, svType.get(), sv.getOwnKey(), newName, Type.SEND);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}
		};
		job.setJobGroup(_jobGroup);
		_modifySvNameMap.put(sv.getOwnKey(), job);
	}

	public void executeChanges(int dialogReturn, IEventBrokerBridge broker, IInstanz shownInstanz) {
		switch (dialogReturn) {
		case 0:
			List<Job> changeJobs = new ArrayList<>();
			changeJobs.addAll(_modifySvMap.values());
			changeJobs.addAll(_modifySvNameMap.values());
			changeJobs.addAll(_deleteSvMap.values());
			
			
			addConsumerOperation(broker, EventConstants.OPEN_OPERATION, changeJobs::addFirst);
			addConsumerOperation(broker, EventConstants.CLOSE_OPERATION, changeJobs::addLast);
			
			changeJobs.forEach(j -> j.schedule());
			try {
				Job.getJobManager().join(this, new NullProgressMonitor());
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	

	private void addConsumerOperation(IEventBrokerBridge broker, String openOperation, Consumer<Job> consumer) {
		Job job = new Job(openOperation) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				broker.send(openOperation, null);
				return Status.OK_STATUS;
			}
		};
		job.setJobGroup(_jobGroup);
		consumer.accept(job);
	}

	private void clear() {
		_modifySvMap.clear();
		_modifySvNameMap.clear();
		_deleteSvMap.clear();
	}
}
