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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Displays.FeaturePositionSelector.FeaturePositionSelectorPanel;

/**
 * Generates probes based around a set of features.  Up to three probes
 * can be designed for each feature (upstream, downstream and within)
 * and the exact size and position of these can be configured.
 */
public class FeatureProbeGenerator extends ProbeGenerator implements Runnable, KeyListener, ActionListener {

	/** The options panel. */
	private FeaturePositionSelectorPanel optionPanel = null;
	private int startValue;
	private int endValue;

	private boolean useSubfeatures = false;
	private boolean removeDuplicates = true;
	private boolean ignoreDirection = false;
	private String featureType;


	/**
	 * Instantiates a new feature probe generator.
	 * 
	 * @param collection The dataCollection into which the probes will be placed.
	 */
	public FeatureProbeGenerator(DataCollection collection) {
		super(collection);
	}

	public boolean requiresExistingProbeSet () {
		return false;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {
		
		ignoreDirection = optionPanel.ignoreDirection();
		removeDuplicates = optionPanel.removeDuplicates();
		useSubfeatures = optionPanel.useSubFeatures();
		featureType = optionPanel.selectedFeatureType();

		startValue = optionPanel.startOffset();
		endValue = optionPanel.endOffset();

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
		
		optionPanel = new FeaturePositionSelectorPanel(collection, true, true);
		
		optionPanel.setUseSubFeatures(useSubfeatures);
		optionPanel.setRemoveDuplicates(removeDuplicates);
		optionPanel.setIgnoreDirection(ignoreDirection);

		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {
		optionsReady();
		return true;
	}

	/**
	 * Make a set of probes within an individual feature.
	 * 
	 * @param feature The individual feature to make probes around
	 * @param chromosome
	 * @param location The actual location to use - must be the whole region
	 * @param newProbes The vector to add the new probes to
	 * @param sets DataSets to check for reads if we're ignoring blanks
	 */
	private void makeProbes (Feature feature, Chromosome chromosome, Location location, Vector<Probe> newProbes) {
		int start;
		int end;
		int strand = location.strand();

		if (ignoreDirection) strand = Location.UNKNOWN;

		if (optionPanel.positionType().equals("Upstream of feature")) {
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = location.start()-startValue;
				end = location.start()+endValue;
			}
			else {
				start = location.end()-endValue;
				end = location.end()+startValue;
			}

			Probe p = makeProbe(feature.name()+"_upstream",chromosome,start,end,strand);
			if (p != null) {

				// Add this probe 
				newProbes.add(p);
			}
		}

		else if (optionPanel.positionType().equals("Over feature")) {
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = location.start()-startValue;
				end = location.end()+endValue;
			}
			else {
				start = location.start()-endValue;
				end = location.end()+startValue;
			}

			Probe p = makeProbe(feature.name(),chromosome,start,end,strand);
			if (p != null) {
				// Add this probe 
				newProbes.add(p);
				
			}
		}


		else if (optionPanel.positionType().equals("Downstream of feature")) {
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = location.end()-startValue;
				end = location.end()+endValue;
			}
			else {
				start = location.start()-endValue;
				end = location.start()+startValue;
			}

			Probe p = makeProbe(feature.name()+"_downstream",chromosome,start,end,strand);
			if (p != null) {
				// Add this probe 
				newProbes.add(p);
				
			}
		}

		else if (optionPanel.positionType().equals("Centered on feature")) {
			
			int center = location.start()+((location.end()-location.start())/2);
		
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = center-startValue;
				end = center+endValue;
			}
			else {
				start = center-endValue;
				end = center+startValue;
			}

			Probe p = makeProbe(feature.name(),chromosome,start,end,strand);
			if (p != null) {
				// Add this probe 
				newProbes.add(p);
				
			}
		}
		
		else {
			throw new IllegalStateException("Unknown position type "+optionPanel.positionType());
		}


	}


	/**
	 * Makes an individual probe
	 * 
	 * @param name The name for the probe
	 * @param c The Chromosome
	 * @param start Start position
	 * @param end End position
	 * @return The newly generated probe
	 */
	private Probe makeProbe (String name,Chromosome c, int start, int end, int strand) {

		if (end > c.length()) end = c.length();
		if (start < 1) start = 1;

		if (end < start) return null;

		Probe p = new Probe(c,start,end,strand);
		p.setName(name);
		return p;

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

			Feature [] features = collection.genome().annotationCollection().getFeaturesForType(chromosomes[c],featureType);

			for (int f=0;f<features.length;f++) {

				// See if we need to quit
				if (cancel) {
					generationCancelled();
					return;
				}

				if (useSubfeatures  && (features[f].location() instanceof SplitLocation)) {
					SplitLocation location = (SplitLocation)features[f].location();
					Location [] subLocations = location.subLocations();
					for (int s=0;s<subLocations.length;s++) {
						makeProbes(features[f],chromosomes[c],subLocations[s],newProbes);
					}
				}
				else {
					makeProbes(features[f], chromosomes[c], features[f].location(), newProbes);
				}
			}			
		}

		Probe [] finalList = newProbes.toArray(new Probe[0]);

		if (removeDuplicates) {
//			System.out.println("Removing duplicates from original list of "+finalList.length+" probes");
			finalList = removeDuplicates(finalList);
//			System.out.println("Unique list has "+finalList.length+" probes");
		}

		ProbeSet finalSet = new ProbeSet(getDescription(), finalList);

		generationComplete(finalSet);
	}

	/**
	 * Gets the description.
	 * 
	 * @return the description
	 */
	private String getDescription () {
		StringBuffer b = new StringBuffer();

		b.append("Feature generator using ");
		b.append(featureType);

		if (useSubfeatures) {
			b.append(" split into subfeatures");
		}
		if (removeDuplicates) {
			b.append(" duplicates removed");
		}
		
		b.append(" ");
		
		b.append(optionPanel.positionType());
		
		b.append(" from ");
		b.append(startValue);
		b.append("-");
		b.append(endValue);

		return b.toString();
	}

	/**
	 * Removes the duplicates.
	 * 
	 * @param original the original
	 * @return the probe[]
	 */
	private Probe [] removeDuplicates (Probe [] original) {

		if (original == null || original.length == 0) return original;

		Arrays.sort(original);

		Vector<Probe> keepers = new Vector<Probe>();

		Probe lastProbe = original[0];
		keepers.add(original[0]);

		for (int i=1;i<original.length;i++) {
			if (original[i].start() == lastProbe.start() && original[i].end() == lastProbe.end() && original[i].chromosome().equals(lastProbe.chromosome())) {
				// This is a duplicate
				continue;
			}
			keepers.add(original[i]);
			lastProbe = original[i];
		}

		return keepers.toArray(new Probe[0]);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Feature Probe Generator";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
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
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		isReady();
	}

}
