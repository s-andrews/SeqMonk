/**
 * Copyright 2010-15 Simon Andrews
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

/**
 * The Class QuantitationType provides a quick and centralised place
 * to define what types of reads can be used for quantitation.  This
 * saves each quantiation method from having to generate its own list
 * and logic for which reads to include.
 */
public class QuantitationStrandType {

	/** The Constant ALL. */
	public static final int ALL = 99;
	
	/** The Constant FORWARD_ONLY. */
	public static final int FORWARD_ONLY = 100;
	
	/** The Constant REVERSE_ONLY. */
	public static final int REVERSE_ONLY = 101;
	
	/** The Constant UNKNOWN_ONLY. */
	public static final int UNKNOWN_ONLY = 102;
	
	/** The Constant FORWARD_OR_REVERSE. */
	public static final int FORWARD_OR_REVERSE = 103;
	
	/** The Constant SAME_STRAND_AS_PROBE. */
	public static final int SAME_STRAND_AS_PROBE = 104;
	
	/** The Constant OPPOSITE_STRAD_TO_PROBE. */
	public static final int OPPOSITE_STRAD_TO_PROBE = 105;
	
	/** The last valid read - used when ignoring duplicates **/
	private long lastRead = 0;
	
	/** Says if we are going to ignore duplicates */
	private boolean ignoreDuplicates = false;
	
	/** The type. */
	protected int type;
	
	/** The name. */
	protected String name;
	
	/** The type options. */
	private static QuantitationStrandType [] typeOptions = null;
	
	/**
	 * Instantiates a new quantitation type.
	 * 
	 * @param type the type
	 * @param name the name
	 */
	protected QuantitationStrandType (int type, String name) {
		this.type = type;
		this.name = name;
	}

	/**
	 * Sets whether to ignore duplicates.
	 * 
	 * NOTE: Because we're only passing in isolated reads
	 * we don't get chromosome information.  This means that if
	 * we get passed two reads with identical positions one
	 * after the other where they sit on different chromosomes
	 * we'll do the wrong thing and reject it as a duplicate.  In
	 * any sane dataset this won't be a problem, but I don't like
	 * that this problem exists.
	 * 
	 * This problem can be worked around by getting any callers
	 * to call 'resetLastRead()' at the end of each chromosome.
	 * 
	 * @param ignoreDuplicates the new ignore duplicates
	 */
	public void setIgnoreDuplicates (boolean ignoreDuplicates) {
		this.ignoreDuplicates = ignoreDuplicates;
	}
	
	/**
	 * Retrieves whether the quantitation is currently ignoring
	 * duplicates.
	 * 
	 * @return
	 */
	public boolean ignoreDuplicates () {
		return ignoreDuplicates;
	}
	
		
	/**
	 * This can be called by quantitation methods and will say
	 * whether a particular read should be used in this analysis
	 * or not.
	 * 
	 * @param probe The probe being quantitated
	 * @param read The read being examined
	 * @return true, if this read should be used
	 */
	public boolean useRead (Location probe, long read) {
		
		if (ignoreDuplicates) {
			if (lastRead != 0 && read == lastRead) {
				return false;
			}
		}
		
		switch (type) {
		case (ALL):
			lastRead = read;
			return true;
		
		case (FORWARD_ONLY):
			if (SequenceRead.strand(read) == Location.FORWARD) {
				lastRead = read;
				return true;
			}
			return false;
			
		case (REVERSE_ONLY):
			if (SequenceRead.strand(read) == Location.REVERSE) {
				lastRead = read;
				return true;
			}
			return false;
			
		case (UNKNOWN_ONLY):
			if (SequenceRead.strand(read) == Location.UNKNOWN) {
				lastRead = read;
				return true;
			}
			return false;
			
		case (FORWARD_OR_REVERSE):
			if (SequenceRead.strand(read) == Location.UNKNOWN) return false;
			lastRead = read;
			return true;

		case (SAME_STRAND_AS_PROBE):
			if (SequenceRead.strand(read) == probe.strand()) {
				lastRead = read;
				return true;
			}
			return false;

		case (OPPOSITE_STRAD_TO_PROBE):
			if (SequenceRead.strand(read) == probe.strand()) return false;
			if (SequenceRead.strand(read) == Location.UNKNOWN || probe.strand() == Location.UNKNOWN) return false;

			lastRead = read;
			return true;

		default:
			throw new IllegalArgumentException("Unknown quantitation type "+type);
		}
	}
	
	/**
	 * If you are calling this class in a situation where you may well encounter
	 * the same read twice in succession (for example two overlapping probes where
	 * a read is the last read in one probe and the first in the next), then you can
	 * call resetLastRead to ensure that the next call to useRead won't fail because
	 * of a duplicate read.
	 */
	public void resetLastRead () {
		lastRead = 0;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return name;
	}
	
	/**
	 * Gets an array of all of the different type options
	 * 
	 * @return the type options
	 */
	public synchronized static QuantitationStrandType [] getTypeOptions () {
		if (typeOptions == null) {
			typeOptions = new QuantitationStrandType [] {
					new QuantitationStrandType(ALL, "All Reads"),	
					new QuantitationStrandType(FORWARD_ONLY, "Forward Only"),	
					new QuantitationStrandType(REVERSE_ONLY, "Reverse Only"),	
					new QuantitationStrandType(UNKNOWN_ONLY, "Unknown Only"),	
					new QuantitationStrandType(FORWARD_OR_REVERSE, "Forward or Reverse"),	
					new QuantitationStrandType(SAME_STRAND_AS_PROBE, "Same Strand as Probe"),	
					new QuantitationStrandType(OPPOSITE_STRAD_TO_PROBE, "Opposite Strand to Probe"),	
			};
		}
		return typeOptions;
	}
	
}
