/**
 * Copyright Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataTypes.Genome;

import java.io.Serializable;

/**
 * A simple class to represent a simple location in the genome.
 * For complex positions containing sublocations you should use
 * the SplitLocation class instead.
 */
public class Location implements Serializable, Comparable<Location> {
	
	private static final long serialVersionUID = 4931115654048485228L;

	private long value;
	public static final int FORWARD = 1;
	public static final int REVERSE = -1;
	public static final int UNKNOWN = 0;
	
	private static final long LAST_31_BIT_MASK = Long.parseLong("0000000000000000000000000000000001111111111111111111111111111111",2);

	// Using the 64th bit is a pain.  We can't use -0 to construct a mask since
	// it gets converted to +0 and loses the 64th bit.  We therefore have to leave
	// the 63rd bit set as well and work around this later.
	private static final long KNOWN_BIT_MASK   =  Long.parseLong("-100000000000000000000000000000000000000000000000000000000000000",2);
	private static final long REVERSE_TEST_MASK = Long.parseLong("0100000000000000000000000000000000000000000000000000000000000000",2);
	private static final long REVERSE_BIT_MASK =  ~REVERSE_TEST_MASK;
	
	/**
	 * Constructs a new Location from a previously packed position.
	 * This constructor should not be used normally.  It's only
	 * valid use is for reconstructing serialised positions which
	 * have been written to a file.
	 * 
	 * @param packedPosition
	 */
	public Location (long packedPosition) {
		this.value = packedPosition;
	}
	
	public Location (int start, int end, int strand) {

		setPosition(start, end, strand);
		if (strand != strand()) {
			System.err.println("Strand "+strand+" didn't match "+strand());
		}
	}
		
	/**
	 * A constant defining the stand of this location.  One of:
	 * Location.FORWARD
	 * Location.REVERSE
	 * Location.UNKNOWN
	 * 
	 * @return The strand
	 */

	public int strand () {
		if ((value & KNOWN_BIT_MASK) == KNOWN_BIT_MASK) {
			// KNOWN_BIT_MASK actually sets both known and forward.
			return FORWARD;
		}
		else if (value < 0) {
			// We can't test for the first bit with a bitmask since java
			// doesn't distinguish -0 and +0 so we just look for a negative
			// value to determine a positive position in bit 1.
			return REVERSE;
		}
		else {
			return UNKNOWN;
		}
	}
	
	/**
	 * Retrieves the interal representation of this position.  Not for
	 * general use - only to be used within the SeqMonkDataWriter
	 * 
	 * @return The internal packed representation of this position
	 */
	public long packedPosition () {
		return value;
	}
	
	/**
	 * The start position of this location.  Guaranteed to not be higher
	 * than the end position
	 * 
	 * @return The start position
	 */
	public int start () {
		return (int)(value & LAST_31_BIT_MASK);
	}
	
	/**
	 * The end position of this location.  Guaranteed to be the same
	 * or higher than the start position.
	 * 
	 * @return The end position
	 */
	public int end () {
		return (int)((value>>31) & LAST_31_BIT_MASK);
	}
	
	public int middle () {
		return start()+((end()-start())/2);
	}
	
	public String toString () {
		return start()+"-"+end();
	}
		
	
	public int compareTo(Location o) {
		if (start() != o.start()) return start() - o.start();
		else if (end() != o.end()) return end()- o.end();
		else if (strand() != o.strand()) return strand() - o.strand();
		else return hashCode() - o.hashCode();
	}
	
	/**
	 * Length.
	 * 
	 * @return the length of this location
	 */
	public int length() {
		return 1+ (end() - start()); 
	}

	
	/**
	 * Provides an EMBL formatted version of the location
	 * 
	 * @return An EMBL format location string
	 */
	public String locationString () {
		if (strand() == REVERSE) {
			return "complement("+start()+".."+end()+")";
		}
		else if (strand() == UNKNOWN) {
			return "unknown("+start()+".."+end()+")";			
		}
		else {
			return start()+".."+end();
		}
	}
	
	protected void setPosition (int start, int end, int strand) {
		if (start < 0 || end < 0) throw new IllegalArgumentException("Negative positions are not allowed");
		
		if (end < start) {
			int temp = start;
			start = end;
			end = temp;
		}
		
		// Base is start
		value = start;
		
		// We need to remove the top sign bit from the end
		// and pack it starting at bit 32
		value += ((((long)end) & LAST_31_BIT_MASK) <<31);
				
		switch (strand) {
			case FORWARD :
				value = value | KNOWN_BIT_MASK; // Sets both forward and known
				break;
			case REVERSE :
				value = value | KNOWN_BIT_MASK; // Sets forward and known
				value = value & REVERSE_BIT_MASK; // Unsets forward
				break;

			case UNKNOWN :
				break; // Leaves known and forward as zero
				
			default :
				throw new IllegalArgumentException("Strand was not FORWARD, REVERSE or UNKNOWN");
				
		}

	}
	
	
}
