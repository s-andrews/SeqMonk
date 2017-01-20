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

import uk.ac.babraham.SeqMonk.SeqMonkException;

/**
 * The Class Chromosome represents a single chromsome in a genome
 */
public class Chromosome implements Comparable<Chromosome>, Serializable {

	/** The name. */
	private String name;
	
	/** The length. */
	private int length = 0;
	
	/**
	 * Instantiates a new chromosome.
	 * 
	 * @param name the name
	 */
	public Chromosome (String name) {
		this.name = new String(name);
	}
	
	/**
	 * Name.
	 * 
	 * @return the name
	 */
	public String name () {
		return name;
	}
	
	/**
	 * Length.
	 * 
	 * @return the length in bp
	 */
	public int length () {
		return length;
	}
	
	/**
	 * Sets the length.  When parsing EMBL files it's possible to 
	 * have to incrementally increase the length of a chromosome so
	 * this call can be made multiple times as long as the length
	 * increases each time.
	 * 
	 * @param length the new length
	 * @throws SeqMonkException if the set length is shorter than the current length
	 */
	public void setLength (int length) throws SeqMonkException {
		
		if (length > this.length) {
			this.length = length;
		}
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return name;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Chromosome o) {
		
		// We try to compare by number first and then by string if that
		// fails.  Numbers come before text.
		//
		// Because the manual genome builder adds a 'pseudo' prefix to 
		// the artificial chromosomes it builds we remove that if it's 
		// present so that doesn't mess up the ordering.
		
		// This needed to be modified to fix a nasty data corruption bug.
		// The sorting rule actually needs to be:
		//
		// If they're both integers, sort as integers
		//
		// If they're both text, sort as text
		//
		// If one is an integer and the other is text then the integer wins
		
		String thisName = name;
		String thatName = o.name;
		
		if (thisName.startsWith("pseudo")) {
			thisName = thisName.substring(6);
		}

		if (thatName.startsWith("pseudo")) {
			thatName = thatName.substring(6);
		}
		
		try {
			int thisNumber = Integer.parseInt(thisName);
			try {
				int thatNumber = Integer.parseInt(thatName);
				
				// They're both numbers
				return thisNumber - thatNumber;
			}
			catch (NumberFormatException e) {
				// That is text, this is number, this wins
				return -1;
			}
		}
		catch (NumberFormatException e) {
			try {
				Integer.parseInt(thatName);
				
				// This is text, that is a number, that wins
				return 1;
			}
			catch (NumberFormatException e2) {
				// They're both text
				return thisName.compareTo(thatName);
			}
			
		}
				
	}
	
}
