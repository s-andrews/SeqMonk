/**
 * Copyright 2014-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities.Features;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

public class FeatureMerging {

	public static Feature [] getNonOverlappingLocationsForFeatures (Feature [] features, boolean useSubFeatures) {
		
		// Start by splitting these into chromosomes
		Hashtable<String, Vector<Feature>> chrs = new Hashtable<String, Vector<Feature>>();
		
		for (int f=0;f<features.length;f++) {
			if (!chrs.containsKey(features[f].chromosomeName())) {
				chrs.put(features[f].chromosomeName(),new Vector<Feature>());
			}
			
			chrs.get(features[f].chromosomeName()).add(features[f]);
		}
		
		// Now we build up a set of features for each chromosome
		
		Vector<Feature>returnFeatures = new Vector<Feature>();
		
		Enumeration<String> chrNames = chrs.keys();
		while (chrNames.hasMoreElements()) {
			String chr = chrNames.nextElement();
			
			Location [] locs = getNonOverlappingFeaturesWithinChromosome(chrs.get(chr).toArray(new Feature[0]), useSubFeatures);
			
			for (int l=0;l<locs.length;l++) {
				Feature f=new Feature("merged_location", chr);
				f.setLocation(locs[l]);
				returnFeatures.add(f);
			}
		
		}
		
		
		return (returnFeatures.toArray(new Feature[0]));
		
		
	}
	
	
	private static Location [] getNonOverlappingFeaturesWithinChromosome (Feature [] features, boolean useSubFeatures) {
		
		// See if we can take some shortcuts
		if (features.length == 1) {
			Location loc = features[0].location();
			if (loc instanceof SplitLocation) {
				return ((SplitLocation)loc).subLocations();
			}
			else {
				return new Location[]{loc};
			}
		}

		LongVector allLocs = new LongVector();

		for (int f=0;f<features.length;f++) {
			Location loc = features[f].location();

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
//			if (debug) {System.err.println("Looking at "+SequenceRead.start(locs[i])+"-"+SequenceRead.end(locs[i])+" current is "+SequenceRead.start(current)+"-"+SequenceRead.end(current));}
			if (SequenceRead.overlaps(current, locs[i]) && SequenceRead.end(locs[i]) > SequenceRead.end(current)) {
//				if (debug) {System.err.println("They overlap, extending...");}					
				current = SequenceRead.packPosition(SequenceRead.start(current), SequenceRead.end(locs[i]), SequenceRead.strand(current));
			}
			else if (SequenceRead.end(locs[i]) <= SequenceRead.end(current)) {
				// Just ignore this since it's a subset of the region we're already looking at.
//				if (debug) {System.err.println("This is a subset, ignoring it");}
				continue;
				
			}
			else {
//				if (debug) {System.err.println("They don't overlap, moving on...");}					
				mergedLocs.add(new Location(current));
				current = locs[i];
			}
		}

		mergedLocs.add(new Location(current));
		
		Location [] finalLocations = mergedLocs.toArray(new Location[0]);
		Arrays.sort(finalLocations);

		return mergedLocs.toArray(finalLocations);

	}
	
	
}
