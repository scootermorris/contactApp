package edu.ucsf.rbvi.contactApp.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class ColorButtonEditor extends AbstractCellEditor implements TableCellEditor {
	static final long serialVersionUID = 1L;
	final ContactBrowserTableModel tableModel;
	boolean isPushed = false;
	Color currentColor = Color.BLACK;
	JButton button;
	JPanel frame;
	int selectedRow;
	int selectedColumn;

	public ColorButtonEditor(ContactBrowserTableModel tableModel) {
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

		this.tableModel = tableModel;
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();
			}
		});
	}

	public Component getTableCellEditorComponent(JTable table, Object color,
	                                             boolean isSelected, 
	                                             int row, int column) {
		selectedRow = row;
		selectedColumn = column;
		currentColor = (Color)color;
		button.setIcon(new ColorIcon(currentColor, 10));
		isPushed = true;
		return frame;
	}

	public Object getCellEditorValue() {
		if (isPushed) {
			isPushed = false;
			// Change the color
			Color newColor = JColorChooser.showDialog(null, "Choose New Color for Nodes", currentColor);
			if (newColor != null) {
				currentColor = newColor;

				button.setIcon(new ColorIcon(currentColor, 10));
				tableModel.changeColor(selectedRow, currentColor);
			}
		}
		return currentColor;
	}

	public boolean stopCellEditing() {
		isPushed = false;
		return super.stopCellEditing();
	}

	protected void fireEditingStopped() {
		try {
			super.fireEditingStopped();
		} catch (IndexOutOfBoundsException e) {}
	}

}

