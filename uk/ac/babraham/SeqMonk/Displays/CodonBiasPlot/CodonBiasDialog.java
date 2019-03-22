/**
 * Copyright 2010-19 Simon Andrews
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class CodonBiasDialog extends JDialog implements ActionListener {

	private AnnotationCollection annot;
	private CodonBiasPanel graphPanel;
	private JComboBox dataStoreBox;
	private JButton selectFeatureButton;
	private JComboBox reverseBox;
	private Feature currentFeature;
	

	public CodonBiasDialog (DataStore [] stores, AnnotationCollection annot) {
		super(SeqMonkApplication.getInstance(),"Codon Bias");
		this.annot = annot;
		getContentPane().setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel();

		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);
		
		JButton saveButton = new JButton("Save");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
	
		JPanel optionsPanel = new JPanel();
		
		dataStoreBox = new JComboBox(stores);
		dataStoreBox.setPrototypeDisplayValue("No longer than this please");
		dataStoreBox.addActionListener(this);
		dataStoreBox.setActionCommand("change_store");
		optionsPanel.add(dataStoreBox);

		selectFeatureButton = new JButton("Select Feature");
		selectFeatureButton.addActionListener(this);
		selectFeatureButton.setActionCommand("select_feature");
		optionsPanel.add(selectFeatureButton);
		
		reverseBox = new JComboBox(new String [] {"Same Strand Specific","Opposing Strand Specific"});
		reverseBox.setActionCommand("select_library_type"); 
		reverseBox.addActionListener(this);
		optionsPanel.add(reverseBox);

		getContentPane().add(optionsPanel,BorderLayout.NORTH);
		
		graphPanel = new CodonBiasPanel();
		
		getContentPane().add(graphPanel,BorderLayout.CENTER);
		
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save")) {
			ImageSaver.saveImage(graphPanel);
		}
		else if (ae.getActionCommand().equals("change_store") || ae.getActionCommand().equals("select_library_type")) {
			if (currentFeature != null) {
				graphPanel.setDisplay(currentFeature, (DataStore)dataStoreBox.getSelectedItem(), reverseBox.getSelectedItem().equals("Opposing Strand Specific"));
			}
		}
		else if (ae.getActionCommand().equals("select_feature")) {
			SingleFeatureSelector sfs = new SingleFeatureSelector(annot,this);
			currentFeature = sfs.getSelectedFeature();
			graphPanel.setDisplay(currentFeature, (DataStore)dataStoreBox.getSelectedItem(), reverseBox.getSelectedItem().equals("Opposing Strand Specific"));
		}
		else {
			throw new IllegalStateException("Unknown command "+ae.getActionCommand());
		}
	} 
	
}
