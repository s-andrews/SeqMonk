/**
 * Copyright Copyright 2016-19 Simon Andrews
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;



public class MultiGenome implements Genome {
	
	private SingleGenome [] genomes;
	private AnnotationCollection annotation;
	
	public MultiGenome (SingleGenome [] genomes) {
		this.genomes = genomes;
		annotation = new AnnotationCollection(this);
		for (int g=0;g<genomes.length;g++) {
			AnnotationSet [] sets = genomes[g].annotationCollection().anotationSets();
			annotation.addAnnotationSets(sets);
		}
	}
	
	public boolean containsGenome (Genome genomeToTest) {
		
		if (genomeToTest == this) return true;
		
		for (int g=0;g<genomes.length;g++) {
			if (genomes[g] == genomeToTest) return true;
		}
		
		return false;
	}
	
	public Genome [] getSubGenomes () {
		return genomes;
	}

	public Chromosome getExactChromsomeNameMatch(String name) {
		for (int g=0;g<genomes.length;g++) {
			Chromosome c = genomes[g].getExactChromsomeNameMatch(name);
			if (c != null) return c;
		}
		
		return null;
	}

	public AnnotationCollection annotationCollection() {
		return annotation;
	}

	public int getChromosomeCount() {
		int count = 0;
		for (int g=0;g<genomes.length;g++) {
			count += genomes[g].getChromosomeCount();
		}
		return count;
	}

	public boolean hasChromosome(Chromosome c) {
		for (int g=0;g<genomes.length;g++) {
			if (genomes[g].hasChromosome(c)) {
				return true;
			}
		}
		return false;
	}

	public Chromosome[] getAllChromosomes() {
		Vector<Chromosome> chrs = new Vector<Chromosome>();
		
		for (int g=0;g<genomes.length;g++) {
			Chromosome [] c = genomes[g].getAllChromosomes();
			for (int i=0;i<c.length;i++) {
				chrs.add(c[i]);
			}
		}
		
		Chromosome [] returnChrs = chrs.toArray(new Chromosome[0]);
		Arrays.sort(returnChrs);
		return returnChrs;
	}

	public ChromosomeWithOffset getChromosome(String name) {
		for (int g=0;g<genomes.length;g++) {
			try {
				return genomes[g].getChromosome(name);
			}
			catch (Exception e) {}
		}
		throw new IllegalArgumentException("Couldn't find a chromosome called '"+name+"'");
	}

	public String[] listUnloadedFeatureTypes() {
		HashSet<String> names = new HashSet<String>();
		
		for (int g=0;g<genomes.length;g++) {
			String [] s = genomes[g].listUnloadedFeatureTypes();
			for (int i=0;i<s.length;i++) {
				names.add(s[i]);
			}
		}
		
		String [] returnNames = names.toArray(new String[0]);
		return returnNames;
	}


	public long getTotalGenomeLength() {
		long length = 0;
		for (int g=0;g<genomes.length;g++) {
			length += genomes[g].getTotalGenomeLength();
		}
		return length;
	}

	public String species() {
		StringBuffer b = new StringBuffer();
		
		b.append(genomes[0].species());
		for (int i=1;i<genomes.length;i++) {
			b.append("|");
			b.append(genomes[i].species());
		}
		
		return b.toString();
	}
	
	public String toString () {
		StringBuffer b = new StringBuffer();
		
		for (int i=0;i<genomes.length;i++) {
			b.append(genomes[i].toString());
			if (i<genomes.length-1) {
				b.append(" + ");
			}
		}
		
		return b.toString();
	}
	

	public String assembly() {
		StringBuffer b = new StringBuffer();
		
		b.append(genomes[0].assembly());
		for (int i=1;i<genomes.length;i++) {
			b.append("|");
			b.append(genomes[i].assembly());
		}
		
		return b.toString();
	}

	public int getLongestChromosomeLength() {
		int longest = 0;
		for (int g=0;g<genomes.length;g++) {
			int thisLength = genomes[g].getLongestChromosomeLength();
			if (thisLength > longest) longest = thisLength;
		}
		return longest;
	}
	
}
