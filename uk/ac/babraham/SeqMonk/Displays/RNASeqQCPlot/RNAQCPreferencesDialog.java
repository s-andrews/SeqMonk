/**
 * Copyright 2014-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.RNASeqQCPlot;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;

public class RNAQCPreferencesDialog extends JDialog implements ActionListener, ProgressListener {

	private DataStore [] stores;
	private JComboBox geneFeaturesBox;
	private JComboBox transcriptFeaturesBox;
	private JCheckBox measureTranscriptsBox;
	private JComboBox rRNAFeaturesBox;
	private JCheckBox measureRRNABox;
	private JList chromosomeList;
	private DataCollection collection;
	
	public RNAQCPreferencesDialog(DataCollection collection, DataStore [] stores)  {
		super(SeqMonkApplication.getInstance(),"RNA-Seq QC Plot");
		
		if (stores.length == 0) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "There are no visible data stores", "Can't draw plot", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		if (collection.genome().annotationCollection().listAvailableFeatureTypes().length == 0) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "You don't have any annotation available", "Can't draw plot", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		this.stores = stores;
		this.collection = collection;
		getContentPane().setLayout(new BorderLayout());

		JPanel choicePanel = new JPanel();
		
		choicePanel.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.gridx=1;
		gbc.gridy=1;
		
		choicePanel.add(new JLabel("Measure Genes"),gbc);
		gbc.gridx++;
		
		geneFeaturesBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
		geneFeaturesBox.setPrototypeDisplayValue("No longer than this please");
		for (int i=0;i<geneFeaturesBox.getModel().getSize();i++) {
			if (geneFeaturesBox.getModel().getElementAt(i).equals("gene")) {
				geneFeaturesBox.setSelectedIndex(i);
				break;
			}
		}
		choicePanel.add(geneFeaturesBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;

		measureTranscriptsBox = new JCheckBox("Measure Transcripts",true);
		choicePanel.add(measureTranscriptsBox,gbc);
		measureTranscriptsBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				transcriptFeaturesBox.setEnabled(measureTranscriptsBox.isSelected());
			}
		});
		gbc.gridx++;
		
		transcriptFeaturesBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
		transcriptFeaturesBox.setPrototypeDisplayValue("No longer than this please");
		for (int i=0;i<transcriptFeaturesBox.getModel().getSize();i++) {
			if (transcriptFeaturesBox.getModel().getElementAt(i).equals("mRNA")) {
				transcriptFeaturesBox.setSelectedIndex(i);
				break;
			}
		}
		
		if (transcriptFeaturesBox.getSelectedItem() == null  || !transcriptFeaturesBox.getSelectedItem().equals("mRNA")) {
			measureTranscriptsBox.setSelected(false);
			transcriptFeaturesBox.setEnabled(false);
		}
		
		choicePanel.add(transcriptFeaturesBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;

		
		measureRRNABox = new JCheckBox("Measure rRNA",true);
		choicePanel.add(measureRRNABox,gbc);
		measureRRNABox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rRNAFeaturesBox.setEnabled(measureRRNABox.isSelected());
			}
		});
		gbc.gridx++;
		
		rRNAFeaturesBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
		rRNAFeaturesBox.setPrototypeDisplayValue("No longer than this please");
		for (int i=0;i<rRNAFeaturesBox.getModel().getSize();i++) {
			if (rRNAFeaturesBox.getModel().getElementAt(i).equals("rRNA")) {
				rRNAFeaturesBox.setSelectedIndex(i);
				break;
			}
		}
		
		if (!rRNAFeaturesBox.getSelectedItem().equals("rRNA")) {
			measureRRNABox.setSelected(false);
			rRNAFeaturesBox.setEnabled(false);
		}
		
		choicePanel.add(rRNAFeaturesBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;

		choicePanel.add(new JLabel("Chrs to measure"),gbc);
		gbc.gridx++;
		
		
		chromosomeList = new JList(collection.genome().getAllChromosomes());
		chromosomeList.setPrototypeCellValue("No longer than this please");
		ArrayList<Integer> indicesToUse = new ArrayList<Integer>();
		for (int i=0;i<chromosomeList.getModel().getSize();i++) {
			if (chromosomeList.getModel().getElementAt(i).toString().toUpperCase().startsWith("MT")  
					|| chromosomeList.getModel().getElementAt(i).toString().toUpperCase().equals("M")
					|| chromosomeList.getModel().getElementAt(i).toString().toUpperCase().equals("ERCC")
					) {
				indicesToUse.add(i);
			}
		}
	
		int [] indicesToUseArray = new int [indicesToUse.size()];
		for (int i=0;i<indicesToUseArray.length;i++) {
			indicesToUseArray[i] = indicesToUse.get(i);
		}
		chromosomeList.setSelectedIndices(indicesToUseArray);
	
		choicePanel.add(new JScrollPane(chromosomeList),gbc);
		
		getContentPane().add(choicePanel,BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		
		JButton plotButton = new JButton("Create Plot");
		plotButton.setActionCommand("plot");
		plotButton.addActionListener(this);
		buttonPanel.add(plotButton);
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		
		setSize(500,500);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("plot")) {
			
			String geneFeatures = (String)geneFeaturesBox.getSelectedItem();
			String transcriptFeatures = null;
			
			if (measureTranscriptsBox.isSelected()) {
				transcriptFeatures= (String)transcriptFeaturesBox.getSelectedItem();
			}
			
			String rRNAFeatures = null;
			if (measureRRNABox.isSelected()) {
				rRNAFeatures = (String)rRNAFeaturesBox.getSelectedItem();
			}
			
			Object [] selectedChromosomes = chromosomeList.getSelectedValues();
			
			Chromosome  [] chromosomes = new Chromosome[selectedChromosomes.length];
			
			for (int c=0;c<chromosomes.length;c++) {
				chromosomes[c] = (Chromosome)selectedChromosomes[c];
			}
			
			RNAQCCalcualtor calc = new RNAQCCalcualtor(collection, geneFeatures, transcriptFeatures, rRNAFeatures, chromosomes, stores);
			calc.addListener(this);
			calc.addListener(new ProgressDialog("RNA-Seq QC Plot", calc));
			calc.startCalculating();

			setVisible(false);

		}
	}

	public void progressExceptionReceived(Exception e) {
		dispose();
	}

	public void progressWarningReceived(Exception e) {}

	public void progressUpdated(String message, int current, int max) {}

	public void progressCancelled() {
		setVisible(true);
	}

	public void progressComplete(String command, Object result) {
		new RNAQCResultsDialog((RNAQCResult)result);
		dispose();
	}

	
	
	
}
