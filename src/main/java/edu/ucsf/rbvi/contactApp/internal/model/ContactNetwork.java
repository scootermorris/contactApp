package edu.ucsf.rbvi.contactApp.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyEdge.Type;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

public class ContactNetwork {
	private final double stress;
	private final List<Pathway> pathways;
	private final CyNetworkFactory networkFactory;
	private CyNetwork network = null;

	public ContactNetwork (CyNetworkFactory networkFactory, double stress) {
		pathways = new ArrayList<Pathway>();
		this.stress = stress;
		this.networkFactory = networkFactory;
	}

	public List<Pathway> getPathways() {
		return pathways;
	}

	public double getStress() {
		return stress;
	}

	public void addPathway(Pathway p) {
		pathways.add(p);
	}

	public CyNetwork getNetwork() {
		if (network == null)
			createNetwork();
		return network;
	}

	public List<CyNetwork> getNetworkComponents() {
		if (network == null)
			createNetwork();

		List<List<CyNode>> connectedComponents = getConnectedComponents(network);

		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
		List<CyNetwork> componentNetworks = new ArrayList<CyNetwork>();

		// Create the networks
		for (List<CyNode> component: connectedComponents) {
			Set<CyEdge> edgeSet = new HashSet<CyEdge>();
			for (CyNode node: component) {
				edgeSet.addAll(network.getAdjacentEdgeList(node, CyEdge.Type.ANY));
			}
			CySubNetwork subNetwork = rootNetwork.addSubNetwork(component, edgeSet, SavePolicy.DO_NOT_SAVE);
			componentNetworks.add(subNetwork);
		}
		return componentNetworks;
	}

	private void createNetwork() {
		Map<Integer, CyNode> nodeMap = new HashMap<Integer, CyNode>();
		Map<CyEdge, Integer> edgeMap = new HashMap<CyEdge, Integer>();
		network = networkFactory.createNetwork(SavePolicy.DO_NOT_SAVE);
		network.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS).createColumn("Count", Integer.class, false);
		network.getTable(CyNode.class, CyNetwork.DEFAULT_ATTRS).createColumn("ResidueNumber", Integer.class, false);
		network.getRow(network).set("Name", "Tstress: "+stress);

		for (Pathway pathway: pathways) {
			int lastResidue = -1;
			for (Integer residue: pathway.getResidues()) {
				if (!nodeMap.containsKey(residue)) {
					nodeMap.put(residue, network.addNode());
				}

				// While we're here, name the node
				CyNode target = nodeMap.get(residue);
				network.getRow(target).set("Name", residue.toString());
				network.getRow(target).set("ResidueNumber", residue);

				if (lastResidue < 0) {
					lastResidue = residue;
					continue;
				}
				CyNode source = nodeMap.get(lastResidue);

				CyEdge edge;
				if (network.containsEdge(source, target)) {
					// There should only be one edge
					edge = network.getConnectingEdgeList(source, target, CyEdge.Type.UNDIRECTED).get(0);
					edgeMap.put(edge, edgeMap.get(edge)+1);
				} else {
					edge = network.addEdge(source, target, false);
					network.getRow(edge).set("Name", Integer.toString(lastResidue)+"-"+residue.toString());
					edgeMap.put(edge, 1);
				}
			}
		}

		for (CyEdge edge: edgeMap.keySet()) {
			// Finally, update the count column
			network.getRow(edge).set("Count", edgeMap.get(edge));
		}
	}

	private List<List<CyNode>> getConnectedComponents(CyNetwork network) {
		Set<CyNode> nodeSeenSet = new HashSet<CyNode>();
		List<List<CyNode>> components = new ArrayList<>();

		List<CyNode> nodeList = network.getNodeList();
		int initialSize = nodeSeenSet.size();

		for (CyNode node: nodeList) {
			if (nodeSeenSet.contains(node)) continue;

			nodeSeenSet.add(node);

			List<CyNode> neighbors = network.getNeighborList(node, CyEdge.Type.ANY);
			if (neighbors == null || neighbors.size() == 0) {
				// Single node component
				components.add(Collections.singletonList(node));
				continue;
			}

			List<CyNode> component = getConnectedComponent(network, nodeSeenSet, neighbors);
			components.add(component);
		}
		return components;
	}

	List<CyNode> getConnectedComponent(CyNetwork network, Set<CyNode> nodeSeenSet, 
	                                   List<CyNode> component) {
		List<CyNode> newComponent = new ArrayList<>(component);

		for (CyNode node: component) {
			if (nodeSeenSet.contains(node))
				continue;

			nodeSeenSet.add(node);

			List<CyNode> neighbors = network.getNeighborList(node, CyEdge.Type.ANY);
			if (neighbors == null || neighbors.size() == 0)
				continue;

			newComponent.addAll(getConnectedComponent(network, nodeSeenSet, neighbors));
		}

		return newComponent;
	}
}
