package edu.ucsf.rbvi.contactApp.internal.ui;

import java.awt.Image;
import javax.swing.ImageIcon;

import org.cytoscape.model.CyNetwork;

public class NetworkImageIcon extends ImageIcon implements Comparable<NetworkImageIcon> {
	protected CyNetwork network;
	static final long serialVersionUID = 1L;

	public NetworkImageIcon() {
		super();
		network = null;
	}

	public NetworkImageIcon(Image image, CyNetwork net) {
		super(image);
		this.network = net;
	}

	public int compareTo(NetworkImageIcon cii2) {
		if ((network == null && cii2.network == null) ||
				(network.getNodeCount() == cii2.network.getNodeCount()))
			return 0;
		else if (network == null || network.getNodeCount() < cii2.network.getNodeCount())
			return -1;
		return 1;
	}

	public CyNetwork getNetwork() {
		return network;
	}
}

