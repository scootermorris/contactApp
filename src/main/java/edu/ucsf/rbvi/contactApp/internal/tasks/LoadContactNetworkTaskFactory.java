package edu.ucsf.rbvi.contactApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class LoadContactNetworkTaskFactory extends AbstractTaskFactory {
	final ContactManager manager;

	public LoadContactNetworkTaskFactory(ContactManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return new TaskIterator(new LoadContactNetworkTask(manager));
	}

}
