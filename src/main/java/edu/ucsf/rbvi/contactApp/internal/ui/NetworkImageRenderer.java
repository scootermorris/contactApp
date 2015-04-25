package edu.ucsf.rbvi.contactApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngineFactory;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;
import static org.cytoscape.view.presentation.property.ArrowShapeVisualProperty.NONE;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;

import edu.ucsf.rbvi.contactApp.internal.model.ContactManager;

public class NetworkImageRenderer implements TableCellRenderer {
	static final long serialVersionUID = 1L;
	final ContactManager contactManager;
	private final Map<CyNetwork, ImageIcon> imageMap = new HashMap<>();
	private final int graphPicSize;

	// Services we'll need
	private final VisualStyleFactory visualStyleFactory;
	private final CyNetworkViewFactory networkViewFactory;
	private final VisualMappingManager visualMappingMgr;
	private final RenderingEngineFactory<CyNetwork> renderingEngineFactory;

	private VisualStyle componentStyle = null;
	private SpringEmbeddedLayouter layouter;

	public NetworkImageRenderer(final ContactManager contactManager, final int graphPicSize) {
		this.contactManager = contactManager;
		this.graphPicSize = graphPicSize;

		visualStyleFactory = contactManager.getService(VisualStyleFactory.class);
		networkViewFactory = contactManager.getService(CyNetworkViewFactory.class);
		visualMappingMgr = contactManager.getService(VisualMappingManager.class);
		renderingEngineFactory = contactManager.getService(RenderingEngineFactory.class);
		layouter = new SpringEmbeddedLayouter();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object network,
	                                               boolean isSelected, boolean hasFocus,
																								 int viewRow, int viewColumn) {
		ContactBrowserTableModel tableModel = (ContactBrowserTableModel)table.getModel();
		CyNetwork net = (CyNetwork)network;
		// System.out.println("Rendering network for row "+viewRow);

		if (!imageMap.containsKey(net)) {
			// System.out.println("Redrawing image for row "+viewRow);
			// generate the image
			Color color = (Color) tableModel.getValueAt(viewRow, 1);
			// System.out.println("Color for row "+viewRow+" is "+color);
			Image image = createNetworkImage(net, color, graphPicSize, graphPicSize, layouter, true);
			ImageIcon icon = new ImageIcon(image);
			imageMap.put(net, icon);
		}
		ImageIcon netIcon = imageMap.get(net);
		JLabel l = new JLabel(netIcon);
		l.setOpaque(true);
		l.setPreferredSize(new Dimension(netIcon.getIconWidth(), netIcon.getIconHeight()));
		l.revalidate();

		return l;
	}

	public void clearImage(CyNetwork net) {
		if (imageMap.containsKey(net))
			imageMap.remove(net);
	}

	public Image createNetworkImage(final CyNetwork net,
									final Color nodeColor,
									final int height,
									final int width,
									SpringEmbeddedLayouter layouter,
									boolean layoutNecessary) {

		// Progress reporters.
		// There are three basic tasks, the progress of each is calculated and then combined
		// using the respective weighting to get an overall progress global progress
		int weightSetupNodes = 20; // setting up the nodes and edges is deemed as 25% of the whole task
		int weightSetupEdges = 5;
		double weightLayout = 75.0; // layout it is 70%
		double goalTotal = weightSetupNodes + weightSetupEdges;

		if (layoutNecessary) {
			goalTotal += weightLayout;
		}

		// keeps track of progress as a percent of the totalGoal
		double progress = 0;

		final VisualStyle vs = getComponentStyle();

		//System.out.println("CCI: after getComponentStyle");
		final CyNetworkView componentView = createNetworkView(net, vs);
		//System.out.println("CCI: after createNetworkView");

		componentView.setVisualProperty(NETWORK_WIDTH, new Double(width));
		componentView.setVisualProperty(NETWORK_HEIGHT, new Double(height));

		for (View<CyNode> nv : componentView.getNodeViews()) {
			// Node position
			final double x;
			final double y;

			// Otherwise, randomize node positions before layout so that they don't all layout in a line
			// (so they don't fall into a local minimum for the SpringEmbedder)
			// If the SpringEmbedder implementation changes, this code may need to be removed
			// size is small for many default drawn graphs, thus +100
			x = (componentView.getVisualProperty(NETWORK_WIDTH) + 100) * Math.random();
			y = (componentView.getVisualProperty(NETWORK_HEIGHT) + 100) * Math.random();

			if (!layoutNecessary) {
				goalTotal += weightLayout;
				progress /= (goalTotal / (goalTotal - weightLayout));
				layoutNecessary = true;
			}

			nv.setVisualProperty(NODE_X_LOCATION, x);
			nv.setVisualProperty(NODE_Y_LOCATION, y);
			nv.setLockedValue(NODE_PAINT, nodeColor);
			nv.setLockedValue(NODE_FILL_COLOR, nodeColor);
		}

		if (componentView.getEdgeViews() != null) {
			for (int i = 0; i < componentView.getEdgeViews().size(); i++) {
				// Weight edges?
			}
		}

		if (layoutNecessary) {
			if (layouter == null) {
				layouter = new SpringEmbeddedLayouter();
			}

			layouter.setGraphView(componentView);

			// The doLayout method should return true if the process completes without interruption
			layouter.doLayout(weightLayout, goalTotal, progress);
		}

		final Image image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = (Graphics2D) image.getGraphics();

		if (SwingUtilities.isEventDispatchThread()) {
			paintNetwork(g, width, height, vs, componentView);
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					//@Override
					public void run() {
						paintNetwork(g, width, height, vs, componentView);
					}
				});
			} catch(Exception e) {}
		}

		layouter.resetDoLayout();

		return image;
	}

	public void paintNetwork(Graphics2D g, int width, int height, VisualStyle vs, CyNetworkView componentView) {
		final Dimension size = new Dimension(width, height);

		JPanel panel = new JPanel();
		panel.setPreferredSize(size);
		panel.setSize(size);
		panel.setMinimumSize(size);
		panel.setMaximumSize(size);
		panel.setBackground((Color) vs.getDefaultValue(NETWORK_BACKGROUND_PAINT));

		JWindow window = new JWindow();
		window.getContentPane().add(panel, BorderLayout.CENTER);

		RenderingEngine<CyNetwork> re = renderingEngineFactory.createRenderingEngine(panel, componentView);

		vs.apply(componentView);
		componentView.fitContent();
		componentView.updateView();
		window.pack();
		window.repaint();

		re.createImage(width, height);
		re.printCanvas(g);
		g.dispose();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public VisualStyle getComponentStyle() {
		if (componentStyle == null) {
			componentStyle = visualStyleFactory.createVisualStyle("Cluster");

			componentStyle.setDefaultValue(NODE_SIZE, 40.0);
			componentStyle.setDefaultValue(NODE_WIDTH, 40.0);
			componentStyle.setDefaultValue(NODE_HEIGHT, 40.0);
			componentStyle.setDefaultValue(NODE_PAINT, Color.RED);
			componentStyle.setDefaultValue(NODE_FILL_COLOR, Color.RED);
			componentStyle.setDefaultValue(NODE_BORDER_WIDTH, 0.0);

			componentStyle.setDefaultValue(EDGE_WIDTH, 5.0);
			componentStyle.setDefaultValue(EDGE_PAINT, Color.BLUE);
			componentStyle.setDefaultValue(EDGE_UNSELECTED_PAINT, Color.BLUE);
			componentStyle.setDefaultValue(EDGE_STROKE_UNSELECTED_PAINT, Color.BLUE);
			componentStyle.setDefaultValue(EDGE_SELECTED_PAINT, Color.BLUE);
			componentStyle.setDefaultValue(EDGE_STROKE_SELECTED_PAINT, Color.BLUE);
			componentStyle.setDefaultValue(EDGE_TARGET_ARROW_SHAPE, NONE);
			componentStyle.setDefaultValue(EDGE_SOURCE_ARROW_SHAPE, NONE);

			// Scale the edge width based on the number of pathways (edge row Count)
			// Get a function factory
			VisualMappingFunctionFactory vmff = contactManager.getService(VisualMappingFunctionFactory.class,
			                                                              "(mapping.type=continuous)");
			ContinuousMapping edgeMapping =
				(ContinuousMapping) vmff.createVisualMappingFunction("Count", Integer.class, EDGE_WIDTH);

			edgeMapping.addPoint(1, new BoundaryRangeValues(1, 1, 1));
			edgeMapping.addPoint(10, new BoundaryRangeValues(20, 20, 20));
			componentStyle.addVisualMappingFunction(edgeMapping);
		}

		return componentStyle;
	}

	public CyNetworkView createNetworkView(final CyNetwork net, VisualStyle vs) {
		final CyNetworkView view = networkViewFactory.createNetworkView(net);
		//System.out.println("inside createNetworkView");
		if (vs == null) vs = visualMappingMgr.getDefaultVisualStyle();
		visualMappingMgr.setVisualStyle(vs, view);
		vs.apply(view);
		view.updateView();

		return view;
	}
}

