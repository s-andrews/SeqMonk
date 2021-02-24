/**
 * Copyright Copyright 2010- 21 Simon Andrews
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

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.Utilities.PositionFormat;

/**
 * This is a collection of methods which allow you to use a primitive long
 * as a genome location (start,end and strand) without the considerable
 * overhead of having to create objects for each of the instances.
 * 
 * Make no mistake this is an UGLY HACK which we've only done reluctantly
 * since there seems to be no other way to reduce the overhead of creating
 * hundreds of millions of objects for sequence reads, which is what we'd
 * otherwise be required to do.
 * 
 */
public class SequenceRead {

	private static final long LAST_31_BIT_MASK = Long.parseLong("0000000000000000000000000000000001111111111111111111111111111111",2);

	// Using the 64th bit is a pain.  We can't use -0 to construct a mask since
	// it gets converted to +0 and loses the 64th bit.  We therefore have to leave
	// the 63rd bit set as well and work around this later.
	private static final long KNOWN_BIT_MASK   =  Long.parseLong("-100000000000000000000000000000000000000000000000000000000000000",2);
	private static final long REVERSE_TEST_MASK = Long.parseLong("0100000000000000000000000000000000000000000000000000000000000000",2);
	private static final long REVERSE_BIT_MASK =  ~REVERSE_TEST_MASK;


	public static long packPosition (int start, int end, int strand) {
		if (start < 0 || end < 0) throw new IllegalArgumentException("Negative positions are not allowed");

		if (end < start) {
			int temp = start;
			start = end;
			end = temp;
		}

		// Base is start
		long value = start;

		// We need to remove the top sign bit from the end
		// and pack it starting at bit 32
		value += ((((long)end) & LAST_31_BIT_MASK) <<31);

		switch (strand) {
		case Location.FORWARD :
			value = value | KNOWN_BIT_MASK; // Sets both forward and known
			break;
		case Location.REVERSE :
			value = value | KNOWN_BIT_MASK; // Sets forward and known
			value = value & REVERSE_BIT_MASK; // Unsets forward
			break;

		case Location.UNKNOWN :
			break; // Leaves known and forward as zero

		default :
			throw new IllegalArgumentException("Strand was not FORWARD, REVERSE or UNKNOWN");
		}

		return value;
	}

	/**
	 * A constant defining the stand of this location.  One of:
	 * Location.FORWARD
	 * Location.REVERSE
	 * Location.UNKNOWN
	 * 
	 * @return The strand
	 */

	public static int strand (long value) {
		if ((value & KNOWN_BIT_MASK) == KNOWN_BIT_MASK) {
			// KNOWN_BIT_MASK actually sets both known and forward.
			return Location.FORWARD;
		}
		else if (value < 0) {
			// We can't test for the first bit with a bitmask since java
			// doesn't distinguish -0 and +0 so we just look for a negative
			// value to determine a positive position in bit 1.
			return Location.REVERSE;
		}
		else {
			return Location.UNKNOWN;
		}
	}
	
	/**
	 * This gives the strand as a symbol (+ / - / .) it's
	 * originally used for BED export but may have value
	 * elsewhere.
	 * 
	 * @param value
	 * @return
	 */
	public static String strandSymbol (long value) {
		int strand = strand(value);
		if (strand == 1) {
			return "+";
		}
		if (strand == -1) {
			return "-";
		}
		return ".";
	}

	/**
	 * Provides the mid-point of this position range.
	 * @return
	 */
	public static int midPoint (long value) {
		return start(value)+((end(value)-start(value))/2);
	}

	/**
	 * The start position of this location.  Guaranteed to not be higher
	 * than the end position
	 * 
	 * @return The start position
	 */
	public static int start (long value) {
		return (int)(value & LAST_31_BIT_MASK);
	}

	/**
	 * The end position of this location.  Guaranteed to be the same
	 * or higher than the start position.
	 * 
	 * @return The end position
	 */
	public static int end (long value) {
		return (int)((value>>31) & LAST_31_BIT_MASK);
	}
	
	/**
	 * Gets the total distance encompassed by two locations on the same 
	 * chromosome.  
	 */
	
	public static int fragmentLength(long value1, long value2) {
		return Math.max(end(value1), end(value2))-Math.min(start(value1),start(value2));
	}

	/**
	 * Says whether two reads which must be known to be on the same
	 * chromosome overlap with each other.
	 * 
	 * @param read1
	 * @param read2
	 * @return true if they overlap, otherwise false
	 */
	public static boolean overlaps (long value1, long value2) {
		return (start(value1) <= end(value2) && end(value1) >= start(value2));
	}


	public static int compare(long o1, long o2) {
		if (o1==o2) return 0;
		if (start(o1) != start(o2)) return start(o1) - start(o2);
		else if (end(o1) != end(o2)) return end(o1)- end(o2);
		else return strand(o1)-strand(o2);
	}

	/**
	 * Length.
	 * 
	 * @return the length of this location
	 */
	public static int length(long value) {
		return 1+ (end(value) - start(value)); 
	}


	public static void sort (long [] values) {

		if (values == null || values.length == 0) return;

		quicksort(values, 0, values.length-1);
	}
	
	public static void sort (long [] reads, int [] counts) {

		if (reads == null || reads.length == 0) return;

		pairedQuicksort(reads, counts, 0, reads.length-1);
	}

	

	/*
	 * Quicksort implementation adapted from http://www.inf.fh-flensburg.de/lang/algorithmen/sortieren/quick/quicken.htm
	 */

	private static void quicksort (long [] a, int lo, int hi) {
		//  lo is the lower index, hi is the upper index
		//  of the region of array a that is to be sorted
		int i=lo, j=hi;
		long h;

		// comparison element x
		long x=a[(lo+hi)/2];

		//  partition
		do  {    
			while (compare(a[i],x)<0) i++; 
			while (compare(a[j],x)>0) j--;
			if (i<=j) {
				h=a[i]; 
				a[i]=a[j]; 
				a[j]=h;
				i++; 
				j--;
			}
		} while (i<=j);

		//  recursion
		if (lo<j) quicksort(a, lo, j);
		if (i<hi) quicksort(a, i, hi);
	}
	
	
	private static void pairedQuicksort (long [] reads, int [] counts, int lo, int hi) {
		//  lo is the lower index, hi is the upper index
		//  of the region of array a that is to be sorted
		int i=lo, j=hi;
		long temp;
		int temp2;

		// comparison element x
		long x=reads[(lo+hi)/2];

		//  partition
		do  {    
			while (compare(reads[i],x)<0) i++; 
			while (compare(reads[j],x)>0) j--;
			if (i<=j) {
				temp=reads[i];
				temp2=counts[i];
				reads[i]=reads[j]; 
				counts[i]=counts[j];
				reads[j]=temp;
				counts[j]=temp2;
				i++; 
				j--;
			}
		} while (i<=j);

		//  recursion
		if (lo<j) pairedQuicksort(reads, counts, lo, j);
		if (i<hi) pairedQuicksort(reads, counts, i, hi);
	}
	
	

	/**
	 * Provides an EMBL formatted version of the location
	 * 
	 * @return An EMBL format location string
	 */
	public static String locationString (long value) {
		if (strand(value) == Location.REVERSE) {
			return "complement("+start(value)+".."+end(value)+")";
		}
		else if (strand(value) == Location.UNKNOWN) {
			return "unknown("+start(value)+".."+end(value)+")";			
		}
		else {
			return start(value)+".."+end(value);
		}
	}

	public static String toString (long value) {
		return locationString(value)+" ("+PositionFormat.formatLength(length(value))+")";
	}


	public static void main (String [] args) {

		long [] testArray = new long[] {
				packPosition(100,200,Location.UNKNOWN),	
				packPosition(10, 20, Location.FORWARD),
				packPosition(10, 50, Location.FORWARD),
				packPosition(10, 40, Location.FORWARD),
				packPosition(10, 20, Location.REVERSE),
				packPosition(10, 20, Location.UNKNOWN),
				packPosition(40, 70, Location.FORWARD),
				packPosition(50, 60, Location.FORWARD),
				packPosition(40, 75, Location.FORWARD),
		};

		sort(testArray);

		for (int i=0;i<testArray.length;i++) {
			System.out.println(i+": start="+start(testArray[i])+" end="+end(testArray[i])+" strand="+strand(testArray[i]));
		}

	}

}
