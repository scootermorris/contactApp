package edu.ucsf.rbvi.contactApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;
import edu.ucsf.rbvi.contactApp.internal.model.ContactNetwork;

public class ContactPanel extends JPanel implements CytoPanelComponent {
  private static final long serialVersionUID = 1L;
	private final ContactManager contactManager;
	private final CyNetwork network;
	private final CyNetworkView networkView;
	private ContactNetworkBrowser contactNetworkBrowser;
	private CyEventHelper eventHelper;

  // table size parameters
	private static final int graphPicSize = 80;
	private static final int defaultRowHeight = graphPicSize + 8;

	public ContactPanel(ContactManager cManager, CyNetwork net) {
		contactManager = cManager;
		if (net == null) {
			this.network = contactManager.getCurrentNetwork();
			this.networkView = contactManager.getCurrentNetworkView();
		} else {
			this.network = net;
			// TODO: get the list of views from the view manager
			this.networkView = contactManager.getCurrentNetworkView();
		}

		setLayout(new BorderLayout());
		contactNetworkBrowser = new ContactNetworkBrowser(this, contactManager);
		add(contactNetworkBrowser, BorderLayout.CENTER);
		this.setSize(this.getMinimumSize());
		eventHelper = (CyEventHelper)contactManager.getService(CyEventHelper.class);

	}

	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	public NetworkImageRenderer getImageRenderer() {
		return contactNetworkBrowser.getImageRenderer();
	}

	@Override
	public String getTitle() {
		return "Contact Networks";
	}

	public void updateTable() {
		if (contactNetworkBrowser != null)
			contactNetworkBrowser.updateTable();
	}

	public void updateData() {
		if (contactNetworkBrowser != null)
			contactNetworkBrowser.updateData();
	}

	public class ContactNetworkBrowser extends JPanel implements ListSelectionListener, ChangeListener {
		private ContactBrowserTableModel tableModel;
		private	NetworkImageRenderer netImageRenderer;
		private final JTable table;
		private final JScrollPane tableScrollPane;
		private JLabel tableLabel;
		private JSlider slider = null;
		private final ContactPanel contactPanel;
		private final ContactManager contactManager;
		private double tStress = 0.25;
		private List<Double> stresses = null;
		protected final DecimalFormat formatter;
		int minStress = 0;
		int maxStress = 0;

		public ContactNetworkBrowser(ContactPanel component, ContactManager contactManager) {
			super();

			contactPanel = component;
			this.contactManager = contactManager;
			formatter = new DecimalFormat("0.00");

			setLayout(new BorderLayout());

			stresses = new ArrayList<Double>(contactManager.getStressSet());
			Collections.sort(stresses);
			minStress = (int)(stresses.get(0)*100.0);
			maxStress = (int)(stresses.get(stresses.size()-1)*100.0);
			tStress = stresses.get(0);

			// Only create the slider panel if we have more than one
			// stress value
			if (minStress < maxStress) {
				JPanel sliderPanel = new JPanel(new BorderLayout());
				JLabel sliderLabel = new JLabel("<html><b style=\"font-size: 8px;\">&nbsp;&nbsp;T<sub>stress</sub></b>&nbsp;&nbsp;</html>");
				sliderPanel.add(sliderLabel, BorderLayout.WEST);

				slider = new JSlider(minStress, maxStress, (int)(tStress*100.0));
				slider.setLabelTable(generateLabels(stresses));
				slider.setPaintLabels(true);
				slider.addChangeListener(this);
				sliderPanel.add(slider, BorderLayout.CENTER);
			
			// Put a border around our sliderPanel
				Border outer = BorderFactory.createEtchedBorder();
				Border inner = BorderFactory.createEmptyBorder(10,10,10,10);
				Border compound = BorderFactory.createCompoundBorder(outer, inner);
				sliderPanel.setBorder(compound);
				add(sliderPanel, BorderLayout.NORTH);
			}

			// Create a new JPanel for the table
			JPanel tablePanel = new JPanel(new BorderLayout());
			String stressLabel = formatter.format(tStress);
			tableLabel = new JLabel("<html><b style=\"font-size: 10px;\">&nbsp;&nbsp;Connected Components for T<sub>stress</sub>="+
											        stressLabel+"</b></html>");
			tablePanel.add(tableLabel, BorderLayout.NORTH);

			tableModel = new ContactBrowserTableModel(contactManager, contactManager.getCurrentNetworkView(), 
			                                          contactPanel, tStress);
			table = new JTable(tableModel);

			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.setAutoCreateRowSorter(true);
			table.setAutoCreateColumnsFromModel(true);
			table.setIntercellSpacing(new Dimension(0, 4)); // gives a little vertical room between clusters
			table.setFocusable(false); // removes an outline that appears when the user clicks on the images
			table.setRowHeight(defaultRowHeight);

			TableRowSorter rowSorter = new TableRowSorter(tableModel);
			rowSorter.setComparator(0, new NetworkSorter());
			// Don't let the user sort
			rowSorter.setSortable(0, false);
			rowSorter.setSortable(1, false);
			table.setRowSorter(rowSorter);

			// Make the headers centered
			JTableHeader tableHeader = table.getTableHeader();
			TableCellRenderer tRenderer = tableHeader.getDefaultRenderer();
			((DefaultTableCellRenderer)tRenderer).setHorizontalAlignment(SwingConstants.CENTER);

			ColorButtonRenderer renderer = new ColorButtonRenderer(tableModel);
			table.setDefaultRenderer(Color.class, renderer);
			ColorButtonEditor editor = new ColorButtonEditor(tableModel);
			table.setDefaultEditor(Color.class, editor);

			netImageRenderer = new NetworkImageRenderer(contactManager, graphPicSize);
			table.setDefaultRenderer(CyNetwork.class, netImageRenderer);

			// Ask to be notified of selection changes.
			ListSelectionModel rowSM = table.getSelectionModel();
			rowSM.addListSelectionListener(this);

			tableScrollPane = new JScrollPane(table);
			//System.out.println("CBP: after creating JScrollPane");
			tableScrollPane.getViewport().setBackground(Color.WHITE);
			tablePanel.add(tableScrollPane, BorderLayout.CENTER);
			add(tablePanel);
		}

		public NetworkImageRenderer getImageRenderer() {
			return netImageRenderer;
		}

		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting()) return;
			contactManager.hideSideChain();
			int[] rows = table.getSelectedRows(); // Get all of the selected rows

			// Clear the current selection
			for (CyNode node: network.getNodeList())
				network.getRow(node).set(CyNetwork.SELECTED, false);

			List<Integer> residues = new ArrayList<>();
			for (int viewRow: rows) {
				int modelRow = table.convertRowIndexToModel(viewRow);
				residues.addAll(tableModel.selectFromRow(modelRow));
				tableModel.clearPathwayEdges();
				if (contactManager.getShowPathway()) {
					List<CyEdge> edges = tableModel.selectEdgesFromRow(modelRow);
					eventHelper.flushPayloadEvents();
					tableModel.styleEdges(edges, modelRow);
				}
			}
			networkView.updateView();
			if (residues.size() > 0) {
				contactManager.showSideChain(residues);
			}
		}

		public void stateChanged(ChangeEvent e) {
			if (e.getSource() != slider) return;
			int stress = slider.getValue();
			double dStress = ((double)stress)/100.0;
			for (int i = 0; i < stresses.size(); i++) {
				double v = stresses.get(i);
				if (dStress > v) continue;
				if (dStress == v) break;
				double vLow = stresses.get(i-1);
				if ((dStress - vLow) < (v - dStress)) {
					dStress = vLow;
					break;
				} else {
					dStress = v;
					break;
				}
			}
			tStress = dStress;

			String stressLabel = formatter.format(tStress);
			tableLabel.setText("<html><h3>&nbsp;&nbsp;Connected Components for T<sub>stress</sub> = "+
			                   stressLabel+"</h3></html>");

			// Create the new tableModel
			// tableModel.updateData(tStress);

			tableModel = new ContactBrowserTableModel(contactManager, contactManager.getCurrentNetworkView(), 
			                                          contactPanel, tStress);
			table.setModel(tableModel);
			updateTable();

			// FIXME: For some reason, the table doesn't update!
			// table.revalidate();
			// tableScrollPane.revalidate();
			// tableScrollPane.repaint();
		}

		public JTable getTable() { return table; }

		public void updateTable() {
			tableModel.fireTableDataChanged();
			tableModel.fireTableStructureChanged();
			tableScrollPane.getViewport().revalidate();
			table.doLayout();
			((TableRowSorter)table.getRowSorter()).sort();
		}

		public void updateData() {
			tableModel.updateData(tStress);
		}

		public Dictionary<Integer, JComponent> generateLabels(List<Double> stresses) {
			Dictionary<Integer, JComponent> table = new Hashtable<>();
			for (Double stress: stresses) {
				int value = (int)(stress.doubleValue()*100.0+0.5);
				if (value%5 != 0) continue;
				String label = formatter.format(stress);
				JLabel jLabel = new JLabel(label); // May have to use a text formatter
				jLabel.setFont(new Font("SansSerif", Font.BOLD, 8));
				table.put(value, jLabel);
			}
			return table;
		}
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
