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

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

/**
 * Generates probes between the members of an existing probe list
 */
public class InterstitialProbeGenerator extends ProbeGenerator implements Runnable, ItemListener {

	private JComboBox probeListBox;
	private JCheckBox makeEndProbesBox;
	private ProbeList initialList;
	private boolean makeEndProbes;
	private JPanel optionPanel = null;

	/**
	 * Instantiates a new interstitial probe generator.
	 * 
	 * @param collection
	 */
	public InterstitialProbeGenerator(DataCollection collection) {
		super(collection);
		makeEndProbes = true;
		if (collection.probeSet() != null) {
			initialList = collection.probeSet().getActiveList();
		}
		else {
			initialList = null;
		}
	}
	
	public boolean requiresExistingProbeSet () {
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {
		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
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
		
		optionPanel.add(new JLabel("Starting Probe List"),gbc);
		
		gbc.gridx = 2;
		if (collection.probeSet() == null) {
			probeListBox = new JComboBox(new ProbeList[0]);			
		}
		else {
			probeListBox = new JComboBox(collection.probeSet().getAllProbeLists());
		}
		probeListBox.setPrototypeDisplayValue("No longer than this please");
		probeListBox.addItemListener(this);
		optionPanel.add(probeListBox,gbc);
		
		gbc.gridy++;
		gbc.gridx = 1;
		optionPanel.add(new JLabel("Make probes at the end of each chromosome"),gbc);
		
		gbc.gridx = 2;
		makeEndProbesBox = new JCheckBox("",makeEndProbes);
		optionPanel.add(makeEndProbesBox,gbc);
				
		return optionPanel;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {
		// See if we've got a selected list
		if (probeListBox.getSelectedItem() != null) {
			initialList = (ProbeList)probeListBox.getSelectedItem();
			optionsReady();
			return true;
			
		}
		else {
			optionsNotReady();
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();
		
		makeEndProbes = makeEndProbesBox.isSelected();
		
		Vector<Probe> newProbes = new Vector<Probe>();
		
		try {
		
			for (int c=0;c<chromosomes.length;c++) {
				// Time for an update
				updateGenerationProgress("Processed "+c+" chromosomes", c, chromosomes.length);
				
				Probe [] startingProbes = initialList.getProbesForChromosome(chromosomes[c]);
				
				// First see if we need to make a probe before the first probe
				// on the chromsoome.
				if (makeEndProbes) {
					if (startingProbes.length > 0) {
						if (startingProbes[0].start()>1) {
							Probe p = makeProbe(chromosomes[c],1,startingProbes[0].start()-1,Probe.UNKNOWN);
						
							if (p != null) {
								newProbes.add(p);
							}
						}
						
					}
					else {
						Probe p = makeProbe(chromosomes[c],1,chromosomes[c].length(),Probe.UNKNOWN);
						
						if (p != null) {
							newProbes.add(p);
						}
					}
				}
				
				int lastEnd = 1;
				int lastStrand = Probe.UNKNOWN;
				
				if (startingProbes.length > 0) {
					lastEnd = startingProbes[0].end()+1;
					lastStrand = startingProbes[0].strand();
				}
				
				// Now we can make the actual interstitial probes
				for (int i=0;i<startingProbes.length-1;i++) {

					if (cancel) {
						generationCancelled();
						return;
					}
					
					if (startingProbes[i].end()+1 > lastEnd) {
						lastEnd = startingProbes[i].end()+1;
						lastStrand = startingProbes[i].strand();
					}
					
					if (startingProbes[i+1].end() <= lastEnd) {
						continue;
					}
					
					int strandToUse = Probe.UNKNOWN;
					if (startingProbes[i+1].strand() == lastStrand) {
						strandToUse = lastStrand;
					}
					
					Probe p = makeProbe(chromosomes[c],lastEnd,startingProbes[i+1].start()-1,strandToUse);
					
					if (p != null) {
						newProbes.add(p);
					}
					
				}
				
				// Finally we can make an end probe if we need to
				if (makeEndProbes) {
					if (startingProbes.length > 0) {
						if (startingProbes[startingProbes.length-1].end()<chromosomes[c].length()) {
							Probe p = makeProbe(chromosomes[c],startingProbes[startingProbes.length-1].end()+1,chromosomes[c].length(), Probe.UNKNOWN);
						
							if (p != null) {
								newProbes.add(p);
							}
						}
						
					}
				}
			}
		}
		catch (SeqMonkException e) {
			generationExceptionReceived(e);			
		}
		
		Probe [] finalList = newProbes.toArray(new Probe[0]);
		ProbeSet finalSet = new ProbeSet(getDescription(),finalList);
		
		generationComplete(finalSet);
	}
	
	private Probe makeProbe (Chromosome c, int start, int end, int strand) throws SeqMonkException {
	
		if (end < start+1) return null;
		Probe p = new Probe(c, start, end,strand);
		return p;
				
	}
	
	/**
	 * Gets a text description of the current set of options.
	 * 
	 * @return Text describing the current options
	 */
	private String getDescription () {
		StringBuffer b = new StringBuffer();
		b.append("Interstitial probes between ");
		b.append(initialList.name());
		b.append(" created by ");
		b.append(initialList.description());
		if (makeEndProbes) {
			b.append("End probes were included");
		}
		else {
			b.append("End probes were not included");			
		}
		return b.toString();
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
		return "Interstitial Probe Generator";
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		isReady();
	}

}
