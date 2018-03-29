/**
 * Copyright 2012-18 Simon Andrews
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import uk.ac.babraham.SeqMonk.Utilities.LongVector;
import uk.ac.babraham.SeqMonk.Utilities.LongSorter.HiCLongComparator;
import uk.ac.babraham.SeqMonk.Utilities.LongSorter.IntSorter;


/**
 * This class is used to return a set of results from a HiC query, whether
 * this is the set of reads covering a probe, or all reads from a chromosome
 * 
 * The structure allows the read information to be kept as a set of encoded
 * long values, with the chromosome information being kept in a controlled
 * data structure, and not reproduced with each read.
 * @author andrewss
 *
 */
public class HiCHitCollection implements Serializable {

	/**
	 * We have to store the chromosome name as a string rather than an 
	 * actual chromsome object as we need to be able to serialise and 
	 * recreate this object and if we do that then the chromosome objects
	 * we end up with aren't the same as the ones we started with.  Using
	 * strings makes this easier since the comparisons still work, and 
	 * chromosome names need to be unique anyway.
	 */
	private String sourceChromosome;
	
	/**
	 * The main data structure is a hash with keys which are the 
	 * chromosomes of the other ends of the region being searched.  The
	 * value is always a 2 element array of longs with the first being
	 * the position of the first end (the one which was queried), and the
	 * second being the position of the other end (the one which relates to
	 * the Chromosome key
	 */
	private Hashtable<String, LongVector[]> hits;
	
	
	public HiCHitCollection (String sourceChromosomeName) {
		this.sourceChromosome = sourceChromosomeName;
		hits = new Hashtable<String, LongVector[]>();
	}
	
	public void addHit (String hitChromsome, long sourcePosition, long hitPosition) {
		if (! hits.containsKey(hitChromsome)) {
			hits.put(hitChromsome, new LongVector [] {new LongVector(),new LongVector()});
		}
				
		hits.get(hitChromsome)[0].add(sourcePosition);
		hits.get(hitChromsome)[1].add(hitPosition);		
	}
	
	public String [] getChromosomeNamesWithHits () {
		String [] chromosomes =  hits.keySet().toArray(new String[0]);
		Arrays.sort(chromosomes);
		return chromosomes;
	}
	
	public String getSourceChromosomeName () {
		return sourceChromosome;
	}

	public long [] getSourcePositionsForChromosome (String chromosomeName) {
		if (! hits.containsKey(chromosomeName)) {
			return new long[0];
		}
		
		return hits.get(chromosomeName)[0].toArray();
		
	}
	
	public long [] getAllSourcePositions () {
		LongVector lv = new LongVector();
		Enumeration<String>en = hits.keys();
		while (en.hasMoreElements()) {
			long [] thisSourcePositions = getSourcePositionsForChromosome(en.nextElement());
			for (int i=0;i<thisSourcePositions.length;i++) {
				lv.add(thisSourcePositions[i]);
			}
		}
		long [] returnArray = lv.toArray();
		SequenceRead.sort(returnArray);
		return returnArray;
	}

	public long [] getHitPositionsForChromosome (String chromosomeName) {
		if (! hits.containsKey(chromosomeName)) return new long[0];
		
		return hits.get(chromosomeName)[1].toArray();
	}
	
	public void addCollection (HiCHitCollection collection2) {

		String [] chromosomes = collection2.getChromosomeNamesWithHits();
		for (int c=0;c<chromosomes.length;c++) {
			long [] source = collection2.getSourcePositionsForChromosome(chromosomes[c]);
			long [] hits = collection2.getHitPositionsForChromosome(chromosomes[c]);
			
			for (int i=0;i<source.length;i++) {
				addHit(chromosomes[c], source[i], hits[i]);
			}
		}
			
	}
		
	public void sortCollection () {

//		System.err.println("Sorting collection");
		String [] chromosomes = getChromosomeNamesWithHits();
		
		for (int c=0;c<chromosomes.length;c++) {
			long [] sourcePositions = getSourcePositionsForChromosome(chromosomes[c]);
			long [] hitPositions = getHitPositionsForChromosome(chromosomes[c]);

			int [] indices = new int[sourcePositions.length];
			for (int i=0;i<indices.length;i++) indices[i] = i;
			
			HiCLongComparator comparator = new HiCLongComparator(sourcePositions, hitPositions);
			
//			long sortStartTime = System.currentTimeMillis();
			IntSorter.sortInts(indices, comparator);
//			long sortEndTime = System.currentTimeMillis();
			
//			System.err.println("Sort of "+getSourceChromosomeName()+" vs "+chromosomes[c]+" with "+indices.length+" took "+((sortEndTime-sortStartTime)/1000d));
			
			
			long [] sortedSourcePositions = new long[sourcePositions.length];
			for (int i=0;i<sortedSourcePositions.length;i++) {
				sortedSourcePositions[i] = sourcePositions[indices[i]];
			}
			
			long [] sortedHitPositions = new long[hitPositions.length];
			for (int i=0;i<sortedHitPositions.length;i++) {
				sortedHitPositions[i] = hitPositions[indices[i]];
			}
						
			// Now overwrite the original arrays
			hits.get(chromosomes[c])[0].setValues(sortedSourcePositions);
			hits.get(chromosomes[c])[1].setValues(sortedHitPositions);
			
		}
	}
	
	public void trim () {
		String [] chromosomes = getChromosomeNamesWithHits();
				
		for (int c=0;c<chromosomes.length;c++) {
			hits.get(chromosomes[c])[0].trim();
			hits.get(chromosomes[c])[1].trim();
		}
	}
	
	public void deduplicateCollection () {
		//TODO: Write deduplication code
	}
	
}
