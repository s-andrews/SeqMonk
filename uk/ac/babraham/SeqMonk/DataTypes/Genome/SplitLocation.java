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
import java.util.Arrays;

import uk.ac.babraham.SeqMonk.SeqMonkException;

/**
 * SplitLocation can be used to represent complex genomic
 * positions built up from several sublocations.
 */
public class SplitLocation extends Location implements Serializable {

	private Location [] subLocations;
	
	/**
	 * Instantiates a new split location.
	 * 
	 * @param subLocations The set of sublocations from which the whole feature will be built
	 * @param strand Which strand the feature is on
	 * @throws SeqMonkException
	 */
	public SplitLocation (Location [] subLocations) throws SeqMonkException {
		super(0,0,UNKNOWN);
		if (subLocations == null || subLocations.length == 0) {
			throw new IllegalArgumentException("There must be at least one sublocation to define a feature");
		}
		this.subLocations = subLocations;
		Arrays.sort(this.subLocations);
		setPosition(subLocations[0].start(),subLocations[subLocations.length-1].end(),subLocations[0].strand());
	}
	
	/**
	 * Instantiates a new split location from an EMBL format location string
	 * 
	 * @param EMBLString An EMBL format location string
	 * @throws SeqMonkException
	 */
	public SplitLocation (String EMBLString) throws SeqMonkException {
		this(EMBLString,0);
	}
		
	/**
	 * Instantiates a new split location from an EMBL string with an
	 * arbitary offset value applied.  This is useful because older
	 * genome files gave coordinates within a BAC clone and then had
	 * an overall offset for that BAC in the genome assembly.
	 * 
	 * @param EMBLString An EMBL format location string
	 * @param offset The offset to apply (in bp)
	 * @throws SeqMonkException
	 */
	public SplitLocation (String EMBLString, int offset) throws SeqMonkException {
		super(0,0,FORWARD);
		int strand = FORWARD;
		int start=0;
		int end=0;
		if (EMBLString.indexOf("complement")>=0) {
			strand = REVERSE;
		}
		else if (EMBLString.indexOf("unknown")>=0) {
			strand = UNKNOWN;
		}
		else {
			strand = FORWARD;
		}
		
		EMBLString = EMBLString.replaceAll("join\\(","");
		EMBLString = EMBLString.replaceAll("complement\\(","");
		EMBLString = EMBLString.replaceAll("unknown\\(","");
		EMBLString = EMBLString.replaceAll("\\)","");

		// We need to remove any position ambiguities too
		EMBLString = EMBLString.replaceAll("[<>]","");
		
		String [] subLocationStrings = EMBLString.split(",");
		subLocations = new Location[subLocationStrings.length];
		
		for (int i=0;i<subLocationStrings.length;i++) {
			String [] positions = subLocationStrings[i].split("\\.\\.");

			// It's possible that only a single base is found in which case
			// some EMBL writers will include only a single position rather
			// than a range.  We'll convert this to a 2 number form for consistency
			
			if (positions.length == 1) {
				positions = new String[] {positions[0],positions[0]};
			}
			subLocations[i] = new Location(Integer.parseInt(positions[0])+offset,Integer.parseInt(positions[1])+offset,strand);
			if (start == 0) {
				start = subLocations[i].start();
				end = subLocations[i].end();
			}
			else {
				if (subLocations[i].start()< start) {
					start = subLocations[i].start();					
				}
				if (subLocations[i].end() > end) {
					end = subLocations[i].end();
				}
			}
		}
		Arrays.sort(subLocations);
		
		setPosition(start, end, strand);
		// Don't store more than we have to
		if (subLocations.length == 1) {
			subLocations = null;
		}
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.Location#subLocations()
	 */
	public Location [] subLocations () {
		if (subLocations == null) {
			return new Location[] {this};
		}
		return subLocations;
	}	
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.Location#locationString()
	 */
	public String locationString () {
		
		// We optimise by not storing subLocations if there is only one location
		// in the string.  In this case we use the simpler method from the superclass.
		if (subLocations == null) {
			return super.locationString();
		}
		
		StringBuffer b = new StringBuffer();
		if (strand() == REVERSE) {
			b.append("complement(");
		}
		if (strand() == UNKNOWN) {
			b.append("unknown(");
		}
		b.append(subLocations[0].start());
		b.append("..");
		b.append(subLocations[0].end());
		for (int i=1;i<subLocations.length;i++) {
			b.append(",");
			b.append(subLocations[i].start());
			b.append("..");
			b.append(subLocations[i].end());
		}
		if (strand() == REVERSE || strand() == UNKNOWN) {
			b.append(")");
		}
		return b.toString();
	}
	
}
