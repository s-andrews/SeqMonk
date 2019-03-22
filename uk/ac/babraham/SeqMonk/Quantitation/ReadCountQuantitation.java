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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;

/**
 * A quantitation based on the number of reads overlapping each probe
 */
public class ReadCountQuantitation extends Quantitation {

	private JPanel optionPanel = null;
	private JComboBox strandLimit;
		
	/** Do we do a total read count correction */
	private JCheckBox correctTotalBox;
	
	/** Which kinds of reads are we going to use */
	private QuantitationStrandType quantitationType;
	
	/** Do we correct for total read count */
	private static boolean correctTotal = true;
	
	/** Do we get our total read count only from within the probes defined */
	private JCheckBox correctOnlyInProbesBox;
	
	/** Do we correct to the largest dataset, or per million probes */
	private JComboBox correctToWhatBox;
	private boolean correctPerMillion = false;

	
	/** Do we get our total read count for correction just from within the probes we're quantitating. */
	private static boolean correctOnlyInProbes = false;
	
	/** Do we correct for the length of each probe */
	private JCheckBox correctLengthBox;
	
	/** Do we correct for the length of each probe */
	private static boolean correctLength = false;
	
	/** Do we log transform all counts */
	private JCheckBox logTransformBox;
	
	/** Do we log transform all counts */
	private static boolean logTransform = true;

	/** Do we want to ignore duplicated reads */
	private JCheckBox ignoreDuplicatesBox;
	private static boolean ignoreDuplicates = false;
		
	/** A fixed value to make log2 calculation quicker */
	private final float log2 = (float)Math.log(2);
	
	/** The stores we're going to quantitate. */
	private DataStore [] data;
		

	public ReadCountQuantitation(SeqMonkApplication application) {
		super(application);
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		this.data = data;
		
		this.quantitationType = (QuantitationStrandType)strandLimit.getSelectedItem();
		
		correctTotal = correctTotalBox.isSelected();
		if (correctToWhatBox.getSelectedItem().equals("Per Million Reads")) {
			correctPerMillion = true;
		}
		else {
			correctPerMillion = false;
		}

		correctLength = correctLengthBox.isSelected();
		correctOnlyInProbes = correctOnlyInProbesBox.isSelected();
		logTransform = logTransformBox.isSelected();
		ignoreDuplicates = ignoreDuplicatesBox.isSelected();
		quantitationType.setIgnoreDuplicates(ignoreDuplicates);
		
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
		
		optionPanel.add(new JLabel("Count reads on strand"),gbc);
		
		gbc.gridx = 2;
		strandLimit = new JComboBox(QuantitationStrandType.getTypeOptions());
		optionPanel.add(strandLimit,gbc);
		
		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Correct for total read count"),gbc);
		
		gbc.gridx = 2;
		correctTotalBox = new JCheckBox();
		correctTotalBox.setSelected(correctTotal);
		correctTotalBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (correctTotalBox.isSelected()) {
					correctOnlyInProbesBox.setEnabled(true);
					correctToWhatBox.setEnabled(true);
				}
				else {
					correctOnlyInProbesBox.setEnabled(false);
					correctToWhatBox.setEnabled(false);
				}
		
			}
		});
		
		optionPanel.add(correctTotalBox,gbc);

		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Correct to what?"),gbc);
		
		gbc.gridx = 2;
		correctToWhatBox = new JComboBox(new String [] {"Largest DataStore","Per Million Reads"});
		correctToWhatBox.setEnabled(correctTotal);
		optionPanel.add(correctToWhatBox,gbc);

		
		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Count total only within probes"),gbc);
		
		gbc.gridx = 2;
		correctOnlyInProbesBox = new JCheckBox();
		correctOnlyInProbesBox.setSelected(correctOnlyInProbes);
		correctOnlyInProbesBox.setEnabled(correctTotal);
		optionPanel.add(correctOnlyInProbesBox,gbc);

		gbc.gridx = 1;
		gbc.gridy++;
		
		
		optionPanel.add(new JLabel("Correct for probe length"),gbc);
		
		gbc.gridx = 2;
		correctLengthBox = new JCheckBox();
		correctLengthBox.setSelected(correctLength);
		optionPanel.add(correctLengthBox,gbc);

		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Log Transform Count"),gbc);
		
		gbc.gridx = 2;
		logTransformBox = new JCheckBox();
		logTransformBox.setSelected(logTransform);
		optionPanel.add(logTransformBox,gbc);
		
		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Count duplicated reads only once"),gbc);
		
		gbc.gridx = 2;
		ignoreDuplicatesBox = new JCheckBox();
		ignoreDuplicatesBox.setSelected(ignoreDuplicates);
		optionPanel.add(ignoreDuplicatesBox,gbc);
		
		return optionPanel;
	}
	
	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("Read Count Quantitation using ");
		sb.append(quantitationType.toString());
		
		if (correctTotal) {
			sb.append(" correcting for total count");
			if (correctOnlyInProbes) {
				sb.append(" only in probes");
			}
			
			if (correctPerMillion) {
				sb.append(" per million reads");
			}
			else {
				sb.append(" to largest store");
			}
		}
		
		if (correctLength) {
			sb.append(" corrected for probe length");
		}
		if (logTransform) {
			sb.append(" log transformed");
		}
		if (quantitationType.ignoreDuplicates()) {
			sb.append(" duplicates ignored");
		}
		
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
				
		float [] corrections = new float [data.length];
		if (correctTotal) {
			
			float largest = 0;
			
			if (correctPerMillion) {
				largest = 1000000;
			}

			
			for (int d=0;d<data.length;d++) {
				
				if (correctOnlyInProbes) {
					corrections[d] = getTotalCountInProbes(data[d],probes);
				}
				else {
					corrections[d] = data[d].getTotalReadCount();
				}
				if (d==0 && !correctPerMillion) {
					largest = corrections[d];
				}
				else {
					if (!correctPerMillion && corrections[d]>largest) {
						largest = corrections[d];
					}
				}
			}
			
			// We correct everything by the largest count
			for (int d=0;d<corrections.length;d++) {
				corrections[d] = largest/corrections[d];
			}
			
		}

		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			
			double lengthCorrection = 1;
			if (correctLength) {
				// We assume a 'normal' probe length of 1kb
				lengthCorrection = (double)1000/probes[p].length();
			}

			progressUpdated(p, probes.length);
			
			for (int d=0;d<data.length;d++) {
				
				quantitationType.resetLastRead();
				
				// We initially make this a double so we don't hit the
				// limit of int counts (2^23) if we use a float.  This
				// will still break later, but if we're log transforming
				// then using a double here will save us.
				double count = 0;

				long [] reads = data[d].getReadsForProbe(probes[p]);

				for (int r=0;r<reads.length;r++) {
					
					// Check if we can ignore this one					
					if (quantitationType.useRead(probes[p], reads[r])) {
						++count;
					}
				}
				
				/*
				 * Log transforming is a pain due to infinite values coming
				 * from zero counts. We've tried a few different solutions but the
				 * one we're going with now is that if we're log transforming then
				 * we set zero counts to 0.9 counts.  All of the subsequent 
				 * corrections for total read count and length are then applied as
				 * normal.  The downside of this is that zero counts end up with
				 * different values in different datasets (due to total count correction)
				 * and a range of values in the same dataset (due to read length correction)
				 * but at least we are guaranteed that the zero counts are always 
				 * lower than the probes which actually have a count.
				 */
				
				if (logTransform && count == 0) {
					count = 0.9;
				}
								
				if (correctTotal) {
					count *= corrections[d];
				}
				
				if (correctLength) {
					count *= lengthCorrection;
				}
								
				if (logTransform) {
					count = (float)Math.log(count)/log2;
				}
				
				data[d].setValueForProbe(probes[p], (float)count);					
					
				
			}
			
		}

		quantitatonComplete();
		
	}
	
	/**
	 * Gets the count for the number of reads overlapping a set of probes.  Used
	 * to calculate a total count correction just from within the current probeset.
	 * 
	 * @param store The dataStore to use
	 * @param probes The set of probe to count in
	 * @return The total number of reads overlapping any probe in the set
	 */
	private int getTotalCountInProbes (DataStore store, Probe [] probes) {
		int total = 0;
		
		for (int p=0;p<probes.length;p++) {
			total += store.getReadsForProbe(probes[p]).length;
		}
		
		return total;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Read Count Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
