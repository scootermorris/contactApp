package edu.ucsf.rbvi.contactApp.internal.model;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.contactApp.internal.ui.ContactPanel;

public class ContactManager implements TaskObserver {
	public static String BANDS = "Ball and Stick";
	public static String SPHERE = "Spheres";

	final CyServiceRegistrar serviceRegistrar;
	CyNetworkFactory networkFactory = null;
	CyNetworkManager networkManager = null;
	SynchronousTaskManager taskManager = null;
	CyEventHelper eventHelper = null;
	CommandExecutorTaskFactory commandTaskFactory = null;
	Map<Double, ContactNetwork> contactNetworkMap;
	File contactFile = null;
	ContactPanel contactPanel = null;
	int modelNumber = -1;
	String modelName = null;

	// Settings
	String displayType = BANDS;
	boolean showPathway = true;

	// A couple of useful services that we want to cache
	CyApplicationManager cyAppManager = null;

	public ContactManager(CyServiceRegistrar registrar) {
		this.serviceRegistrar = registrar;
		contactNetworkMap = new HashMap<>();
	}

	public void reset() {
		contactNetworkMap.clear();
		contactFile = null;
		contactPanel = null;
	}

	public ContactNetwork getContactNetwork(double stress) {
		if (contactNetworkMap.containsKey(stress))
			return contactNetworkMap.get(stress);
		return null;
	}

	public Set<Double> getStressSet() {
		if (contactNetworkMap == null)
			return null;
		return contactNetworkMap.keySet();
	}

	public int getNetworkCount() {
		return contactNetworkMap.size();
	}

	public String getFileName() {
		if (contactFile == null) return null;
		return contactFile.getName();
	}

	public ContactPanel getResultsPanel() {
		return contactPanel;
	}

	public void setResultsPanel(ContactPanel panel) {
		contactPanel = panel;
	}

	public int loadContactNetwork(File contactFile) throws IOException, FileNotFoundException {
		this.contactFile = contactFile;

		if (networkFactory == null)
			networkFactory = getService(CyNetworkFactory.class);

		FileReader reader = new FileReader(contactFile);

		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

			// At the top level, we have the set of Tstress
			for (Object stress: jsonObject.keySet()) {
				if (!(stress instanceof String))
					continue;

				double tstress = Double.parseDouble(((String)stress).substring(7));
				ContactNetwork cn = new ContactNetwork(networkFactory ,tstress);
				contactNetworkMap.put(tstress, cn);

				JSONObject pathways = (JSONObject)jsonObject.get(stress);
				for (Object pathway: pathways.keySet()) {
					if (!(pathway instanceof String))
						continue;

					Pathway p = new Pathway((String)pathway);
					JSONArray residues = (JSONArray)pathways.get(pathway);
					for (Object residue: residues) {
						if (residue instanceof Number) {
							p.addResidue(((Number)residue).intValue());
						} else {
							System.out.println("Unknown residue identifier: "+residue);
						}
					}
					cn.addPathway(p);
				}
			}
		}
		catch (ParseException pe) {
			System.out.println("Unable to parse "+contactFile+": "+pe);
		}
		return contactNetworkMap.size();
	}

	public void loadPDBFile(File pdbFile) {
		getTaskServices();

		Map<String, Object> args = new HashMap<>();
		args.put("structureFile", pdbFile.getPath());
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "open", args, this);
		taskManager.execute(ti);

		// Now bring up the dialog
		args = new HashMap<>();
		ti = commandTaskFactory.createTaskIterator("structureViz", "showDialog", args, null);
		taskManager.execute(ti);
	}

	public void createRIN() {
		getTaskServices();

		// First, select the model
		Map<String, Object> args = new HashMap<>();
		args.put("command", "sel #"+modelNumber);
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "send", args, null);
		taskManager.execute(ti);

		try {
			// Wait for things to process
			Thread.sleep(500);
		} catch (Exception e) {}

		args = new HashMap<>();
		ti = commandTaskFactory.createTaskIterator("structureViz", "createRIN", args, null);
		taskManager.execute(ti);
	}

	public void hideSideChain() {
		getTaskServices();

		Map<String, Object> args = new HashMap<>();
		args.put("command", "~show sel");
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "send", args, null);
		taskManager.execute(ti);
	}

	public void showSideChain(List<Integer> residues) {
		getTaskServices();

		String residue = "#"+modelNumber+":";
		for (Integer residueID: residues)
			residue += ""+residueID+",";

		residue = residue.substring(0, residue.length()-1);

		Map<String, Object> args = new HashMap<>();
		String repr = "bs";
		if (displayType == SPHERE)
			repr = "sphere";

		// Show the side chain.  Note that we want to explicitly change the color since the 
		// single color that came from cytoscape may be confusing.  Color byelement helps.
		String command = "show "+residue+"; repr "+repr+" "+residue+"; color byelement "+residue;
		// System.out.println("Sending command: '"+command+"'");
		args.put("command", command);
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "send", args, null);
		taskManager.execute(ti);

	}

	public void syncColors() {
		getTaskServices();
		eventHelper.flushPayloadEvents();
		Map<String, Object> args = new HashMap<>();
		args.put("chimeraToCytoscape", "false");
		args.put("cytoscapeToChimera", "true");
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "syncColors", args, null);
		taskManager.execute(ti);
	}

	public void	closeChimeraStructure() {
		// Shouldn't need to do this
		getTaskServices();

		Map<String, Object> args = new HashMap<>();
		args.put("modelList", modelName);
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "close", args, null);
		taskManager.execute(ti);
		modelNumber = -1;
		modelName = null;
	}

	public void taskFinished(ObservableTask task) {
		String models = task.getResults(String.class);
		int offset = models.indexOf(' ');
		String model = models.substring(1, offset);
		modelName = new String(models.substring(offset+1, models.length()-1));

		try {
			modelNumber = Integer.parseInt(model);
		} catch (Exception e) {}
	}

	public void allFinished(FinishStatus finishStatus) {}

	public void createNetworks(boolean register) {
		if (networkManager == null)
			networkManager = getService(CyNetworkManager.class);

		for (Double stress: contactNetworkMap.keySet()) {
			ContactNetwork cn = contactNetworkMap.get(stress);
			CyNetwork net = cn.getNetwork();
			if (register && net != null)
				networkManager.addNetwork(net);
		}
	}

	// Settings
	public boolean getShowPathway() { return showPathway; }
	public void setShowPathway(boolean p) { showPathway = p; }
	public String getDisplayResidueType() { return displayType; }
	public void setDisplayResidueType(String type) { displayType = type; }

	public int getCurrentModel() {
		return modelNumber;
	}

	public CyNetwork getCurrentNetwork() {
		if (cyAppManager == null) {
			cyAppManager = getService(CyApplicationManager.class);
		}
		return cyAppManager.getCurrentNetwork();
	}

	public CyNetworkView getCurrentNetworkView() {
		if (cyAppManager == null) {
			cyAppManager = getService(CyApplicationManager.class);
		}
		return cyAppManager.getCurrentNetworkView();
	}

	public <S> S getService(Class<S> serviceClass) {
		return serviceRegistrar.getService(serviceClass);
	}

	public <S> S getService(Class<S> serviceClass, String filter) {
		return serviceRegistrar.getService(serviceClass, filter);
	}

	public void registerService(Object service, Class<?> serviceClass, Properties props) {
		serviceRegistrar.registerService(service, serviceClass, props);
	}

	public void unregisterService(Object service, Class<?> serviceClass) {
		serviceRegistrar.unregisterService(service, serviceClass);
	}

	private void getTaskServices() {
		if (taskManager == null) {
			taskManager = getService(SynchronousTaskManager.class);
		}
		if (commandTaskFactory == null) {
			commandTaskFactory = getService(CommandExecutorTaskFactory.class);
		}
		if (eventHelper == null) {
			eventHelper = getService(CyEventHelper.class);
		}
	}

	private String chimeraColor(Color color) {
		double r = (double)color.getRed()/255.0;
		double g = (double)color.getGreen()/255.0;
		double b = (double)color.getBlue()/255.0;
		double a = (double)color.getAlpha()/255.0;
		return ""+r+","+g+","+b+","+a;
	}


}
