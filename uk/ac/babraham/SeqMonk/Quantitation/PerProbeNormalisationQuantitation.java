/**
 * Copyright Copyright 2010- 21 Simon Andrews and 2009 Ieuan Clay
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

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * A quantitation which corrects all dataStores relative to a reference store.
 */
public class PerProbeNormalisationQuantitation extends Quantitation {

	private static final int SUBTRACT = 1;
	private static final int LOGDIVIDE = 2;
	private static final int SCALE = 3;
	private static final int DIVIDE = 4;


	private JPanel optionPanel = null;
	private JComboBox correctionActions;
	private JComboBox averageMethod;
	private DataStore [] data = null;
	private int correctionAction;

	public PerProbeNormalisationQuantitation(SeqMonkApplication application) {
		super(application);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();

		boolean useMean = averageMethod.getSelectedItem().equals("Mean");
		boolean useSum = averageMethod.getSelectedItem().equals("Sum");
		
		try {
			for (int p=0;p<probes.length;p++) {

				// See if we need to quit
				if (cancel) {
					progressCancelled();
					return;
				}

				progressUpdated(p, probes.length);

				double [] values = new double [data.length];

				for (int d=0;d<data.length;d++) {
					values[d] = data[d].getValueForProbe(probes[p]);
				}

				
				float summaryValue = 0;
				
				if (useMean) {
					summaryValue = (float)SimpleStats.mean(values);
				}
				else if (useSum) {
					for (int i=0;i<values.length;i++) {
						if (values[i] < 0) {
							progressWarningReceived(new SeqMonkException("Negative values found when taking the sum - transformed values won't make sense"));
						}
						summaryValue += values[i];
					}
				}
				else {
					summaryValue  = (float)SimpleStats.median(values);
				}
				
				if (correctionAction == SUBTRACT) {
					for (int d=0;d<data.length;d++) {
						data[d].setValueForProbe(probes[p], data[d].getValueForProbe(probes[p])-summaryValue);
					}
				}
				else if (correctionAction == LOGDIVIDE) {
					for (int d=0;d<data.length;d++) {
						data[d].setValueForProbe(probes[p], (float)(Math.log(data[d].getValueForProbe(probes[p])/summaryValue)/Math.log(2)));
					}
				}
				else if (correctionAction == DIVIDE) {
					for (int d=0;d<data.length;d++) {
						data[d].setValueForProbe(probes[p], (float)(data[d].getValueForProbe(probes[p])/summaryValue));
					}
				}
				else if (correctionAction == SCALE) {
					Arrays.sort(values);
					
					if (values[0] == values[values.length-1]) {
						for (int d=0;d<data.length;d++) {
							data[d].setValueForProbe(probes[p], 0.5f);
						}						
					}
					
					else {
						for (int d=0;d<data.length;d++) {
							data[d].setValueForProbe(probes[p], (float)((data[d].getValueForProbe(probes[p])-values[0])/(values[values.length-1]-values[0])));
						}
					}
				}

			}
		}
		catch (SeqMonkException e) {
			progressExceptionReceived(e);
		}

		quantitatonComplete();

	}
	
	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		return existingDescription+" per-probe normalised using "+correctionActions.getSelectedItem().toString();
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
		correctionActions = new JComboBox(new String [] {"Subtract","Divide","Log Divide","Scale"});
		optionPanel.add(correctionActions,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		optionPanel.add(new JLabel("Averaging Method"),gbc);
		gbc.gridx = 2;
		averageMethod = new JComboBox(new String [] {"Median","Mean","Sum"});
		optionPanel.add(averageMethod,gbc);
		
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {

		return true;

	}	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {

		Vector<DataStore>quantitatedStores = new Vector<DataStore>();

		for (int d=0;d<data.length;d++) {
			if (data[d].isQuantitated()) {
				quantitatedStores.add(data[d]);
			}
		}

		this.data = quantitatedStores.toArray(new DataStore[0]);

		// We need to set the correction action
		if (correctionActions.getSelectedItem().equals("Subtract")) {
			correctionAction = SUBTRACT;
		}
		else if (correctionActions.getSelectedItem().equals("Log Divide")) {
			correctionAction = LOGDIVIDE;
		}
		else if (correctionActions.getSelectedItem().equals("Divide")) {
			correctionAction = DIVIDE;
		}
		else if (correctionActions.getSelectedItem().equals("Scale")) {
			correctionAction = SCALE;
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
		return "Per Probe Normalisation Quantitation";
	}

}