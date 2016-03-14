package edu.ucsf.rbvi.contactApp.internal.tasks;

import java.io.File;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;
import edu.ucsf.rbvi.contactApp.internal.ui.ContactPanel;

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

		if (contactManager.getNetworkCount() > 0) {
			monitor.setStatusMessage("Clearing previous contact network load");

			// Close the open structure
			contactManager.closeChimeraStructure();

			// Hide the panel
			ContactPanel contactPanel = contactManager.getResultsPanel();
			if (contactPanel != null)
				contactManager.unregisterService(contactPanel, CytoPanelComponent.class);

			// Delete the network
			contactManager.getService(CyNetworkManager.class).destroyNetwork(contactManager.getCurrentNetwork());
			
			// Clear the manager
			contactManager.reset();
		}

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

		SynchronousTaskManager tm = contactManager.getService(SynchronousTaskManager.class);

		// Unregister the results panel
		TaskIterator ti = new TaskIterator(new ShowContactNetworksPanelTask(contactManager, false));
		tm.execute(ti);

		// If we have a network view (why don't we *always* have a network view?), show the networks panel
		if (contactManager.getCurrentNetworkView() != null) {
			// Show the results panel
			ti = new TaskIterator(new ShowContactNetworksPanelTask(contactManager, true));
			tm.execute(ti);

			contactManager.getResultsPanel().updateData();
		}
	}
}
