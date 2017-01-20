/**
 * Copyright Copyright 2010-17 Simon Andrews
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

/**
 * A quantitaion method based on what percentage of reads overlapping
 * a probe are duplicates of other reads at the same position.
 */
public class DuplicationQuantitation extends Quantitation {

	private JPanel optionPanel = null;

	/** The stores we're going to quantitate. */
	private DataStore [] data;
	
	public DuplicationQuantitation(SeqMonkApplication application) {
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
		return "Duplication Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();		
		
		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			

			progressUpdated(p, probes.length);
			
			for (int d=0;d<data.length;d++) {

				int rawCount = 0;
				int duplicateCount = 0;
				
				long [] reads = data[d].getReadsForProbe(probes[p]);
				
				rawCount = reads.length;
				
				for (int r=1;r<reads.length;r++) {
					if (reads[r] == reads[r-1]) {
						++duplicateCount;
					}
				}
				
				// Since the final stored value will be a float we do the conversion
				// now before we apply any corrections
				float percentage = (float)(duplicateCount*100)/(float)rawCount;
				
				// To avoid infinite values we fix a value if there are no reads.
				if (rawCount == 0) percentage = 0;
					
				data[d].setValueForProbe(probes[p], percentage);

			}
			
		}

		quantitatonComplete();
		
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Duplication Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
