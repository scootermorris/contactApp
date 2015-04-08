package edu.ucsf.rbvi.contactApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class ShowContactNetworksPanelTaskFactory extends AbstractNetworkTaskFactory {
	final ContactManager manager;
	final boolean showHide;

	public ShowContactNetworksPanelTaskFactory(ContactManager manager, boolean show) {
		super();
		this.manager = manager;
		this.showHide = show;
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		// TODO Auto-generated method stub
		return new TaskIterator(new ShowContactNetworksPanelTask(manager, showHide));
	}

	public boolean isReady(CyNetwork network) {
		if (network == null) return false;
		if (manager.getNetworkCount() == 0) return false;
		if (showHide && manager.getResultsPanel() == null)
			return true;
		else if (!showHide && manager.getResultsPanel() != null)
			return true;
		return false;
	}

}
