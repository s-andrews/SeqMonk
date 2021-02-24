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
package uk.ac.babraham.SeqMonk.Analysis.Statistics;

import uk.ac.babraham.SeqMonk.GeneSets.MappedGeneSet;

/**
 * The Class ProbeGroupTTestValue provides a wrapper which can store both
 * raw and corrected p-values for a group of probes.
 */
public class MappedGeneSetTTestValue implements Comparable<MappedGeneSetTTestValue> {

	/** The probes. */
	public MappedGeneSet mappedGeneSet;
	
	/** The p. */
	public double p;
	
	/** The q. */
	public Double q = null;
	
	/**
	 * Instantiates a new probe group t test value.
	 * 
	 * @param probes the probes
	 * @param p the p
	 */
	public MappedGeneSetTTestValue (MappedGeneSet mappedGeneSet, double p) {
		this.mappedGeneSet = mappedGeneSet;
		this.p = p;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(MappedGeneSetTTestValue o) {
		return Double.compare(p,o.p);
	}
}
