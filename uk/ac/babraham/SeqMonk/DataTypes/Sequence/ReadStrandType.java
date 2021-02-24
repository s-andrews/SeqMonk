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
package uk.ac.babraham.SeqMonk.DataTypes.Sequence;


/**
 * The Class StrandType provides a quick and centralised place
 * to define what types of reads can be used for building probes.  This
 * saves each generator method from having to generate its own list
 * and logic for which reads to include.
 * 
 * For quantitations you should use the QuantitationStrandType which
 * is basically the same, but allows some extra options relating
 * to the direction of the probes being quantitated.
 */
public class ReadStrandType extends QuantitationStrandType {
	
	/** The type options. */
	private static ReadStrandType [] typeOptions = null;
	
	protected ReadStrandType(int type, String name) {
		super(type,name);
	}
	
	/**
	 * Gets an array of all of the different type options
	 * 
	 * @return the type options
	 */
	public synchronized static ReadStrandType [] getTypeOptions () {
		if (typeOptions == null) {
			typeOptions = new ReadStrandType [] {
					new ReadStrandType(ALL, "All Reads"),	
					new ReadStrandType(FORWARD_ONLY, "Forward Only"),	
					new ReadStrandType(REVERSE_ONLY, "Reverse Only"),	
					new ReadStrandType(UNKNOWN_ONLY, "Unknown Only"),	
					new ReadStrandType(FORWARD_OR_REVERSE, "Forward or Reverse"),	
			};
		}
		return typeOptions;
	}
	
	/**
	 * Says whether a particular read should be used in construcing probes
	 * 
	 * @param read the read to test
	 * @return true, if the read should be used.
	 */
	public boolean useRead (long read) {
		// We can get away with passing a null since we know that this
		// class only produces options which don't require knowledge of
		// the strand of the probe.
		return super.useRead(null, read);
	}
	
}
