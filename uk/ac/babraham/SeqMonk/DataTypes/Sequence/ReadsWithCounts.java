/**
 * Copyright Copyright 2017-19 Simon Andrews
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
		
		// The previous implementation of this was horribly slow where we found
		// the unique positions first then assembled the set of counts through
		// multiple iterations.  We can try a simpler approach where we just 
		// concatenate the reads and counts into two new arrays, sort them and
		// then collapse them. Hopefully this will be quicker...
		
		int readCount = 0;
		
		for (int i=0;i<readsToMerge.length;i++) {
			readCount += readsToMerge[i].reads.length;
		}
		
		// Now we can make the temporary values
		long [] tempReads = new long[readCount];
		int [] tempCounts = new int[readCount];

		int index = 0;
		for (int i=0;i<readsToMerge.length;i++) {
			for (int j=0;j<readsToMerge[i].reads.length;j++) {
				tempReads[index] = readsToMerge[i].reads[j];
				tempCounts[index] = readsToMerge[i].counts[j];
				++index;
			}
		}
		
		// Now we sort them
		SequenceRead.sort(tempReads, tempCounts);
		
		// Now we find the unique values
		LongVector uniqueReads = new LongVector();
		IntVector uniqueCounts = new IntVector();
		if (tempReads.length>0) {
			uniqueReads.add(tempReads[0]);
			uniqueCounts.add(tempCounts[0]);
		}
		
		for (int i=1;i<tempReads.length;i++) {
			if (tempReads[i]==tempReads[i-1]) {
				uniqueCounts.increaseLastBy(tempCounts[i]);
			}
			else {
				uniqueReads.add(tempReads[i]);
				uniqueCounts.add(tempCounts[i]);
			}
		}

		reads = uniqueReads.toArray();
		counts = uniqueCounts.toArray();

	}

	public int totalCount () {
		int count = 0;
		for (int i=0;i<counts.length;i++) {
			count += counts[i];
		}

		return count;
	}

	/** 
	 * This method turns the two arrays (reads and counts) into a single expanded read
	 * array with the counts expanded out into a single linear stream.
	 * @param reads
	 * @param counts
	 * @return
	 */

	public long [] expandReads () {
		// Get the total size of the array to return

		int totalSize = 0;

		for (int i=0;i<counts.length;i++) {
			totalSize += counts[i];
		}

		// Make the array to return
		long [] returnArray = new long[totalSize];

		// Expand the set.
		int currentPosition = 0;
		for (int i=0;i<reads.length;i++) {
			for (int j=0;j<counts[i];j++) {
				returnArray[currentPosition] = reads[i];
				currentPosition++;
			}
		}

		return(returnArray);

	}
}
