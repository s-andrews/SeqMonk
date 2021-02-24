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
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Takes a set of overlapping probes and merges them together
 */
public class DeduplicationProbeGenerator extends ProbeGenerator implements Runnable, ItemListener {

	private JComboBox probeListBox;
	private ProbeList initialList;
	private JTextField maxDistanceField;
	private JCheckBox separateStrandsBox;
	private JPanel optionPanel = null;

	/**
	 * Instantiates a new deduplication probe generator.
	 * 
	 * @param collection
	 */
	public DeduplicationProbeGenerator(DataCollection collection) {
		super(collection);
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
		optionPanel.add(new JLabel("Merge probes separated by less than (bp)"),gbc);

		gbc.gridx = 2;
		maxDistanceField = new JTextField("0");
		maxDistanceField.addKeyListener(new NumberKeyListener(false, false));
		optionPanel.add(maxDistanceField,gbc);

		gbc.gridy++;
		gbc.gridx = 1;
		optionPanel.add(new JLabel("Merge strands separately"),gbc);

		gbc.gridx = 2;
		separateStrandsBox = new JCheckBox("",true);
		optionPanel.add(separateStrandsBox,gbc);
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

		boolean separateStrands = separateStrandsBox.isSelected();

		int maxDistance = 0;

		if (maxDistanceField.getText().length() > 0) {
			maxDistance = Integer.parseInt(maxDistanceField.getText());
		}

		Vector<Probe> newProbes = new Vector<Probe>();

		for (int c=0;c<chromosomes.length;c++) {

			// Time for an update
			updateGenerationProgress("Processed "+c+" chromosomes", c, chromosomes.length);

			Probe [] startingProbes = initialList.getProbesForChromosome(chromosomes[c]);

			// For directional merging we use 3 probes
			Probe currentForward = null;
			Probe currentReverse = null;
			Probe currentUnknown = null;

			// For non-directional merging we use only one
			Probe currentProbe = null;

			// Now we can make the actual probes
			for (int i=0;i<startingProbes.length;i++) {

				if (cancel) {
					generationCancelled();
					return;
				}

				if (separateStrands) {

					switch (startingProbes[i].strand()) {

					case (Probe.FORWARD):
						if (currentForward == null) {
							currentForward = new Probe(chromosomes[c], startingProbes[i].packedPosition());
						}	
						else if (startingProbes[i].start() <= currentForward.end()+maxDistance) {
							if (startingProbes[i].end() > currentForward.end()) {
								// Extend the current probe
								currentForward = new Probe(chromosomes[c],currentForward.start(),startingProbes[i].end(),currentForward.strand(),currentForward.name());
							}
						}
						else {
							newProbes.add(currentForward);

							currentForward = new Probe(chromosomes[c], startingProbes[i].packedPosition());
						}
					break;

					case (Probe.REVERSE):
						if (currentReverse == null) {
							currentReverse = new Probe(chromosomes[c], startingProbes[i].packedPosition());
						}	
						else if (startingProbes[i].start() <= currentReverse.end()+maxDistance) {
							if (startingProbes[i].end() > currentReverse.end()) {
								// Extend the current probe
								currentReverse = new Probe(chromosomes[c],currentReverse.start(),startingProbes[i].end(),currentReverse.strand(),currentReverse.name());
							}
						}
						else {
							newProbes.add(currentReverse);

							currentReverse = new Probe(chromosomes[c], startingProbes[i].packedPosition());
						}
					break;

					case (Probe.UNKNOWN):
						if (currentUnknown == null) {
							currentUnknown = new Probe(chromosomes[c], startingProbes[i].packedPosition());
						}	
						else if (startingProbes[i].start() <= currentUnknown.end()+maxDistance) {
							if (startingProbes[i].end() > currentUnknown.end()) {
								// Extend the current probe
								currentUnknown = new Probe(chromosomes[c],currentUnknown.start(),startingProbes[i].end(),currentUnknown.strand(),currentUnknown.name());
							}
						}
						else {
							newProbes.add(currentUnknown);

							currentUnknown = new Probe(chromosomes[c], startingProbes[i].packedPosition());
						}

					break;
					}


				}
				else {
					// We're not separating strands so just merge everything

					if (currentProbe == null) {
						currentProbe = new Probe(chromosomes[c], startingProbes[i].packedPosition());

					}	
					else if (startingProbes[i].start() <= currentProbe.end()+maxDistance) {

						// This overlaps

						if (startingProbes[i].end() > currentProbe.end() || startingProbes[i].strand() != currentProbe.strand()) {
							// Update the current probe
							int usedStrand = currentProbe.strand();
							if (startingProbes[i].strand() != currentProbe.strand()) {
								usedStrand = Probe.UNKNOWN;
							}
							currentProbe = new Probe(chromosomes[c],currentProbe.start(),Math.max(startingProbes[i].end(),currentProbe.end()),usedStrand,currentProbe.name());
						}
					}
					else {
						newProbes.add(currentProbe);

						currentProbe = new Probe(chromosomes[c], startingProbes[i].packedPosition());
					}	
				}

			}
			
			// At the end of each chromosome we add any probes we have remaining
			if (currentProbe != null) 
					newProbes.add(currentProbe);

			if (currentForward != null) 
				newProbes.add(currentForward);

			if (currentReverse != null) 
				newProbes.add(currentReverse);

			if (currentUnknown != null) 
				newProbes.add(currentUnknown);


		}			



		Probe [] finalList = newProbes.toArray(new Probe[0]);
		ProbeSet finalSet = new ProbeSet(getDescription(),finalList);

		generationComplete(finalSet);
	}


	/**
	 * Gets a text description of the current set of options.
	 * 
	 * @return Text describing the current options
	 */
	private String getDescription () {
		StringBuffer b = new StringBuffer();
		b.append("Deduplicated probes from ");
		b.append(initialList.name());
		b.append(" created by ");
		b.append(initialList.description());
		b.append("Max Distance was ");
		if (maxDistanceField.getText().length()>0) {
			b.append(maxDistanceField.getText());
		}
		else {
			b.append(0);
		}
		if (separateStrandsBox.isSelected());
		b.append(" strands were merged separately");
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
		return "Deduplication Probe Generator";
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		isReady();
	}

}
