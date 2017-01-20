/**
 * Copyright Copyright 2010-17 Simon Andrews
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
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A quantitation based on the ratio of hits to the previous and subsequent
 * probes
 */
public class HiCPrevNextQuantitation extends Quantitation {

	private JPanel optionPanel = null;

	/** Do we remove duplicate pairs */
	private boolean removeDuplicates;

	/** Do we want to ignore duplicated reads */
	private JCheckBox ignoreDuplicatesBox;

	private JTextField distanceField;
	private int distance = 2000000;

	private JTextField minCountField;
	private int minCount;

	/** The stores we're going to quantitate. */
	private HiCDataStore [] data;


	public HiCPrevNextQuantitation(SeqMonkApplication application) {
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

		removeDuplicates = ignoreDuplicatesBox.isSelected();

		if (distanceField.getText().length() == 0) {
			distance = 50000;
		}
		else {
			distance = Integer.parseInt(distanceField.getText());
		}


		if (minCountField.getText().length()==0) {
			minCount = 1;
		}
		else {
			minCount = Integer.parseInt(minCountField.getText());
		}

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

		optionPanel.add(new JLabel("Count duplicated reads only once"),gbc);

		gbc.gridx = 2;
		ignoreDuplicatesBox = new JCheckBox();
		ignoreDuplicatesBox.setSelected(false);
		optionPanel.add(ignoreDuplicatesBox,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Distance to measure around probe"),gbc);

		gbc.gridx = 2;
		distanceField = new JTextField(""+distance,7);
		distanceField.addKeyListener(new NumberKeyListener(false, false));
		optionPanel.add(distanceField,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Min Absolute Count"),gbc);

		gbc.gridx = 2;
		minCountField = new JTextField(""+minCount,7);
		minCountField.addKeyListener(new NumberKeyListener(false, false));
		optionPanel.add(minCountField,gbc);

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
		sb.append("HiC Prev Next Quantitation ");

		if (ignoreDuplicatesBox.isSelected()) {
			sb.append(" duplicates ignored");
		}

		sb.append(" over distance of ");
		sb.append(distance);

		sb.append(" min count of ");
		sb.append(minCount);

		return sb.toString();
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		Probe [] probes = application.dataCollection().probeSet().getAllProbes();

		Arrays.sort(probes);

		for (int p=0;p<probes.length;p++) {

			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			if (probes[p].start()-distance < 0 || probes[p].end()+distance > probes[p].chromosome().length()) {
				for (int d=0;d<data.length;d++) {
					((DataStore)data[d]).setValueForProbe(probes[p], 0);
				}
				continue;
			}

			// Make up a pseudo probe for the previous and next regions
			Probe previousProbe = new Probe(probes[p].chromosome(), probes[p].start()-distance, probes[p].start());
			Probe nextProbe = new Probe(probes[p].chromosome(),probes[p].end(),probes[p].end()+distance);


			progressUpdated(p, probes.length);

			for (int d=0;d<data.length;d++) {

				// Get the counts for the previous and next probes
				int previousTotalCount = data[d].getHiCReadCountForProbe(previousProbe);
				HiCHitCollection hiCHits = data[d].getHiCReadsForProbe(probes[p]);
				int nextTotalCount = data[d].getHiCReadCountForProbe(nextProbe);

				// If either the upstream or downstream regions don't have
				// any reads then give up on this one.
				if (previousTotalCount == 0 || nextTotalCount == 0) {
					((DataStore)data[d]).setValueForProbe(probes[p], 0);
					continue;
				}

				int previousCount=0;
				int nextCount=0;

				long [] hitPositions = hiCHits.getHitPositionsForChromosome(hiCHits.getSourceChromosomeName());
				
				SequenceRead.sort(hitPositions);

				for (int r=0;r<hitPositions.length;r++) {

					// Check if we can ignore this one
					if (removeDuplicates) {
						if (r>0 && hitPositions[r] == hitPositions[r-1]) continue;
					}
					
					// Check if the other end maps to either the prev or next probe
					if (SequenceRead.overlaps(hitPositions[r], previousProbe.packedPosition())) {
						++previousCount;
					}
					if (SequenceRead.overlaps(hitPositions[r], nextProbe.packedPosition())) {
						++nextCount;
					}
				}	
				
				// If both of the actual counts are zero then don't try to calculate a real value
				if (previousCount == 0 && nextCount == 0) {
					((DataStore)data[d]).setValueForProbe(probes[p], 0);
					continue;
				}
				
				
				// Basically what happens here is that we calculate a normal X^2 value and then
				// turn it into a negative depending on the direction of the deviation from the
				// expected proportion.
				
				// We need to calculate expected values for the two directions based on the proportion
				// of reads falling into those two regions in total.
				
				// Correct the counts based on the proportions of the two totals
				double expectedProportion = (previousTotalCount/(double)(nextTotalCount+previousTotalCount));

				int totalObservedCount = previousCount+nextCount;
				
				double expectedPreviousCount = totalObservedCount*expectedProportion;
				double expectedNextCount = totalObservedCount-expectedPreviousCount;
				
				// Now we can calculate the X^2 values to compare the expected and observed values
				
				double chisquare = (Math.pow(previousCount-expectedPreviousCount,2)/expectedPreviousCount)+(Math.pow(nextCount-expectedNextCount, 2)/expectedNextCount);

				// We now negate this count if the proportions bias towards the next count

				double observedProportion = (previousCount/(double)totalObservedCount);

				if (observedProportion > expectedProportion) {
					chisquare = 0-chisquare;
				}
				

//				System.err.println("Raw counts "+previousCount+","+nextCount+" Totals "+previousTotalCount+","+nextTotalCount+" Expected "+expectedPreviousCount+","+expectedNextCount+" Chi "+chisquare);


				// TODO: This is icky since the inheritance between HiCDataStore and DataStore
				// isn't properly sorted out.
				((DataStore)data[d]).setValueForProbe(probes[p], (float)chisquare);					

			}

		}

		quantitatonComplete();

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "HiC Prev/Next Quantitation";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
