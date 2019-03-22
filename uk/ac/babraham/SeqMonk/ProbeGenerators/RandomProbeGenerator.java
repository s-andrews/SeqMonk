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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

/**
 * Generates a random set of probes of fixed size.
 */
public class RandomProbeGenerator extends ProbeGenerator implements Runnable, KeyListener {

	private JTextField windowSizeField;
	private JTextField numberToGenerateField;
	private JCheckBox designWithinExistingBox;
	private int windowSize;
	private int numberToGenerate;
	private boolean designWithinExisting;
	private String existingListName;
	private JPanel optionPanel = null;

	/**
	 * Instantiates a new running window probe generator.
	 * 
	 * @param collection
	 */
	public RandomProbeGenerator(DataCollection collection) {
		super(collection);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {
		
		if (designWithinExistingBox == null || !designWithinExistingBox.isSelected()) {
			designWithinExisting = false;
		}
		else {
			designWithinExisting = true;
			existingListName = collection.probeSet().getActiveList().name();
		}
		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}
	
	public boolean requiresExistingProbeSet () {
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {
		
		if (optionPanel != null) {
			// We've done this already
			return optionPanel;
		}
		
		int suggestedSize = ProbeGeneratorUtilities.suggestWindowSize(collection);
		
		optionPanel = new JPanel();
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		optionPanel.add(new JLabel("Window Size (bp)"),gbc);
		
		gbc.gridx = 2;
		windowSizeField = new JTextField(""+suggestedSize);
		windowSizeField.addKeyListener(this);
		optionPanel.add(windowSizeField,gbc);
		
		gbc.gridy++;
		gbc.gridx = 1;
		optionPanel.add(new JLabel("Number of probes to generate"),gbc);
		
		gbc.gridx = 2;
		numberToGenerateField = new JTextField(""+suggestedSize);
		numberToGenerateField.addKeyListener(this);
		optionPanel.add(numberToGenerateField,gbc);

		if (collection.probeSet() != null) {
			gbc.gridy++;
			gbc.gridx = 1;
			optionPanel.add(new JLabel("Design only within active probe list"),gbc);
		
			gbc.gridx = 2;
			designWithinExistingBox = new JCheckBox();
			optionPanel.add(designWithinExistingBox,gbc);
		}
		
		
		return optionPanel;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {
		// See if we've got data in both fields
		if (windowSizeField.getText().length() > 0 && numberToGenerateField.getText().length() > 0) {
			try {
				windowSize = Integer.parseInt(windowSizeField.getText());
				numberToGenerate = Integer.parseInt(numberToGenerateField.getText());
				if (windowSize < 1) {
					throw new NumberFormatException("Window size must be at least 1");
				}
				if (numberToGenerate < 1) {
					throw new NumberFormatException("Step size must be at least 1");
				}
			}
			catch (NumberFormatException nfe) {
				optionsNotReady();
				return false;
			}
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

		ProbeSet finalSet;
		if (designWithinExisting) {
			finalSet = designPerProbe();
		}
		else {
			finalSet = designPerChromosome();
		}
		
		if (finalSet == null) {
			generationCancelled();
			return;
		}
		
		generationComplete(finalSet);
	}
	
	private ProbeSet designPerChromosome () {
		
		Chromosome [] chromosomes = collection.genome().getAllChromosomes();
		long [] offsets = new long [chromosomes.length];
			
		offsets[0] = chromosomes[0].length();
		
		for (int i=1;i<chromosomes.length;i++) {
			offsets[i] = offsets[i-1]+chromosomes[i].length();
		}
		
		
		// Now make up the probes
		Vector<Probe> newProbes = new Vector<Probe>();
		
		Random rand = new Random();
		
		while (newProbes.size() < numberToGenerate) {
			
			if (cancel) {
				return null;
			}
			
			if (newProbes.size() % 1000 == 0) {
				updateGenerationProgress("Designed "+newProbes.size()+" probes", newProbes.size(), numberToGenerate);
			}
			
			// Select a random position in the genome
			long random = (long)(collection.genome().getTotalGenomeLength() * rand.nextDouble());
			
			Chromosome chromosome = null;
			int start = 0;
			int end = 0;
			
			// Find out which chromosome this comes from
			for (int o=0;o<offsets.length;o++) {
				if (random < offsets[o]) {
										
					chromosome = chromosomes[o];
					if (o>0) {
						start = (int)(random - offsets[o-1])+1;
					}
					else {
						start = (int)random+1;
					}
					end = start + (windowSize-1);
					break;
				}
			}
			
			// Check that this is valid
			
			if (end > chromosome.length()) {
				continue; // Try again
			}
			
			// Make a probe
			newProbes.add(new Probe(chromosome, start, end));
			
		}
		
		Probe [] finalList = newProbes.toArray(new Probe[0]);
		ProbeSet finalSet = new ProbeSet(getDescription(),finalList);
		
		return finalSet;

	}
	
	private ProbeSet designPerProbe () {
		
		Probe [] probes = collection.probeSet().getActiveList().getAllProbes();
		// TODO: We could remove all probes which are smaller than the windows we're going to
		// generate?
		long [] offsets = new long [probes.length];
			
		offsets[0] = probes[0].length();
		
		for (int i=1;i<probes.length;i++) {
			offsets[i] = offsets[i-1]+probes[i].length();
		}
		
		long totalLength = offsets[offsets.length-1];
		
		
		// Now make up the probes
		Vector<Probe> newProbes = new Vector<Probe>();
		
		Random rand = new Random();
		
		RANDLOOP: while (newProbes.size() < numberToGenerate) {
			
			if (cancel) {
				return null;
			}
			
			if (newProbes.size() % 1000 == 0) {
				updateGenerationProgress("Designed "+newProbes.size()+" probes", newProbes.size(), numberToGenerate);
			}
			
			// Select a random position in the genome
			long random = (long)(totalLength * rand.nextDouble());
			
			Chromosome chromosome = null;
			int start = 0;
			int end = 0;
			
			// Find out which chromosome this comes from
			for (int o=0;o<offsets.length;o++) {
				if (random < offsets[o]) {
										
					chromosome = probes[o].chromosome();
					if (o>0) {
						start = (int)(random - offsets[o-1])+probes[o].start();
					}
					else {
						start = (int)random+probes[o].start();
					}
					end = start + (windowSize-1);
					
					// Check that this is valid
					if (end > probes[o].end()) {
						continue RANDLOOP; // Try again
					}

					break;
				}
			}
						
			// Make a probe
			newProbes.add(new Probe(chromosome, start, end));
			
		}
		
		Probe [] finalList = newProbes.toArray(new Probe[0]);
		ProbeSet finalSet = new ProbeSet(getDescription(),finalList);
		
		return finalSet;
	}
	
	
	/**
	 * Gets a text description of the current set of options.
	 * 
	 * @return Text describing the current options
	 */
	private String getDescription () {
		StringBuffer b = new StringBuffer();
		b.append("Random windows of size ");
		b.append(windowSize);
		b.append(" with total number of ");
		b.append(numberToGenerate);
		if (designWithinExisting) {
			b.append(" only within existing probes in ");
			b.append(existingListName);
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
		return "Random Position Generator";
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

}
