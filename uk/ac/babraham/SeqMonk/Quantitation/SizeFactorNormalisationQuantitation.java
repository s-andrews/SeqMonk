/**
 * Copyright Copyright 2010-19 Simon Andrews and 2009 Ieuan Clay
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

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * A quantitation which corrects all dataStores relative to an averaged
 * reference based on the median difference between them.
 */

public class SizeFactorNormalisationQuantitation extends Quantitation {

	private static final int ADD = 1;
	private static final int MULTIPLY = 3;

	private JPanel optionPanel = null;
	private JComboBox correctionActions;
	private DataStore [] data = null;
	private int correctionAction;
	private JComboBox calculateFromProbeList;

	public SizeFactorNormalisationQuantitation(SeqMonkApplication application) {
		super(application);
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		if (! isReady()) {
			progressExceptionReceived(new SeqMonkException("Options weren't set correctly"));
		}

		Probe [] allProbes = application.dataCollection().probeSet().getAllProbes();

		Probe [] calculateProbes = ((ProbeList)calculateFromProbeList.getSelectedItem()).getAllProbes();


		// We don't want to use probes which have a zero count.  Since we don't know what
		// transformation they've done to their data we're going to assume that the lowest
		// value in each dataset is the zero value, and we'll ignore anything which has 
		// that value, whatever it is.

		float [] lowestValues = new float[data.length];

		for (int p=0;p<calculateProbes.length;p++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			if (p % 1000 == 0) {
				progressUpdated("Calculating baseline values",p,calculateProbes.length*3);
			}

			try {
				for (int d=0;d<data.length;d++) {
					if (p==0) {
						lowestValues[d] = data[d].getValueForProbe(calculateProbes[p]);
					}
					else {
						if (data[d].getValueForProbe(calculateProbes[p]) < lowestValues[d]) {
							lowestValues[d] = data[d].getValueForProbe(calculateProbes[p]);
						}
					}
				}
			}
			catch(SeqMonkException sme) {
				progressExceptionReceived(sme);
				return;
			}
		}
		
//		for (int d=0;d<lowestValues.length;d++) {
//			System.err.println("Lowest value for "+data[d]+" was "+lowestValues[d]);
//		}

		// Now we want to build the average reference and record which probes we 
		// used to make it.

		boolean [] useProbe = new boolean[calculateProbes.length];
		for (int i=0;i<useProbe.length;i++)useProbe[i] = true;


		double [] averageValues = new double[calculateProbes.length];

		for (int p=0;p<calculateProbes.length;p++) {

			if (p % 1000 == 0) {
				progressUpdated("Calculating average store",p+calculateProbes.length,calculateProbes.length*3);
			}

			if (cancel) {
				progressCancelled();
				return;
			}
			try {
				for (int d=0;d<data.length;d++) {
					if (data[d].getValueForProbe(calculateProbes[p]) <= lowestValues[d]) {
						useProbe[p] = false;
						break;
					}
					averageValues[p] += data[d].getValueForProbe(calculateProbes[p]);
				}
			}
			catch (SeqMonkException sme) {
				progressExceptionReceived(sme);
				return;
			}
		}

		// We have the sum of values in the array now.  Divide by the 
		// number of stores to get the mean.
		for (int i=0;i<averageValues.length;i++) {
			averageValues[i] /= data.length;
		}

		// Time for a sanity check.  We need to know if we have any probes
		// left to work with.

		int validProbeCount = 0;
		for (int i=0;i<useProbe.length;i++) {
			if (useProbe[i]) ++validProbeCount;
		}

		System.err.println("Valid probe count was "+validProbeCount);
		
		if (validProbeCount == 0) {
			progressExceptionReceived(new SeqMonkException("All probes had at least one unusable (normally zero) value. Can't continue"));
		}

		// Finally we can go through the data and calculate the specific
		// correction factors to apply the adjustments.

		double [] correctionFactors = new double[data.length];

		for (int d=0;d<data.length;d++) {

			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated(d, data.length);

			// Get the set of differences for this data

			double [] differences = new double[validProbeCount];

			int lastIndex = 0;
			try {
				for (int p=0;p<calculateProbes.length;p++) {
					if (!useProbe[p]) continue;

					if (correctionAction == ADD) {
						differences[lastIndex] = data[d].getValueForProbe(calculateProbes[p]) - averageValues[p];
					}
					else {
						differences[lastIndex] = data[d].getValueForProbe(calculateProbes[p]) / averageValues[p];
					}

					++lastIndex;
				}
			}
			catch (SeqMonkException sme) {
				progressExceptionReceived(sme);
				return;
			}

			// Quick sanity check
			if (lastIndex != validProbeCount) {
				throw new IllegalStateException("Last index didn't match expected number of valid probes ("+lastIndex+" vs "+validProbeCount+")");
			}

			// Find the correction factor
			correctionFactors[d] = SimpleStats.median(differences);

			System.err.println("Correction factor for "+data[d]+" is "+correctionFactors[d]);

			// More sanity checking...
			if (correctionAction == MULTIPLY) {
				if (correctionFactors[d] <= 0) {
					progressExceptionReceived(new SeqMonkException("Got a zero or negative correction factor for "+data[d]+". You should probably be using ADD instead of MULTIPLY to correct"));
				}
			}
		}		

		// Finally we can apply the corrections

		for (int d=0;d<data.length;d++){	
			// Apply the correction to all probes
			try {
				for (int p=0;p<allProbes.length;p++) {

					// See if we need to quit
					if (cancel) {
						progressCancelled();
						return;
					}

					if (correctionAction == ADD) {
						data[d].setValueForProbe(allProbes[p], (float)(data[d].getValueForProbe(allProbes[p])-correctionFactors[d]));					
					}
					else if (correctionAction == MULTIPLY) {
						data[d].setValueForProbe(allProbes[p], (float)(data[d].getValueForProbe(allProbes[p])/correctionFactors[d]));					
					}
				}
			}
			catch (SeqMonkException e) {
				progressExceptionReceived(e);
			}	

		}	
		quantitatonComplete();
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
		gbc.weighty=0.1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		optionPanel.add(new JLabel("Method of correction"),gbc);
		gbc.gridx = 2;
		correctionActions = new JComboBox(new String [] {"Add","Multiply"});
		optionPanel.add(correctionActions,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		optionPanel.add(new JLabel("Calculate from probe list"),gbc);
		ProbeList [] currentLists = application.dataCollection().probeSet().getAllProbeLists();
		calculateFromProbeList = new JComboBox(currentLists);
		calculateFromProbeList.setPrototypeDisplayValue("No longer than this please");
		for (int i=0;i<currentLists.length;i++) {
			if (currentLists[i] == application.dataCollection().probeSet().getActiveList()) {
				calculateFromProbeList.setSelectedIndex(i);
			}
		}

		gbc.gridx++;

		optionPanel.add(calculateFromProbeList,gbc);

		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {

		return true;

	}	


	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		return existingDescription+" transformed by size factor normalisation using "+correctionActions.getSelectedItem().toString()+" using the  "+calculateFromProbeList.getSelectedItem().toString()+" probe list";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {

		this.data = data;

		// We need to set the correction action
		if (correctionActions.getSelectedItem().equals("Add")) {
			correctionAction = ADD;
		}
		else if (correctionActions.getSelectedItem().equals("Multiply")) {
			correctionAction = MULTIPLY;
		}
		else {
			throw new IllegalArgumentException("Didn't undestand correction action "+correctionActions.getSelectedItem());
		}

		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Size Factor Normalisation Quantitation";
	}


}