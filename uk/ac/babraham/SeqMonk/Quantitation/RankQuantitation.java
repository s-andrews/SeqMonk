/**
 * Copyright Copyright 2010-18 Simon Andrews
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * A quantitation method which turns existing quantitation values into
 * their ranked equivalents
 */
public class RankQuantitation extends Quantitation {

	private DataStore [] data = null;
	private JPanel optionsPanel = null;
		
	public RankQuantitation(SeqMonkApplication application) {
		super(application);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
		
		try {
			for (int d=0;d<data.length;d++) {
				progressUpdated("Quantitating "+data[d].name(), d, data.length);
	
				// We need to work out the ranks for this dataset
				HashMap<Float, Vector<Probe>>rankedValues = new HashMap<Float, Vector<Probe>>();
				
				for (int p=0;p<probes.length;p++) {
					
					if (cancel) {
						progressCancelled();
						return;
					}
					
					if (rankedValues.containsKey(data[d].getValueForProbe(probes[p]))) {
						rankedValues.get(data[d].getValueForProbe(probes[p])).add(probes[p]);
					}
					else {
						Vector<Probe> v = new Vector<Probe>();
						v.add(probes[p]);
						rankedValues.put(data[d].getValueForProbe(probes[p]), v);
					}
				}
				
				// Now we need to sort these values
				Float [] sortedValues = rankedValues.keySet().toArray(new Float[0]);
				
				Arrays.sort(sortedValues);
				
				// Now we go back through the sorted values assigning the normalised
				// ranks to the probes in that set
				
				int currentCount = 0;
				
				for (int s=0;s<sortedValues.length;s++) {
					
					float rankToUse = currentCount+(rankedValues.get(sortedValues[s]).size()/2f);
					
					float normalisedRank = ((float)rankToUse/probes.length)*100;
					
					Enumeration<Probe>en = rankedValues.get(sortedValues[s]).elements();
					while (en.hasMoreElements()) {
						data[d].setValueForProbe(en.nextElement(), normalisedRank);
					}
					
					currentCount += rankedValues.get(sortedValues[s]).size();
					
				}
			}
		}
		catch (SeqMonkException sme) {
			progressExceptionReceived(sme);
		}
			
		quantitatonComplete();
		
	}
	
	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		return existingDescription+" transformed into ranks";
	}
	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {		
		if (optionsPanel == null) {
			optionsPanel = new JPanel();
			optionsPanel.setLayout(new BorderLayout());
			optionsPanel.add(new JLabel("No Options", JLabel.CENTER),BorderLayout.CENTER);
		}
		return optionsPanel;
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
		return "Rank Quantitation";
	}
	
}