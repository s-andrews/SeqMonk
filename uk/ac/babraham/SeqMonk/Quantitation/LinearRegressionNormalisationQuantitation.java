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
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A quantitation which calculates a linear line between two 
 * percentiles and then flattens the data around that line.
 */
public class LinearRegressionNormalisationQuantitation extends Quantitation {

	private JPanel optionPanel = null;
	private DataStore [] data = null;
	private JTextField lowerPercentileField;
	private JTextField upperPercentileField;
	private JCheckBox ignoreUnquantitatedBox;
	private JComboBox calculateFromProbeList;
	private float lowerPercentile = 20;
	private float upperPercentile = 90;

	public LinearRegressionNormalisationQuantitation(SeqMonkApplication application) {
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

		float [] lowerPercentileValues = new float[data.length];
		float [] upperPercentileValues = new float[data.length];

		// Work out the value at the appropriate percentile
		for (int d=0;d<data.length;d++) {

			progressUpdated("Calculating correction for "+data[d].name(),d, data.length);

			float [] theseValues = new float[calculateProbes.length];
			for (int p=0;p<calculateProbes.length;p++) {
				try {
					theseValues[p] = data[d].getValueForProbe(calculateProbes[p]);
				} 
				catch (SeqMonkException e) {
					progressExceptionReceived(e);
				}
			}

			Arrays.sort(theseValues);

			lowerPercentileValues[d] = getPercentileValue(theseValues, lowerPercentile);
			upperPercentileValues[d] = getPercentileValue(theseValues, upperPercentile);
						
		}

		
		for (int d=0;d<data.length;d++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated(d, data.length);
						
			// Apply the correction to all probes.  We apply two corrections.  The
			// First is an addition for the lower percentile to put it at zero.  The
			// second is a linearly scaled addition based on the distance of the values
			// from the lower to the upper percentile.
			
						
			try {
				
				// To make this work we need to order all of the probes since this provides
				// the linear distance we're going to use for scaling.
			
				final DataStore compareStore = data[d];
				
				Arrays.sort(allProbes,new Comparator<Probe>() {
					@Override
					public int compare(Probe o1, Probe o2) {
						try {
							return(Float.compare(compareStore.getValueForProbe(o1),compareStore.getValueForProbe(o2)));
						} catch (SeqMonkException e) {return 0;}
					}
				});
				
				
				// We need to find the index for lower and upper in the full set of probes
				
				int lowerIndex = -1;
				int upperIndex = -1;
				
				for (int p=0;p<allProbes.length;p++) {
					// See if we need to quit
					if (cancel) {
						progressCancelled();
						return;
					}

					float probeValue = data[d].getValueForProbe(allProbes[p]);
					
					if (lowerIndex < 0 && probeValue >= lowerPercentileValues[d]) {
						lowerIndex = p;
					}
					if (upperIndex < 0 && probeValue >= upperPercentileValues[d]) {
						upperIndex = p;
						break;
					}
					
				}
				
				// Now we can go through and assign the values
				for (int p=0;p<allProbes.length;p++) {

					// See if we need to quit
					if (cancel) {
						progressCancelled();
						return;
					}

					float probeValue = data[d].getValueForProbe(allProbes[p]);
					
					float newValue = probeValue - lowerPercentileValues[d];
					
					float scaling = (p - (float)lowerIndex) / (upperIndex - lowerIndex);
										
					scaling *= (upperPercentileValues[d]-lowerPercentileValues[d]);					
					
					newValue -= scaling;
					
					data[d].setValueForProbe(allProbes[p], newValue);					
				}
			}
			catch (SeqMonkException e) {
				progressExceptionReceived(e);
			}	

		}	
		quantitatonComplete();
	}


	private float getPercentileValue (float [] allValues, float percentile) {

		int actualLength = allValues.length-1;

		if (ignoreUnquantitatedBox.isSelected()) {
			// We find the last index which is a valid number (NaN values
			// sort after real values).
			for (int i=allValues.length-1;i>=0;i--) {
				if (! Float.isNaN(allValues[i])) {
					actualLength = i;
					break;
				}
			}
		}

		return(allValues[(int)((actualLength*percentile)/100)]);


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
		
		optionPanel.add(new JLabel("Lower Percentile"),gbc);


		lowerPercentileField = new JTextField(""+lowerPercentile);
		lowerPercentileField.addKeyListener(new NumberKeyListener(true, false));

		gbc.gridx++;

		optionPanel.add(lowerPercentileField,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Upper Percentile"),gbc);
		gbc.gridx = 2;
		upperPercentileField = new JTextField(""+upperPercentile);
		upperPercentileField.addKeyListener(new NumberKeyListener(true, false));
		optionPanel.add(upperPercentileField,gbc);

		gbc.gridx=1;
		gbc.gridy++;


		optionPanel.add(new JLabel("Ignore unquantitated probes"),gbc);

		ignoreUnquantitatedBox = new JCheckBox("",true);

		gbc.gridx++;

		optionPanel.add(ignoreUnquantitatedBox, gbc);

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

		try {
			lowerPercentile = Float.parseFloat(lowerPercentileField.getText());
			upperPercentile = Float.parseFloat(upperPercentileField.getText());
		}
		catch (Exception e) {
			return false;
		}

		if (lowerPercentile < 0 || lowerPercentile > 100) {
			return false;
		}

		if (upperPercentile < 0 || upperPercentile > 100) {
			return false;
		}

		if (upperPercentile <= lowerPercentile) return false;

		return true;

	}	


	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		return existingDescription+" transformed by linear regression quantitation from percentiles "+lowerPercentile+" to "+upperPercentile;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {
		
		// We don't need to check for quantitation as this is all handled by the
		// upstream code which calls us.

		this.data = data;

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
		return "Linear Regression Normalisation Quantitation";
	}


}