/**
 * Copyright Copyright 2010- 21 Simon Andrews
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
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

/**
 * A generator which makes a new probeset by randomly repositioning an existing probelist.
 */
public class ShuffleListProbeGenerator extends ProbeGenerator implements Runnable {

	private JComboBox selectedListBox;
	private JCheckBox keepChromosomalDistributionBox;
	private JCheckBox limitEndsBox;
	private ProbeList selectedList;
	private JPanel optionPanel = null;
	private ProbeList [] currentLists = new ProbeList[0];

	/**
	 * Instantiates a new probe list probe generator.
	 * 
	 * @param collection The DataCollection used to generate the new set.
	 */
	public ShuffleListProbeGenerator(DataCollection collection) {
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
		
		optionPanel.add(selectedListBox, gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		gbc.weightx=0.1;
				
		optionPanel.add(new JLabel("Maintain Chromosomal Distribution"),gbc);

		gbc.gridx++;
		gbc.weightx=0.9;
		keepChromosomalDistributionBox = new JCheckBox("",true);
		
		optionPanel.add(keepChromosomalDistributionBox,gbc);

		
		gbc.gridx=1;
		gbc.gridy++;
		gbc.weightx=0.1;
				
		optionPanel.add(new JLabel("Constrain within the probeset positions"),gbc);

		gbc.gridx++;
		gbc.weightx=0.9;
		limitEndsBox = new JCheckBox("",true);
		
		optionPanel.add(limitEndsBox,gbc);

		
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
		
		String description = collection.probeSet().description()+" then shuffled "+selectedList.name()+" into random positions.";
		
		if (keepChromosomalDistributionBox.isSelected()) {
			description += " Chromosomal distribution was maintained.";
		}

		if (limitEndsBox.isSelected()) {
			description += " Positions were constrained within the limits of the existing probeset.";
		}

		Probe [] probes = selectedList.getAllProbes();
		
		long totalGenomeLength = collection.genome().getTotalGenomeLength();
		
		// If we're constraining within the limits of the probes then 
		// we need to work out where the ends are.
		
		HashMap<Chromosome, int []> chromosomeLimits = new HashMap<Chromosome, int[]>();
		
		Chromosome [] chromosomes = collection.genome().getAllChromosomes();
		
		for (int c=0;c<chromosomes.length;c++) {
			Probe [] thisChrProbes = selectedList.getProbesForChromosome(chromosomes[c]);
			
			for (int p=0;p<thisChrProbes.length;p++) {
				if (!chromosomeLimits.containsKey(chromosomes[c])) {
					chromosomeLimits.put(chromosomes[c], new int [] {probes[p].start(),probes[p].end()});
					continue;
				}
				
				if (thisChrProbes[p].start() < chromosomeLimits.get(chromosomes[c])[0]) {
					if (probes[p].start() < 0) {
						System.err.println("Probe "+thisChrProbes[p].name()+" started at "+thisChrProbes[p].start());
					}
					chromosomeLimits.get(chromosomes[c])[0] = thisChrProbes[p].start();
				}

				if (thisChrProbes[p].end() > chromosomeLimits.get(chromosomes[c])[1]) {
					if (thisChrProbes[p].end() > chromosomes[c].length()) {
						System.err.println("Probe "+thisChrProbes[p].name()+" ended at "+thisChrProbes[p].end()+" which is beyond "+chromosomes[c].length()+" for chr "+chromosomes[c].name());
					}
					chromosomeLimits.get(chromosomes[c])[1] = thisChrProbes[p].end();
				}
			}
		}
		
		if (limitEndsBox.isSelected()) {
			totalGenomeLength = 0;
			for (int c=0;c<chromosomes.length;c++) {
				if (chromosomeLimits.containsKey(chromosomes[c])) {
					
					int length = (chromosomeLimits.get(chromosomes[c])[1] - chromosomeLimits.get(chromosomes[c])[0])+1;
					
//					System.err.println("Length of "+chromosomes[c].name()+" is "+length+" from "+chromosomeLimits.get(chromosomes[c])[1]+" and "+chromosomeLimits.get(chromosomes[c])[0]+" compared to "+chromosomes[c].length());
					
					totalGenomeLength += length;
				}
			}
		}
		else {
			for (int c=0;c<chromosomes.length;c++) {
				chromosomeLimits.put(chromosomes[c], new int [] {1,chromosomes[c].length()});
			}
		}
		
//		System.err.println("Effective genome length is "+totalGenomeLength+" compared to "+collection.genome().getTotalGenomeLength());
		

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
				

			Chromosome chromosomeToUse = probes[p].chromosome();
			
			if (!keepChromosomalDistributionBox.isSelected()) {
				chromosomeToUse = selectRandomChromosome(totalGenomeLength,chromosomeLimits,probes[p].length());
			}
			
			// Now we need to select a random position within that chromosome.  We need it to start within
			// the range of viable positions.
			
			int validStart = chromosomeLimits.get(chromosomeToUse)[0];
			int validEnd = chromosomeLimits.get(chromosomeToUse)[1] - probes[p].length();
			
			int actualStart = validStart + (int)(Math.random()*(validEnd-validStart));
			int actualEnd = actualStart + (probes[p].length()-1);
						
			
			// We leave probes of unknown strand as unknown, but we randomly shuffle known ones
			int strand = Location.UNKNOWN;
			if (probes[p].strand() != Location.UNKNOWN) {
				if (Math.random()>=0.5) {
					strand = Location.FORWARD;
				}
				else {
					strand = Location.REVERSE;
				}
			}
			
			newProbes.add(new Probe(chromosomeToUse,actualStart,actualEnd,strand,probes[p].name()));

		}
		
		Probe [] finalList = newProbes.toArray(new Probe[0]);
		ProbeSet finalSet = new ProbeSet(description, finalList);
		
		generationComplete(finalSet);
	}
	
	private Chromosome selectRandomChromosome (long totalGenomeLength, HashMap<Chromosome, int []> limits, int probeLength) {
		
		int count = 0;
		
		while (true) {
			
			// Select a random position in the genome
			long randomPos = (long)(Math.random()*totalGenomeLength);
			
			
			// Figure out which chromosome this relates to
			long currentPos = 0;
			
			Chromosome selectedChr = null;
			
			for (Chromosome c : limits.keySet()) {				
				if (currentPos + c.length() >= randomPos) {
					// It's this chromosome
					selectedChr = c;
					break;
				}
				currentPos += c.length();
			}
			
			//  Check that this chromosome has enough space to accommodate the probe
			
			if ((limits.get(selectedChr)[1]-limits.get(selectedChr)[0])+1 >= probeLength) {
				return selectedChr;
			}
			
			++count;
			
			if (count > 1000) {
				System.err.println("Couldn't randomly generate a viable position for probe of length "+probeLength);
				return null;
			}
		}
		
		
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
		return "Shuffle Existing List Generator";
	}
	

}
