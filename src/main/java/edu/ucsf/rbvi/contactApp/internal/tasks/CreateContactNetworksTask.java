package edu.ucsf.rbvi.contactApp.internal.tasks;

import java.io.File;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class CreateContactNetworksTask extends AbstractTask {
	final ContactManager contactManager;

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public CreateContactNetworksTask(ContactManager manager) {
		super();
		this.contactManager = manager;
	}

	@ProvidesTitle
	public String getTitle() { return "Create Contact Networks"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Create Contact Networks");

		// Load the contact network
		contactManager.createNetworks(true);
	}
}
