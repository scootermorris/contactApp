package edu.ucsf.rbvi.contactApp.internal.tasks;

import java.util.Properties;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;
import edu.ucsf.rbvi.contactApp.internal.ui.ContactPanel;

public class ShowContactNetworksPanelTask extends AbstractTask {
	final ContactManager contactManager;
	final boolean showHide;

	/*
	@Tunable(description="File containing PDB data")
	public File pdbFile;
	*/

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public ShowContactNetworksPanelTask(ContactManager manager, boolean showHide) {
		super();
		this.contactManager = manager;
		this.showHide = showHide;
	}

	@ProvidesTitle
	public String getTitle() { return "Show Contact Network Panel"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		CySwingApplication swingApplication = contactManager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
		if (showHide) {
			monitor.setTitle("Showing contact network panel");
			// If we don't have a network view, yet.  Just bail.
			if (contactManager.getCurrentNetworkView() == null)
				return;
			ContactPanel contactPanel = new ContactPanel(contactManager, null);
			contactManager.registerService(contactPanel, CytoPanelComponent.class, new Properties());
			contactManager.setResultsPanel(contactPanel);
			if (cytoPanel.getState() == CytoPanelState.HIDE)
				cytoPanel.setState(CytoPanelState.DOCK);
		} else {
			monitor.setTitle("Hiding contact network panel");
			ContactPanel contactPanel = contactManager.getResultsPanel();
			if (contactPanel != null)
				contactManager.unregisterService(contactPanel, CytoPanelComponent.class);
		}
	}
}
