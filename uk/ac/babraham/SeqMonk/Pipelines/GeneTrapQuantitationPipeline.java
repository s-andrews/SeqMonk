/**
 * Copyright 2011-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Pipelines;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

/**
 * This pipeline quantitates gene trap libraries.  It is a read count quantitation
 * but only counts reads which either overlap an exon in a transcript, or which 
 * are within an intron, but are in the same orientation as the gene.
 * 
 * @author andrewss
 *
 */
public class GeneTrapQuantitationPipeline extends Pipeline {

	private GeneTrapOptionsPanel optionsPanel;

	public GeneTrapQuantitationPipeline (DataCollection collection) {
		super(collection);
		optionsPanel = new GeneTrapOptionsPanel(collection.genome().annotationCollection().listAvailableFeatureTypes());
	}

	public JPanel getOptionsPanel(SeqMonkApplication application) {
		return optionsPanel;
	}

	public boolean isReady() {
		return true;
	}

	public boolean createsNewProbes () {
		return true;
	}

	private FeatureGroup [] mergeTranscripts (Feature [] features) {


		Hashtable<String, FeatureGroup> groupedNames = new Hashtable<String, FeatureGroup>();

		for (int i=0;i<features.length;i++) {
			String name = features[i].name().replaceFirst("-\\d\\d\\d$", "");
			if (!groupedNames.containsKey(name)) {
				groupedNames.put(name,new FeatureGroup(name));
			}

			groupedNames.get(name).addFeature(features[i]);
		}


		FeatureGroup [] mergedTranscripts = new FeatureGroup [groupedNames.size()];

		int i=0;
		Enumeration<String> en = groupedNames.keys();
		while (en.hasMoreElements()) {
			mergedTranscripts[i] = groupedNames.get(en.nextElement());
			i++;
		}

		//			System.err.println("Before splitting we had "+mergedTranscripts.length+" groups");

		// Add an extra constraint that the features within the group must overlap
		// to avoid problems where the same gene name is used multiple times on different
		// parts of the chromosome.
		mergedTranscripts = splitNonOverlappingTranscripts(mergedTranscripts);

		//			System.err.println("After splitting we had "+mergedTranscripts.length+" groups");

		// We now need to sort these
		Arrays.sort(mergedTranscripts);

		return mergedTranscripts;

	}

	private FeatureGroup [] splitNonOverlappingTranscripts (FeatureGroup [] startingGroup) {

		Vector<FeatureGroup> splitGroup = new Vector<GeneTrapQuantitationPipeline.FeatureGroup>();

		for (int i=0;i<startingGroup.length;i++) {

			FeatureGroup [] splitGroups = splitNonOverlappingFeatureGroup(startingGroup[i]);
			for (int j=0;j<splitGroups.length;j++) {
				splitGroup.add(splitGroups[j]);
			}
		}

		return splitGroup.toArray(new FeatureGroup[0]);
	}

	private FeatureGroup [] splitNonOverlappingFeatureGroup (FeatureGroup group) {

		Feature [] features = group.features();
		//		System.err.println("Group has "+features.length+" features");

		// Make a set of non-overlapping feature groups.

		Vector<FeatureGroup> splitGroups = new Vector<GeneTrapQuantitationPipeline.FeatureGroup>();

		FEATURE: for (int f=0;f<features.length;f++) {
			Enumeration<FeatureGroup> en = splitGroups.elements();
			while (en.hasMoreElements()) {
				FeatureGroup fg = en.nextElement();
				if (fg.overlaps(features[f].location())) {
					//					System.err.println("Feature from "+features[f].location().start()+" to "+features[f].location().end()+" overlaps with "+fg.start()+" to "+fg.end());
					fg.addFeature(features[f]);
					continue FEATURE;
				}
			}

			// If we get here then we need to make a new group.
			//			System.err.println("Feature from "+features[f].location().start()+" to "+features[f].location().end()+" needs a new group");
			FeatureGroup newFeatureGroup = new FeatureGroup(group.name);
			newFeatureGroup.addFeature(features[f]);
			splitGroups.add(newFeatureGroup);
		}

		//		System.err.println("Initial split groups is "+splitGroups.size());
		// Now we need to go round trying to merge groups together.

		boolean joinedSomething = true;

		JOIN: while (joinedSomething) {
			joinedSomething = false;

			Enumeration<FeatureGroup> en = splitGroups.elements();

			while (en.hasMoreElements()) {
				FeatureGroup primaryGroup = en.nextElement();

				Enumeration<FeatureGroup> en2 = splitGroups.elements();
				while (en2.hasMoreElements()) {
					FeatureGroup compareGroup = en2.nextElement();

					if (compareGroup == primaryGroup) continue; // Can't merge with itself.

					if (compareGroup.start()<=primaryGroup.end() && compareGroup.end() >= primaryGroup.start()) {
						//						System.err.println("Group from "+compareGroup.start()+" to "+compareGroup.end()+" overlaps with "+primaryGroup.start()+" to "+primaryGroup.end());
						// They match and can be merged.
						Feature [] featuresToMerge = compareGroup.features();
						for (int f=0;f<featuresToMerge.length;f++) {
							primaryGroup.addFeature(featuresToMerge[f]);
						}

						splitGroups.remove(compareGroup);
						joinedSomething = true;
						continue JOIN;
					}
				}
			}


		}



		return splitGroups.toArray(new FeatureGroup[0]);
	}

	protected void startPipeline() {

		// We first need to generate probes over all of the features listed in
		// the feature types.  The probes should cover the whole area of the
		// feature regardless of where it splices.

		Vector<Probe> probes = new Vector<Probe>();
		boolean logTransform = optionsPanel.logTransform();
		boolean rawCounts = optionsPanel.rawCountsBox.isSelected();
		if (rawCounts) {
			logTransform = false;
		}

		Chromosome [] chrs = collection().genome().getAllChromosomes();

		for (int c=0;c<chrs.length;c++) {
			//			System.err.println("Processing chr "+chrs[c].name());
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Making features for chr"+chrs[c].name(), c, chrs.length*2);

			Feature [] features = collection().genome().annotationCollection().getFeaturesForType(chrs[c], optionsPanel.getSelectedFeatureType());
			Arrays.sort(features);

			FeatureGroup [] mergedTranscripts = mergeTranscripts(features);

			for (int f=0;f<mergedTranscripts.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}
				probes.add(new Probe(chrs[c], mergedTranscripts[f].start(), mergedTranscripts[f].end(), mergedTranscripts[f].strand(),mergedTranscripts[f].name));

			}
		}

		Probe [] allProbes = probes.toArray(new Probe[0]);

		collection().setProbeSet(new ProbeSet("Transcript features over "+optionsPanel.getSelectedFeatureType(), allProbes));

		// Having made probes we now need to quantitate them.  We'll fetch the
		// probes overlapping each sub-feature and then aggregate these together
		// to get the final quantitation.

		int currentIndex = 0;
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Quantitating features on chr"+chrs[c].name(), chrs.length+c, chrs.length*2);

			Feature [] features = collection().genome().annotationCollection().getFeaturesForType(chrs[c], optionsPanel.getSelectedFeatureType());
			Arrays.sort(features);

			FeatureGroup [] mergedTranscripts = mergeTranscripts(features);

			for (int f=0;f<mergedTranscripts.length;f++) {

				Location [] subLocations = mergedTranscripts[f].getSubLocations();


				//				System.err.println("Transcript "+mergedTranscripts[f].name+" had length "+totalLength);


				for (int d=0;d<data.length;d++) {
					if (cancel) {
						progressCancelled();
						return;
					}

					int totalCount = 0;

					long [] reads = data[d].getReadsForProbe(new Probe(chrs[c], mergedTranscripts[f].start(), mergedTranscripts[f].end()));

					READ: for (int r=0;r<reads.length;r++) {

						if (SequenceRead.strand(reads[r]) == mergedTranscripts[f].strand()) {
							//TODO: Should we check if we're within the CDS?
							++totalCount;
							continue;
						}
						
						// We'll still count this if it overlaps with an exon
						for (int s=0;s<subLocations.length;s++) {
							if (SequenceRead.overlaps(reads[r], subLocations[s].packedPosition())) {
								++totalCount;
								continue READ;
							}
						}
						
					}

					float value = totalCount;

					// If we're log transforming then we need to set zero values to 0.9
					if (logTransform && value == 0) {
						value = 0.9f;
					}


					//					System.err.println("Length corrected read count for "+mergedTranscripts[f].name+" is "+totalCount);


					// We also correct by the total read count
					if (!rawCounts) {
						value /= (data[d].getTotalReadCount()/1000000f);
					}

					//					System.err.println("Total corrected read count for "+mergedTranscripts[f].name+" is "+totalCount);


					// Finally we do the log transform if we've been asked to
					if (logTransform) {
						value = (float)Math.log(value)/log2;
					}

					data[d].setValueForProbe(allProbes[currentIndex], value);

				}

				currentIndex++;
			}
		}

		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Gene trap quantitation on transcripts of type ");
		quantitationDescription.append(optionsPanel.getSelectedFeatureType());
		

		if (logTransform) {
			quantitationDescription.append(". Log transformed");
		}

		collection().probeSet().setCurrentQuantitation(quantitationDescription.toString());

		quantitatonComplete();

	}

	public String name() {
		return "Gene trap quantitation pipeline";
	}


	private class GeneTrapOptionsPanel extends JPanel {

		JComboBox featureTypeBox;
		JCheckBox rawCountsBox;
		JCheckBox logTransformBox;

		public GeneTrapOptionsPanel (String [] featureTypes) {

			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			add(new JLabel("Transcript features "),gbc);

			gbc.gridx=2;

			featureTypeBox = new JComboBox(featureTypes);
			featureTypeBox.setPrototypeDisplayValue("No longer than this please");
			for (int i=0;i<featureTypes.length;i++) {
				if (featureTypes[i].toLowerCase().equals("mrna")) {
					featureTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(featureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Generate Raw Counts"),gbc);

			gbc.gridx=2;

			rawCountsBox = new JCheckBox();
			rawCountsBox.setSelected(false);
			rawCountsBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					logTransformBox.setEnabled(!rawCountsBox.isSelected());
				}
			});

			add(rawCountsBox,gbc);	

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Log transform"),gbc);

			gbc.gridx=2;

			logTransformBox = new JCheckBox();
			logTransformBox.setSelected(true);

			add(logTransformBox,gbc);	

		}

		public boolean logTransform () {
			return logTransformBox.isSelected();
		}


		public String getSelectedFeatureType () {
			return (String)featureTypeBox.getSelectedItem();
		}

	}

	private class FeatureGroup implements Comparable<FeatureGroup> {

		private String name;
		private Vector<Feature>features = new Vector<Feature>();
		public FeatureGroup (String name) {
			this.name = name;
		}

		public void addFeature (Feature f) {
			features.add(f);
		}

		public int start () {
			int start = 0;
			Enumeration<Feature> en = features.elements();
			while (en.hasMoreElements()) {
				int thisStart = en.nextElement().location().start();

				if (start == 0 || thisStart < start) start = thisStart;

			}

			return start;			
		}

		public int end () {
			int end = 0;
			Enumeration<Feature> en = features.elements();
			while (en.hasMoreElements()) {
				int thisEnd = en.nextElement().location().end();

				if (end == 0 || thisEnd > end) end = thisEnd;

			}

			return end;

		}

		public boolean overlaps (Location l) {
			if (l.start()<=end() && l.end() >= start()) return true;
			return false;
		}

		public Feature [] features () {
			return features.toArray(new Feature[0]);
		}

		public int strand () {
			return features.elementAt(0).location().strand();
		}

		public Location [] getSubLocations () {

			// See if we can take some shortcuts

			if (features.size() == 1) {
				Location loc = features.elementAt(0).location();
				if (loc instanceof SplitLocation) {
					return ((SplitLocation)loc).subLocations();
				}
				else {
					return new Location[]{loc};
				}
			}

			LongVector allLocs = new LongVector();

			Enumeration<Feature>en = features.elements();
			while (en.hasMoreElements()) {
				Location loc = en.nextElement().location();

				if (loc instanceof SplitLocation) {
					Location [] subLocs = ((SplitLocation)loc).subLocations();
					for (int s=0;s<subLocs.length;s++) {
						allLocs.add(subLocs[s].packedPosition());
					}
				}
				else {
					allLocs.add(loc.packedPosition());
				}
			}

			long [] locs = allLocs.toArray();

			SequenceRead.sort(locs);

			Vector<Location> mergedLocs = new Vector<Location>();

			long current = locs[0];

			for (int i=1;i<locs.length;i++) {
				//				if (debug) {System.err.println("Looking at "+SequenceRead.start(locs[i])+"-"+SequenceRead.end(locs[i])+" current is "+SequenceRead.start(current)+"-"+SequenceRead.end(current));}
				if (SequenceRead.overlaps(current, locs[i]) && SequenceRead.end(locs[i]) > SequenceRead.end(current)) {
					//					if (debug) {System.err.println("They overlap, extending...");}					
					current = SequenceRead.packPosition(SequenceRead.start(current), SequenceRead.end(locs[i]), SequenceRead.strand(current));
				}
				else if (SequenceRead.end(locs[i]) <= SequenceRead.end(current)) {
					// Just ignore this since it's a subset of the region we're already looking at.
					//					if (debug) {System.err.println("This is a subset, ignoring it");}
					continue;

				}
				else {
					//					if (debug) {System.err.println("They don't overlap, moving on...");}					
					mergedLocs.add(new Location(current));
					current = locs[i];
				}
			}

			mergedLocs.add(new Location(current));

			return mergedLocs.toArray(new Location[0]);

		}

		public int compareTo(FeatureGroup o) {
			if (start() != o.start()) return start() - o.start();
			else if (end() != o.end()) return end()- o.end();
			else return strand()-o.strand();
		}

	}

}
