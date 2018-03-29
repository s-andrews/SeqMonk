/**
 * Copyright Copyright 2010-18 Simon Andrews and 2009 Ieuan Clay
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
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A quantitation which corrects all dataStores relative to a reference store.
 */
public class PercentileNormalisationQuantitation extends Quantitation {

	private static final int ADD = 1;
	private static final int MULTIPLY = 3;

	private JPanel optionPanel = null;
	private JComboBox correctionActions;
	private DataStore [] data = null;
	private int correctionAction;
	private JTextField percentileField;
	private JCheckBox autoPercentileBox;
	private JCheckBox ignoreUnquantitatedBox;
	private JComboBox calculateFromProbeList;
	private float percentile = 75;

	public PercentileNormalisationQuantitation(SeqMonkApplication application) {
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
		
		float [][] percentileValues = new float[data.length][];
		float [] minValues = new float[data.length];

		// Work out the value at the appropriate percentile
		for (int d=0;d<data.length;d++) {

			// If we're using a fixed percentage then we just want a single value for the percentile values
			// Otherwise we'll calculate for each percentage (0-100)
			if (autoPercentileBox.isSelected()) {
				percentileValues[d] = new float[101];
			}
			else {
				percentileValues[d] = new float[1];
			}
			
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

			if (autoPercentileBox.isSelected()) {
				for (int i=0;i<=100;i++) {
//					percentileValues[d][i] = theseValues[(int)((theseValues.length-1)*i)/100];					
					percentileValues[d][i] = getPercentileValue(theseValues, i);
				}
			}
			else {
//				percentileValues[d][0] = theseValues[(int)((theseValues.length-1)*percentile)/100];
				percentileValues[d][0] = getPercentileValue(theseValues, percentile);
			}
			minValues[d] = theseValues[0];
		}

		float [] maxPercentiles = new float[percentileValues[0].length];
				
		for (int i=0;i<percentileValues.length;i++) {
			for (int j=0;j<maxPercentiles.length;j++) {
				if (i==0 || percentileValues[i][j] > maxPercentiles[j]) {
					maxPercentiles[j] = percentileValues[i][j];
				}
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
			float [] correctionFactors = new float [percentileValues[0].length];

			if (correctionAction == ADD) {
				for (int i=0;i<correctionFactors.length;i++) {
					correctionFactors[i] = maxPercentiles[i]-percentileValues[d][i];
				}
			}
			else if (correctionAction == MULTIPLY) {
				for (int i=0;i<correctionFactors.length;i++) {
					correctionFactors[i] = (maxPercentiles[i]-minValues[d])/(percentileValues[d][i]-minValues[d]);
				}
			}
			
			// Now we work out the correction factor we're actually going to use
			float correctionFactor = SimpleStats.median(correctionFactors);
			
			
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
						data[d].setValueForProbe(allProbes[p], minValues[d]+((data[d].getValueForProbe(allProbes[p])-minValues[d])*correctionFactor));					
					}
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

		optionPanel.add(new JLabel("Method of correction"),gbc);
		gbc.gridx = 2;
		correctionActions = new JComboBox(new String [] {"Add","Multiply"});
		optionPanel.add(correctionActions,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		optionPanel.add(new JLabel("Percentile"),gbc);
		
		JPanel percentilePanel = new JPanel();
		percentilePanel.setLayout(new BorderLayout());
		
		autoPercentileBox = new JCheckBox("Auto",false);
		
		percentilePanel.add(autoPercentileBox,BorderLayout.WEST);
		
		percentileField = new JTextField(""+percentile);
		percentileField.addKeyListener(new NumberKeyListener(true, false));
		percentilePanel.add(percentileField, BorderLayout.CENTER);
		
		autoPercentileBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				percentileField.setEnabled(!autoPercentileBox.isSelected());
			}
		});
		
		gbc.gridx++;

		optionPanel.add(percentilePanel,gbc);

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
			percentile = Float.parseFloat(percentileField.getText());
		}
		catch (Exception e) {
			return false;
		}
		
		if (percentile < 0 || percentile > 100) {
			return false;
		}
		
		return true;

	}	
	
	
	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		return existingDescription+" transformed by percentile normalisation using "+correctionActions.getSelectedItem().toString()+" to the "+percentile+" percentile";
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
		return "Percentile Normalisation Quantitation";
	}


}