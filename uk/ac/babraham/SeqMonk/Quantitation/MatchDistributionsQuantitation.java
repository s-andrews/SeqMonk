/**
 * Copyright Copyright 2010-19 Simon Andrews
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
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;

/**
 * A quantitation method which exactly matches the distributions of
 * multiple existing quantitations.  This is different to the 
 * percentile normalisation quantitation in that it applies a dynamic
 * adjustment to each member of the set such that the distribution of
 * all of the sets is the same and is the average distribution of the
 * original individual sets.
 */
public class MatchDistributionsQuantitation extends Quantitation implements ListSelectionListener {
	
	private DataStore [] data = null;
	private JPanel optionsPanel = null;
	private JList usedStoresList;
		
	public MatchDistributionsQuantitation(SeqMonkApplication application) {
		super(application);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
	
		// First we need to work out an averaged profile so we collect and
		// sort the values from each dataset and then work out the average
		// profile from that.
		
		float [] averageProfile = new float[probes.length];
		
		try {
			for (int d=0;d<data.length;d++) {
				progressUpdated("Normalising "+data[d].name(), d, data.length*2);
	
				float [] values = new float[probes.length];
				
				for (int p=0;p<probes.length;p++) {
					
					if (cancel) {
						progressCancelled();
						return;
					}

					values[p] = data[d].getValueForProbe(probes[p]);

				}
				
				// Now we sort these values and add them to the average
				// profile
				
				Arrays.sort(values);
				
				for (int i=0;i<values.length;i++) {
					averageProfile[i] += values[i];
				}
			}
			// We now divide by the number of datasets to get a proper average
			// profile
			
			for (int i=0;i<averageProfile.length;i++) {
				averageProfile[i] /= data.length;
			}
			
			// Now we need to go back through each dataset assigning the
			// value from the averaged profile to the appropriate position
			// in the real dataset.
			
			Integer [] sortedIndices = new Integer [probes.length];
			for (int i=0;i<sortedIndices.length;i++) {
				sortedIndices[i] = i;
			}
			
			for (int d=0;d<data.length;d++) {
				progressUpdated("Quantitating "+data[d].name(), data.length+d, data.length*2);
	
				// First we need a sorted list of the indices 
				Arrays.sort(sortedIndices,new DataIndexValueSorter(data[d], probes));
				
				// Now we go through the sorted indices assigning the appropriate
				// value from the averaged distribution.
				
				float currentValueSum = averageProfile[0];
				int currentValueCount = 1;
				
				for (int i=1;i<sortedIndices.length;i++) {
					
					// If this sorted value is different to the last value
					// then we can assign the last sorted value, otherwise
					// we just increase the stored total until we hit a 
					// different value
					
					if (data[d].getValueForProbe(probes[sortedIndices[i]]) != data[d].getValueForProbe(probes[sortedIndices[i-1]])) {
						float valueToAssign = currentValueSum/currentValueCount;
						
						for (int j=i-1;j>=i-currentValueCount;j--) {
							data[d].setValueForProbe(probes[sortedIndices[j]], valueToAssign);
						}
						
						currentValueCount = 0;
						currentValueSum = 0;
					}
					
					currentValueCount++;
					currentValueSum += averageProfile[i];
				}
				
				// Assign the last set of indices
				float valueToAssign = currentValueSum/currentValueCount;
				for (int j=probes.length-1;j>=probes.length-currentValueCount;j--) {
					data[d].setValueForProbe(probes[sortedIndices[j]], valueToAssign);
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
			optionsPanel.setLayout(new BorderLayout());
			optionsPanel.add(new JLabel("Data Stores to Normalise", JLabel.CENTER),BorderLayout.NORTH);
			
			DataStore [] stores = application.dataCollection().getAllDataStores();
			Vector<DataStore> usableStores = new Vector<DataStore>();
			
			for (int i=0;i<stores.length;i++) {
				if (stores[i] instanceof ReplicateSet) continue;
				if (! stores[i].isQuantitated()) continue;
				
				usableStores.add(stores[i]);
			}

			usedStoresList = new JList(usableStores.toArray(new DataStore[0]));
			usedStoresList.getSelectionModel().addListSelectionListener(this);
			usedStoresList.setCellRenderer(new TypeColourRenderer());
			
			int [] selectedIndices = new int [usableStores.size()];
			for (int i=0;i<selectedIndices.length;i++) selectedIndices[i] = i;
			usedStoresList.setSelectedIndices(selectedIndices);
			optionsPanel.add(new JScrollPane(usedStoresList),BorderLayout.CENTER);
			
		}
		return optionsPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return usedStoresList.getSelectedIndices().length > 0;
	}	
	
	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		return existingDescription+" transformed by matching distributions";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {
		
		// For this method we actually ignore the data they supply and use
		// what was selected in our options panel.
		Object [] objects = usedStoresList.getSelectedValues();
				
		this.data = new DataStore[objects.length];
		for (int o=0;o<objects.length;o++) {
			this.data[o] = (DataStore)objects[o];
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
		return "Match Distribution Quantitation";
	}
	
	public void valueChanged(ListSelectionEvent e) {
		optionsChanged();
	}

	private class DataIndexValueSorter implements Comparator<Integer> {

		private DataStore d;
		private Probe [] probes;
	
		public DataIndexValueSorter (DataStore d, Probe [] probes) {
			this.d = d;
			this.probes = probes;
		}
		public int compare(Integer i1, Integer i2) {
			try {
				return Float.compare(d.getValueForProbe(probes[i1]), d.getValueForProbe(probes[i2]));
			}
			catch (SeqMonkException sme) {
				return 0;
			}
		}
		
	}
		
}