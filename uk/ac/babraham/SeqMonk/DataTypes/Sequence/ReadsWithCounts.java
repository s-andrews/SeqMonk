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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

/**
 * This class is just a simple way to pass around paired read and count
 * arrays in a single object.
 * @author andrewss
 *
 */
public class ReadsWithCounts implements Serializable {

	private static final long serialVersionUID = 137753900274L;
	public long [] reads;
	public int [] counts;
	
	/**
	 * We assume that he reads and counts have already been
	 * collapsed and that sorted.
	 * @param reads
	 * @param counts
	 */
	public ReadsWithCounts (long [] reads, int [] counts) {
		if (reads == null || counts == null) {
			throw new IllegalStateException("Unexpected null: Reads ="+reads+" counts="+counts);
		}
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
		LongVector rv = new LongVector();
		IntVector cv = new IntVector();
		
		for (int i=0;i<reads.length;i++) {
			if (i>0 && reads[i-1]==reads[i]) {
				cv.increaseLastBy(1);
			}
			else {
				rv.add(reads[i]);
				cv.add(1);
			}
		}
		
		this.reads = rv.toArray();
		this.counts = cv.toArray();
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
		
		
		// Let's try a new approach.  We can merge and deduplicate the reads 
		// first and then sum up the counts as a second step.
		
		// First we get the set of unique positions between all of the sets
		
		Set<Long> uniqueReads = new HashSet<Long>();
		
		for (int m=0;m<readsToMerge.length;m++) {
			long [] reads = readsToMerge[m].reads;
			for (int r=0;r<reads.length;r++) {
				uniqueReads.add(reads[r]);
			}
		}
		
		reads = new long[uniqueReads.size()];
		
		Iterator<Long> it = uniqueReads.iterator();
		int i=0;
		while (it.hasNext()) {
			reads[i] = it.next();
			i++;
		}
		
		// Now we sort it so it should be in the same order as the 
		// original reads.
		
		SequenceRead.sort(reads);
		
		uniqueReads.clear();
				
		counts = new int [reads.length];
		
		
		// Now we go through each of the original lists comparing
		// them to the latest value and incrementing the total 
		// counts as appropriate.  Because the lists are always in
		// order then we should always see everything.
		
		int [] currentIndices = new int[readsToMerge.length];
				
		for (int r=0;r<reads.length;r++) {
			
			long lowestValue = reads[r];

			for (int j=0;j<currentIndices.length;j++) {
				if (currentIndices[j] == readsToMerge[j].reads.length) continue; // Skip datasets we've already emptied
				
				if (readsToMerge[j].reads[currentIndices[j]] == lowestValue) {
					counts[r] += readsToMerge[j].counts[currentIndices[j]];
					++currentIndices[j];
				}
			}
		}
		
		// As a final sanity check we can see if we ended up with the final
		// index position recorded for each of the lists.
		for (int d=0;d<readsToMerge.length;d++) {
			if (currentIndices[d] != readsToMerge[d].reads.length) {
				throw new IllegalStateException("In index "+d+" expected "+readsToMerge[d].reads.length+" reads but got "+currentIndices[d]);
			}
		}
		
		// We can also check that the summed counts is the same.
		
		// This is a bit of a performance hit so we comment this out when we're not testing.
		
//		int totalCount = 0;
//		for (int d=0;d<readsToMerge.length;d++) {
//			totalCount += readsToMerge[d].totalCount();
//		}
//		
//		if (totalCount != totalCount()) {
//			throw new IllegalStateException("Total counts weren't the same");
//		}
		
		
	}
	
	public int totalCount () {
		int count = 0;
		for (int i=0;i<counts.length;i++) {
			count += counts[i];
		}
		
		return count;
	}
}
