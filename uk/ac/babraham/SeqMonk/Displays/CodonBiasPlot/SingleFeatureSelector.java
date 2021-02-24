/**
 * Copyright 2013- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.CodonBiasPlot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;

public class SingleFeatureSelector extends JDialog implements ActionListener, KeyListener, MouseListener {

	private static String selectedType = null;
	private Feature selectedFeature = null;
	private Feature [] availableFeatures;
	
	private AnnotationCollection annot;
	private JComboBox featureTypes;
	private JTextField searchBox;
	private JTable featureTable;
	private FeatureTableModel model;
	
	public SingleFeatureSelector (AnnotationCollection annot, Component c) {
		super(SeqMonkApplication.getInstance(),"Select feature");
		setModal(true);
		this.annot = annot;
		
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BorderLayout());
		JPanel topPanel = new JPanel();
		
		featureTypes = new JComboBox(annot.listAvailableFeatureTypes());
		featureTypes.setActionCommand("feature_type_changed");
		featureTypes.addActionListener(this);
		topPanel.add(featureTypes);
		
		if (selectedType == null) {
			selectedType = annot.listAvailableFeatureTypes()[0];
		}
		
		availableFeatures = annot.getFeaturesForType(selectedType);
		
		searchBox = new JTextField(10);
		searchBox.addKeyListener(this);
		topPanel.add(searchBox);
		
		searchPanel.add(topPanel,BorderLayout.NORTH);
		
		model = new FeatureTableModel();
		featureTable = new JTable(model);
		featureTable.addMouseListener(this);
		
		searchPanel.add(new JScrollPane(featureTable),BorderLayout.CENTER);
		
		setContentPane(searchPanel);
		setSize(500,300);
		setLocationRelativeTo(c);
		
		
	}
	
	public Feature getSelectedFeature () {
		setVisible(true);
		
		return selectedFeature;
	}
	
	
	public void keyPressed(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}


	public void keyReleased(KeyEvent ke) {

		String searchString = searchBox.getText().trim().toLowerCase();
		
		if (searchString.length() == 0) {
			availableFeatures = annot.getFeaturesForType(selectedType);
		}
		
		else {
			Feature [] allFeatures = annot.getFeaturesForType(selectedType);
		
			Vector<Feature> keepers = new Vector<Feature>();
			for (int f=0;f<allFeatures.length;f++) {
				if (allFeatures[f].name().toLowerCase().contains(searchString)) {
					keepers.add(allFeatures[f]);
				}
			}
			
			availableFeatures = keepers.toArray(new Feature[0]);
		}
		
		model.fireTableDataChanged();
	}



	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("feature_type_changed")) {
			selectedType = (String)featureTypes.getSelectedItem();
			keyReleased(new KeyEvent(this, 1, 0, 0, 0,'a'));
		}
		
	}


	public void mouseClicked(MouseEvent me) {
		if (me.getSource().equals(featureTable) && me.getClickCount() >=2) {
			int r = featureTable.getSelectedRow();
			if (r>=0) {
				selectedFeature = availableFeatures[r];
				setVisible(false);
				dispose();
			}
		}
		
	}

	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}


	private class FeatureTableModel extends AbstractTableModel {
	
		public int getColumnCount() {
			return 2;
		}
	
		public int getRowCount() {
			return availableFeatures.length;
		}
	
		public Object getValueAt(int r, int c) {
			if (c==0) {
				return (availableFeatures[r].name());
			}
			else if (c==1) {
				return (availableFeatures[r].description());
			}
			else {
				return null;
			}
		}
		
	}
	
}
