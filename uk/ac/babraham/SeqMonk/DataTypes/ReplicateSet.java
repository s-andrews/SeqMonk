/**
 * Copyright 2010-19 Simon Andrews
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

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;


/**
 * A replicate set is a way to group together data stores
 * which are biological replicates of each other.  Unlike
 * DataStores replicate sets do not store their own
 * quantitations but simply aggregate the distribution of
 * quantitated values from their component members.
 * 
 */
public class ReplicateSet extends DataStore implements HiCDataStore {

	/** The data stores. */
	private DataStore [] dataStores;
	
	private ReadsWithCounts cachedReadsWithCounts = null;
	private Chromosome lastUsedChromosome = null;

	
	/**
	 * Instantiates a new replicate set.
	 * 
	 * @param name the name
	 * @param dataStores the data stores
	 */
	public ReplicateSet (String name, DataStore [] dataStores) {
		super(name);
		setDataStores(dataStores);
	}
		
	/**
	 * Data stores.
	 * 
	 * @return the data store[]
	 */
	public DataStore [] dataStores () {
		return dataStores;
	}
	
	
	/**
	 * Sets the data sets.
	 * 
	 * @param sets the new data sets
	 */
	public void setDataStores (DataStore [] stores) {
		
		// Check for invalid stores
		for (int s=0;s<stores.length;s++) {
			if (stores[s] instanceof ReplicateSet) {
				throw new IllegalStateException("Can't add a replicate set to another replicate set");
			}
		}
		
		// Reset caches, since they're not valid any more
		lastUsedChromosome = null;
		cachedReadsWithCounts = null;

		
		dataStores = stores;
		if (collection() != null) {
			collection().replicateSetStoresChanged(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#setName(java.lang.String)
	 */
	public void setName (String name) {
		super.setName(name);
		if (collection() != null) {
			collection().replicateSetRenamed(this);
		}
	}

	
	/**
	 * Contains data store.
	 * 
	 * @param s the s
	 * @return true, if successful
	 */
	public boolean containsDataStore (DataStore s) {
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i]==s)
				return true;
		}
		return false;
	}
	
	/**
	 * Removes a data store.
	 * 
	 * @param s the s
	 */
	public void removeDataStore (DataStore s) {
		if (! containsDataStore(s)) return;
		
		// Reset caches, since they're not valid any more
		lastUsedChromosome = null;
		cachedReadsWithCounts = null;
		
		
		DataStore [] newSet = new DataStore[dataStores.length-1];
		int j=0;
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] == s) continue;
			newSet[j] = dataStores[i];
			j++;
		}
		
		dataStores = newSet;
		
		if (collection() != null) {
			collection().replicateSetStoresChanged(this);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadCountForChromosome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public int getReadCountForChromosome(Chromosome c) {
		int count = 0;
		for (int i=0;i<dataStores.length;i++) {
			count += dataStores[i].getReadCountForChromosome(c);
		}
		return count;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadsForChromsome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public ReadsWithCounts getReadsForChromosome(Chromosome c) {
		
		if (lastUsedChromosome != null && lastUsedChromosome == c) {
			return cachedReadsWithCounts;
		}
		
		ReadsWithCounts [] readsFromAllChrs = new ReadsWithCounts[dataStores.length];
		
		for (int i=0;i<dataStores.length;i++) {
			readsFromAllChrs[i] = dataStores[i].getReadsForChromosome(c);
		}
		
		cachedReadsWithCounts =  new ReadsWithCounts(readsFromAllChrs);
		lastUsedChromosome = c;
		return (cachedReadsWithCounts);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getTotalReadCount()
	 */
	public long getTotalReadCount() {
		long count = 0;
		for (int i=0;i<dataStores.length;i++) {
			count += dataStores[i].getTotalReadCount();
		}
		return count;
	}
	
	public long getTotalPairCount () {
		return getTotalReadCount()/2;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadCountForStrand()
	 */
	public long getReadCountForStrand(int strand) {
		long count = 0;
		for (int i=0;i<dataStores.length;i++) {
			count += dataStores[i].getReadCountForStrand(strand);
		}
		return count;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getTotalReadLength()
	 */
	public long getTotalReadLength() {
		long count = 0;
		for (int i=0;i<dataStores.length;i++) {
			count += dataStores[i].getTotalReadLength();
		}
		return count;
	}
	
	public int getMaxReadLength() {

		int max = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (i==0 || dataStores[i].getMaxReadLength() > max) max = dataStores[i].getMaxReadLength();
		}

		return max;
	}

	public int getMinReadLength() {
		int min = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (i==0 || dataStores[i].getMinReadLength() < min) min = dataStores[i].getMinReadLength();
		}

		return min;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadsForProbe(uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe)
	 */
	public ReadsWithCounts getReadsWithCountsForProbe(Probe p) {
		ReadsWithCounts [] returnReads = new ReadsWithCounts [dataStores.length];
		for (int i=0;i<dataStores.length;i++) {
			returnReads[i] = dataStores[i].getReadsWithCountsForProbe(p);
		}
		return new ReadsWithCounts(returnReads);
	}
	
	
	public long [] getReadsForProbe (Probe p) {
		return getReadsWithCountsForProbe(p).expandReads();
	}

	
	
	/**
	 * Checks if is quantitated.  Only true if all of the stores
	 * in this set are quantitated.
	 * 
	 * @return true, if is quantitated
	 */
	public boolean isQuantitated () {
		
		if (dataStores.length == 0) return false;
		
		for (int i=0;i<dataStores.length;i++) {
			if (!dataStores[i].isQuantitated()) {
				return false;
			}
		}
		
		return true;	
	}
	
	/**
	 * Sets the value for probe.  You can't do this to a replicate set
	 * since it doesn't store probe values, so this will always throw
	 * an error.
	 * 
	 * @param p the p
	 * @param f the f
	 */
	public void setValueForProbe (Probe p, float f) {
		throw new IllegalArgumentException("You can't set probe values for a replicate set");
	}
	
	/**
	 * Checks whether we have a value for this probe.  This is only
	 * true if all of the data stores in this replicate set have a
	 * value for this probe
	 * 
	 * @param p the p
	 * @return true, if successful
	 */
	public boolean hasValueForProbe (Probe p) {

		for (int i=0;i<dataStores.length;i++) {
			if (!dataStores[i].hasValueForProbe(p)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Gets the mean value for this probe across all of the
	 * data stores underlying this set.
	 * 
	 * @param p the p
	 * @return the mean value for probe
	 * @throws SeqMonkException the seq monk exception
	 */
	public float getValueForProbe(Probe p) throws SeqMonkException {
		
		if (! hasValueForProbe(p)) {
			throw new SeqMonkException("No quantitation for probe "+p+" in "+name());			
		}
		
		if (dataStores.length == 0) {
			return 0;
		}

		float total = 0;
		for (int i=0;i<dataStores.length;i++) {
			total += dataStores[i].getValueForProbe(p);
		}
		
		return total/dataStores.length;
		
	}
	
	public float getValueForProbeExcludingUnmeasured(Probe p) throws SeqMonkException {
		
		if (! hasValueForProbe(p)) {
			throw new SeqMonkException("No quantitation for probe "+p+" in "+name());			
		}
		
		if (dataStores.length == 0) {
			return 0;
		}

		float total = 0;
		int count = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (!Float.isNaN(dataStores[i].getValueForProbe(p))) {
				total += dataStores[i].getValueForProbe(p);
				++count;
			}
		}
		
		if (count == 0) return Float.NaN;
		
		return total/count;
		
	}
	
	public float [] getValuesForProbe (Probe p) throws SeqMonkException {
		float [] values = new float[dataStores.length];
		
		for (int d=0;d<values.length;d++) {
			values[d] = dataStores[d].getValueForProbe(p);
		}
		
		return values;
	}
	
	public float getStDevForProbe (Probe p) throws SeqMonkException {
		float [] values = getValuesForProbe(p);
		return SimpleStats.stdev(values);
	}
	
	public float getSEMForProbe (Probe p) throws SeqMonkException {
		float [] values = getValuesForProbe(p);
		return (float)(SimpleStats.stdev(values)/Math.sqrt(values.length));
	}
	
	public float getCoefVarForProbe (Probe p) throws SeqMonkException {
		float [] values = getValuesForProbe(p);
		return SimpleStats.stdev(values)/getValueForProbe(p);
	}

	public float getQuartileCoefDispForProbe (Probe p) throws SeqMonkException {
		float [] values = getValuesForProbe(p);
		float lowerQuartile = SimpleStats.percentile(values, 25);
		float upperQuartile = SimpleStats.percentile(values, 75);
		
		return ((upperQuartile-lowerQuartile)/(upperQuartile+lowerQuartile));
	}
	
	public float getUnmeasuredCountForProbe (Probe p) throws SeqMonkException {
		float [] values = getValuesForProbe(p);
		
		int nullCount = 0;
		
		for (int v=0;v<values.length;v++) {
			if (values[v] == dataStores[v].nullValue()) ++nullCount;
		}
		
		return nullCount;
		
	}
	
	
	public boolean isValidHiC() {
		if (dataStores.length == 0) return false;
		
		for (int i=0;i<dataStores.length;i++) {
			if (! (dataStores[i] instanceof HiCDataStore  && ((HiCDataStore)dataStores[i]).isValidHiC())) {
				return false;
			}
		}
		
		return true;
	}

	public HiCHitCollection getHiCReadsForProbe(Probe p) {
		
		HiCHitCollection collection = new HiCHitCollection(p.chromosome().name());
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				HiCHitCollection thisCollection = ((HiCDataStore)dataStores[i]).getHiCReadsForProbe(p);
				collection.addCollection(thisCollection);				
			}
		}
		
		collection.sortCollection();
		
		return collection;	
	}

	public HiCHitCollection getHiCReadsForChromosome(Chromosome c) {
		HiCHitCollection collection = new HiCHitCollection(c.name());
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				HiCHitCollection thisCollection = ((HiCDataStore)dataStores[i]).getHiCReadsForChromosome(c);
				collection.addCollection(thisCollection);				
			}
		}
		
		collection.sortCollection();
		
		return collection;	
	}

	public HiCHitCollection getExportableReadsForChromosome(Chromosome c) {
		HiCHitCollection collection = new HiCHitCollection(c.name());
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				HiCHitCollection thisCollection = ((HiCDataStore)dataStores[i]).getExportableReadsForChromosome(c);
				collection.addCollection(thisCollection);				
			}
		}
		
		collection.sortCollection();
		
		return collection;	
	}

	
	public float getCorrectionForLength(Chromosome c, int minDist, int maxDist) {
		return 0;
	}
	
	public int getCisCountForChromosome(Chromosome c) {
		int total = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataStores[i]).getCisCountForChromosome(c);
			}
		}
		return total;
	}

	public int getTransCountForChromosome(Chromosome c) {
		int total = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataStores[i]).getTransCountForChromosome(c);
			}
		}
		return total;
	}

	public long getCisCount() {
		long total = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataStores[i]).getCisCount();
			}
		}
		return total;
	}

	public long getTransCount() {
		long total = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataStores[i]).getTransCount();
			}
		}
		return total;
	}
	
	public int getHiCReadCountForProbe(Probe p) {
		int total = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataStores[i]).getHiCReadCountForProbe(p);
			}
		}
		return total;
	}

	public int getHiCReadCountForChromosome(Chromosome c) {
		int total = 0;
		for (int i=0;i<dataStores.length;i++) {
			if (dataStores[i] instanceof HiCDataStore) {
				total += ((HiCDataStore)dataStores[i]).getHiCReadCountForChromosome(c);
			}
		}
		return total;
	}

	
}
