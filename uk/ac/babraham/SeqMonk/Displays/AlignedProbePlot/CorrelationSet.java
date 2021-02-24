/**
 * Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.AlignedProbePlot;

import java.util.Vector;

public class CorrelationSet {

	private int primaryIndex = -1;
	private Vector<Integer> allIndices = new Vector<Integer>();
	
	public void addIndex (int index) {
		if (primaryIndex < 0) {
			primaryIndex = index;
		}
		allIndices.add(index);
	}
	
	public void addSet (CorrelationSet set) {
		int [] ints = set.getAllIndices();
		for (int i=0;i<ints.length;i++) {
			addIndex(ints[i]);
		}
	}
	
	public int [] getAllIndices () {
		Integer [] allIntegers = allIndices.toArray(new Integer[0]);
		int [] ints = new int[allIntegers.length];
		for (int i=0;i<ints.length;i++) {
			ints[i] = allIntegers[i];
		}
		return ints;
	}
	
}
