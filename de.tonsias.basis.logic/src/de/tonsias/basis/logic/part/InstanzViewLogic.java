package de.tonsias.basis.logic.part;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.e4.core.services.events.IEventBroker;

import de.tonsias.basis.logic.function.PentaConsumer;
import de.tonsias.basis.logic.function.QuadFunction;
import de.tonsias.basis.logic.function.TriFunction;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;

public class InstanzViewLogic {

	private final List<Job> _changeJobs = new LinkedList<Job>();

	private final JobGroup _jobGroup;

	public InstanzViewLogic() {
		_jobGroup = new JobGroup("InstanzViewLogic JobGroup", 1, 0);
	}

	public Job createTriFunctionJob(TriFunction<String, String, IEventBrokerBridge.Type, Boolean> serviceFunction, String valueKey,
			String newValue) {
		Job job = new Job("") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				serviceFunction.apply(valueKey, newValue, IEventBrokerBridge.Type.SEND);
				return Status.OK_STATUS;
			}
			
			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}
			
		};
		job.setJobGroup(_jobGroup);
		_changeJobs.add(job);
		return job;
	}
	public Job createBiFunctionJob(BiFunction<String, String, Boolean> serviceFunction, String valueKey,
			String newValue) {
		Job job = new Job("") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				serviceFunction.apply(valueKey, newValue);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}

		};
		job.setJobGroup(_jobGroup);
		_changeJobs.add(job);
		return job;
	}

	public Job createPentaConsumerJob(
			PentaConsumer<String, SingleValueType, String, String, IEventBrokerBridge.Type> serviceConsumer,
			String ownKey, SingleValueType type, String key, String text) {
		Job job = new Job("") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				serviceConsumer.accept(ownKey, type, key, text, IEventBrokerBridge.Type.SEND);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}

		};
		job.setJobGroup(_jobGroup);
		_changeJobs.add(job);
		return job;
	}

	public Job createBiAndQuadFunctionJob(BiFunction<ISingleValue<?>, IEventBrokerBridge.Type, Boolean> function, ISingleValue<?> data,
			QuadFunction<Collection<String>, SingleValueType, String, IEventBrokerBridge.Type, Boolean> quadFunction) {
		Job job = new Job("") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				function.apply(data, IEventBrokerBridge.Type.SEND);
				quadFunction.apply(data.getConnectedInstanzKeys(), SingleValueType.getByClass(data.getClass()).get(),
						data.getOwnKey(), IEventBrokerBridge.Type.SEND);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}
		};
		_changeJobs.add(job);
		return job;
	}

	public void executeChanges(int dialogReturn, IEventBrokerBridge broker, IInstanz shownInstanz) {
		switch (dialogReturn) {
		case 0:
			addConsumerOperation(broker, EventConstants.OPEN_OPERATION, _changeJobs::addFirst);
			addConsumerOperation(broker, EventConstants.CLOSE_OPERATION, _changeJobs::addLast);
			_changeJobs.forEach(j -> j.schedule());
			try {
				Job.getJobManager().join(this, new NullProgressMonitor());
			} catch (Exception e) {
				e.printStackTrace();
			}
			_changeJobs.clear();
			return;
		case 1:
			_changeJobs.clear();
			return;
		case 2:
			_changeJobs.clear();
			InstanzEvent data = new InstanzEventConstants.InstanzEvent(shownInstanz.getOwnKey(), null);
			broker.send(InstanzEventConstants.SELECTED, Map.of(IEventBroker.DATA, data));
			return;
		}
	}

	private void addConsumerOperation(IEventBrokerBridge broker, String topic, Consumer<Job> changeJobFunction) {
		Job job = new Job("") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				broker.post(topic, null);
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == InstanzViewLogic.this;
			}
		};
		job.setJobGroup(_jobGroup);
		changeJobFunction.accept(job);
	}

}
