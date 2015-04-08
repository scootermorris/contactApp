package edu.ucsf.rbvi.contactApp.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
// Commented out until 3.2 is released
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;
import edu.ucsf.rbvi.contactApp.internal.tasks.LoadContactNetworkTaskFactory;
import edu.ucsf.rbvi.contactApp.internal.tasks.CreateContactNetworksTaskFactory;
import edu.ucsf.rbvi.contactApp.internal.tasks.ShowContactNetworksPanelTaskFactory;


public class CyActivator extends AbstractCyActivator {
	private static Logger logger = LoggerFactory
			.getLogger(edu.ucsf.rbvi.contactApp.internal.CyActivator.class);

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		// See if we have a graphics console or not
		boolean haveGUI = true;
		CySwingApplication cySwingApplication = null;
		ServiceReference ref = bc.getServiceReference(CySwingApplication.class.getName());

		if (ref == null) {
			haveGUI = false;
			// Issue error and return
		} else {
			cySwingApplication = getService(bc, CySwingApplication.class);
		}

		try {
		CyServiceRegistrar registrar = getService(bc, CyServiceRegistrar.class);
		ContactManager contactManager = new ContactManager(registrar);

		{
			LoadContactNetworkTaskFactory loadNetwork = 
				new LoadContactNetworkTaskFactory(contactManager);
			Properties loadProps = new Properties();
			loadProps.setProperty(PREFERRED_MENU, "Apps.contactApp");
			loadProps.setProperty(TITLE, "Load contact network");
			loadProps.setProperty(MENU_GRAVITY, "1.0");
			registerService(bc, loadNetwork, TaskFactory.class, loadProps);
		}

		{
			ShowContactNetworksPanelTaskFactory showNetworksPanel = 
				new ShowContactNetworksPanelTaskFactory(contactManager, true);
			Properties showProps = new Properties();
			showProps.setProperty(PREFERRED_MENU, "Apps.contactApp");
			showProps.setProperty(TITLE, "Show contact network panel");
			showProps.setProperty(MENU_GRAVITY, "2.0");
			registerService(bc, showNetworksPanel, NetworkTaskFactory.class, showProps);
		}
		
		{
			ShowContactNetworksPanelTaskFactory hideNetworksPanel = 
				new ShowContactNetworksPanelTaskFactory(contactManager, false);
			Properties hideProps = new Properties();
			hideProps.setProperty(PREFERRED_MENU, "Apps.contactApp");
			hideProps.setProperty(TITLE, "Hide contact network panel");
			hideProps.setProperty(MENU_GRAVITY, "3.0");
			registerService(bc, hideNetworksPanel, NetworkTaskFactory.class, hideProps);
		}

		{
			CreateContactNetworksTaskFactory createNetworks = 
				new CreateContactNetworksTaskFactory(contactManager);
			Properties createProps = new Properties();
			createProps.setProperty(PREFERRED_MENU, "Apps.contactApp");
			createProps.setProperty(TITLE, "Create contact network");
			createProps.setProperty(MENU_GRAVITY, "10.0");
			registerService(bc, createNetworks, TaskFactory.class, createProps);
		}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
