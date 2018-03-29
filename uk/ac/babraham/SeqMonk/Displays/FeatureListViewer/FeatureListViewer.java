/**
 * Copyright Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.FeatureListViewer;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.sun.java.TableSorter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

/**
 * The Class FeatureListViewer displays the hits when searching through
 * the features.
 */


public class FeatureListViewer extends JTable implements MouseListener {
		
	private SeqMonkApplication application;
	/**
	 * Instantiates a new feature list viewer.
	 * 
	 * @param features the features
	 * @param application the application
	 */
	@SuppressWarnings("rawtypes")
	public FeatureListViewer (Feature [] features) {

		this.application = SeqMonkApplication.getInstance();


		String [] headers = new String [] {"Feature","Type","Description","Chr","Start","End"};
		Class [] classes = new Class [] {String.class,String.class,String.class,String.class,Integer.class,Integer.class};
		
		Object [][] rowData = new Object [features.length][headers.length];
		
		for (int i=0;i<features.length;i++) {
			rowData[i][0] = features[i];
			rowData[i][1] = features[i].type();
			rowData[i][2] = features[i].description();
			if (features[i].chromosomeName() != null) {
				rowData[i][3] = features[i].chromosomeName();
			}
			else {
				rowData[i][3] = "No chr";				
			}
			rowData[i][4] = new Integer(features[i].location().start());
			rowData[i][5] = new Integer(features[i].location().end());
		}

		TableSorter sorter = new TableSorter(new FeatureTableModel(rowData,headers,classes));
		setModel(sorter);
		addMouseListener(this);
		sorter.setTableHeader(getTableHeader());
	}
	
	public Feature [] getSelectedFeatures () {
		
		int [] selectedIndices = getSelectedRows();
		Feature [] features = new Feature[selectedIndices.length];
		
		for (int i=0;i<features.length;i++) {
			features[i] = (Feature)getValueAt(selectedIndices[i], 0);
		}
		
		return features;
	}

	/**
	 * The Class FeatureTableModel.
	 */
	private class FeatureTableModel extends AbstractTableModel {

		/** The data. */
		private Object [][] data;
		
		/** The headers. */
		private String [] headers;
		
		/** The classes. */
		@SuppressWarnings("rawtypes")
		private Class [] classes;
		
		/**
		 * Instantiates a new feature table model.
		 * 
		 * @param data the data
		 * @param headers the headers
		 * @param classes the classes
		 */
		@SuppressWarnings("rawtypes")
		public FeatureTableModel (Object [][] data, String [] headers, Class [] classes) {
			super();
			this.data = data;
			this.headers = headers;
			this.classes = classes;
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return data.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return data[0].length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			return data[r][c];
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			return headers[c];
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Class getColumnClass (int c) {
			return classes[c];
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
		 */
		public boolean isCellEditable (int r, int c) {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent me) {
		//We're only interested in double clicks
		if (me.getClickCount() !=2 ) return;
		// This is only linked from the report JTable
		JTable t = (JTable)me.getSource();
		int r = t.getSelectedRow();
		Feature f = (Feature)t.getValueAt(r,0);
				
		DisplayPreferences.getInstance().setLocation(application.dataCollection().genome().getChromosome(f.chromosomeName()).chromosome(),SequenceRead.packPosition(f.location().start(),f.location().end(),Location.UNKNOWN));
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent arg0) {
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent arg0) {
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent arg0) {
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent arg0) {
	}
}
