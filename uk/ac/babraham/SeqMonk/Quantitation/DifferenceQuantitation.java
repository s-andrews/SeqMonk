/**
 * Copyright Copyright 2010-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Quantitation;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A Quantitation based on the relative number of forward / reverse / unknown
 * reads.
 */
public class DifferenceQuantitation extends Quantitation implements ItemListener {

	private static final int MINUS = 1;
	private static final int DIVIDE = 2;
	private static final int LOG_DIVIDE = 3;
	private static final int PERCENTAGE = 4;
		
	private JPanel optionPanel = null;
	private JComboBox subtractStartBox;
	private JComboBox subtractEndBox;
	private JComboBox diffTypeBox;
	private JCheckBox ignoreDuplicates;
	private JTextField minCountField;
	
	/** What kind of difference we're doing (taken from static values) */
	private int diffType;
	
	private int minCount;
	
	/** The first strand */
	private QuantitationStrandType startStrand;
	
	/** The second strand */
	private QuantitationStrandType endStrand;
	
	/** A constant to speed up log2 calculation */
	private final static float log2 = (float)Math.log(2);
	
	private DataStore [] data;
	

	public DifferenceQuantitation(SeqMonkApplication application) {
		super(application);
	}
		
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		
		this.data = data;
		
		startStrand = (QuantitationStrandType)subtractStartBox.getSelectedItem();
		endStrand = (QuantitationStrandType)subtractEndBox.getSelectedItem();
		startStrand.setIgnoreDuplicates(ignoreDuplicates.isSelected());
		endStrand.setIgnoreDuplicates(ignoreDuplicates.isSelected());
		
		minCount = 1;
		if (minCountField.getText().length()>0) {
			minCount = Integer.parseInt(minCountField.getText());
		}
			
		// Combine type
		if (diffTypeBox.getSelectedItem().equals("Minus")) {
			diffType = MINUS;
			minCount = 0; // Never filter for min count.
		}
		else if (diffTypeBox.getSelectedItem().equals("Divided by")) {
			diffType = DIVIDE;
		}
		else if (diffTypeBox.getSelectedItem().equals("Log Divided by")) {
			diffType = LOG_DIVIDE;
		}
		else if (diffTypeBox.getSelectedItem().equals("As Percentage of")) {
			diffType = PERCENTAGE;
		}
		else {
			throw new IllegalArgumentException("Unknown difference type '"+diffTypeBox.getSelectedItem()+"'");
		}

		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {		
		if (optionPanel != null) {
			// We've done this already
			return optionPanel;
		}
		
		optionPanel = new JPanel();
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		optionPanel.add(new JLabel("Calculate difference as"),gbc);
		
		gbc.gridx = 2;
		JPanel diffTypePanel1 = new JPanel();
		diffTypePanel1.setLayout(new BorderLayout());
		subtractStartBox = new JComboBox(QuantitationStrandType.getTypeOptions());
		subtractStartBox.setSelectedIndex(1);
		subtractStartBox.addItemListener(this);
		diffTypePanel1.add(subtractStartBox,BorderLayout.CENTER);
		optionPanel.add(diffTypePanel1,gbc);
		
		gbc.gridy++;
		JPanel diffTypePanel2 = new JPanel();
		diffTypePanel2.setLayout(new BorderLayout());
		diffTypeBox = new JComboBox(new String [] {"Minus","Divided by","Log Divided by","As Percentage of"});		
		diffTypeBox.setSelectedIndex(3);
		diffTypePanel2.add(diffTypeBox,BorderLayout.CENTER);
		optionPanel.add(diffTypePanel2,gbc);
		
		gbc.gridy++;
		JPanel diffTypePanel3 = new JPanel();
		diffTypePanel3.setLayout(new BorderLayout());
		subtractEndBox = new JComboBox(QuantitationStrandType.getTypeOptions());
		subtractEndBox.setSelectedIndex(0);
		subtractEndBox.addItemListener(this);
		diffTypePanel3.add(subtractEndBox,BorderLayout.CENTER);
		
		optionPanel.add(diffTypePanel3,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		
		optionPanel.add(new JLabel("Min count "),gbc);
		gbc.gridx=2;
		minCountField = new JTextField("10");
		minCountField.setEnabled(true);
		minCountField.addKeyListener(new NumberKeyListener(false, false));
		optionPanel.add(minCountField,gbc);

		diffTypeBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (diffTypeBox.getSelectedItem().equals("Minus")) {
					minCountField.setEnabled(false);
				}
				else {
					minCountField.setEnabled(true);
				}
			}
		});

		
		gbc.gridx=1;
		gbc.gridy++;
		
		optionPanel.add(new JLabel("Ignore duplicates "),gbc);
		gbc.gridx=2;
		ignoreDuplicates = new JCheckBox();
		optionPanel.add(ignoreDuplicates,gbc);
		
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		if (subtractStartBox.getSelectedItem() != subtractEndBox.getSelectedItem()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("Difference Quantitation measuring ");
		sb.append(subtractStartBox.getSelectedItem().toString());
		sb.append(" ");
		sb.append(diffTypeBox.getSelectedItem().toString());
		sb.append(" ");
		sb.append(subtractEndBox.getSelectedItem().toString());
		return sb.toString();
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
				
		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			
			progressUpdated(p, probes.length);

			for (int d=0;d<data.length;d++) {
				
				startStrand.resetLastRead();
				endStrand.resetLastRead();

				long [] reads = data[d].getReadsForProbe(probes[p]);
				int startCount = 0;
				int endCount = 0;
				
				// Go through all reads
				for (int r=0;r<reads.length;r++) {
					
					// See if we can add this read to the start count
					if (startStrand.useRead(probes[p], reads[r])) startCount++;

					// See if we can add this read to the end count
					if (endStrand.useRead(probes[p], reads[r])) endCount++;

				}
				
				// If we don't have enough data to make a measurement then skip it
				if (endCount < minCount) {
					data[d].setValueForProbe(probes[p], Float.NaN);
					continue;
				}
				
				float value;
				
				switch (diffType) {
					case MINUS : value = startCount - endCount; break;
					
					case DIVIDE : value = (startCount+1)/(float)(endCount+1); break;

					case LOG_DIVIDE : value = (float)Math.log((startCount+1)/(float)(endCount+1))/log2; break;

					case PERCENTAGE : 
						// We want to avoid infinite values
						if (endCount == 0) {
							if (startCount == 0) {
								value = 0;
							}
							else {
								value = 100;
							}
						}
						else {
							value = (startCount*100f)/endCount;
						}
						break;

					default: throw new IllegalArgumentException("Unknonwn difference type code "+diffType);
				}
				data[d].setValueForProbe(probes[p], value);
			}
			
		}

		quantitatonComplete();
		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Difference Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent ie) {
		optionsChanged();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
