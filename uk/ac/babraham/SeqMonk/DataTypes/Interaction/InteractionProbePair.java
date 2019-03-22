/**
 * Copyright 2011-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataTypes.Interaction;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

public class InteractionProbePair implements Comparable<InteractionProbePair> {

	private Probe probe1;
	private int probe1Index;
	private Probe probe2;
	private int probe2Index;
	private float value;
	private int absoluteValue;
	private boolean lowestIsProbe1 = false;
	private float signficance = 0;

	public InteractionProbePair (Probe probe1, int probe1Index, Probe probe2, int probe2Index, float value, int absoluteValue) {

		this.probe1 = probe1;
		this.probe1Index = probe1Index;
		this.probe2 = probe2;
		this.probe2Index = probe2Index;
		this.value = value;
		this.absoluteValue = absoluteValue;
		
		if (probe1.compareTo(probe2) <=0) {
			lowestIsProbe1 = true;
		}
	}

	/**
	 * Sets the assymetry value (range 0 - 100%) which will later
	 * be returned by getAsymmetry().  This value will default to
	 * 0% if it has never been set.
	 * 
	 * @param new asymmetry value
	 */
	public void setSignificance (float significance) {
		this.signficance = significance;
	}
	
	/**
	 * A simple test to say if the two probes in this interaction
	 * fall onto the same chromsome.  This test should be performed
	 * before using the distance() method.
	 * 
	 * @return True if the two probes are on the same chromosome
	 */
	public boolean sameChromosome () {
		return probe1().chromosome() == probe2().chromosome();
	}
	
	/**
	 * Calculates the minimal distance between ends of probes. If probes
	 * overlap returns 0.
	 * 
	 * This method assumes that you've already checked using sameChromosome()
	 * that the probes are on the same chromosome.  If they're not then we'll
	 * throw an IllegalArgumentException.
	 * 
	 * @return inter-probe distance
	 */
	public int distance () {
		if (sameChromosome()) {
			int distance = Math.max(probe1().start()-probe2().end(),probe2().start()-probe1().end());
		
			// Account for overlaps
			if (distance < 0) distance = 0;
		
			return distance;
		}
		
		throw new IllegalArgumentException("Probes were not on the same chromosome.");
	}
	
	
	/**
	 * Gets the significance value which was last assigned by the setSignificance()
	 * method. This value cannot be calculated by the data stored by this 
	 * object so relies on the object creator setting the value correctly.
	 * 
	 * If the value has never been set returns 100.
	 * 
	 * @return
	 */
	public float signficance () {
		return signficance;
	}
	
	/**
	 * The maximum strength of interaction across any of the datasets analysed
	 * 
	 * @return
	 */
	public float strength () {
		return value;
	}

	
	public int absolute () {
		return absoluteValue;
	}
		
	public Probe probe1 () {
		return probe1;
	}
	
	public int probe1Index () {
		return probe1Index;
	}
	
	public int probe2Index () {
		return probe2Index;
	}
	
	public Probe probe2 () {
		return probe2;
	}
		
	public Probe lowestProbe () {
		if (lowestIsProbe1) return probe1;
		return probe2;
	}
	
	public Probe highestProbe () {
		if (lowestIsProbe1) return probe2;
		return probe1;
	}

	public int compareTo(InteractionProbePair o) {

		// We compare probe positions to start with.
		if (lowestProbe() != o.lowestProbe()) return lowestProbe().compareTo(o.lowestProbe());
		if (highestProbe() != o.highestProbe()) return highestProbe().compareTo(o.highestProbe());

		// In the case of probe list searches we can have the same interaction
		// listed multiple times because the same probes appear in different lists.
		if (lowestIsProbe1) {
			if (o.lowestIsProbe1) {
				if (probe1Index != o.probe1Index) {
					return probe1Index-o.probe1Index;
				}
			}
			else {
				if (probe1Index != o.probe2Index) {
					return probe1Index-o.probe2Index;
				}
			}
		}
		else {
			if (o.lowestIsProbe1) {
				if (probe2Index != o.probe1Index) {
					return probe2Index-o.probe1Index;
				}
			}
			else {
				if (probe2Index != o.probe2Index) {
					return probe2Index-o.probe2Index;
				}
			}
			
		}
		
		return 0;
	}

	
}
