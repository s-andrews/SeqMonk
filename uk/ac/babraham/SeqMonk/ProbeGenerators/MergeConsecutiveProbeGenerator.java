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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Merges together a number of consecutive probes
 */
public class MergeConsecutiveProbeGenerator extends ProbeGenerator implements Runnable {

	private JTextField numberToMergeField;
	private int numberToMerge;
	private JTextField stepSizeField;
	private int stepSize;
	private JPanel optionPanel = null;

	/**
	 * Instantiates a new merge consecutive probe generator.
	 * 
	 * @param collection
	 */
	public MergeConsecutiveProbeGenerator(DataCollection collection) {
		super(collection);
	}

	public boolean requiresExistingProbeSet () {
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {

		if (numberToMergeField.getText().length() > 0) {
			numberToMerge = Integer.parseInt(numberToMergeField.getText());
		}
		else {
			numberToMerge = 5;
		}

		if (numberToMerge <= 0) numberToMerge = 5;

		if (stepSizeField.getText().length() > 0) {
			stepSize = Integer.parseInt(stepSizeField.getText());
		}
		else {
			stepSize = numberToMerge;
		}

		if (stepSize <=0) stepSize = numberToMerge;

		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {

		if (optionPanel == null) {

			optionPanel = new JPanel();
			optionPanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.fill = GridBagConstraints.HORIZONTAL;

			optionPanel.add(new JLabel("Number of probes to merge"),gbc);

			gbc.gridx = 2;
			numberToMergeField = new JTextField("5");
			numberToMergeField.addKeyListener(new NumberKeyListener(false, false));
			optionPanel.add(numberToMergeField,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			optionPanel.add(new JLabel("Step Size"),gbc);

			gbc.gridx = 2;
			stepSizeField = new JTextField("5");
			stepSizeField.addKeyListener(new NumberKeyListener(false, false));
			optionPanel.add(stepSizeField,gbc);

		}
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		Vector<Probe> newProbes = new Vector<Probe>();

		for (int c=0;c<chromosomes.length;c++) {
			// Time for an update
			updateGenerationProgress("Processed "+c+" chromosomes", c, chromosomes.length);

			Probe [] startingProbes = collection.probeSet().getProbesForChromosome(chromosomes[c]);

			Arrays.sort(startingProbes);

			// Now we can make the actual probes

			int index = 0;		
			while (index < startingProbes.length-1) {

				int start = 0;
				int end = 0;
				int strand = 0;

				for (int i=index;i<startingProbes.length && i<index+numberToMerge;i++) {

					if (cancel) {
						generationCancelled();
						return;
					}

					if (i==index || startingProbes[i].start() < start) {
						start = startingProbes[i].start();
					}

					if (i==index || startingProbes[i].end() > end) {
						end = startingProbes[i].end();
					}

					if (i==index) {
						strand = startingProbes[i].strand();
					}
					else {
						if (startingProbes[i].strand() != strand) {
							strand = Probe.UNKNOWN;
						}
					}
				}

				newProbes.add(new Probe(chromosomes[c], start, end, strand));

				index += stepSize;
			}
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
		b.append("Sets of ");
		b.append(numberToMerge);
		b.append(" merged consecutive probes from probe set ");
		b.append(collection.probeSet().description());
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
		return "Merge Consecutive Probe Generator";
	}


}
