/**
 * Copyright 2012-17 Simon Andrews
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

public class IntSorter {
	
	public static void sortInts (int [] values, IntComparator comparator) {
		
		if (values == null || values.length == 0) return;

		quicksort(values, 0, values.length-1, comparator);
	}

	/*
	 * Quicksort implementation adapted from http://www.inf.fh-flensburg.de/lang/algorithmen/sortieren/quick/quicken.htm
	 */

	private static void quicksort (int [] a, int lo, int hi, IntComparator comparator) {
		//  lo is the lower index, hi is the upper index
		//  of the region of array a that is to be sorted
		int i=lo, j=hi;
		int h;

		// comparison element x
		int x=a[(lo+hi)/2];

		//  partition
		do  {    
			while (comparator.compare(a[i],x)<0) i++; 
			while (comparator.compare(a[j],x)>0) j--;
			if (i<=j) {
				h=a[i]; 
				a[i]=a[j]; 
				a[j]=h;
				i++; 
				j--;
			}
		} while (i<=j);

		//  recursion
		if (lo<j) quicksort(a, lo, j,comparator);
		if (i<hi) quicksort(a, i, hi,comparator);
	}
	
}
