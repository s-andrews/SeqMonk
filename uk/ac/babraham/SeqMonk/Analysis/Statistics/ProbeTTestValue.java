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

import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * The Class ProbeTTestValue is a wrapper which can raw a raw and corrected
 * p-value for a single probe.
 */
public class ProbeTTestValue implements Comparable<ProbeTTestValue> {

	/** The probe. */
	public Probe probe;
	
	/** The p. */
	public double p;
	
	/** The q. */
	public double q;
	
	/** The difference */
	public double diff;
	
	/**
	 * Instantiates a new probe t test value.
	 * 
	 * @param probe the probe
	 * @param p the p
	 */
	public ProbeTTestValue (Probe probe, double p, double diff) {
		this.probe = probe;
		this.p = p;
		this.diff = diff;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ProbeTTestValue o) {
		return Double.compare(p,o.p);
	}
}
