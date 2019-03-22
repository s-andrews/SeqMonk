/**
 * Copyright 2014-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Pipelines.Transcription;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;

public class ActiveTranscriptionPipeline extends RNASeqPipeline {

	public ActiveTranscriptionPipeline(DataCollection collection) {
		super(collection);
	}

	protected FeatureGroup [] mergeTranscripts (Feature [] features, boolean merge) {

//		System.err.println("Getting merged transcripts for introns");
		
		if (merge) {
			Hashtable<String, FeatureGroup> groupedNames = new Hashtable<String, FeatureGroup>();

			for (int i=0;i<features.length;i++) {
				String name = features[i].name().replaceFirst("-\\d\\d\\d$", "");
				if (!groupedNames.containsKey(name)) {
					groupedNames.put(name,new IntronFeatureGroup(name));
				}

				groupedNames.get(name).addFeature(features[i]);
			}

			
			FeatureGroup [] mergedTranscripts = new IntronFeatureGroup [groupedNames.size()];
			
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

			mergedTranscripts = removeEmptyTranscripts(mergedTranscripts);
			
			// We now need to sort these
			Arrays.sort(mergedTranscripts);

			return mergedTranscripts;

		}
		else {
			FeatureGroup [] mergedTranscripts = new IntronFeatureGroup [features.length];
			for (int i=0;i<features.length;i++) {
				FeatureGroup fg = new IntronFeatureGroup(features[i].name());
				fg.addFeature(features[i]);
				mergedTranscripts[i] = fg;
			}

			return mergedTranscripts;
		}
	}
	
	private FeatureGroup [] removeEmptyTranscripts (FeatureGroup [] startingGroup) {
		
		Vector<FeatureGroup> keepers = new Vector<FeatureGroup>();
		
		for (int i=0;i<startingGroup.length;i++) {
			
			if (startingGroup[i].getSubLocations().length == 0) continue;
			keepers.add(startingGroup[i]);
		}
		 
		return keepers.toArray(new FeatureGroup[0]);
	}
	

	
	protected String getQuantitationDescription (boolean mergeTranscripts, boolean applyTranscriptLengthCorrection, boolean correctDNAContamination, boolean logTransform, boolean rawCounts) {
		
		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Active transcription pipeline quantitation");
		if (mergeTranscripts) {
			quantitationDescription.append(" on merged transcripts");
		}

		quantitationDescription.append(" counting reads over introns");
		
		if (rawCounts) {
			quantitationDescription.append(" as raw counts");
		}

		else {
			if (applyTranscriptLengthCorrection) {
				quantitationDescription.append(" correcting for feature length");
			}

			if (correctDNAContamination) {
				quantitationDescription.append(" correcting for DNA contamination");
			}

			if (logTransform) {
				quantitationDescription.append(". Log transformed");
			}
		}

		quantitationDescription.append(". Assuming a ");
		quantitationDescription.append(optionsPanel.libraryTypeBox.getSelectedItem());
		quantitationDescription.append(" library");
		
		return (quantitationDescription.toString());
	}
	
	protected FeatureGroup [] splitNonOverlappingFeatureGroup (FeatureGroup group) {
		
		Feature [] features = group.features();
//		System.err.println("Group has "+features.length+" features");
		
		// Make a set of non-overlapping feature groups.
		
		Vector<FeatureGroup> splitGroups = new Vector<FeatureGroup>();
		
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
			FeatureGroup newFeatureGroup = new IntronFeatureGroup(group.name());
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

	
	
	public String name() {
		return "Active transcription quantitation pipeline";
	}
}
