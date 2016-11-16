/**
 * Copyright Copyright 2010-15 Simon Andrews
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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

/**
 * Generates an evenly spaced set of probes over the whole genome
 */
public class RunningWindowProbeGenerator extends ProbeGenerator implements Runnable, KeyListener {

	private JTextField probeSizeField;
	private JTextField stepSizeField;
	private JCheckBox designWithinExistingBox;
	private JComboBox limitRegionBox;
	private int probeSize;
	private int stepSize;
	private boolean designWithinExisting;
	private String existingListName;
	private JPanel optionPanel = null;

	/**
	 * Instantiates a new running window probe generator.
	 * 
	 * @param collection
	 */
	public RunningWindowProbeGenerator(DataCollection collection) {
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
		
		optionPanel.add(new JLabel("Probe Size (bp)"),gbc);
		
		gbc.gridx = 2;
		probeSizeField = new JTextField(""+suggestedSize);
		probeSizeField.addKeyListener(this);
		optionPanel.add(probeSizeField,gbc);
		
		gbc.gridy++;
		gbc.gridx = 1;
		optionPanel.add(new JLabel("Step Size (bp)"),gbc);
		
		gbc.gridx = 2;
		stepSizeField = new JTextField(""+suggestedSize);
		stepSizeField.addKeyListener(this);
		optionPanel.add(stepSizeField,gbc);

		if (collection.probeSet() != null) {
			gbc.gridy++;
			gbc.gridx = 1;
			optionPanel.add(new JLabel("Limit by region"),gbc);
		
			gbc.gridx = 2;
			
			JPanel limitPanel = new JPanel();
			limitPanel.setLayout(new BorderLayout());
			
			designWithinExistingBox = new JCheckBox();
			limitPanel.add(designWithinExistingBox,BorderLayout.WEST);
			
			limitRegionBox = new JComboBox(new String [] {"Active Probe List","Currently Visible Region"});

			limitRegionBox.setEnabled(false);
			designWithinExistingBox.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent arg0) {
					limitRegionBox.setEnabled(designWithinExistingBox.isSelected());
				}
			});
			
			limitPanel.add(limitRegionBox,BorderLayout.CENTER);
			
			optionPanel.add(limitPanel,gbc);
		}
		
		
		return optionPanel;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {
		// See if we've got data in both fields
		if (probeSizeField.getText().length() > 0 && stepSizeField.getText().length() > 0) {
			try {
				probeSize = Integer.parseInt(probeSizeField.getText());
				stepSize = Integer.parseInt(stepSizeField.getText());
				if (probeSize < 1) {
					throw new NumberFormatException("Window size must be at least 1");
				}
				if (stepSize < 1) {
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
		
		generationComplete(finalSet);
	}
	
	private ProbeSet designPerChromosome () {
		Chromosome [] chromosomes = collection.genome().getAllChromosomes();
		
		Vector<Probe> newProbes = new Vector<Probe>();
		
		for (int c=0;c<chromosomes.length;c++) {
			// Time for an update
			updateGenerationProgress("Processed "+c+" chromosomes", c, chromosomes.length);
			
			int pos=1;
			while (pos < chromosomes[c].length()) {
				
				// See if we need to quit
				if (cancel) {
					generationCancelled();
				}
				
				int end = pos+(probeSize-1);
				if (end > chromosomes[c].length()) end = chromosomes[c].length();
				
				Probe p = new Probe(chromosomes[c],pos,end);
				
				newProbes.add(p);
				

				pos += stepSize;
			}
		}
		
		Probe [] finalList = newProbes.toArray(new Probe[0]);
		ProbeSet finalSet = new ProbeSet(getDescription(),finalList);
		
		return finalSet;

	}
	
	private ProbeSet designPerProbe () {
		
		Chromosome [] chromosomes = collection.genome().getAllChromosomes();
		ProbeList activeList;
		if (limitRegionBox.getSelectedItem().equals("Active Probe List")) {
			activeList = collection.probeSet().getActiveList();
		}
		
		else {
			// We're just analysing a single probe over the current region
			existingListName = "Currently visible region";
			Probe p = new Probe(DisplayPreferences.getInstance().getCurrentChromosome(), DisplayPreferences.getInstance().getCurrentLocation());
			activeList = new ProbeSet("Current Region", 1);
			activeList.addProbe(p, 0f);
		}
		
		Vector<Probe> newProbes = new Vector<Probe>();
		
		for (int c=0;c<chromosomes.length;c++) {
			// Time for an update
			updateGenerationProgress("Processed "+c+" chromosomes", c, chromosomes.length);
			
			Probe [] probes = activeList.getProbesForChromosome(chromosomes[c]);
			
			for (int p=0;p<probes.length;p++) {
			
				int pos=probes[p].start();
				while (pos < probes[p].end()-(probeSize-1)) {
				
					// See if we need to quit
					if (cancel) {
						generationCancelled();
					}
				
					int end = pos+(probeSize-1);
					if (end > chromosomes[c].length()) end = chromosomes[c].length();
				
					Probe pr = new Probe(chromosomes[c],pos,end,probes[p].strand());
				
					newProbes.add(pr);

					pos += stepSize;
				}
			}
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
		b.append("Running windows of size ");
		b.append(probeSize);
		b.append(" with step size ");
		b.append(stepSize);
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
		return "Running Window Generator";
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
