/**
 * Copyright Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataTypes;


import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.Utilities.LongSorter.LongSetSorter;

/**
 * The Class DataGroup is a virtual DataStore which can combine
 * one or more DataSets.  It does not store read information, but
 * does store its own quantitated data.
 */
public class DataGroup extends DataStore implements HiCDataStore {
	
	/** The data sets. */
	private DataSet [] dataSets;

	
	/**
	 * Instantiates a new data group.
	 * 
	 * @param name the name
	 * @param dataSets the data sets
	 */
	public DataGroup (String name, DataSet [] dataSets) {
		super(name);
		this.dataSets = dataSets;
	}
		
	/**
	 * Data sets.
	 * 
	 * @return the data set[]
	 */
	public DataSet [] dataSets () {
		return dataSets;
	}
	
	
	/**
	 * Sets the data sets.
	 * 
	 * @param sets the new data sets
	 */
	public void setDataSets (DataSet [] sets) {
		dataSets = sets;
		if (collection() != null) {
			collection().dataGroupSamplesChanged(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#setName(java.lang.String)
	 */
	public void setName (String name) {
		super.setName(name);
		if (collection() != null) {
			collection().dataGroupRenamed(this);
		}
	}

	
	/**
	 * Contains data set.
	 * 
	 * @param s the s
	 * @return true, if successful
	 */
	public boolean containsDataSet (DataSet s) {
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i]==s)
				return true;
		}
		return false;
	}
	
	/**
	 * Removes the data set.
	 * 
	 * @param s the s
	 */
	public void removeDataSet (DataSet s) {
		if (! containsDataSet(s)) return;
		
		DataSet [] newSet = new DataSet[dataSets.length-1];
		int j=0;
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] == s) continue;
			newSet[j] = dataSets[i];
			j++;
		}
		
		dataSets = newSet;
		
		if (collection() != null) {
			collection().dataGroupSamplesChanged(this);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadCountForChromosome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public int getReadCountForChromosome(Chromosome c) {
		int count = 0;
		for (int i=0;i<dataSets.length;i++) {
			count += dataSets[i].getReadCountForChromosome(c);
		}
		return count;
	}
	
	public boolean isQuantitated() {
		if (dataSets.length == 0) return false;
		
		return super.isQuantitated();
	}
	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadsForChromsome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public long [] getReadsForChromosome(Chromosome c) {
		
		long [][] readsFromAllChrs = new long[dataSets.length][];

//		int totalCount = 0;
		
		for (int i=0;i<dataSets.length;i++) {
			readsFromAllChrs[i] = dataSets[i].getReadsForChromosome(c);
		}
		
		return LongSetSorter.sortLongSets(readsFromAllChrs);
		
//		int [] currentIndices = new int[dataSets.length];
//		
//		long [] returnedReads = new long[totalCount];
//		
//		for (int i=0;i<returnedReads.length;i++) {	
//			// Add the lowest read to the full set
//			int lowestIndex = -1;
//			long lowestValue = 0;
//			for (int j=0;j<currentIndices.length;j++) {
//				if (currentIndices[j] == readsFromAllChrs[j].length) continue; // Skip datasets we've already emptied
//				if (lowestValue == 0 || SequenceRead.compare(readsFromAllChrs[j][currentIndices[j]],lowestValue) < 0) {
//					lowestIndex = j;
//					lowestValue = readsFromAllChrs[j][currentIndices[j]];
//				}
//			}
//			
//			returnedReads[i] = lowestValue;
//			currentIndices[lowestIndex]++;
//			
//		}
//		
//		return returnedReads;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getTotalReadCount()
	 */
	public int getTotalReadCount() {
		int count = 0;
		for (int i=0;i<dataSets.length;i++) {
			count += dataSets[i].getTotalReadCount();
		}
		return count;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadCountForStrand()
	 */
	public int getReadCountForStrand(int strand) {
		int count = 0;
		for (int i=0;i<dataSets.length;i++) {
			count += dataSets[i].getReadCountForStrand(strand);
		}
		return count;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getTotalReadLength()
	 */
	public long getTotalReadLength() {
		long count = 0;
		for (int i=0;i<dataSets.length;i++) {
			count += dataSets[i].getTotalReadLength();
		}
		return count;
	}
	
	public int getMaxReadLength() {

		int max = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (i==0 || dataSets[i].getMaxReadLength() > max) max = dataSets[i].getMaxReadLength();
		}

		return max;
	}

	public int getMinReadLength() {
		int min = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (i==0 || dataSets[i].getMinReadLength() < min) min = dataSets[i].getMinReadLength();
		}

		return min;
	}

	public int getTotalPairCount () {
		return getTotalReadCount()/2;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadsForProbe(uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe)
	 */
	public long [] getReadsForProbe(Probe p) {
		long [][] returnReads = new long [dataSets.length][];
		for (int i=0;i<dataSets.length;i++) {
			returnReads[i] = dataSets[i].getReadsForProbe(p);
		}
		return LongSetSorter.sortLongSets(returnReads);
	}
	

	public boolean isValidHiC() {
		if (dataSets.length == 0) return false;
		
		for (int i=0;i<dataSets.length;i++) {
			if (! (dataSets[i] instanceof HiCDataStore  && ((HiCDataStore)dataSets[i]).isValidHiC())) {
				return false;
			}
		}
		
		return true;
	}

	public HiCHitCollection getHiCReadsForProbe(Probe p) {

		HiCHitCollection collection = new HiCHitCollection(p.chromosome().name());
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				HiCHitCollection thisCollection = ((HiCDataStore)dataSets[i]).getHiCReadsForProbe(p);
				collection.addCollection(thisCollection);				
			}
		}
		
		// TODO: Do an incremental add to this collection so that we don't have 
		// to re-sort it each time.  This is nastily inefficient
		collection.sortCollection();

		return collection;	
	}

	public HiCHitCollection getHiCReadsForChromosome(Chromosome c) {
				
		HiCHitCollection collection = new HiCHitCollection(c.name());
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				HiCHitCollection thisCollection = ((HiCDataStore)dataSets[i]).getHiCReadsForChromosome(c);
				collection.addCollection(thisCollection);				
			}
		}

		// TODO: Do an incremental add to this collection so that we don't have 
		// to re-sort it each time.  This is nastily inefficient
		collection.sortCollection();

		
		return collection;	
	}

	public HiCHitCollection getExportableReadsForChromosome(Chromosome c) {
		HiCHitCollection collection = new HiCHitCollection(c.name());
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				HiCHitCollection thisCollection = ((HiCDataStore)dataSets[i]).getExportableReadsForChromosome(c);
				collection.addCollection(thisCollection);				
			}
		}
		
		collection.sortCollection();
		
		return collection;	
	}

	
	public float getCorrectionForLength(Chromosome c, int minDist, int maxDist) {
		// This hadn't been filled in at all.
		//
		// We'll start by doing something simple, an average of the weights from
		// all of the datasets we contain.  We will have to improve on this 
		// eventually.
		
		float correctionFactor = 0;
		
		for (int d=0;d<dataSets.length;d++) {
			correctionFactor += Math.log10(((HiCDataStore)dataSets[d]).getCorrectionForLength(c, minDist, maxDist));
		}
		
		return (float)Math.pow(10, correctionFactor/dataSets.length);
	
	}

	public int getCisCountForChromosome(Chromosome c) {
		int total = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataSets[i]).getCisCountForChromosome(c);
			}
		}
		return total;
	}

	public int getTransCountForChromosome(Chromosome c) {
		int total = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataSets[i]).getTransCountForChromosome(c);
			}
		}
		return total;
	}

	public int getCisCount() {
		int total = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataSets[i]).getCisCount();
			}
		}
		return total;
	}

	public int getTransCount() {
		int total = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataSets[i]).getTransCount();
			}
		}
		return total;
	}

	public int getHiCReadCountForProbe(Probe p) {
		int total = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataSets[i]).getHiCReadCountForProbe(p);
			}
		}
		return total;
	}

	public int getHiCReadCountForChromosome(Chromosome c) {
		int total = 0;
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataSets[i]).getHiCReadCountForChromosome(c);
			}
		}
		return total;
	}



}
