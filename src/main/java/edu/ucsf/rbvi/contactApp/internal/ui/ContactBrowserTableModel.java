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

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
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
		ContactNetwork contactNetwork = contactManager.getContactNetwork(tStress);
		componentNetworks = contactNetwork.getNetworkComponents();
		Collections.sort(componentNetworks, new NetworkSorter());
		componentColors = generateColors(componentNetworks.size());

		this.data = new Object[componentNetworks.size()][columnNames.length];

		for (int i = 0; i < componentNetworks.size(); i++) {
			CyNetwork componentNetwork = componentNetworks.get(i);
			Color color = componentColors.get(i);

			// setValueAt(updateNetworkImage(componentNetwork, color), i, 0);
			setValueAt(componentNetwork, i, 0);

			setValueAt(color, i, 1);

			colorRIN(componentNetwork, networkView, color);
		}
		setDataVector(this.data, columnNames);
		fireTableDataChanged();
		networkView.updateView();
		networkBrowser.updateTable();
	}

	public void changeColor(int row, Color color) {
		NetworkImageRenderer renderer = networkBrowser.getImageRenderer();
		CyNetwork componentNetwork = (CyNetwork)getValueAt(row, 0);
		renderer.clearImage(componentNetwork);
		componentColors.set(row, color);
		setValueAt(componentNetwork, row, 0);
		colorRIN(componentNetwork, networkView, color);
		setValueAt(color, row, 1);
		fireTableRowsInserted(row, row);
		networkBrowser.updateTable();
	}

	public void colorRIN(CyNetwork componentNetwork, CyNetworkView RINNetworkView, Color color) {
		for (CyNode cNode: componentNetwork.getNodeList()) {
			Integer resId = new Integer(componentNetwork.getRow(cNode).get("ResidueNumber", Integer.class));
			if (residueMap.containsKey(resId)) {
				CyNode targetNode = residueMap.get(resId);
				View<CyNode> nv = RINNetworkView.getNodeView(targetNode);
				// nv.setLockedValue(NODE_PAINT, color);
				nv.setVisualProperty(NODE_FILL_COLOR, color);
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

	public void selectFromRow(int modelRow) {
		CyNetwork net = (CyNetwork)getValueAt(modelRow, 0);

		for (CyNode cNode: net.getNodeList()) {
			Integer resId = new Integer(net.getRow(cNode).get("ResidueNumber", Integer.class));
			if (residueMap.containsKey(resId)) {
				CyNode targetNode = residueMap.get(resId);
				network.getRow(targetNode).set(CyNetwork.SELECTED, true);
			}
		}
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
