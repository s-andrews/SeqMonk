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


import javax.swing.JPanel;
import javax.swing.JLabel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * A quantitation which sets all values in all stores to the length of the probe
 */
public class ProbeLengthQuantitation extends Quantitation {

	private JPanel optionPanel = null;
	private DataStore [] data = null;
	
	public ProbeLengthQuantitation(SeqMonkApplication application) {
		super(application);
	}	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
		
		for (int d=0;d<data.length;d++) {

			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated(d, data.length);

			for (int p=0;p<probes.length;p++) {

				// See if we need to quit
				if (cancel) {
					progressCancelled();
					return;
				}


				data[d].setValueForProbe(probes[p], probes[p].length());


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
		optionPanel.add(new JLabel("No Options",JLabel.CENTER));

		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}
	
	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("Probe length quantitation");
		
		return sb.toString();
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
		return false;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Probe Length Quantitation";
	}
	
}