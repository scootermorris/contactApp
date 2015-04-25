package edu.ucsf.rbvi.contactApp.internal.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class UpdateLayoutTask extends AbstractTask implements TaskObserver {
	final ContactManager contactManager;

	/*
	@Tunable(description="File containing PDB data")
	public File pdbFile;
	*/

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public UpdateLayoutTask(ContactManager manager) {
		super();
		this.contactManager = manager;
	}

	@ProvidesTitle
	public String getTitle() { return "Update RIN Layout"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Updating layout");
		CommandExecutorTaskFactory commandTaskFactory = contactManager.getService(CommandExecutorTaskFactory.class);
		SynchronousTaskManager taskManager = contactManager.getService(SynchronousTaskManager.class);

		// Update annotations
		monitor.setStatusMessage("Updating coordinates");
		Map<String, Object> args = new HashMap<>();
		args.put("residueAttributes", "Coordinates");
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "annotateRIN", args, this);
		taskManager.execute(ti);

		// Update layout
		monitor.setStatusMessage("Performing layout");
		args = new HashMap<>();
		ti = commandTaskFactory.createTaskIterator("layout", "rin-layout", args, this);
		taskManager.execute(ti);

	}

	@Override
	public void allFinished(FinishStatus status) {
	}

	@Override
	public void taskFinished(ObservableTask task) {
	}
}
