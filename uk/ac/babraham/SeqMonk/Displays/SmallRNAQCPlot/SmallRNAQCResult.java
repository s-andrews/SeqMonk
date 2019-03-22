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
package uk.ac.babraham.SeqMonk.Displays.SmallRNAQCPlot;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class SmallRNAQCResult {

	private DataStore store;
	private String [] features;
	private int minLength;
	private int [][] lengthCounts; // First dimension is length, second is feature counts
	
	public SmallRNAQCResult (DataStore store, int minLength, int maxLength, String [] features) {
		this.store = store;
		this.features = features;
		this.minLength = minLength;
		
		lengthCounts = new int [(maxLength-minLength)+1][features.length];

	}
	
	public void addCountsForFeatureIndex (int f, int [] counts) {
		if (counts.length != lengthCounts.length) {
			throw new IllegalArgumentException("Counts length "+counts.length+" wasn't the same as length counts length "+lengthCounts.length);
		}
		for (int i=0;i<counts.length;i++) {
			lengthCounts[i][f] = counts[i];
		}
	}
	
	public DataStore store () {
		return store;
	}
	
	public int minLength () {
		return minLength;
	}
	
	public int maxLength () {
		return minLength+lengthCounts.length-1;
	}
	
	public String [] features () {
		return features;
	}
	
	public int [] getCountsForLength (int length) {
		return lengthCounts[length-minLength];
	}
	
}
