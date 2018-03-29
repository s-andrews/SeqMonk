/**
 * Copyright Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Quantitation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * A quantitation based on the distance between each probe and the
 * nearest feature of a given class.
 */
public class DistanceToFeatureQuantitation extends Quantitation {

	private JPanel optionPanel = null;
	private JComboBox featureSelector;
	private JComboBox distanceTypeSelector;
	private JCheckBox logTransform;

	// Some constants to store positions
	private static final int MIDDLE_MIDDLE = 10;
	private static final int CLOSEST = 20;
	
	private static double log2 = Math.log(2);
	
	/** Which feature are we measuring the distance to */
	private String selectedFeature;
	
	/** Which position in the probe are we using */
	private int probePosition;
		
	/** The stores we're going to quantitate. */
	private DataStore [] data;
	
	/** Whether we're log transforming */
		
	public DistanceToFeatureQuantitation(SeqMonkApplication application) {
		super(application);
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		this.data = data;
		
		selectedFeature = featureSelector.getSelectedItem().toString();
		
		probePosition = getPosition(distanceTypeSelector.getSelectedItem().toString());
		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}
	
	
	private int getPosition (String description) {
		if (description.equals("Middle to Middle")) return MIDDLE_MIDDLE;
		if (description.equals("Closest Edges")) return CLOSEST;
		
		throw new IllegalArgumentException("No position matches "+description);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
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
		
		optionPanel.add(new JLabel("Measure distance to feature"),gbc);
		
		gbc.gridx = 2;
		featureSelector = new JComboBox(application.dataCollection().genome().annotationCollection().listAvailableFeatureTypes());
		featureSelector.setPrototypeDisplayValue("No longer than this please");
		optionPanel.add(featureSelector,gbc);
		
		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Distance to measure"),gbc);
		
		gbc.gridx = 2;
		distanceTypeSelector = new JComboBox(new String [] {"Closest Edges","Middle to Middle"});		
		optionPanel.add(distanceTypeSelector,gbc);
		
		gbc.gridx = 1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Log transform"),gbc);
		
		gbc.gridx = 2;
		logTransform = new JCheckBox();
		optionPanel.add(logTransform,gbc);
				
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}
	
	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("Distance to feature quantitation measuring from ");
		sb.append(distanceTypeSelector.getSelectedItem());
		sb.append(selectedFeature);
		if (logTransform.isSelected()) {
			sb.append(" log transformed");
		}
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
		
		Feature [] features = null;
		Chromosome lastChromsome = null;
		int lastIndex = 0;
		
		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			
			// See if we're on the same chromosome as last time
			if (lastChromsome == null || probes[p].chromosome() != lastChromsome) {
				lastChromsome = probes[p].chromosome();
				features = application.dataCollection().genome().annotationCollection().getFeaturesForType(lastChromsome, selectedFeature);
				System.err.println("Found "+features.length+" features of type "+selectedFeature+" on chr "+lastChromsome);
				lastIndex = 0;
			}
			
			int closestDistance = lastChromsome.length();
			int bestIndex = lastIndex;
			
			// Find the closest feature.  Work our way back until we're at the start or we've reached a
			// feature where the end is before our start
			
			for (int i=lastIndex;i>=0  && i<features.length;i--) {
				int thisDistance = getDistanceToFeature(probes[p],features[i]);
				if (thisDistance < closestDistance) {
					closestDistance = thisDistance;
					bestIndex = i;
				}
				
				if (features[i].location().end() < probes[p].start()) break;
				
			}
			
			// Now we go forward until we hit the end or we're after the end of the last best feature
			for (int i=lastIndex+1;i<features.length;i++) {
				int thisDistance = getDistanceToFeature(probes[p],features[i]);
				if (thisDistance < closestDistance) {
					closestDistance = thisDistance;
					bestIndex = i;
				}
				
				if (features[i].location().start() > Math.max(probes[p].end(),features[lastIndex].location().end())) break;
				
			}
			
			
			lastIndex = bestIndex;
			
			
			for (int d=0;d<data.length;d++) {
				if (logTransform.isSelected()) {
					data[d].setValueForProbe(probes[p], (float)(Math.log(closestDistance+1)/log2));
				}
				else {
					data[d].setValueForProbe(probes[p], closestDistance);					
				}
			}
			
		}

		quantitatonComplete();
		
	}
	
	private int getDistanceToFeature (Probe p, Feature f) {

		switch (probePosition) {
		case MIDDLE_MIDDLE : 
			return Math.abs(SequenceRead.midPoint(p.packedPosition())-SequenceRead.midPoint(f.location().packedPosition()));
		
		case CLOSEST : 
			if (SequenceRead.overlaps(p.packedPosition(), f.location().packedPosition())) {
				return 0;
			}
			else {
				if (SequenceRead.start(p.packedPosition()) > SequenceRead.start(f.location().packedPosition())) {
					return (SequenceRead.start(p.packedPosition())-SequenceRead.end(f.location().packedPosition()));
				}
				else {
					return (SequenceRead.start(f.location().packedPosition())-SequenceRead.end(p.packedPosition()));
					
				}
			}
		
		
		default:
			throw new IllegalStateException("Probe position "+probePosition+" didn't match any exepcted value");
		
		}
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Distance to Feature Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
