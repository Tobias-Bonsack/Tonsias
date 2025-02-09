package de.tonsias.delta.view.ui.toolbar;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.delta.view.ui.DeltaView;
import jakarta.inject.Inject;

public class SaveAllHandler {

	@Inject
	IDeltaService _delta;

	@Execute
	@SuppressWarnings("unused")
	public void execute(MPart part) {
		JobGroup jG = new JobGroup("DeltaView_SaveAllHandler", 1, 0);

		Job job = Job.create("Save all in files", (IJobFunction) e -> {
			_delta.saveDeltas();
			return Status.OK_STATUS;
		});
		job.setJobGroup(jG);

		Job job1 = Job.create("Refresh DeltaView", (IJobFunction) e -> {
			if (part.getObject() instanceof DeltaView view) {
				view.updateTree();
			}
			return Status.OK_STATUS;
		});
		job1.setJobGroup(jG);

		job.schedule();
		job1.schedule();
	}
}
