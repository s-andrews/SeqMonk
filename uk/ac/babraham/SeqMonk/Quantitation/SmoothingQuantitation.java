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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A quantitation which smoothes an existing quantitation based on
 * adjacent probes.
 */
public class SmoothingQuantitation extends Quantitation {

	private static final int WINDOW = 10000;
	private static final int ADJACENT = 10001;

	private JPanel optionPanel = null;
	private JComboBox correctionActions;
	private int correctionAction;
	private JTextField distanceField;
	private int distance = 5;
	
	public SmoothingQuantitation(SeqMonkApplication application) {
		super(application);
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		if (! isReady()) {
			progressExceptionReceived(new SeqMonkException("Options weren't set correctly"));
		}
		
		Chromosome [] chromosomes = application.dataCollection().genome().getAllChromosomes();

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

		DataStore [] data = quantitatedStores.toArray(new DataStore[0]);
		
		for (int c=0;c<chromosomes.length;c++) {

			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			

			progressUpdated(c, chromosomes.length);
			
			Probe [] allProbes = application.dataCollection().probeSet().getProbesForChromosome(chromosomes[c]);
			
			float [][] newValues = new float[data.length][allProbes.length];
			
			// Apply the correction to all probes

			try {
				for (int p=0;p<allProbes.length;p++) {

					// See if we need to quit
					if (cancel) {
						progressCancelled();
						return;
					}

					// Find the min and max indices we're going to use.
					int minIndex = p;
					int maxIndex = p;
					
					if (correctionAction == ADJACENT) {
						minIndex = p-(distance/2);
						maxIndex = minIndex+(distance-1);
						if (minIndex < 0) minIndex = 0;
						if (maxIndex > allProbes.length-1) maxIndex = allProbes.length-1;
					}
					else if (correctionAction == WINDOW) {
						for (int i=p;i>=0;i--) {
							if (allProbes[i].end()<allProbes[p].start()-(distance/2)) {
								break;
							}
							minIndex = i;
						}
						
						for (int i=p;i<allProbes.length;i++) {
							if (allProbes[i].start() > allProbes[p].end()+(distance/2)) {
								break;
							}
							maxIndex = i;
						}
					}
					
					// Now go through all of the datasets working out the new value for this range
					float [] tempValues = new float[(maxIndex-minIndex)+1];
					
					for (int d=0;d<data.length;d++) {
						for (int i=minIndex;i<=maxIndex;i++) {
							tempValues[i-minIndex] = data[d].getValueForProbe(allProbes[i]);
						}
						newValues[d][p] = SimpleStats.mean(tempValues);
					}
					
				}
				
				// Now assign the values for the probes on this chromosome
				for (int d=0;d<data.length;d++) {
					for (int p=0;p<allProbes.length;p++) {
						data[d].setValueForProbe(allProbes[p], newValues[d][p]);
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

		optionPanel.add(new JLabel("Smoothing method"),gbc);
		gbc.gridx = 2;
		correctionActions = new JComboBox(new String [] {"Adjacent Probes","Window Size"});
		optionPanel.add(correctionActions,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		optionPanel.add(new JLabel("Size"),gbc);
		distanceField = new JTextField(""+distance);
		distanceField.addKeyListener(new NumberKeyListener(false, false));
		distanceField.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent arg0) {}
			public void keyReleased(KeyEvent arg0) {
				optionsChanged();
			}
			public void keyPressed(KeyEvent arg0) {}
		});
		
		gbc.gridx++;

		optionPanel.add(distanceField,gbc);

		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {

		try {
			distance = Integer.parseInt(distanceField.getText());
		}
		catch (Exception e) {
			return false;
		}
		
		if (distance < 1) {
			return false;
		}
		
		return true;

	}	
	
	
	public String description () {
		StringBuffer description = new StringBuffer();
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			description.append(application.dataCollection().probeSet().currentQuantitation());
		}
		else {
			description.append("Unknown quantitation");
		}
		
		
		description.append(" transformed by smoothing in windows of ");
		description.append(distanceField.getText());
		
		if (correctionActions.getSelectedItem().equals("Window Size")) {
			description.append(" bases");
		}
		else {
			description.append(" adjacent probes");
		}
		
		return description.toString();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {

		// We need to set the correction action
		if (correctionActions.getSelectedItem().equals("Window Size")) {
			correctionAction = WINDOW;
		}
		else if (correctionActions.getSelectedItem().equals("Adjacent Probes")) {
			correctionAction = ADJACENT;
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
		return "Smoothing Quantitation";
	}


}