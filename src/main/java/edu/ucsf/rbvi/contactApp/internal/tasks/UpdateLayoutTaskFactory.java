package edu.ucsf.rbvi.contactApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class UpdateLayoutTaskFactory extends AbstractNetworkTaskFactory {
	final ContactManager manager;

	public UpdateLayoutTaskFactory(ContactManager manager) {
		super();
		this.manager = manager;
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		// TODO Auto-generated method stub
		return new TaskIterator(new UpdateLayoutTask(manager));
	}

	public boolean isReady(CyNetwork network) {
		if (network == null) return false;
		return true;
	}

}
