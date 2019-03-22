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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

/**
 * A generator which makes a new probeset from an existing probelist.
 */
public class ProbeListProbeGenerator extends ProbeGenerator implements Runnable {

	private JComboBox selectedListBox;
	private JCheckBox reverseDirectionBox;
	private ProbeList selectedList;
	private JPanel optionPanel = null;
	private ProbeList [] currentLists = new ProbeList[0];

	/**
	 * Instantiates a new probe list probe generator.
	 * 
	 * @param collection The DataCollection used to generate the new set.
	 */
	public ProbeListProbeGenerator(DataCollection collection) {
		super(collection);
	}
	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {
		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}
	
	public boolean requiresExistingProbeSet () {
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {
		
		if (optionPanel != null) {
			// We've done this already
			return optionPanel;
		}

		ProbeList currentActiveList = null;
		
		if (collection.probeSet() != null) {	
			currentLists = collection.probeSet().getAllProbeLists();
			currentActiveList = collection.probeSet().getActiveList();
		}
		else {
			currentLists = new ProbeList[0];
		}
		
		optionPanel = new JPanel();
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.1;
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		
		optionPanel.add(new JLabel("Select List"),gbc);

		gbc.gridx++;
		gbc.weightx=0.9;
		selectedListBox = new JComboBox(currentLists);
		selectedListBox.setPrototypeDisplayValue("No longer than this please");
		for (int i=0;i<currentLists.length;i++) {
			if (currentLists[i] == currentActiveList) {
				selectedListBox.setSelectedIndex(i);
			}
		}
		selectedListBox.addItemListener(new ItemListener() {
		
			public void itemStateChanged(ItemEvent e) {
				isReady();
			}
		});
		optionPanel.add(selectedListBox,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		gbc.weightx=0.1;
		optionPanel.add(new JLabel("Reverse Probe Direction"),gbc);

		gbc.gridx++;
		gbc.weightx=0.9;
		reverseDirectionBox = new JCheckBox();
		optionPanel.add(reverseDirectionBox,gbc);
		
		return optionPanel;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {
		
		if (selectedListBox.getSelectedItem() != null) {
			selectedList = (ProbeList)selectedListBox.getSelectedItem();
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		Vector<Probe> newProbes = new Vector<Probe>();
		boolean reverse = reverseDirectionBox.isSelected();
		
		
		String description = collection.probeSet().description()+" then took subset of probes in "+selectedList.name();
		
		if (reverse) {
			description = description+" and reversed all probe directions";
		}
		
		
		Probe [] probes = selectedList.getAllProbes();
		

		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				generationCancelled();
				return;
			}
			
			if (p % 10000 == 0) {
				// Time for an update
				updateGenerationProgress("Processed "+p+" probes", p, probes.length);
			}
				
			// We can't just reinsert the same probe object since it will
			// have a different index in the new ProbeSet
			
			if (reverse) {
				
				// We reverse the existing strand.  We don't do anything to 
				// probes with unknown strand.
				int strand = probes[p].strand();
				if (strand == Location.FORWARD) {
					strand = Location.REVERSE;
				}
				else if (strand == Location.REVERSE) {
					strand = Location.FORWARD;
				}
				newProbes.add(new Probe(probes[p].chromosome(), probes[p].start(), probes[p].end(),strand,probes[p].name()));
			}
			else {
				newProbes.add(new Probe(probes[p].chromosome(), probes[p].packedPosition(),probes[p].name()));
			}

		}
		
		Probe [] finalList = newProbes.toArray(new Probe[0]);
		ProbeSet finalSet = new ProbeSet(description, finalList);
		
		generationComplete(finalSet);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Existing Probe List Generator";
	}
	

}
