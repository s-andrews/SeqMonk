/**
 * Copyright Copyright 2010-18 Simon Andrews
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

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.Utilities.PositionFormat;

/**
 * The Class Probe represents a location at which a measurement
 * can be made
 */
public class Probe extends Location {

	/** The chromosome. */
	private Chromosome chromosome;

	/** The name. */
	private String name;


	// Trying a new caching mechanism to make probe storage more efficient
	/** The index. */
	private int index = -1;

	/**
	 * Instantiates a new probe.
	 * 
	 * @param chromosome the chromosome
	 * @param start the start
	 * @param end the end
	 * @param name the name
	 * @throws SeqMonkException the seq monk exception
	 */
	public Probe (Chromosome chromosome, int start, int end, String name) {
		super(start,end,UNKNOWN);
		this.chromosome = chromosome;
		this.name = name;
	}

	public Probe (Chromosome chromosome, int start, int end, int strand, String name) {
		super(start,end,strand);
		this.chromosome = chromosome;
		this.name = name;
	}


	/**
	 * Instantiates a new probe.
	 * 
	 * @param chromosome the chromosome
	 * @param start the start
	 * @param end the end
	 * @throws SeqMonkException the seq monk exception
	 */
	public Probe (Chromosome chromosome, int start, int end) {
		this(chromosome,start,end,null);
	}

	/**
	 * Instantiates a new probe.
	 * 
	 * This constructor should only be used when reconstructing a serialised
	 * seqmonk file.  Don't try to pack a position yourself - use the
	 * constructors which take separate start/end/strand values.
	 * 
	 * @param chromosome the chromosome
	 * @param packed the packed position for start/end/strand
	 * @param name the name for the probe
	 * @throws SeqMonkException the seq monk exception
	 * 
	 */
	public Probe (Chromosome chromosome, long packed, String name) {
		super(packed);
		this.chromosome = chromosome;
		this.name = name;
	}

	/**
	 * Instantiates a new probe.
	 * 
	 * This constructor should only be used when reconstructing a serialised
	 * seqmonk file.  Don't try to pack a position yourself - use the
	 * constructors which take separate start/end/strand values.
	 * 
	 * @param chromosome the chromosome
	 * @param packed the packed position for start/end/strand
	 * @throws SeqMonkException the seq monk exception
	 * 
	 */
	public Probe (Chromosome chromosome, long packed) {
		this(chromosome,packed,null);
	}

	/**
	 * Instantiates a new probe.
	 * 
	 * @param chromosome the chromosome
	 * @param start the start
	 * @param end the end
	 * @param strand the strand
	 * @throws SeqMonkException the seq monk exception
	 */
	public Probe (Chromosome chromosome, int start, int end, int strand) {
		this(chromosome,start,end,strand,null);
	}

	/**
	 * Checks for data for sets.
	 * 
	 * @param sets the sets
	 * @return true, if successful
	 */
	public boolean hasDataForSets (DataSet [] sets) {
		for (int s=0;s<sets.length;s++) {
			if (sets[s].containsReadForProbe(this)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the index.
	 * 
	 * @param index the new index
	 */
	public void setIndex (int index) {
		/**
		 * This method sets the index position of this probe inside
		 * the probe set and is used to allow efficient storage of
		 * data inside dataset objects.
		 * 
		 * It MUST ONLY be called by the dataSet when the probe is
		 * added.  Messing with this otherwise will screw stuff up!
		 */

		/*
		 * Check to see that this isn't being set more than once
		 */

		if (this.index != -1) {
			throw new IllegalArgumentException("Index has alredy been set and can't be reset");
		}

		if (index < 0) {
			throw new IllegalArgumentException("Index must be a positive value");
		}

		this.index = index;
	}

	/**
	 * Index.
	 * 
	 * @return the int
	 */
	public int index () {
		return index;
	}

	/**
	 * Chromosome.
	 * 
	 * @return the chromosome
	 */
	public Chromosome chromosome () {
		return chromosome;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name the new name
	 */
	public void setName (String name) {

		/*
		 * We had a problem in some releases where probe names were
		 * being stored in project files even when no specific name
		 * had been provided.  In effect we were storing the name
		 * we made up dynamically, rather than a blank value. When
		 * we reload it is good to identify these cases and not store
		 * the names again, so this additional memory usage doesn't
		 * propagate.
		 */

		if (name != name()) {
			this.name = name;
		}
	}

	/**
	 * Checks to see if there is a proper assigned name, rather than 
	 * just a default name derived from the location
	 * 
	 * @return
	 */
	public boolean hasDefinedName () {
		return name != null;
	}

	/**
	 * Name.
	 * 
	 * @return the string
	 */
	public String name () {
		if (name != null) {
			return name;
		}
		return location();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Location p2) {
		
		/*
		 * This method compares to a location rather than a probe since you can't 
		 * have more than one compareTo method for a class, and since we extend
		 * location we have to compare to that.
		 * 
		 * This method caused a data corruption bug in v0.19.0 so be very careful
		 * about messing with it.  Probe list saving and the combining of existing
		 * lists depend on consistent ordering of probes after sorting. This method
		 * therefore goes through chromosome, position, name and ultimately memory
		 * location to guarantee that probes at the same position will still have
		 * a defined sorting order.  It was getting this sorting messed up which
		 * caused all the grief with v0.19.0.  The memory location will change 
		 * between runs of the program, but as long as it's consistent within a
		 * run then the internal sorting will be consistent and everything will
		 * work.
		 * 
		 * Don't change this now unless you're *really* sure of what you're doing, 
		 * and do plenty of regression testing for multiple probes at the same
		 * position.
		 */

		if (p2 instanceof Probe) {
			if (((Probe)p2).chromosome != chromosome) {
				return chromosome.compareTo(((Probe)p2).chromosome);
			}

			else if (super.compareTo(p2) != 0) {
				return super.compareTo(p2);
			}

			else if (hasDefinedName() && ((Probe)p2).hasDefinedName()  && (! name().equals(((Probe)p2).name()))) {
				return name().compareTo(((Probe)p2).name());
			}
			else {
//				if (this != p2) {
//					System.err.println(name()+" is exactly the same as "+((Probe)p2).name());
//				}
				return hashCode()-p2.hashCode();
			}
		}
		else {
			return super.compareTo(p2);
		}
		
	}		



	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		
		String strand = "UNK";
		if (strand() == Location.FORWARD) {
			strand = "FOR";
		}
		if (strand() == Location.REVERSE) {
			strand = "REV";
		}
		
		if (name != null) {
			return name+" "+location()+" "+strand+" ("+PositionFormat.formatLength(length())+")";
		}
		return location()+" "+strand+" ("+PositionFormat.formatLength(length())+")";
	}

	private String location () {
		return "Chr"+chromosome+":"+start()+"-"+end();

	}

}
