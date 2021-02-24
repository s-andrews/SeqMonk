/**
 * Copyright 2010- 21 Simon Andrews
 *
 *    This file is part of SeqMonk.
 *
 *    SeqMonk is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SeqMonk; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;

/**
 * Displays a dialog showing the different types of feature contained
 * in an annotation set and the counts for each.  The counts are
 * calculated asynchronously so the dialog displays quickly even if
 * lots of number crunching needs to be done to get the annotation back
 * off disk.
 */
public class AnnotationSetPropertiesDialog extends JDialog {

	private AnnotationSet set;
	private String [] types;
	private Integer [] counts;
	
	private AnnotationSetTableModel model;
	
	/**
	 * Instantiates a new annotation set properties dialog.
	 * 
	 * @param set The AnnotationSet to use
	 */
	public AnnotationSetPropertiesDialog(AnnotationSet set) {
		super(SeqMonkApplication.getInstance(), set.name());
		this.set = set;
		types = set.getAvailableFeatureTypes();
		counts = new Integer[types.length];
		
		for (int i=0;i<types.length;i++) {
			counts[i] = set.getCountForFeatureType(types[i]);
		}
		
		model = new AnnotationSetTableModel();
		JTable table = new JTable(model);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(table),BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();				
			}
		});
		buttonPanel.add(closeButton);

		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(300,300);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
		
	}
	
	/**
	 * Provides a tableModel for the results table
	 */
	private class AnnotationSetTableModel extends AbstractTableModel {

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return 2;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return types.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int column) {
			if (column == 0) {
				return "Feature Type";
			}
			else if (column == 1) {
				return "Count";
			}
			else return null;
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return types[rowIndex];
			}
			else if (columnIndex == 1) {
				if (counts[rowIndex] == null) return "Counting...";
				return counts[rowIndex];
			}
			return null;
		}
		
	}
	
	
}
