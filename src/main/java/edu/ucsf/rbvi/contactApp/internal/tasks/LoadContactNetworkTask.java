package edu.ucsf.rbvi.contactApp.internal.tasks;

import java.io.File;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class LoadContactNetworkTask extends AbstractTask {
	final ContactManager contactManager;

	@Tunable(description="File containing contact network", params="input=true")
	public File contactFile;

	@Tunable(description="File containing the PDB structure", params="input=true")
	public File pdbFile = null;

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public LoadContactNetworkTask(ContactManager manager) {
		super();
		this.contactManager = manager;
	}

	@ProvidesTitle
	public String getTitle() { return "Load Contact Network"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Load Contact Network");

		if (pdbFile != null) {
			// Load the PDB structure
			monitor.setStatusMessage("Loading PDB structure into UCSF Chimera");
			contactManager.loadPDBFile(pdbFile);

			monitor.setStatusMessage("Creating RIN network in Cytoscape");
			contactManager.createRIN();
		}

		// Load the contact network
		int networks = contactManager.loadContactNetwork(contactFile);
		monitor.setStatusMessage("Loaded "+networks+" contact networks");
		insertTasksAfterCurrentTask(new ShowContactNetworksPanelTask(contactManager, false), 
		                            new ShowContactNetworksPanelTask(contactManager, true));
	}
}
