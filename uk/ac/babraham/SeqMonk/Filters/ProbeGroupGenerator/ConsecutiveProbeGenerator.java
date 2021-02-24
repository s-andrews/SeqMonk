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
package uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator;

import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * The ConsecutiveProbeGenerator is a utility which iterates through a probe list putting
 * together sets of probes of a defined size which fall next to each other. 
 */
public class ConsecutiveProbeGenerator implements ProbeGroupGenerator {

	private Probe [] probes;
	private int startIndex;
	private Vector<Probe> v = new Vector<Probe>();
	
	private int probeCount;
	
	/**
	 * Instantiates a new probe window generator.
	 * 
	 * @param probes The list of probes from which the sets will be composed
	 * @param probeCount The size of the window in base pairs
	 * @throws SeqMonkException if the dataColleciton isn't quantitated
	 */
	public ConsecutiveProbeGenerator (Probe [] probes, int probeCount) {
		
		this.probes = probes;
		this.probeCount = probeCount;
		startIndex = 0;
	}
	
	/**
	 * Provides the next set of probe from this generator
	 * 
	 * @return A list of probes in the next set.
	 */
	public Probe [] nextSet (){
		
		if (startIndex >= probes.length) {
			return null;
		}
		
		int windowEnd = startIndex+probeCount;

		v.removeAllElements();
		v.add(probes[startIndex]);
		for (int i=startIndex+1;(i<probes.length  && i<windowEnd);i++) {
			if (probes[i].chromosome() != probes[startIndex].chromosome()) break;
			v.add(probes[i]);
		}
		++startIndex;
		return v.toArray(new Probe[0]);
	}
	
}
