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
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * A quantitaion method based on how enriched a given region
 * is relative to a completely random distribution of bases.
 */
public class EnrichmentQuantitation extends Quantitation {

	private JPanel optionPanel = null;

	/** The stores we're going to quantitate. */
	private DataStore [] data;
	
	public EnrichmentQuantitation(SeqMonkApplication application) {
		super(application);
	}
		
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		this.data = data;		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {
		
		if (optionPanel == null) {
			optionPanel = new JPanel();
			optionPanel.setLayout(new BorderLayout());
			optionPanel.add(new JLabel("No Options", JLabel.CENTER),BorderLayout.CENTER);
		}
		
		
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}
	
	public String description () {
		return "Enrichment Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();

		// Get the base density for reads for all datasets
		float [] dataDensities = new float [data.length];
		
		for (int d=0;d<data.length;d++) {
			dataDensities[d] = ((float)data[d].getTotalReadLength())/application.dataCollection().genome().getTotalGenomeLength();
		}
		
		
		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			

			progressUpdated(p, probes.length);
			
			for (int d=0;d<data.length;d++) {

				// Since the length counts can get bigger than an int and a float
				// doesn't have the granularity to increment this big we do the
				// initial count in a long
				long rawCount = 0;
				
				long [] reads = data[d].getReadsForProbe(probes[p]);
				
				for (int r=0;r<reads.length;r++) {
					rawCount += getOverlap(reads[r],probes[p]);
				}
				
				// Since the final stored value will be a float we do the conversion
				// now before we apply any corrections
				float count = (float)rawCount/probes[p].length();
				
				// To avoid infinite values we can set an arbitrarily low
				// cutoff for the low value
				if (count == 0) count = dataDensities[d]/100; // Count will be down by 100X

				count /=  dataDensities[d];
				count = (float)(Math.log(count)/Math.log(2));
					
				data[d].setValueForProbe(probes[p], count);					
					
			}
			
		}

		quantitatonComplete();
		
	}
	
	/**
	 * Gets size of the overlap (in bp) between a read and a probe
	 * 
	 * @param read
	 * @param probe
	 * @return The size of the overlap (in bp)
	 */
	private int getOverlap (long read, Probe probe) {
		return 1+ (Math.min(SequenceRead.end(read),probe.end())-Math.max(SequenceRead.start(read),probe.start()));
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Enrichment Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
