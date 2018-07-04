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
package uk.ac.babraham.SeqMonk.Quantitation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

/**
 * A quantitation based on the number of reads exactly overlapping each probe
 */
public class ExactOverlapQuantitation extends Quantitation {

	private JPanel optionPanel = null;
	private JComboBox strandLimit;
		
	/** Do we do a total read count correction */
	private JCheckBox correctTotalBox;
	
	/** Which kinds of reads are we going to use */
	private QuantitationStrandType quantitationType;
	
	/** Do we correct for total read count */
	private boolean correctTotal;
	
	/** Do we get our total read count only from within the probes defined */
	private JCheckBox correctOnlyInProbesBox;
	
	/** Do we correct to the largest dataset, or per million probes */
	private JComboBox correctToWhatBox;
	private boolean correctPerMillion = false;
	
	/** Do we get our total read count for correction just from within the probes we're quantitating. */
	private boolean correctOnlyInProbes;
		
	/** Do we log transform all counts */
	private JCheckBox logTransformBox;
	
	/** Do we log transform all counts */
	private boolean logTransform;
		
	/** A fixed value to make log2 calculation quicker */
	private final float log2 = (float)Math.log(2);
	
	/** The stores we're going to quantitate. */
	private DataStore [] data;
		
	public ExactOverlapQuantitation(SeqMonkApplication application) {
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

		correctOnlyInProbes = correctOnlyInProbesBox.isSelected();
		logTransform = logTransformBox.isSelected();
		quantitationType.setIgnoreDuplicates(false);
		
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
		correctTotalBox.setSelected(false);
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
		correctToWhatBox.setEnabled(false);
		optionPanel.add(correctToWhatBox,gbc);

		
		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Count total only within probes"),gbc);
		
		gbc.gridx = 2;
		correctOnlyInProbesBox = new JCheckBox();
		correctOnlyInProbesBox.setSelected(false);
		correctOnlyInProbesBox.setEnabled(false);
		optionPanel.add(correctOnlyInProbesBox,gbc);

		gbc.gridx = 1;
		gbc.gridy++;
		
		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Log Transform Count"),gbc);
		
		gbc.gridx = 2;
		logTransformBox = new JCheckBox();
		logTransformBox.setSelected(false);
		optionPanel.add(logTransformBox,gbc);
				
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}
	
	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("Exact Overlap Quantitation using ");
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
		
		if (logTransform) {
			sb.append(" log transformed");
		}
		
		return sb.toString();
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
		
		
		// To make this more efficient we'll do this chromosome by chromosome
		Chromosome [] chrs = application.dataCollection().genome().getAllChromosomes();
		
		for (int c=0;c<chrs.length;c++) {
			
			progressUpdated("Quantiating probes on "+chrs[c].name(),c,chrs.length);
						
			Probe [] thisChrProbes = application.dataCollection().probeSet().getProbesForChromosome(chrs[c]);
			Arrays.sort(thisChrProbes);
			
			for (int d=0;d<data.length;d++) {
			
				if (cancel) {
					progressCancelled();
					return;
				}
				
				// We'll fetch all reads for this chr and then do a count per position
				
				ReadsWithCounts reads = data[d].getReadsForChromosome(chrs[c]);
								
				quantitationType.resetLastRead();
				
				int startIndex = 0;
				
				for (int p=0;p<thisChrProbes.length;p++) {
					
					int rawCount = 0;
					
					for (int r=startIndex;r<reads.reads.length;r++) {
						if (SequenceRead.start(reads.reads[r]) < thisChrProbes[p].start()) {
							startIndex = r;
						}
						
						if (SequenceRead.start(reads.reads[r]) > thisChrProbes[p].start()) break;
						
						if (quantitationType.useRead(thisChrProbes[p], reads.reads[r])) {
							if (SequenceRead.start(reads.reads[r])==thisChrProbes[p].start() && SequenceRead.end(reads.reads[r])==thisChrProbes[p].end()) {
								rawCount += reads.counts[r];
							}
						}
					}
					
					// We have the counts now work out any correction.
					float count = rawCount;
					
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
					
					
					if (logTransform && count==0) {
						count=0.9f;
					}
									
					if (correctTotal) {
						count *= corrections[d];
					}
					
					if (logTransform) {
						count = (float)Math.log(count)/log2;
					}
					
					data[d].setValueForProbe(thisChrProbes[p], count);					
						
				}
				
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
			long [] reads = store.getReadsForProbe(probes[p]);
			for (int r=0;r<reads.length;r++) {
				if (SequenceRead.start(reads[r]) == probes[p].start() && SequenceRead.end(reads[r])==probes[p].end()) {
					total += store.getReadsForProbe(probes[p]).length;
				}
			}	
		}
		
		return total;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Exact Overlap Count Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
