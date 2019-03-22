/**
 * Copyright Copyright 2010-19 Simon Andrews
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

import java.util.Arrays;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * The ConsecutiveProbeGenerator is a utility which iterates through a probe list putting
 * together sets of probes of a defined size which fall next to each other. 
 */
public class FeatureProbeGroupGenerator implements ProbeGroupGenerator {

	private Probe [] probes;
	private Feature [] features;
	private int probeStartIndex;
	private int featureIndex;
	private Vector<Probe> v = new Vector<Probe>();
		
	/**
	 * Instantiates a new probe window generator.
	 * 
	 * @param probes The list of probes from which the sets will be composed
	 * @param probeCount The size of the window in base pairs
	 * @throws SeqMonkException if the dataColleciton isn't quantitated
	 */
	public FeatureProbeGroupGenerator (Probe [] probes, Feature [] features) {
		
		this.probes = probes;
		this.features = features;
		this.features = new Feature[features.length];
		for (int f=0;f<features.length;f++)this.features[f] = features[f];
		Arrays.sort(this.features);
		probeStartIndex = 0;
		featureIndex = 0;
	}
	
	/**
	 * Provides the next set of probe from this generator
	 * 
	 * @return A list of probes in the next set.
	 */
	public Probe [] nextSet (){
		
		if (featureIndex >= features.length) {
			return null;
		}
		
		Feature thisFeature = features[featureIndex];
		
		int thisStartIndex = probeStartIndex;
		if (thisStartIndex >= probes.length) {
			thisStartIndex = probes.length-1;
		}
		v.clear();
		
		// Go back from this position until we find that we're more than 
		// 5kb before the current feature
		while (thisStartIndex > 0) {
			if (!probes[thisStartIndex].chromosome().name().equals(thisFeature.chromosomeName())) {
				thisStartIndex++;
				break;
			}
			
			if (probes[thisStartIndex].end() < thisFeature.location().start()-5000) break;
			
			thisStartIndex--;
		}
		
		if (thisStartIndex < 0) thisStartIndex = 0;
		
		while (true) {
			if (thisStartIndex >= probes.length) break;
			if (!probes[thisStartIndex].chromosome().name().equals(thisFeature.chromosomeName())) break;
			if (SequenceRead.overlaps(probes[thisStartIndex].packedPosition(), thisFeature.location().packedPosition())) {
				v.add(probes[thisStartIndex]);
			}
			thisStartIndex++;
		}

		probeStartIndex = thisStartIndex;
		++featureIndex;
		return v.toArray(new Probe[0]);
	}
	
}
