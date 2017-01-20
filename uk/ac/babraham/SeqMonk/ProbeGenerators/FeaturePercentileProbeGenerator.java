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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Displays.FeaturePositionSelector.FeaturePercentileSelectorPanel;

/**
 * Generates probes tiled over a set of features.  The same number of probes is made
 * over each feature.  The probes can either be fixed size, or prorportion to the length
 * of the feature.
 */
public class FeaturePercentileProbeGenerator extends ProbeGenerator implements Runnable, KeyListener, ActionListener {


	
	/** The options panel. */
	private FeaturePercentileSelectorPanel optionPanel = null;

	private boolean useSubfeatures = false;
	private boolean ignoreDirection = false;
	private String featureType;

	private int lengthType = FeaturePercentileSelectorPanel.PROPORTIONAL_LENGTH;
	private int fixedLength = 100;
	private boolean includeStartEnd = true;
	private int probesPerFeature = 5;
	

	/**
	 * Instantiates a new feature probe generator.
	 * 
	 * @param collection The dataCollection into which the probes will be placed.
	 */
	public FeaturePercentileProbeGenerator(DataCollection collection) {
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
		lengthType = optionPanel.lengthType();
		fixedLength = optionPanel.fixedLength();
		useSubfeatures = optionPanel.useSubFeatures();
		featureType = optionPanel.selectedFeatureType();
		includeStartEnd = optionPanel.includeStartEnd();
		probesPerFeature = optionPanel.probesPerFeature();

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
		
		optionPanel = new FeaturePercentileSelectorPanel(collection);
		
		optionPanel.setUseSubFeatures(useSubfeatures);
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

		if (lengthType == FeaturePercentileSelectorPanel.PROPORTIONAL_LENGTH) {
			
			int probeLength = feature.location().length()/probesPerFeature;
			
			
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = location.start();
				end = location.start();
			}
			else {
				start = location.end();
				end = location.end();
			}

			for (int i=0;i<probesPerFeature;i++) {
			
				int localStart;
				int localEnd;

				if (strand == Location.REVERSE) {
					localStart = end-(probeLength*(i+1));
					localEnd = localStart+probeLength;
				}
				else {
					localStart = start+(probeLength*i);
					localEnd = localStart+probeLength;
				}

				
				Probe p = makeProbe(feature.name()+"_"+(i+1),chromosome,localStart,localEnd,strand);
				if (p != null) {

					// Add this probe 
					newProbes.add(p);
				
				}
			}
		}

		else if (lengthType == FeaturePercentileSelectorPanel.FIXED_LENGTH) {
			int proportionalLength;
			
			if (includeStartEnd) {
				proportionalLength = feature.location().length()/(probesPerFeature-1);
			}
			else {
				proportionalLength = feature.location().length()/probesPerFeature;
			}
			
			
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				if (includeStartEnd) {
					start = location.start();
					end = location.end();
				}
				else {
					start = location.start()+(proportionalLength/2);
					end = location.end()-(proportionalLength/2);					
				}
			}
			else {
				if (includeStartEnd) {
					start = location.end();
					end = location.end();
				}
				else {
					start = location.end()+(proportionalLength/2);
					end = location.end()-(proportionalLength/2);					
				}
			}

			for (int i=0;i<probesPerFeature;i++) {
			
				int localStart;
				int localEnd;
				int localCentre;

				if (strand == Location.REVERSE) {
					localCentre = (end-(proportionalLength*i));
					localStart = localCentre-(fixedLength/2);
					localEnd = localStart+fixedLength;
				}
				else {
					localCentre = start+(proportionalLength*i);
					localStart = localCentre-(fixedLength/2);
					localEnd = localStart+fixedLength;
				}

				
				Probe p = makeProbe(feature.name()+"_"+(i+1),chromosome,localStart,localEnd,strand);
				if (p != null) {

					// Add this probe 
					newProbes.add(p);
					
				}
			}
		}


		else {
			throw new IllegalStateException("Unknown length type "+optionPanel.lengthType());
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

		b.append("Proportional feature generator using ");
		b.append(featureType);

		if (useSubfeatures) {
			b.append(" split into subfeatures");
		}
				
		if (ignoreDirection) {
			b.append(" direction ignored");
		}
		
		if (lengthType == FeaturePercentileSelectorPanel.FIXED_LENGTH) {
			b.append(" using ");
			b.append(probesPerFeature);
			b.append(" fixed width probes of size ");
			b.append(fixedLength);
			
			if (!includeStartEnd) {
				b.append(" excluding start and end positions");
			}
		}
		else {
			b.append(" using ");
			b.append(probesPerFeature);
			b.append(" variable width probes");
		}

		return b.toString();
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Feature Percentile Probe Generator";
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
