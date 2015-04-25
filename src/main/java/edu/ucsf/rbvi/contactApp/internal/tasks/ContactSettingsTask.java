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

public class ContactSettingsTask extends AbstractTask {
	final ContactManager contactManager;

	@Tunable(description="Display Chimera residues as")
	public ListSingleSelection displayResidues = new ListSingleSelection(ContactManager.BANDS, ContactManager.SPHERE);

	@Tunable(description="Show pathway as edges in RIN")
	public boolean showPathway;

	public ContactSettingsTask(ContactManager manager) {
		super();
		this.contactManager = manager;
		displayResidues.setSelectedValue(contactManager.getDisplayResidueType());
		showPathway = contactManager.getShowPathway();
	}

	@ProvidesTitle
	public String getTitle() { return "contactApp Settings"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		contactManager.setShowPathway(showPathway);
		contactManager.setDisplayResidueType((String)displayResidues.getSelectedValue());
	}
}
