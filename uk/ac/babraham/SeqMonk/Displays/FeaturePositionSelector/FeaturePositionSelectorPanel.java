/**
 * Copyright 2013-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.FeaturePositionSelector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class FeaturePositionSelectorPanel extends JPanel {

	private JList<String> featureTypeBox;
	private JComboBox subFeatureTypeBox;
	private JCheckBox removeDuplicatesCheckbox;
	private JCheckBox ignoreDirectionCheckbox;
	private JComboBox positionTypeBox;
	private JTextField lowValueField;
	private JTextField endField;
	private DataCollection collection;
	private Vector<ChangeListener> listeners = new Vector<ChangeListener>();


	public FeaturePositionSelectorPanel (DataCollection collection, boolean showDirectionOptions, boolean showDuplicatesOptions, boolean allowMultiSelection) {
		this.collection = collection;
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=2;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2, 2, 2, 2);

		add(new JLabel("Features to design around"),gbc);
		
		gbc.gridx = 3;
		gbc.weighty = 0.9;
		gbc.fill = GridBagConstraints.BOTH;
		featureTypeBox = new JList(collection.genome().annotationCollection().listAvailableFeatureTypes());
		if (!allowMultiSelection) {
			featureTypeBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		featureTypeBox.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				fireChanged();
			}
		});
		add(new JScrollPane(featureTypeBox),gbc);

		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		add(new JLabel ("Split into subfeatures"),gbc);

		gbc.gridx = 3;
		subFeatureTypeBox = new JComboBox(new String [] {"No","Exons","Introns"});
		add(subFeatureTypeBox,gbc);

		if (showDuplicatesOptions) {
			gbc.gridy++;
			gbc.gridx = 2;
			gbc.weightx = 0.1;
			add(new JLabel ("Remove exact duplicates"),gbc);

			gbc.gridx = 3;
			removeDuplicatesCheckbox = new JCheckBox();
			removeDuplicatesCheckbox.setSelected(true);
			add(removeDuplicatesCheckbox,gbc);
		}

		if (showDirectionOptions) {

			gbc.gridy++;
			gbc.gridx = 2;
			gbc.weightx = 0.1;
			add(new JLabel ("Ignore feature strand information"),gbc);

			gbc.gridx = 3;
			ignoreDirectionCheckbox = new JCheckBox();
			ignoreDirectionCheckbox.setSelected(false);
			add(ignoreDirectionCheckbox,gbc);

		}

		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		add(new JLabel ("Make probes"),gbc);


		gbc.gridx=3;
		gbc.weightx=0.5;
		JPanel positionPanel = new JPanel();
		positionPanel.setLayout(new BoxLayout(positionPanel, BoxLayout.X_AXIS));

		positionTypeBox = new JComboBox(new String [] {"Over feature","Centered on feature","Upstream of feature","Downstream of feature"});
		positionPanel.add(positionTypeBox,gbc);
		positionPanel.add(new JLabel(" From - "));
		lowValueField = new JTextField("0",5);
		lowValueField.addKeyListener(new NumberKeyListener(false, true));
		positionPanel.add(lowValueField);
		positionPanel.add(new JLabel(" to + "));
		endField = new JTextField("0",5);
		endField.addKeyListener(new NumberKeyListener(false, true));
		positionPanel.add(endField);
		positionPanel.add(new JLabel(" bp"));
		add(positionPanel,gbc);

	}
	
	public boolean isFixedWidth() {
		// This will only *not* be fixed width if the position type is "Over Feature"
		return (! positionTypeBox.getSelectedItem().equals("Over feature"));
	}
	
	public void addChangeListener (ChangeListener l) {
		if (!listeners.contains(l)) {
			listeners.addElement(l);
		}
	}

	public void removeChangeListener (ChangeListener l) {
		if (listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	private void fireChanged() {
		Iterator<ChangeListener>it = listeners.iterator();
		while (it.hasNext()) {
			it.next().stateChanged(new ChangeEvent(this));
		}
	}
	
	public String [] selectedFeatureTypes () {
		Object [] values = featureTypeBox.getSelectedValues();
		String [] returnValues = new String[values.length];
		for (int i=0;i<values.length;i++) {
			returnValues[i] = (String)values[i];
		}
		
		return returnValues;
	}
	
	public boolean useSubFeatures() {
		return !subFeatureTypeBox.getSelectedItem().equals("No");
	}
	
	public boolean useExonSubfeatures () {
		return subFeatureTypeBox.getSelectedItem().equals("Exons");
	}
	
	public void setUseSubFeatures (boolean useSubFeatures, boolean useExons) {
		// The order of the options is No, Exons, Introns
		if (useSubFeatures) {
			if (useExons) {
				subFeatureTypeBox.setSelectedIndex(1);
			}
			else {
				subFeatureTypeBox.setSelectedIndex(2);
			}
		}
		else {
			subFeatureTypeBox.setSelectedIndex(0);
		}
	}
	
	public boolean removeDuplicates () {
		if (removeDuplicatesCheckbox != null) {
			return removeDuplicatesCheckbox.isSelected();
		}
		return false;
	}
	
	public void setRemoveDuplicates (boolean b) {
		removeDuplicatesCheckbox.setSelected(b);
	}
	
	public boolean ignoreDirection () {
		if (ignoreDirectionCheckbox != null) {
			return ignoreDirectionCheckbox.isSelected();
		}
		return false;
	}
	
	public void setIgnoreDirection (boolean b) {
		ignoreDirectionCheckbox.setSelected(b);
	}
	
	public String positionType () {
		return (String)positionTypeBox.getSelectedItem();
	}
	
	public int startOffset () {
		if (lowValueField.getText().length()>0) {
			return Integer.parseInt(lowValueField.getText());
		}
		return 0;
	}

	public int endOffset () {
		if (endField.getText().length()>0) {
			return Integer.parseInt(endField.getText());
		}
		return 0;
	}
	
	/**
	 * Gets the set of probes with appropriate context for the options 
	 * currently set.
	 * @return
	 */
	public Probe [] getProbes () {
		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		Vector<Probe> newProbes = new Vector<Probe>();

		for (int c=0;c<chromosomes.length;c++) {

			Vector<Feature> allFeatures = new Vector<Feature>();
			
			String [] selectedFeatureTypes = selectedFeatureTypes();
			
			for (int f=0; f<selectedFeatureTypes.length;f++) {
				Feature [] features = collection.genome().annotationCollection().getFeaturesForType(chromosomes[c],selectedFeatureTypes[f]);
				
				for (int i=0;i<features.length;i++) {
					allFeatures.add(features[i]);
				}
			}

			Feature [] features = allFeatures.toArray(new Feature[0]);
						
			for (int f=0;f<features.length;f++) {

				
				if (useSubFeatures()) {
					// We need to split this up so get the sub-features
					if (features[f].location() instanceof SplitLocation) {
						SplitLocation location = (SplitLocation)features[f].location();
						Location [] subLocations = location.subLocations();
						if (useExonSubfeatures()) {
//							System.err.println("Making exon probes");
							for (int s=0;s<subLocations.length;s++) {
								makeProbes(features[f],chromosomes[c],subLocations[s],newProbes,false);
							}							
						}
						else {
//							System.err.println("Making intron probes");
							// We're making introns
							for (int s=1;s<subLocations.length;s++) {
								makeProbes(features[f],chromosomes[c],new Location(subLocations[s-1].end()+1, subLocations[s].start()-1, features[f].location().strand()),newProbes,false);
							}							
						}
					}
					else {
						if (useExonSubfeatures()) {
							// We can still make a single probe
							makeProbes(features[f], chromosomes[c], features[f].location(), newProbes,false);
						}
						// If we're making introns then we're stuffed and we give up.
					}
				}
				else {
					makeProbes(features[f], chromosomes[c], features[f].location(), newProbes,false);
				}
			}			
		}

		Probe [] finalList = newProbes.toArray(new Probe[0]);
		
		if (removeDuplicates()) {
			finalList = removeDuplicates(finalList);
		}

		return finalList;

	}
	
	/**
	 * Gets the set of locations for the core of each feature.  This wouldn't
	 * include additional context added by the options, but would have subtracted
	 * context removed by the options.
	 * 
	 * @return
	 */
	public Probe [] getCoreProbes () {
		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		Vector<Probe> newProbes = new Vector<Probe>();

		for (int c=0;c<chromosomes.length;c++) {

			Vector<Feature> allFeatures = new Vector<Feature>();
			
			String [] selectedFeatureTypes = selectedFeatureTypes();
			
			for (int f=0; f<selectedFeatureTypes.length;f++) {
				Feature [] features = collection.genome().annotationCollection().getFeaturesForType(chromosomes[c],selectedFeatureTypes[f]);
				
				for (int i=0;i<features.length;i++) {
					allFeatures.add(features[i]);
				}
			}

			Feature [] features = allFeatures.toArray(new Feature[0]);

			for (int f=0;f<features.length;f++) {

				if (useSubFeatures()) {
					// We need to split this up so get the sub-features
					if (features[f].location() instanceof SplitLocation) {
						SplitLocation location = (SplitLocation)features[f].location();
						Location [] subLocations = location.subLocations();
						if (useExonSubfeatures()) {
							for (int s=0;s<subLocations.length;s++) {
								makeProbes(features[f],chromosomes[c],subLocations[s],newProbes,true);
							}							
						}
						else {
							// We're making introns
							for (int s=1;s<subLocations.length;s++) {
								makeProbes(features[f],chromosomes[c],new Location(subLocations[s-1].end()+1, subLocations[s].start()-1, features[f].location().strand()),newProbes,true);
							}							
						}
					}
					else {
						if (useExonSubfeatures()) {
							// We can still make a single probe
							makeProbes(features[f], chromosomes[c], features[f].location(), newProbes,true);
						}
						// If we're making introns then we're stuffed and we give up.
					}
				}
				else {
					makeProbes(features[f], chromosomes[c], features[f].location(), newProbes,true);
				}
			}			
		}

		Probe [] finalList = newProbes.toArray(new Probe[0]);

		if (removeDuplicates()) {
			finalList = removeDuplicates(finalList);
		}
		
		return finalList;	
	}
	
	
	/**
	 * Gets the set of additional upstream context regions for each feature given
	 * the current options.  If the current options don't allow for any upstream
	 * context then this returns null.
	 * @return
	 */
	public Probe [] getUpstreamProbes () {
		
		// This is horribly inefficient since we make the core list
		// multiple times
		Probe [] coreProbes = getCoreProbes();
		
		
		if (positionType().equals("Over feature")  && startOffset() > 0) {
			Vector<Probe> upstreamProbes = new Vector<Probe>();
			
			for (int p=0;p<coreProbes.length;p++) {
				if (coreProbes[p].strand() == Probe.REVERSE) {
					int start = coreProbes[p].end()+1;
					int end = coreProbes[p].end()+startOffset();
					
					if (start < 1 || end > coreProbes[p].chromosome().length()) continue;
					
					upstreamProbes.add(new Probe(coreProbes[p].chromosome(),start,end,coreProbes[p].strand()));
				}
				else {
					int start = coreProbes[p].start()-startOffset();
					int end = coreProbes[p].start()-1;
					
					if (start < 1 || end > coreProbes[p].chromosome().length()) continue;

					upstreamProbes.add(new Probe(coreProbes[p].chromosome(),start,end,coreProbes[p].strand()));
				}
			}
			
			
			return upstreamProbes.toArray(new Probe[0]);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Gets the set of additional downstream context regions for each feature given
	 * the current options.  If the current options don't allow for any downstream
	 * context then this returns null.
	 * @return
	 */
	public Probe [] getDownstreamProbes () {
		// This is horribly inefficient since we make the core list
		// multiple times
		Probe [] coreProbes = getCoreProbes();
		
		
		if (positionType().equals("Over feature")  && endOffset() > 0) {
			Vector<Probe> downstreamProbes = new Vector<Probe>();
			
			for (int p=0;p<coreProbes.length;p++) {
				if (coreProbes[p].strand() == Probe.REVERSE) {
					
					int start = coreProbes[p].start()-endOffset();
					int end = coreProbes[p].start()-1;
					
					if (start < 1 || end > coreProbes[p].chromosome().length()) continue;

					
					downstreamProbes.add(new Probe(coreProbes[p].chromosome(),start,end,coreProbes[p].strand()));
				}
				else {
					
					int start = coreProbes[p].end()+1;
					int end = coreProbes[p].end()+endOffset();
					
					if (start < 1 || end > coreProbes[p].chromosome().length()) continue;
					
					downstreamProbes.add(new Probe(coreProbes[p].chromosome(),start,end,coreProbes[p].strand()));
				}
			}
			
			return downstreamProbes.toArray(new Probe[0]);
		}
		else {
			return null;
		}
	}
	
	
	
	private void makeProbes (Feature feature, Chromosome chromosome, Location location, Vector<Probe> newProbes, boolean justCore) {
		int start;
		int end;
		int strand = location.strand();

		if (ignoreDirection()) strand = Location.UNKNOWN;

		if (positionType().equals("Upstream of feature")) {
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = location.start()-startOffset();
				end = location.start()+endOffset();
			}
			else {
				start = location.end()-endOffset();
				end = location.end()+startOffset();
			}

			Probe p = makeProbe(feature.name()+"_upstream",chromosome,start,end,strand);
			if (p != null) {
				newProbes.add(p);
			}
		}

		else if (positionType().equals("Over feature")) {
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				if (justCore && startOffset()>0) {
					start = location.start();
				}
				else {
					start = location.start()-startOffset();
				}
				if (justCore && endOffset() > 0) {
					end = location.end();
				}
				else {
					end = location.end()+endOffset();					
				}
			}
			else {
				if (justCore && endOffset()>0) {
					start = location.start();
				}
				else {
					start = location.start()-endOffset();					
				}
				if (justCore && startOffset() > 0) {
					end = location.end();
				}
				else {
					end = location.end()+startOffset();					
				}
			}

			Probe p = makeProbe(feature.name(),chromosome,start,end,strand);
			if (p != null) {
				newProbes.add(p);
			}
		}


		else if (positionType().equals("Downstream of feature")) {
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = location.end()-startOffset();
				end = location.end()+endOffset();
			}
			else {
				start = location.start()-endOffset();
				end = location.start()+startOffset();
			}

			Probe p = makeProbe(feature.name()+"_downstream",chromosome,start,end,strand);
			if (p != null) {
				newProbes.add(p);
			}
		}

		else if (positionType().equals("Centered on feature")) {
			
			int center = location.start()+((location.end()-location.start())/2);
		
			if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
				start = center-startOffset();
				end = center+endOffset();
			}
			else {
				start = center-endOffset();
				end = center+startOffset();
			}

			Probe p = makeProbe(feature.name(),chromosome,start,end,strand);
			if (p != null) {
				newProbes.add(p);
			}
		}
		
		else {
			throw new IllegalStateException("Unknown position type "+positionType());
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




}
