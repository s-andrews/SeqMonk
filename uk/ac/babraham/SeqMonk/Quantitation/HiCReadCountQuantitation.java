/**
 * Copyright Copyright 2010-15 Simon Andrews
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
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * A quantitation based on the number of HiC read pairs overlapping each probe
 */
public class HiCReadCountQuantitation extends Quantitation {

	private JPanel optionPanel = null;

	/** Do we do a total read count correction */
	private JCheckBox correctTotalBox;

	/** Do we correct for total read count */
	private boolean correctTotal;

	/** Do we remove duplicate pairs */
	private boolean removeDuplicates;

	//	/** Do we get our total read count only from within the probes defined */
	//	private JCheckBox correctOnlyInProbesBox;

	/** Do we correct to the largest dataset, or per million probes */
	private JComboBox correctToWhatBox;
	private boolean correctPerMillion = false;


	//	/** Do we get our total read count for correction just from within the probes we're quantitating. */
	//	private boolean correctOnlyInProbes;

	/** Do we correct for the length of each probe */
	private JCheckBox correctLengthBox;

	/** Do we correct for the length of each probe */
	private boolean correctLength;

	/** Do we log transform all counts */
	private JCheckBox logTransformBox;

	/** Do we log transform all counts */
	private boolean logTransform;

	/** Do we want to ignore duplicated reads */
	private JCheckBox ignoreDuplicatesBox;

	/** A fixed value to make log2 calculation quicker */
	private final float log2 = (float)Math.log(2);

	/** The stores we're going to quantitate. */
	private HiCDataStore [] data;


	public HiCReadCountQuantitation(SeqMonkApplication application) {
		super(application);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {

		Vector<HiCDataStore> hiCDataStores = new Vector<HiCDataStore>();
		for (int d=0;d<data.length;d++) {
			if (data[d] instanceof HiCDataStore && ((HiCDataStore)data[d]).isValidHiC()) {
				hiCDataStores.add((HiCDataStore)data[d]);
			}
		}

		this.data = hiCDataStores.toArray(new HiCDataStore[0]);

		correctTotal = correctTotalBox.isSelected();
		if (correctToWhatBox.getSelectedItem().equals("Per Million Reads")) {
			correctPerMillion = true;
		}
		else {
			correctPerMillion = false;
		}

		correctLength = correctLengthBox.isSelected();
		//		correctOnlyInProbes = correctOnlyInProbesBox.isSelected();
		logTransform = logTransformBox.isSelected();


		removeDuplicates = ignoreDuplicatesBox.isSelected();

		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	public boolean requiresHiC () {
		return true;
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

		optionPanel.add(new JLabel("Correct for total read count"),gbc);

		gbc.gridx = 2;
		correctTotalBox = new JCheckBox();
		correctTotalBox.setSelected(false);
		correctTotalBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (correctTotalBox.isSelected()) {
					//					correctOnlyInProbesBox.setEnabled(true);
					correctToWhatBox.setEnabled(true);
				}
				else {
					//					correctOnlyInProbesBox.setEnabled(false);
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


		//		gbc.gridx = 1;
		//		gbc.gridy++;
		//
		//		optionPanel.add(new JLabel("Count total only within probes"),gbc);
		//		
		//		gbc.gridx = 2;
		//		correctOnlyInProbesBox = new JCheckBox();
		//		correctOnlyInProbesBox.setSelected(false);
		//		correctOnlyInProbesBox.setEnabled(false);
		//		optionPanel.add(correctOnlyInProbesBox,gbc);

		gbc.gridx = 1;
		gbc.gridy++;


		optionPanel.add(new JLabel("Correct for probe length"),gbc);

		gbc.gridx = 2;
		correctLengthBox = new JCheckBox();
		correctLengthBox.setSelected(false);
		optionPanel.add(correctLengthBox,gbc);

		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Log Transform Count"),gbc);

		gbc.gridx = 2;
		logTransformBox = new JCheckBox();
		logTransformBox.setSelected(false);
		optionPanel.add(logTransformBox,gbc);

		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Count duplicated reads only once"),gbc);

		gbc.gridx = 2;
		ignoreDuplicatesBox = new JCheckBox();
		ignoreDuplicatesBox.setSelected(false);
		optionPanel.add(ignoreDuplicatesBox,gbc);

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
		sb.append("HiC Read Count Quantitation ");

		if (correctTotal) {
			sb.append(" correcting for total count");

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
		if (ignoreDuplicatesBox.isSelected()) {
			sb.append(" duplicates ignored");
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

				//				if (correctOnlyInProbes) {
				//					corrections[d] = getTotalCountInProbes(data[d],probes);
				//				}
				//				else {
				corrections[d] = ((DataStore)data[d]).getTotalReadCount();
				//				}
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

				double count = 0;

				HiCHitCollection hiCHits = data[d].getHiCReadsForProbe(probes[p]);

				String [] chromosomeNames = hiCHits.getChromosomeNamesWithHits();

				for (int c=0;c<chromosomeNames.length;c++) {

					long [] sourceReads = hiCHits.getSourcePositionsForChromosome(chromosomeNames[c]);
					long [] hitReads = hiCHits.getHitPositionsForChromosome(chromosomeNames[c]);
					
					for (int r=0;r<sourceReads.length;r++) {

						// Check if we can ignore this one
						if (removeDuplicates) {
							if (r>0 && sourceReads[r] == sourceReads[r-1] && hitReads[r] == hitReads[r-1]) continue;
						}

						// If this is a reversed read pair and the other end is
						// also in this probe then don't count it.

						if (SequenceRead.overlaps(hitReads[r], probes[p].packedPosition()) && SequenceRead.compare(hitReads[r], sourceReads[r])> 0) {
							continue;
						}
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


				// TODO: This is icky since the inheritance between HiCDataStore and DataStore
				// isn't properly sorted out.
				((DataStore)data[d]).setValueForProbe(probes[p], (float)count);					


			}

		}

		quantitatonComplete();

	}

	//	/**
	//	 * Gets the count for the number of reads overlapping a set of probes.  Used
	//	 * to calculate a total count correction just from within the current probeset.
	//	 * 
	//	 * @param store The dataStore to use
	//	 * @param probes The set of probe to count in
	//	 * @return The total number of reads overlapping any probe in the set
	//	 */
	//	private int getTotalCountInProbes (DataStore store, Probe [] probes) {
	//		int total = 0;
	//		
	//		for (int p=0;p<probes.length;p++) {
	//			total += store.getReadsForProbe(probes[p]).length;
	//		}
	//		
	//		return total;
	//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "HiC Read Count Quantitation";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
