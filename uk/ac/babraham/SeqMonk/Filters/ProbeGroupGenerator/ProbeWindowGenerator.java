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
package uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator;

import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * The ProbeWindowGenerator is a utility which iterates through a probe list putting
 * together sets of probes which fall within a window of a defined length
 */
public class ProbeWindowGenerator implements ProbeGroupGenerator {

	private Probe [] probes;
	private int startPos;
	private Vector<Probe> v = new Vector<Probe>();
	
	private int windowSize;
	
	/**
	 * Instantiates a new probe window generator.
	 * 
	 * @param probes The list of probes from which the sets will be composed
	 * @param windowSize The size of the window in base pairs
	 * @throws SeqMonkException if the dataColleciton isn't quantitated
	 */
	public ProbeWindowGenerator (Probe [] probes, int windowSize) {
		this.probes = probes;
		this.windowSize = windowSize;
		startPos = 0;		
	}
	
	/**
	 * Provides the next set of probe from this generator
	 * 
	 * @return A list of probes in the next set.
	 */
	public Probe [] nextSet (){
		
		if (startPos >= probes.length) {
			return null;
		}
		int windowEnd = probes[startPos].start()+windowSize;

		v.removeAllElements();
		v.add(probes[startPos]);
		Chromosome c = probes[startPos].chromosome();
		for (int i=startPos+1;i<probes.length;i++) {
			if (probes[i].chromosome() != c) break;
			if (probes[i].start() > windowEnd) break;
			if (probes[i].end() <=windowEnd) {
				v.add(probes[i]);
			}
		}
		++startPos;
		return v.toArray(new Probe[0]);
	}
	
}
