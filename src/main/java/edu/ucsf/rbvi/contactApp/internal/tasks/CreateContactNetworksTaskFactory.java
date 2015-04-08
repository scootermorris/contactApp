package edu.ucsf.rbvi.contactApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class CreateContactNetworksTaskFactory extends AbstractTaskFactory {
	final ContactManager manager;

	public CreateContactNetworksTaskFactory(ContactManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new CreateContactNetworksTask(manager));
	}

	public boolean isReady() {
		if (manager.getNetworkCount() > 0)
			return true;
		return false;
	}

}
