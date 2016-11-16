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
package uk.ac.babraham.SeqMonk.DataTypes.Probes;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * The Class QuantitationType provides a quick and centralised place
 * to define what types of reads can be used for quantitation.  This
 * saves each quantiation method from having to generate its own list
 * and logic for which reads to include.
 */
public class ProbeStrandType {

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
			
	/** The type. */
	protected int type;
	
	/** The name. */
	protected String name;
	
	/** The type options. */
	private static ProbeStrandType [] typeOptions = null;
	
	/**
	 * Instantiates a new quantitation type.
	 * 
	 * @param type the type
	 * @param name the name
	 */
	protected ProbeStrandType (int type, String name) {
		this.type = type;
		this.name = name;
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
	public boolean useProbe (Probe probe) {
		
		
		switch (type) {
		case (ALL):
			return true;
		
		case (FORWARD_ONLY):
			if (probe.strand() == Location.FORWARD) {
				return true;
			}
			return false;
			
		case (REVERSE_ONLY):
			if (probe.strand() == Location.REVERSE) {
				return true;
			}
			return false;
			
		case (UNKNOWN_ONLY):
			if (probe.strand() == Location.UNKNOWN) {
				return true;
			}
			return false;
			
		case (FORWARD_OR_REVERSE):
			if (probe.strand() == Location.UNKNOWN) return false;
			return true;

		default:
			throw new IllegalArgumentException("Unknown quantitation type "+type);
		}
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
	public synchronized static ProbeStrandType [] getTypeOptions () {
		if (typeOptions == null) {
			typeOptions = new ProbeStrandType [] {
					new ProbeStrandType(ALL, "All Probes"),	
					new ProbeStrandType(FORWARD_ONLY, "Forward Only"),	
					new ProbeStrandType(REVERSE_ONLY, "Reverse Only"),	
					new ProbeStrandType(UNKNOWN_ONLY, "Unknown Only"),	
					new ProbeStrandType(FORWARD_OR_REVERSE, "Forward or Reverse"),	
			};
		}
		return typeOptions;
	}
	
}
