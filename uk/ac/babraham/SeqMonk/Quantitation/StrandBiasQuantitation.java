/**
 * Copyright Copyright 2010- 24 Simon Andrews
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A Quantitation originally used for replicate forks which is
 * reverse-forward / reverse + forward.
 */
public class StrandBiasQuantitation extends Quantitation implements ItemListener {
		
	private JPanel optionPanel = null;
	private JTextField minCountField;
	
	/** What kind of difference we're doing (taken from static values) */
	private int minCount;
	
	private DataStore [] data;
	

	public StrandBiasQuantitation(SeqMonkApplication application) {
		super(application);
	}
		
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		
		this.data = data;
				
		minCount = 1;
		if (minCountField.getText().length()>0) {
			minCount = Integer.parseInt(minCountField.getText());
		}
		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
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
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		
		optionPanel.add(new JLabel("Min count "),gbc);
		gbc.gridx=2;
		minCountField = new JTextField("10");
		minCountField.setEnabled(true);
		minCountField.addKeyListener(new NumberKeyListener(false, false));
		optionPanel.add(minCountField,gbc);
		
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
		sb.append("Strand Bias Quantitation ");
		
		if (minCountField.isEnabled()) {
			sb.append(" min count was ");
			sb.append(minCount);
		}
		return sb.toString();
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
				
				long [] reads = data[d].getReadsForProbe(probes[p]);
				int forwardCount = 0;
				int reverseCount = 0;
				
				// Go through all reads
				for (int r=0;r<reads.length;r++) {
					
					// See if we can add this read to the forward count
					if (SequenceRead.strand(reads[r]) == Location.FORWARD )forwardCount++;

					// See if we can add this read to the end count
					if (SequenceRead.strand(reads[r]) == Location.REVERSE) reverseCount++;

				}
				
				// If we don't have enough data to make a measurement then skip it
				if (forwardCount+reverseCount < minCount) {
					data[d].setValueForProbe(probes[p], Float.NaN);
					continue;
				}
				
				float value = 100*(reverseCount-forwardCount)/(float)(reverseCount+forwardCount);
				
				data[d].setValueForProbe(probes[p], value);
			}
			
		}

		quantitatonComplete();
		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Strand Bias Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent ie) {
		optionsChanged();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
