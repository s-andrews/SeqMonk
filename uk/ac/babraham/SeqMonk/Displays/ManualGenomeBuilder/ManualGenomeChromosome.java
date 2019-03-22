/**
 * Copyright 2013-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.ManualGenomeBuilder;

public class ManualGenomeChromosome {

	private String name;
	private int currentLength = 1;
	private int minLength = 1;
	private boolean hasFeatures = false;
	private String pseudoChromosomeName;
	private int pseudoChromosomeOffset;
	private ManualGenome genome;
	
	public ManualGenomeChromosome (String name,ManualGenome genome) {
		this.name = name;
		this.genome = genome;
	}
	
	public String name() {
		if (name.toLowerCase().startsWith("chr")) {
			return name.substring(3);
		}
		return name;
	}
	
	public int length() {
		return currentLength;
	}
	
	public void setMinLength (int minLength) {
		if (minLength > this.minLength) this.minLength = minLength;
		
		if (currentLength < minLength) currentLength = minLength;
	}
	
	public void setLength (int length) {
		
		if (length > minLength) {
			currentLength = length;
			genome.lengthUpdated(this);
			System.err.println("Updated length");
		}
		else {
			System.err.println("New length "+length+" is shorter than the min length "+minLength);
		}
	}
	
	public void setHasFeatures() {
		if (hasFeatures == false) {
			hasFeatures = true;
			genome.addedFeatures(this);
		}
	}
	
	public boolean hasFeatures () {
		return hasFeatures;
	}
	
	public void setPseudoChromosome (String name, int offset) {
		pseudoChromosomeName = name;
		pseudoChromosomeOffset = offset;
		genome.pseudoChromosomeUpdated(this);
	}
	
	public String pseudoChromosomeName () {
		if (pseudoChromosomeName != null) {
			return pseudoChromosomeName;
		}
		else {
			return null;
		}
	}
	
	public int pseudoChromosomeOffset () {
		if (pseudoChromosomeName != null) {
			return pseudoChromosomeOffset;
		}
		else {
			return 0;
		}
	}


}
