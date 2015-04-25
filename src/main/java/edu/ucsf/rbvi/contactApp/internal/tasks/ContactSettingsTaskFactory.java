package edu.ucsf.rbvi.contactApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class ContactSettingsTaskFactory extends AbstractTaskFactory {
	final ContactManager manager;

	public ContactSettingsTaskFactory(ContactManager manager) {
		super();
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ContactSettingsTask(manager));
	}

	public boolean isReady() {
		return true;
	}

}
