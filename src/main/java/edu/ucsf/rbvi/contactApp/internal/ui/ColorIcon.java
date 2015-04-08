package edu.ucsf.rbvi.contactApp.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class ColorIcon implements Icon {
	static final long serialVersionUID = 1L;

	private final Color color;
	private final int size;

	public ColorIcon(Color color, int size) {
		this.color = color;
		this.size = size;
	}

	@Override
	public int getIconHeight() { return size; }

	@Override
	public int getIconWidth() { return size; }

	public void paintIcon(Component c, Graphics g, int x, int y) {
		Color oldColor = g.getColor();
		g.setColor(color);
		g.fillRect(x, y, size, size);
		g.setColor(oldColor);
	}
}
