/**
 * Copyright Copyright 2010- 21 Simon Andrews
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
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * A quantitaion method based on how many bases of seqence read
 * overlap a given probe.
 */
public class BasePairQuantitation extends Quantitation {

	private DataCollection collection;
	private JPanel optionPanel = null;
	private JComboBox strandLimit;
	private QuantitationStrandType quantitationType = null;
	
	/** Do we do a total read count correction */
	private JCheckBox correctTotalBox;
	
	private static boolean correctTotal = true;
	
	/** Do we get our total read count only from within the probes defined */
	private JCheckBox correctOnlyInProbesBox;	
	private static boolean correctOnlyInProbes;
	
	/** Do we correct to the largest dataset, or per million probes */
	private JComboBox correctToWhatBox;
	private static boolean correctPerMillion = false;
	
	/** Do we correct for the length of each probe */
	private JCheckBox correctLengthBox;
	private static boolean correctLength = false;
	
	/** Do we log transform all counts */
	private JCheckBox logTransformBox;
	private static boolean logTransform = true;

	/** Do we want to ignore duplicated reads */
	private JCheckBox ignoreDuplicatesBox;
	private static boolean ignoreDuplicates = false;
		
	/** The stores we're going to quantitate. */
	private DataStore [] data;
			

	public BasePairQuantitation(SeqMonkApplication application) {
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
	
	public void quantitate (DataCollection collection, DataStore [] data, QuantitationStrandType quantitationType, boolean correctTotal, boolean correctPerMillion, boolean correctLength, boolean correctOnlyInProbes, boolean logTransform, boolean ignoreDuplicates) {
		this.collection = collection;
		this.data = data;
		this.quantitationType = quantitationType;
		
		BasePairQuantitation.correctTotal = correctTotal;
		BasePairQuantitation.correctPerMillion = correctPerMillion;

		BasePairQuantitation.correctLength = correctLength;
		BasePairQuantitation.correctOnlyInProbes = correctOnlyInProbes;
		BasePairQuantitation.logTransform = logTransform;
		BasePairQuantitation.ignoreDuplicates = ignoreDuplicates;
		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
		
	}
	
	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("Base Pair Quantitation using ");
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
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {
		this.collection = application.dataCollection();
		
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
		
		Probe [] probes = collection.probeSet().getAllProbes();
				
		double [] corrections = new double [data.length];
		if (correctTotal) {
			
			double largest = 0;
			
			if (correctPerMillion) {
				largest = 1000000;
			}
			

			for (int d=0;d<data.length;d++) {

				progressUpdated("Working out correction for "+data[d].name(), d, data.length);

				if (correctOnlyInProbes) {
					corrections[d] = getTotalLengthInProbes(data[d],probes);
				}
				else {
					corrections[d] = data[d].getTotalReadCount();
				}
				if (d==0  && ! correctPerMillion) {
					largest = corrections[d];
				}
				else {
					if (! correctPerMillion && corrections[d]>largest) largest = corrections[d];
				}
			}
			
			// We correct everything by the largest length (or 1million if they chose that)
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
				// We correct per bp of probe
				lengthCorrection = 1d/probes[p].length();
			}

			progressUpdated(p, probes.length);
			
			for (int d=0;d<data.length;d++) {
				
				// Make sure we don't clash with previous probes
				// in our duplicate detection
				quantitationType.resetLastRead();


				// Since the length counts can get bigger than an int and a float
				// doesn't have the granularity to increment this big we do the
				// initial count in a long
				long rawCount = 0;
				
				long [] reads = data[d].getReadsForProbe(probes[p]);
				
				for (int r=0;r<reads.length;r++) {
					if (quantitationType.useRead(probes[p], reads[r])) rawCount += getOverlap(reads[r],probes[p]);
				}
				
				// Since the final stored value will be a float we do the conversion
				// now before we apply any corrections
				double count = rawCount;
				
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
				
				if (logTransform  && count == 0) {
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
	 * Gets size of the overlap (in bp) between a read and a probe
	 * 
	 * @param read
	 * @param probe
	 * @return The size of the overlap (in bp)
	 */
	private int getOverlap (long read, Probe probe) {
		return 1+ (Math.min(SequenceRead.end(read),probe.end())-Math.max(SequenceRead.start(read),probe.start()));
	}
	
	/**
	 * Gets the total length of all reads overlapping a set of probes.
	 * 
	 * @param store The dataStore containing the reads to use
	 * @param probes The set of probes
	 * @return The total length, in bp, of all reads overlapping the probes
	 */
	private long getTotalLengthInProbes (DataStore store, Probe [] probes) {
		long total = 0;
		
		//TODO: Should we be using the same filters here as for the calculation (strand, duplicates etc?)
		
		for (int p=0;p<probes.length;p++) {
			long [] reads = store.getReadsForProbe(probes[p]);
			for (int r=0;r<reads.length;r++) {
				total += getOverlap(reads[r], probes[p]);
			}
		}
		
		return total;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Base Pair Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
