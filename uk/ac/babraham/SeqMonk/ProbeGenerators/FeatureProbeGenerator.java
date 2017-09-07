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
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
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
	private boolean useExonSubfeatures = false;
	private boolean removeDuplicates = true;
	private boolean ignoreDirection = false;
	private String [] featureTypes;


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
		useExonSubfeatures = optionPanel.useExonSubfeatures();
		featureTypes = optionPanel.selectedFeatureTypes();

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
		
		optionPanel = new FeaturePositionSelectorPanel(collection, true, true,true);
		
		optionPanel.setUseSubFeatures(useSubfeatures, useExonSubfeatures);
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

	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// We get the problem here that we won't get per chromosome updates, but this should be
		// so quick I don't think we care:
		
		updateGenerationProgress("Making probes...", 0, 1);
		
		Probe [] finalList = optionPanel.getProbes();

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
		for (int i=0;i<featureTypes.length;i++) {
			b.append(featureTypes[i]);
			b.append(" ");
		}

		if (useSubfeatures) {
			if (useExonSubfeatures) {
				b.append(" split into exons");
			}
			else {
				b.append(" split into introns");
			}
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
