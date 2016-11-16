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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * A quantitation method which turns existing quantitation values into
 * their z-score equivalents. This is based on the Iglewicz and Hoaglin
 * variant of the z-score which is based on the median rather than the
 * mean and is more robust to the effect of outliers (which we see pretty
 * often in HTS data).
 */
public class ZScoreQuantitation extends Quantitation {

	private DataStore [] data = null;
	private JPanel optionsPanel = null;
	private JComboBox probeListBox;
		
	public ZScoreQuantitation(SeqMonkApplication application) {
		super(application);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
		
		Probe [] distProbes = ((ProbeList)probeListBox.getSelectedItem()).getAllProbes();
		
		try {
			for (int d=0;d<data.length;d++) {
				progressUpdated("Quantitating "+data[d].name(), d, data.length);
	
				// We need the median value

				float [] values = new float[distProbes.length];
				
				for (int p=0;p<distProbes.length;p++) {
					
					if (cancel) {
						progressCancelled();
						return;
					}

					values[p] = data[d].getValueForProbe(distProbes[p]);

				}
				float median = SimpleStats.median(values);
				
				// Now we swap the values for the difference from the median
				for (int p=0;p<distProbes.length;p++) {
					
					if (cancel) {
						progressCancelled();
						return;
					}

					values[p] = Math.abs(data[d].getValueForProbe(distProbes[p]) - median);
				}
				
				float mad = SimpleStats.median(values);
				
				
				// Now we go back through the sorted values assigning the normalised
				// z-scores to the probes in that set
				for (int p=0;p<probes.length;p++) {
					
					if (cancel) {
						progressCancelled();
						return;
					}

					data[d].setValueForProbe(probes[p], 0.6745f * ((data[d].getValueForProbe(probes[p])-median)/mad));
				}
				
				
				
			}
		}
		catch (SeqMonkException sme) {
			progressExceptionReceived(sme);
		}
			
		quantitatonComplete();
		
	}
	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {		
		if (optionsPanel == null) {
			optionsPanel = new JPanel();
			optionsPanel.setLayout(new GridBagLayout());
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.99;
			
			
			optionsPanel.add(new JPanel(),gbc);
			
			gbc.weighty=0.01;
			gbc.gridy++;
			
			JPanel options1 = new JPanel();
			options1.setLayout(new BorderLayout());
			options1.add(new JLabel("Probe List ",JLabel.RIGHT),BorderLayout.WEST);
			probeListBox = new JComboBox(application.dataCollection().probeSet().getAllProbeLists());
			probeListBox.setPrototypeDisplayValue("No longer than this please");
			options1.add(probeListBox,BorderLayout.CENTER);
			
			optionsPanel.add(options1,gbc);
			gbc.weighty=0.99;
			gbc.gridy++;
			optionsPanel.add(new JPanel(),gbc);
		}
		return optionsPanel;
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
		return existingDescription+" transformed by converting to z-scores using list "+probeListBox.getSelectedItem();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {
		
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
		return "Z-Score Quantitation";
	}
	
}