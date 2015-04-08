package edu.ucsf.rbvi.contactApp.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ColorButtonRenderer extends JButton implements TableCellRenderer {
	static final long serialVersionUID = 1L;
	final ContactBrowserTableModel tableModel;
	Color currentColor;
	JButton button;
	JPanel frame;

	public ColorButtonRenderer(ContactBrowserTableModel tableModel) {
		super("Change color");
		this.tableModel = tableModel;
		setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object color,
	                                               boolean isSelected, boolean hasFocus,
																								 int row, int column) {
		frame = new JPanel();
		frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));
		Box box = new Box(BoxLayout.Y_AXIS);
		frame.add(box);
		button = new JButton("Change color");
		button.setSize(new Dimension(60,40));
		button.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		box.add(Box.createVerticalGlue());
		box.add(button);
		box.add(Box.createVerticalGlue());

		currentColor = (Color) color;
		// Create icon
		Icon icon = new ColorIcon(currentColor, 10);
		// Add to button
		button.setIcon(icon);
		return frame;
	}
}

