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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A quantitation which log transforms an existing set of quantitated values
 */
public class LogTransformQuantitation extends Quantitation {

	private JPanel optionPanel = null;
	private JTextField logBaseField;
	private double logBase = 2;
	private JTextField thresholdField;
	private double threshold = 1;
	
	public LogTransformQuantitation(SeqMonkApplication application) {
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
						
			// Apply the correction to all probes

			try {
				for (int p=0;p<allProbes.length;p++) {

					// See if we need to quit
					if (cancel) {
						progressCancelled();
						return;
					}
										
					for (int d=0;d<data.length;d++) {
						data[d].setValueForProbe(allProbes[p], (float)(Math.log(Math.max(data[d].getValueForProbe(allProbes[p]), threshold))/Math.log(logBase)));
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

		optionPanel.add(new JLabel("Log Base "),gbc);
		gbc.gridx = 2;
		logBaseField = new JTextField(""+logBase);
		logBaseField.addKeyListener(new NumberKeyListener(true, false));
		logBaseField.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent arg0) {}
			public void keyReleased(KeyEvent arg0) {
				optionsChanged();
			}
			public void keyPressed(KeyEvent arg0) {}
		});
		
		optionPanel.add(logBaseField,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		optionPanel.add(new JLabel("Threshold "),gbc);
		thresholdField = new JTextField(""+threshold);
		thresholdField.addKeyListener(new NumberKeyListener(true, false));
		thresholdField.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent arg0) {}
			public void keyReleased(KeyEvent arg0) {
				optionsChanged();
			}
			public void keyPressed(KeyEvent arg0) {}
		});
		
		gbc.gridx++;

		optionPanel.add(thresholdField,gbc);

		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {

		try {
			threshold = Double.parseDouble(thresholdField.getText());
			logBase = Double.parseDouble(logBaseField.getText());
		}
		catch (Exception e) {
			return false;
		}
		
		if (threshold <= 0) {
			return false;
		}
		if (logBase <= 0) {
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
		
		
		description.append(" transformed by log transforming with a base of ");
		description.append(logBaseField.getText());
		description.append(" with a lower limit of ");
		description.append(thresholdField.getText());
				
		return description.toString();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {

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
		return "Log Transform Quantitation";
	}


}