package edu.ucsf.rbvi.contactApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_PAINT;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_VISIBLE;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.EDGE_WIDTH;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_FILL_COLOR;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;
import edu.ucsf.rbvi.contactApp.internal.model.ContactNetwork;

public class ContactBrowserTableModel extends DefaultTableModel {
	private final ContactManager contactManager;
	private final CyNetwork network;
	private final CyNetworkView networkView;
	private final ContactPanel networkBrowser;
	private Object[][] data;
	private List<CyNetwork> componentNetworks;
	private List<Color> componentColors;
	private final String[] columnNames = { "Subnetwork", "Color" };
	private Map<Integer, CyNode> residueMap;

	public ContactBrowserTableModel(ContactManager contactManager, 
	                                CyNetworkView networkView, 
	                                ContactPanel networkBrowser, 
																	double tStress) {
		super(1,2);
		this.contactManager = contactManager;
		this.networkView = networkView;
		this.network = networkView.getModel();
		this.networkBrowser = networkBrowser;
		residueMap = new HashMap<>();

		// Build our residue index to node map
		if (network.getDefaultNodeTable().getColumn("Resindex") != null) {
			for (CyNode node: network.getNodeList()) {
				Integer index = network.getRow(node).get("Resindex", Integer.class);
				residueMap.put(index, node);
				View<CyNode> nv = networkView.getNodeView(node);
				nv.clearValueLock(NODE_FILL_COLOR);
			}
		}
		updateData(tStress);
	}

	public void updateData(double tStress) {
		// First, reset all of the RIN nodes to grey
		for (View<CyNode> nv: networkView.getNodeViews())
			nv.setLockedValue(NODE_FILL_COLOR, Color.GRAY);

		// Now handle all of our component networks
		ContactNetwork contactNetwork = contactManager.getContactNetwork(tStress);
		componentNetworks = contactNetwork.getNetworkComponents();
		Collections.sort(componentNetworks, new NetworkSorter());
		componentColors = generateColors(componentNetworks.size());

		this.data = new Object[componentNetworks.size()][columnNames.length];

		for (int i = 0; i < componentNetworks.size(); i++) {
			// Default grey
			Color color = new Color(192,192,192,128);
			CyNetwork componentNetwork = componentNetworks.get(i);
			// System.out.println("Network component "+i+" is "+componentNetwork.getSUID());
			if (componentNetwork.getNodeCount() > 2)
				color = componentColors.get(i);

			data[i][0] = componentNetwork;

			data[i][1] = color;

			colorRIN(componentNetwork, networkView, color);
		}
		setDataVector(this.data, columnNames);
		fireTableDataChanged();
		networkView.updateView();
		networkBrowser.updateTable();
		contactManager.syncColors();
	}

	public void changeColor(int row, Color color) {
		NetworkImageRenderer renderer = networkBrowser.getImageRenderer();
		CyNetwork componentNetwork = (CyNetwork)getValueAt(row, 0);
		renderer.clearImage(componentNetwork);
		componentColors.set(row, color);
		// System.out.println("Setting color for row "+row);
		setValueAt(color, row, 1);
		// System.out.println("Redrawing network for row "+row);
		setValueAt(componentNetwork, row, 0);
		colorRIN(componentNetwork, networkView, color);
		// fireTableRowsInserted(row, row);
		networkBrowser.updateTable();
		contactManager.syncColors();
	}

	public void colorRIN(CyNetwork componentNetwork, CyNetworkView RINNetworkView, Color color) {
		// Now color according to the pathway
		for (CyNode cNode: componentNetwork.getNodeList()) {
			Integer resId = new Integer(componentNetwork.getRow(cNode).get("ResidueNumber", Integer.class));
			if (residueMap.containsKey(resId)) {
				CyNode targetNode = residueMap.get(resId);
				View<CyNode> nv = RINNetworkView.getNodeView(targetNode);
				nv.clearValueLock(NODE_FILL_COLOR);
				nv.setLockedValue(NODE_FILL_COLOR, color);
			}
		}
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public int getColumnCount() {
		if (columnNames == null) return 2;
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		if (data == null) return 1;
		return data.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	@Override
	public void setValueAt(Object object, int row, int col) {
		data[row][col] = object;
		fireTableCellUpdated(row, col);
	}

	@Override
	public Class<?> getColumnClass(int c) {
		if (c == 0) 
			return CyNetwork.class;
		return Color.class;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (column == 1) return true;
		return false;
	}

	public List<Integer> selectFromRow(int modelRow) {
		CyNetwork net = (CyNetwork)getValueAt(modelRow, 0);
		List<Integer> residueList = new ArrayList<>();

		for (CyNode cNode: net.getNodeList()) {
			Integer resId = new Integer(net.getRow(cNode).get("ResidueNumber", Integer.class));
			if (residueMap.containsKey(resId)) {
				CyNode targetNode = residueMap.get(resId);
				network.getRow(targetNode).set(CyNetwork.SELECTED, true);
				residueList.add(resId);
			}
		}
		return residueList;
	}

	public List<CyEdge> selectEdgesFromRow(int modelRow) {
		CyNetwork net = (CyNetwork)getValueAt(modelRow, 0);
		List<CyEdge> edgeList = new ArrayList<>();

		for (CyEdge cEdge: net.getEdgeList()) {
			CyNode source = cEdge.getSource();
			CyNode target = cEdge.getTarget();
			Integer sourceResId = new Integer(net.getRow(source).get("ResidueNumber", Integer.class));
			Integer targetResId = new Integer(net.getRow(target).get("ResidueNumber", Integer.class));
			int count = net.getRow(cEdge).get("PathwayCount", Integer.class);
			if (residueMap.containsKey(sourceResId) && residueMap.containsKey(targetResId)) {
				CyNode sourceNode = residueMap.get(sourceResId);
				CyNode targetNode = residueMap.get(targetResId);
				edgeList.add(addEdgeIfNecessary(sourceNode, targetNode, count));
			}
		}
		return edgeList;
	}

	public void clearPathwayEdges() {
		for (CyEdge edge: network.getEdgeList()) {
			if (network.getRow(edge).get(CyEdge.INTERACTION, String.class).equals("ContactPathway"))
				networkView.getEdgeView(edge).setLockedValue(EDGE_VISIBLE, false);
		}
	}

	public void styleEdges(List<CyEdge> edges, int modelRow) {
		Color color = (Color) getValueAt(modelRow, 1);

		int max = -1;

		// First pass -- calculate min/max
		for (CyEdge edge: edges) {
			int width = network.getRow(edge).get("PathwayCount", Integer.class);
			if (width > max)
				max = width;
		}

		double factor = 1.0;
		if (max > 50)
			factor = 5.0;
		else if (max > 40)
			factor = 4.0;
		else if (max > 30)
			factor = 3.0;
		else if (max > 20)
			factor = 2.0;

		for (CyEdge edge: edges) {
			int width = network.getRow(edge).get("PathwayCount", Integer.class);
			View<CyEdge> edgeView = networkView.getEdgeView(edge);
			if (edgeView == null) continue;

			// We need to use a locked property because RINalyzer has a discrete mapping
			// for edge color
			edgeView.setLockedValue(EDGE_PAINT, color);
			edgeView.setLockedValue(EDGE_WIDTH, (double)width/factor);
		}
	}

	private CyEdge addEdgeIfNecessary(CyNode source, CyNode target, int count) {
		CyTable edgeTable = network.getDefaultEdgeTable();
		if (edgeTable.getColumn("PathwayCount") == null)
			edgeTable.createColumn("PathwayCount", Integer.class, false);

		if (edgeTable.getColumn(CyEdge.INTERACTION) == null)
			edgeTable.createColumn(CyEdge.INTERACTION, String.class, false);

		CyEdge pathwayEdge = null;
		if (network.containsEdge(source, target)) {
			for (CyEdge edge: network.getConnectingEdgeList(source, target, CyEdge.Type.DIRECTED)) {
				if (network.getRow(edge).get(CyEdge.INTERACTION, String.class).equals("ContactPathway")) {
					pathwayEdge = edge;
					networkView.getEdgeView(pathwayEdge).clearValueLock(EDGE_VISIBLE);
					break;
				}
			}
		}
		if (pathwayEdge == null) {
			pathwayEdge = network.addEdge(source, target, true);
			network.getRow(pathwayEdge).set(CyEdge.INTERACTION, "ContactPathway");
		}
		network.getRow(pathwayEdge).set("PathwayCount", count);
		return pathwayEdge;
	}

	private List<Color> generateColors(int number) {
		int[][] colorArray = new int[][] {
						{0,0,153,255}, // Dark Blue
						{153,0,0,255}, // Dark Red
						{0,153,0,255}, // Dark Green
						{255,153,0,255}, // Orange
						{0,255,0,255}, // Green
						{0,255,255,255}, // Cyan
						{255,0,255,255}, // Magenta
						{0,153,153,255}, // Dark Cyan
						{153,0,153,255}, // Dark Magenta
						{0,153,255,255}, // Light blue
						{255,102,153,255}, // Light red
						{0,255,153,255}, // Light green

			};
		List<Color> colors = new ArrayList<>();
		for (int i = 0; i < number; i++) {
			if (i < colorArray.length)
				colors.add(new Color(colorArray[i][0], colorArray[i][1], 
				                     colorArray[i][2], colorArray[i][3]));
			else
				colors.add(new Color(192,192,192,128));
		}
		return colors;
	}

	private class NetworkSorter implements Comparator<CyNetwork> {
		public NetworkSorter() { }

		public int compare(CyNetwork n1, CyNetwork n2) {
			if (n1 == null && n2 == null) return 0;
			if (n1 == null && n2 != null) return -1;
			if (n2 == null && n1 != null) return 1;

			if(n1.getNodeCount() < n2.getNodeCount()) return 1;
			if(n1.getNodeCount() > n2.getNodeCount()) return -1;
			return 0;
		}
	}


}
