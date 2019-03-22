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
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * A quantitation which corrects all dataStores relative to a sub list of probes.  This
 * is like a global normalisation but using only this subset.
 */
public class SubsetNormalisationQuantitation extends Quantitation {

	private static final int ADD = 1;
	private static final int MULTIPLY = 3;
	
	private static final int TARGET_LARGEST = 4;
	private static final int TARGET_ONE = 5;
	
	private static final int VALUE_SUM = 6;
	private static final int VALUE_MEAN = 7;

	private JPanel optionPanel = null;
	private JComboBox correctionActions;
	private JComboBox targetActions;
	private JComboBox summationActions;
	private DataStore [] data = null;
	private int correctionAction;
	private int targetAction;
	private int summationAction;
	private JComboBox calculateFromProbeList;

	public SubsetNormalisationQuantitation(SeqMonkApplication application) {
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

		float [] summedValues = new float[data.length];

		// Work out the value we're going to correct against.  We'll start by summing
		// since we need that whether or not they're taking the mean.
		for (int d=0;d<data.length;d++) {

			progressUpdated("Calculating correction for "+data[d].name(),d, data.length);

			for (int p=0;p<calculateProbes.length;p++) {
				try {
					summedValues[d] += data[d].getValueForProbe(calculateProbes[p]);
				} 
				catch (SeqMonkException e) {
					progressExceptionReceived(e);
				}
			}

		}
		
		// If they want to use the mean as the summation value then divide the 
		// sums we've calculated by the number of probes in the sublist we're 
		// using.
		
		if (summationAction == VALUE_MEAN) {
			for (int i=0;i<summedValues.length;i++) {
				summedValues[i] /= calculateProbes.length;
			}
		}
		

		float targetValue = 1;
		
		if (targetAction == TARGET_LARGEST) {
			targetValue = summedValues[0];
			for (int i=1;i<summedValues.length;i++) {
				if (summedValues[i] > targetValue) targetValue = summedValues[i];
			}
		}

		for (int d=0;d<data.length;d++) {

			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated(d, data.length);


			// Get the correction value
			float correctionFactor = 1;

			if (correctionAction == ADD) {
				correctionFactor = targetValue-summedValues[d];
			}
			else if (correctionAction == MULTIPLY) {
				correctionFactor = targetValue/summedValues[d];
			}
			
			System.err.println("For "+data[d].name()+" correction factor is "+correctionFactor);
			
			// Apply the correction to all probes

			try {
				for (int p=0;p<allProbes.length;p++) {

					// See if we need to quit
					if (cancel) {
						progressCancelled();
						return;
					}

					if (correctionAction == ADD) {
						data[d].setValueForProbe(allProbes[p], data[d].getValueForProbe(allProbes[p])+correctionFactor);					
					}
					else if (correctionAction == MULTIPLY) {
						data[d].setValueForProbe(allProbes[p], data[d].getValueForProbe(allProbes[p])*correctionFactor);					
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
		optionPanel.add(new JLabel("Summation Method"),gbc);
		gbc.gridx = 2;
		summationActions = new JComboBox(new String [] {"Mean","Sum"});
		optionPanel.add(summationActions,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		optionPanel.add(new JLabel("Target Value"),gbc);
		gbc.gridx = 2;
		targetActions = new JComboBox(new String [] {"Largest","1"});
		optionPanel.add(targetActions,gbc);
		
		
		Vector<DataStore>quantitatedStores = new Vector<DataStore>();

		DataSet [] sets = application.dataCollection().getAllDataSets();
		for (int s=0;s<sets.length;s++) {
			if (sets[s].isQuantitated()) {
				quantitatedStores.add(sets[s]);
			}
		}
		DataGroup [] groups = application.dataCollection().getAllDataGroups();
		for (int g=0;g<groups.length;g++) {
			if (groups[g].isQuantitated()) {
				quantitatedStores.add(groups[g]);
			}
		}

		data = quantitatedStores.toArray(new DataStore[0]);


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
		return existingDescription+" transformed by subset normalisation using "+correctionActions.getSelectedItem().toString()+" on the "+summationActions.getSelectedItem()+" value with a target of "+targetActions.getSelectedItem();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {

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
		
		// We need to set the summation action
		if (summationActions.getSelectedItem().equals("Mean")) {
			summationAction = VALUE_MEAN;
		}
		else if (summationActions.getSelectedItem().equals("Sum")) {
			summationAction = VALUE_SUM;
		}
		else {
			throw new IllegalArgumentException("Didn't undestand summation action "+summationActions.getSelectedItem());
		}
		
		// We need to set the target value
		if (targetActions.getSelectedItem().equals("Largest")) {
			targetAction = TARGET_LARGEST;
		}
		else if (targetActions.getSelectedItem().equals("1")) {
			targetAction = TARGET_ONE;
		}
		else {
			throw new IllegalArgumentException("Didn't undestand target action "+targetActions.getSelectedItem());
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
		return "Subset Normalisation Quantitation";
	}


}