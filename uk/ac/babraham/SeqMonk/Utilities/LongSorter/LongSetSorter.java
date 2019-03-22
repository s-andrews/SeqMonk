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
package uk.ac.babraham.SeqMonk.Utilities.LongSorter;

import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

public class LongSetSorter {

	/*
	 * This is an optimised sort to merge together two or more lists of longs which 
	 * are individually sorted.  Up until now we've been putting these into a big
	 * array and doing a quicksort but hopefully this approach will be quicker
	 * since we can make assumptions about the sorting order of the reads in each
	 * sublist
	 */
	
	public static long [] sortLongSets (long [][] sets) {
		
		if (sets.length == 0) return new long [0];
		if (sets.length == 1) return sets[0];
		
		int totalLength = 0;
		
		for (int i=0;i<sets.length;i++) {
			totalLength += sets[i].length;
		}
		
		long [] returnArray = new long[totalLength];
		
		int [] currentIndices = new int[sets.length];
		
		int lowestIndex = 0;

		long lowestValue = 0;
		
		// Need to do something when we reach the end of a sublist
		
		for (int i=0;i<returnArray.length;i++) {	
			// Add the lowest read to the full set
			lowestIndex = -1;
			lowestValue = 0;
			for (int j=0;j<currentIndices.length;j++) {
				if (currentIndices[j] == sets[j].length) continue; // Skip datasets we've already emptied
				if (lowestValue == 0 || SequenceRead.compare(sets[j][currentIndices[j]],lowestValue) < 0) {
					lowestIndex = j;
					lowestValue = sets[j][currentIndices[j]];
				}
			}
			
			returnArray[i] = lowestValue;
			currentIndices[lowestIndex]++;
			
		}		
		
		return returnArray;
		
		
	}
	
	
}
