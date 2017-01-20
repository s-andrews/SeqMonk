/**
 * Copyright 2014-17 Simon Andrews
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

import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

public class IntronFeatureGroup extends FeatureGroup {

	public IntronFeatureGroup(String name) {
		super(name);
	}

	public Location [] getSubLocations () {

//		System.err.println("Getting sublocs for introns");
		
		// See if we can take some shortcuts

		if (features.size() == 1) {
			Location loc = features.elementAt(0).location();
			if (loc instanceof SplitLocation) {
				Location [] subLocs = ((SplitLocation)loc).subLocations();
				
				Location [] interLocs = new Location[subLocs.length-1];
				for (int i=0;i<subLocs.length-1;i++) {
					interLocs[i] = new Location(subLocs[i].end()+1, subLocs[i+1].start()-1, subLocs[i].strand());
				}
				
				return interLocs;
				
			}
			else {
				return new Location[0];
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

		Location [] interLocs = new Location[mergedLocs.size()-1];
		for (int i=0;i<mergedLocs.size()-1;i++) {
			interLocs[i] = new Location(mergedLocs.elementAt(i).end()+1, mergedLocs.elementAt(i+1).start()-1, mergedLocs.elementAt(i).strand());
		}
		
		return interLocs;			

	}

	
}
