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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * A quantitation method based on how deep the overlaps are between reads
 * overlapping a given probe.
 */
public class CoverageDepthQuantitation extends Quantitation {

	private JPanel optionPanel = null;
	private JComboBox depthType;
	private JComboBox overlapType;
	private boolean exactOverlap;
	private boolean needMaxValue;
	private QuantitationStrandType quantitationType;
	private JComboBox strandType;
	private boolean expressAsPercentage = true;
	private JCheckBox percentageBox;
	private JCheckBox ignoreDuplicates;
	private DataStore [] data;

	public CoverageDepthQuantitation(SeqMonkApplication application) {
		super(application);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		this.data = data;
		
		this.quantitationType = (QuantitationStrandType)strandType.getSelectedItem();
		quantitationType.setIgnoreDuplicates(ignoreDuplicates.isSelected());
		
		if (depthType.getSelectedItem().equals("max")) {
			needMaxValue = true;
		}
		else {
			needMaxValue = false;
		}
		
		if (overlapType.getSelectedItem().equals("exact")) {
			exactOverlap = true;
		}
		else {
			exactOverlap = false;
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
		
		optionPanel.add(new JLabel("Find "),gbc);
		
		gbc.gridx = 2;
		depthType = new JComboBox(new String [] {"max","min"});
		optionPanel.add(depthType,gbc);
		
		gbc.gridx = 3;
		optionPanel.add(new JLabel("  depth for "),gbc);
		
		gbc.gridx = 4;
		overlapType = new JComboBox(new String [] {"any","exact"});
		optionPanel.add(overlapType,gbc);

		gbc.gridx = 5;
		optionPanel.add(new JLabel("  overlaps"),gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		gbc.gridwidth = 3;
		
		optionPanel.add(new JLabel("Express as % of all reads"),gbc);
		
		gbc.gridx=5;
		percentageBox = new JCheckBox();
		percentageBox.setSelected(true);
		optionPanel.add(percentageBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		gbc.gridwidth = 3;
		
		optionPanel.add(new JLabel("Use reads from strand"),gbc);
		
		gbc.gridx=3;
		strandType = new JComboBox(QuantitationStrandType.getTypeOptions());
		optionPanel.add(strandType,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		gbc.gridwidth = 3;
		
		optionPanel.add(new JLabel("Ignore Duplicates "),gbc);
		
		gbc.gridx=3;
		ignoreDuplicates = new JCheckBox();
		optionPanel.add(ignoreDuplicates,gbc);
		
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
		sb.append("Coverage Depth Quantitation using ");
		sb.append(quantitationType.toString());
		sb.append(" finding ");
		sb.append(depthType.getSelectedItem().toString());
		sb.append(" depth for ");
		sb.append(overlapType.getSelectedItem().toString());
		sb.append(" overlaps");
		if (quantitationType.ignoreDuplicates()) {
			sb.append(" duplicates ignored");
		}
		
		if (expressAsPercentage) {
			sb.append(" expressed as percentage of all reads");
		}
		
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		expressAsPercentage = percentageBox.isSelected();
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
		LinkedList<Long> currentStack = new LinkedList<Long>();
		ArrayList<Long> removeList = new ArrayList<Long>();
		
		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			
			progressUpdated(p, probes.length);
			
			for (int d=0;d<data.length;d++) {
				
				quantitationType.resetLastRead();

				// See if we need to quit
				if (cancel) {
					progressCancelled();
					return;
				}
				
				long [] reads = data[d].getReadsForProbe(probes[p]);
				
				int maxDepth = 0;
				int minDepth = reads.length;
				
				// Go through the reads finding which ones overlap
				
				currentStack.clear();
				removeList.clear();
				Iterator<Long> i;
				for (int r=0;r<reads.length;r++) {
					
					if (! quantitationType.useRead(probes[p], reads[r])) {
						continue;
					}
					
//					System.err.println("Adding read "+r);
					
					// We can skip the check through the current stack if this read
					// has the same position as the last one.
					
					if (!(r>0 && SequenceRead.start(reads[r])==SequenceRead.start(reads[r-1]) && SequenceRead.end(reads[r])==SequenceRead.end(reads[r-1]))) {
					
						i=currentStack.iterator();
						while (i.hasNext()) {
							Long s = i.next();
							
							if (exactOverlap) {
								if (SequenceRead.end(s) != SequenceRead.end(reads[r]) || SequenceRead.start(s) != SequenceRead.start(reads[r])) {
									removeList.add(s);
								}							
							}
							else {
								if (SequenceRead.start(reads[r]) > SequenceRead.end(s)) {
									removeList.add(s);
								}
							}
						}
												
						i = removeList.iterator();
						while (i.hasNext()) {
							currentStack.remove(i.next());
						}
						removeList.clear();
					}
						
					currentStack.add(reads[r]);
					if (currentStack.size()>maxDepth)
						maxDepth = currentStack.size();
					
					if (currentStack.size() < minDepth) {
						minDepth = currentStack.size();
					}
					
				}
				
				if (needMaxValue) {
					
					// If we're expressing the depth as a percentage of all reads we
					// need to check that there were some reads there in the first place
					// otherwise we'll end up dividing by zero.  If there weren't any we
					// just enter zero for the value.
					
					if (expressAsPercentage) {
						if (reads.length > 0) {
							data[d].setValueForProbe(probes[p], (((float)maxDepth)/reads.length)*100);
						}
						else {
							data[d].setValueForProbe(probes[p],0);
						}
					}
					else {
						data[d].setValueForProbe(probes[p], maxDepth);						
					}
				}
				else {
					if (expressAsPercentage) {
						if (reads.length > 0) {
							data[d].setValueForProbe(probes[p], (((float)minDepth)/reads.length)*100);
						}
						else {
							data[d].setValueForProbe(probes[p],0);
						}
					}
					else {
						data[d].setValueForProbe(probes[p], minDepth);
					}
				}
			}
			
			
		}

		quantitatonComplete();
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Coverage Depth Quantitation";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
