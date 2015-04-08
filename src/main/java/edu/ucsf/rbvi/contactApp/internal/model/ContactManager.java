package edu.ucsf.rbvi.contactApp.internal.model;

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
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;

import edu.ucsf.rbvi.contactApp.internal.ui.ContactPanel;

public class ContactManager {
	final CyServiceRegistrar serviceRegistrar;
	CyNetworkFactory networkFactory = null;
	CyNetworkManager networkManager = null;
	Map<Double, ContactNetwork> contactNetworkMap;
	File contactFile = null;
	ContactPanel contactPanel = null;

	// A couple of useful services that we want to cache
	CyApplicationManager cyAppManager = null;

	public ContactManager(CyServiceRegistrar registrar) {
		this.serviceRegistrar = registrar;
		contactNetworkMap = new HashMap<>();
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

	public void createNetworks(boolean register) {
		if (networkManager == null)
			networkManager = getService(CyNetworkManager.class);

		for (Double stress: contactNetworkMap.keySet()) {
			ContactNetwork cn = contactNetworkMap.get(stress);
			CyNetwork net = cn.getNetwork();
			System.out.println("Got network for Tstress: "+cn.getStress());
			System.out.println("...network has "+net.getNodeCount()+" nodes and "+net.getEdgeCount()+" edges");
			if (register && net != null)
				networkManager.addNetwork(net);
		}
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


}
