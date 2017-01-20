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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A quantitation method based on how much of a probe is covered with reads.
 */
public class CoverageQuantitation extends Quantitation implements KeyListener {

	private JPanel optionPanel = null;
	private JComboBox strandLimit;
	private JTextField depthLimitField;
	private int depthLimit = 1;
	private QuantitationStrandType quantitationType;
	private DataStore [] data;
	
	public CoverageQuantitation(SeqMonkApplication application) {
		super(application);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		this.data = data;
		
		this.quantitationType = (QuantitationStrandType)strandLimit.getSelectedItem();
		
		if (depthLimitField.getText().trim().length() > 0) {
			depthLimit = Integer.parseInt(depthLimitField.getText());
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
		
		optionPanel.add(new JLabel("Count reads on strand"),gbc);
		
		gbc.gridx = 2;
		strandLimit = new JComboBox(QuantitationStrandType.getTypeOptions());
		optionPanel.add(strandLimit,gbc);
		
		gbc.gridx = 1;
		gbc.gridy++;
		
		optionPanel.add(new JLabel("Min Read Depth"),gbc);
		
		gbc.gridx = 2;
		depthLimitField = new JTextField(""+depthLimit,5);
		depthLimitField.addKeyListener(new NumberKeyListener(false, false));
		
		optionPanel.add(depthLimitField,gbc);
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
		sb.append("Coverage Quantitation using ");
		sb.append(quantitationType.toString());
		sb.append(" with minimum depth ");
		sb.append(depthLimit);
		
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
		
		int [] coverage = new int [0];
		int coverStart;
		int coverEnd;
		int total;
		
		
		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			
			int requiredLength = (probes[p].end()-probes[p].start())+1;
			
			/*
			 * Assigning and then clearing these potentially long arrays
			 * is really slow, but I can't see a better way to do this.  You
			 * have to keep track of state somehow.
			 */
			
			if (coverage.length < requiredLength) {
				coverage = new int [requiredLength];
			}
			
			progressUpdated(p, probes.length);
			
			for (int d=0;d<data.length;d++) {
				
				// Reset previous data
				for (int i=0;i<requiredLength;i++) {
					coverage[i] = 0;
				}
				
				long [] reads = data[d].getReadsForProbe(probes[p]);
				for (int r=0;r<reads.length;r++) {
					if (! quantitationType.useRead(probes[p], reads[r])) {
						continue;
					}
					
					coverStart = SequenceRead.start(reads[r]) - probes[p].start();
					coverEnd = SequenceRead.end(reads[r]) - probes[p].start();
					
					if (coverStart < 0) coverStart = 0;
					if (coverEnd >= requiredLength) coverEnd = requiredLength-1;
					
					for (int c=coverStart;c<=coverEnd;c++) {
						coverage[c]++;
					}
				}
				
				total = 0;
				for (int c=0;c<requiredLength;c++) {
					if (coverage[c] >= depthLimit) {
						total++;
					}
				}
				
				data[d].setValueForProbe(probes[p], ((float)total/requiredLength)*100);

			}
			
		}

		quantitatonComplete();
		
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "% Coverage Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent k) {
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent k) {
		isReady();
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent k) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
