/**
 * Copyright 2009-19 Simon Andrews
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

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;

/**
 * The Class SequenceReadWithChromosome is used in places where 
 * both the read and chromsome need to passed together.  Sequence
 * Reads do not store their chromosome by default to save memory
 */
public class SequenceReadWithChromosome implements Comparable<SequenceReadWithChromosome>{

	/**
	 * This class is only to be used by data parsers which temporarily
	 * need to associate a sequence read with a chromosome in a single
	 * object.  All of the main classes use the SequenceRead object which
	 * doesn't store the chromosome to save memory.
	 */
	
	public Chromosome chromosome;
	
	/** The read. */
	public long read;
	
	/**
	 * Instantiates a new sequence read with chromosome.
	 * 
	 * @param c the c
	 * @param r the r
	 */
	public SequenceReadWithChromosome (Chromosome c, long r) {
		chromosome = c;
		read = r;
	}

	public int compareTo(SequenceReadWithChromosome s2) {

		if (this.chromosome != s2.chromosome) {
			return this.chromosome.compareTo(s2.chromosome);
		}
		else {
			return SequenceRead.compare(read,s2.read);
		}
	}
	
	public String toString () {
		return chromosome.name()+":"+SequenceRead.start(read)+"-"+SequenceRead.end(read);
	}
}
