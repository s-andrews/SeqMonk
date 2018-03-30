/**
 * Copyright Copyright 2017-18 Simon Andrews
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


package uk.ac.babraham.SeqMonk.DataTypes.Sequence;

import java.io.Serializable;

import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

/**
 * This class is just a simple way to pass around paired read and count
 * arrays in a single object.
 * @author andrewss
 *
 */
public class ReadsWithCounts implements Serializable {

	public long [] reads;
	public int [] counts;
	
	/**
	 * We assume that he reads and counts have already been
	 * collapsed and that sorted.
	 * @param reads
	 * @param counts
	 */
	public ReadsWithCounts (long [] reads, int [] counts) {
		this.reads = reads;
		this.counts = counts;
	}
	
	
	/**
	 * In this version we take uncollapsed reads and collapse them
	 * at this stage.  We assume that they're sorted before they
	 * get passed to us.
	 * @param reads
	 */
	public ReadsWithCounts (long [] reads) {
		
	}
	
	/**
	 * This is used to merge together multiple sets into a single
	 * set of observations.
	 * @param readsToMerge
	 */
	public ReadsWithCounts (ReadsWithCounts [] readsToMerge) {
		// We'll assume that these come sorted, which they should...
		
		if (readsToMerge.length == 0) {
			reads = new long[0];
			counts = new int[0];
			return;
		}
		if (readsToMerge.length == 1) {
			reads = readsToMerge[0].reads;
			counts = readsToMerge[0].counts;
			return;
		}
		
		LongVector tempreads = new LongVector();
		IntVector tempcounts = new IntVector();
						
		int [] currentIndices = new int[readsToMerge.length];
		
		long lowestValue = 0;
		
		// Need to do something when we reach the end of a sublist
		
		while (true) {
			boolean allFinished = true;
			
			// Add the lowest read to the full set
			lowestValue = 0;
			for (int j=0;j<currentIndices.length;j++) {
				if (currentIndices[j] == readsToMerge[j].reads.length) continue; // Skip datasets we've already emptied
				allFinished = false;
				if (lowestValue == 0 || SequenceRead.compare(readsToMerge[j].reads[currentIndices[j]],lowestValue) < 0) {
					lowestValue = readsToMerge[j].reads[currentIndices[j]];
				}
			}
			
			if (allFinished) break;
			
			tempreads.add(lowestValue);
			tempcounts.add(0);

			for (int j=0;j<currentIndices.length;j++) {
				if (currentIndices[j] == readsToMerge[j].reads.length) continue; // Skip datasets we've already emptied
				if (readsToMerge[j].reads[currentIndices[j]] == lowestValue) {
					tempcounts.increaseLastBy(readsToMerge[j].counts[currentIndices[j]]);
					++currentIndices[j];
				}
			}
		}
		
		reads = tempreads.toArray();
		counts = tempcounts.toArray();
		
	}
	
	public int totalCount () {
		int count = 0;
		for (int i=0;i<counts.length;i++) {
			count += counts[i];
		}
		
		return count;
	}
}
