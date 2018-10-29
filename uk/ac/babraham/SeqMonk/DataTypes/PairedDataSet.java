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
package uk.ac.babraham.SeqMonk.DataTypes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.NonThreadSafeIntCounter;
import uk.ac.babraham.SeqMonk.Utilities.ThreadSafeIntCounter;

/**
 * Paired Data Set is used for HiC type experiments.  It's not intended for
 * use for normal paired end reads where the pairs represent part of a continuous
 * insert, but for cases where two distinct regions are associated.
 * 
 * This class is responsible for storing its own data since the superclass has
 * moved to just passive primitive longs around.
 * 
 */
public class PairedDataSet extends DataSet implements HiCDataStore {

	/**
	 * This is the resolution of the length corrections we're going to
	 * apply to HiC data.
	 */
	private static final int DISTANCE_GROUP_LENGTH = 10000;

	/** This value lets us see if any of the data added needs to be sorted.  We can save time if we can miss out this step */
	private boolean needToSort = false;

	/** These values are used when caching reads which are to be paired
	 * when adding more data.
	 */
	private long lastRead = 0;
	private Chromosome lastChromosome = null;

	private int minDistance;

	private int removeDuplicates;
	private boolean ignoreTrans;

	private Hashtable<Chromosome, ChromosomeDataStore> readData = new Hashtable<Chromosome, ChromosomeDataStore>();

	private Hashtable<Chromosome, NonThreadSafeIntCounter> cisChromosomeCounts = new Hashtable<Chromosome, NonThreadSafeIntCounter>();
	private Hashtable<Chromosome, NonThreadSafeIntCounter> transChromosomeCounts = new Hashtable<Chromosome, NonThreadSafeIntCounter>();

	private long cisCount = 0;
	private long transCount = 0;

	/** A flag to say if we've optimised this dataset */
	private boolean isFinalised = false;

	/** This count allows us to keep track of the progress of finalisation for the individual chromosomes */
	private ThreadSafeIntCounter chromosomesStillToFinalise;

	// These are cached values used when we're saving excess data to temp files

	/** The reads last loaded from the cache */
	private HiCHitCollection lastCachedHits = null;

	/** The last index at which a read was found on each chromosome */
	private int [] lastIndices = null;
	private String [] lastChromosomeHitNames = null;
	
	private boolean filterOnMinDistance;



	/** 
	 * This variable controls how many thread we allow to finalise at the same time.
	 * 
	 * We'll make as many threads as we have CPUs up to a limit of 6, above which we're 
	 * likely to hurt the throughput as we just thrash the underlying disks.
	 * 
	 * */

	private static final int MAX_CONCURRENT_FINALISE = Math.min(Runtime.getRuntime().availableProcessors(), 6);


	public PairedDataSet (String name, String fileName, int removeDuplicates, String importOptions, int minDistance, boolean ignoreTrans) {

		/*
		 * Because our duplicate removal will be based on HiC duplication (ie both ends are the
		 * same) we always set the superclass flag to be false and then we only pass on the reads
		 * which are deduplicated.  This also means we need to store the actual remove duplicates
		 * flag value here so we don't rely on the one from the superclass.
		 */

		super(name, fileName, DataSet.DUPLICATES_REMOVE_NO, importOptions);
		this.minDistance = minDistance;
		filterOnMinDistance = minDistance > 0;
		this.removeDuplicates = removeDuplicates;
		this.ignoreTrans = ignoreTrans;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#isHiC()
	 */
	public boolean isHiC() {
		return true;
	}

	public long getTotalReadCount () {
		return cisCount+transCount;
	}
	
	public long getTotalPairCount () {
		return getTotalReadCount()/2;
	}

	/**
	 * This method is called if a sequence is read from the input file
	 * but encounters an error which causes it to not be imported.  If
	 * we are holding on to one sequence of a pair then we need to
	 * discard that and return false.  If we're not then we return
	 * true to tell them to skip the next line
	 */
	public boolean importSequenceSkipped () {
		if (lastRead != 0) {
			lastRead = 0;
			lastChromosome = null;
			return false;
		}
		return true;
	}


	public void addData (Chromosome c, long read) {
		addData(c, read,false);
	}

	/**
	 * This method is used to add data to the paired data set and should be called
	 * by all parsers which create a new set.  Pairs of reads should be added 
	 * sequentially to the data set and will be paired by it internally.
	 * 
	 * Only the SeqMonk parser should ever set the noReverse parameter to true.  Specifying
	 * this for all data lets the dataset skip the sorting step when caching which is
	 * otherwise really slow, but if this is incorrectly skipped then subsequent results
	 * returned by the data set will be wrong.
	 * 
	 * @param c
	 * @param read
	 * @param noReverse
	 */
	public void addData (Chromosome c, long read, boolean skipSorting) {


		if (!skipSorting) needToSort = true;

		if (lastChromosome == null) {
			// We'll just store this read for now until we can pair it with
			// the next read which is submitted
			lastRead = read;
			lastChromosome = c;
		}
		else if (lastChromosome != c && ignoreTrans) {

			// Skip this all together.

			lastRead = 0;
			lastChromosome = null;

		}
		// Skip cis reads which are too close together
		else if (filterOnMinDistance && c == lastChromosome && SequenceRead.fragmentLength(lastRead, read)<minDistance) {

			lastRead = 0;
			lastChromosome = null;
		}

		else {

			// We're actually going to add this pair
			
			int increment = 2;
			if (skipSorting) increment = 1;
			
			if (lastChromosome != c) {
				// Increment the trans counts for each chromosome
				transCount += increment;
				
				if (transChromosomeCounts.containsKey(lastChromosome)) {
					transChromosomeCounts.get(lastChromosome).increment();
				}
				else {
					transChromosomeCounts.put(lastChromosome,new NonThreadSafeIntCounter());
					transChromosomeCounts.get(lastChromosome).increment();
				}
				if (!skipSorting) {
					if (transChromosomeCounts.containsKey(c)) {
						transChromosomeCounts.get(c).increment();
					}
					else {
						transChromosomeCounts.put(c,new NonThreadSafeIntCounter());
						transChromosomeCounts.get(c).increment();
					}
				}
			}
			else {
				// Increment the cis counts for each chromosome

				cisCount += increment;
				if (cisChromosomeCounts.containsKey(c)) {
					cisChromosomeCounts.get(lastChromosome).increment();
				}
				else {
					cisChromosomeCounts.put(c,new NonThreadSafeIntCounter());
					cisChromosomeCounts.get(lastChromosome).increment();
				}
			}


			//				System.err.println("Keeping read pair from "+c+" and "+lastChromosome+" with distance "+(Math.max(SequenceRead.start(read), SequenceRead.start(lastRead))-Math.min(SequenceRead.end(read),SequenceRead.end(lastRead)))+" start is "+SequenceRead.start(read)+" end is "+SequenceRead.end(read)+" start2="+SequenceRead.start(lastRead)+" end2="+SequenceRead.end(lastRead));


			try {

				if (isFinalised) {
					throw new SeqMonkException("This data set is finalised.  No more data can be added");
				}		

				// Add the forward pair.
				if (!readData.containsKey(lastChromosome)) {
					ChromosomeDataStore cds = new ChromosomeDataStore(lastChromosome);
					readData.put(lastChromosome, cds);
				}
				readData.get(lastChromosome).hitCollection.addHit(c.name(), lastRead, read);


				if (!skipSorting) {
					// Add the reverse pair.
					if (!readData.containsKey(c)) {
						ChromosomeDataStore cds = new ChromosomeDataStore(c);
						readData.put(c, cds);
					}
					readData.get(c).hitCollection.addHit(lastChromosome.name(), read, lastRead);
				}

			}
			catch (SeqMonkException sme) {
				throw new IllegalStateException(sme);
			}

			lastRead = 0;
			lastChromosome = null;
		}


	}


	public boolean isValidHiC() {
		return true;
	}

	public int getHiCReadCountForProbe (Probe p) {
		return getCountForHitCollection(getHiCReadsForProbe(p));
	}

	public int getHiCReadCountForChromosome (Chromosome c) {
		return getCountForHitCollection(getHiCReadsForChromosome(c));
	}


	private int getCountForHitCollection (HiCHitCollection collection) {
		int count = 0;
		String [] chromosomes = collection.getChromosomeNamesWithHits();
		for (int c=0;c<chromosomes.length;c++) {
			count += collection.getSourcePositionsForChromosome(chromosomes[c]).length;
		}

		return count;
	}
	
	private synchronized HiCHitCollection updateCacheToChromosome (Chromosome c) {

		if (! isFinalised) finalise();

		// See if we need to update the cache
		if (lastCachedHits == null || ! c.name().equals(lastCachedHits.getSourceChromosomeName())) {

//			if (lastCachedHits == null) {
//				System.err.println("Updating cache to load "+c.name()+" since nothing is loaded");
//			}
//			else {
//				System.err.println("Updating cache to load "+c.name()+" since it doesn't match "+lastCachedHits.getSourceChromosomeName());				
//			}
			lastCachedHits = getHiCReadsForChromosome(c);

			/*
			 * We don't want to have to do an exhaustive search for reads
			 * so we make an educated guess as to where to start.
			 * 
			 * If we are coming at this fresh we make a guess as to where 
			 * to start.  This is based on there being an even distribution 
			 * of reads over the whole chromosome.
			 * 
			 * Because this can be a poor assumption in some datasets we also
			 * use a caching mechanism where we remember the last location we
			 * actually looked up.  If the next request is close to the last
			 * one then we start from the last position instead.  This works
			 * because most sets of requests come from quantitation methods
			 * and usually operate sequentially along the chromosome.
			 * 
			 * We also make the assumption that no read is >5kb in length 
			 * (which we can easily modify later if we have to) otherwise we'd 
			 * have to start from the beginning of the chromosome each time.
			 */

		}
		
		return lastCachedHits;

	}

	public HiCHitCollection getHiCReadsForProbe(Probe p) {

		HiCHitCollection hitCollection = new HiCHitCollection(p.chromosome().name());

		HiCHitCollection lastCachedHits = updateCacheToChromosome(p.chromosome());

		//		int hitCount = 0;

		if (lastChromosomeHitNames.length != lastIndices.length) {
			throw new IllegalArgumentException("Chr names "+lastChromosomeHitNames.length+" but indices "+lastIndices.length);
		}
		
		for (int c=0;c<lastChromosomeHitNames.length;c++) {

			long [] sourceReads = lastCachedHits.getSourcePositionsForChromosome(lastChromosomeHitNames[c]);
			long [] hitReads = lastCachedHits.getHitPositionsForChromosome(lastChromosomeHitNames[c]);

			// This was being triggered if a different thread updated the cache 
			// whilst we're doing this calculation. We now cache a local version
			// of the hit collection so that this can't happen and we're synchronized
			// the update cache code so that there can't be any contention there
			// either.  It seems to work OK.
			
			if (sourceReads.length != hitReads.length) {
				throw new IllegalStateException("Source reads and hit reads had different lengths for chr "+lastChromosomeHitNames[c]+" "+sourceReads.length+" vs "+hitReads.length+" current values are "+lastCachedHits.getSourcePositionsForChromosome(lastChromosomeHitNames[c]).length+" and "+lastCachedHits.getSourcePositionsForChromosome(lastChromosomeHitNames[c]).length);
			}
			
			if (sourceReads.length==0) continue;
			
			int startPos = lastIndices[c];

			if (startPos < 0) {
//				System.err.println("Started looking for reads at position "+startPos+" (below 0)");
				startPos = 0; // Shouldn't happen, but better safe than sorry
			}
			if (startPos >= sourceReads.length) {
				startPos = sourceReads.length-1;
			}

			// We can now backtrack from here to find the first possible hit

			boolean setCache = false;

//			System.err.println("Starting chr "+chromosomes[c]+" at index "+startPos+" position "+SequenceRead.midPoint(sourceReads[startPos]));
			for (int i=startPos-1;i>=0;i--) {
				// Reads come in order, so we can stop when we've seen enough.
				if (SequenceRead.end(sourceReads[i]) < p.start()-5000) {
//					System.err.println("Finished backtracking at index "+i+" position "+SequenceRead.midPoint(sourceReads[i]));
					break;
				}

				if (SequenceRead.overlaps(sourceReads[i], p.packedPosition())) {
					// They overlap.
					//					++hitCount;
					hitCollection.addHit(lastChromosomeHitNames[c], sourceReads[i], hitReads[i]);
					if (i != startPos-1 && ! setCache) {
						// This is the last read so set the cache position
						lastIndices[c] = i;
						setCache = true;
					}
				}			
			}

			// We now start one later than we did before to see if there are
			// any reads the other way

			for (int i=startPos;i<sourceReads.length;i++) {
				// Reads come in order, so we can stop when we've seen enough.
				if (SequenceRead.start(sourceReads[i]) > p.end()) {
//					System.err.println("Finished forward tracking at index "+i+" position "+SequenceRead.midPoint(sourceReads[i]));
					break;
				}

				if (SequenceRead.overlaps(sourceReads[i], p.packedPosition())) {
					// They overlap.
					//					++hitCount;
					hitCollection.addHit(lastChromosomeHitNames[c], sourceReads[i], hitReads[i]);
					lastChromosome = p.chromosome();
					lastIndices[c] = i;
				}
			}
		}

		//		System.err.println("Hit count for "+p+" is "+hitCount);

		return hitCollection;
	}

	public long [] getReadsForProbe(Probe p) {
		HiCHitCollection hits = getHiCReadsForProbe(p);
		// This is sorted by the Hit Collection
		return hits.getAllSourcePositions();
	}

	public boolean containsReadForProbe (Probe p) {
		return getReadsForProbe(p).length>0;
	}


	public synchronized HiCHitCollection getHiCReadsForChromosome(Chromosome c) {

		if (! isFinalised) finalise();

		if (readData.containsKey(c)) {

			if (readData.get(c).hitCollection != null) {
				// We're not caching, so just give them back the reads
				return readData.get(c).hitCollection;
			}
			else {
				// This is a serialised dataset.

				// Check if we've cached this data
				if (lastCachedHits != null && lastCachedHits.getSourceChromosomeName() == c.name()) {
					return lastCachedHits;
				}

				// Signal that we're accessing the cache so the cache icon can blink!
				SeqMonkApplication.getInstance().cacheUsed();

				// If not then we need to reload the data from the
				// temp file
				try {
					ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(readData.get(c).tempFile)));
					lastCachedHits = (HiCHitCollection)ois.readObject();
					ois.close();
					
					lastChromosomeHitNames = lastCachedHits.getChromosomeNamesWithHits();
					lastIndices = new int[lastChromosomeHitNames.length];
					
					return lastCachedHits;
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}

		}
		else {
			lastChromosomeHitNames = new String[0];
			lastCachedHits = new HiCHitCollection(c.name());
			lastIndices = new int[0];
			return lastCachedHits;
		}
	}

	public ReadsWithCounts getReadsForChromosome(Chromosome c) {
		// This is sorted by the Hit Collection
		return new ReadsWithCounts(getHiCReadsForChromosome(c).getAllSourcePositions());
	}
	
	public int getReadCountForChromosome (Chromosome c) {
		return getReadsForChromosome(c).totalCount();
	}



	public synchronized HiCHitCollection getExportableReadsForChromosome(Chromosome c) {

		if (! isFinalised) finalise();

		HiCHitCollection exportableReads = new HiCHitCollection(c.name());

		HiCHitCollection redundantReads = getHiCReadsForChromosome(c);

		String [] chrs = redundantReads.getChromosomeNamesWithHits();

		for (int i=0;i<chrs.length;i++) {

			// The way we have to do this lookup is ugly, but that's how it is

			// We only include data for chromosomes the same or later than our own

			Chromosome thisChr = SeqMonkApplication.getInstance().dataCollection().genome().getChromosome(chrs[i]).chromosome();

			if (c.compareTo(thisChr) < 0)  {
				System.err.println("Ignoring "+thisChr+" when exporting "+c);
				continue;
			}

			long [] source = redundantReads.getSourcePositionsForChromosome(chrs[i]);
			long [] hits = redundantReads.getHitPositionsForChromosome(chrs[i]);

			if (c != thisChr) {
				// Put everything from this chromosome into the results
				for (int j=0;j<source.length;j++) {
					exportableReads.addHit(chrs[i], source[j], hits[j]);
				}
			}
			else {
				// Put only the entries where the hit is later than the source in
				for (int j=0;j<source.length;j++) {

					// We include only hits where the hit is greater than the source
					// to avoid repetition.  If the two are the same then we take half
					// of the hits based on the current index.

					if ((source[j] == hits[j] && j%2==0) || SequenceRead.compare(source[j],hits[j])>=0) {
						exportableReads.addHit(chrs[i], source[j], hits[j]);
					}
				}
			}
		}	

		return exportableReads;
	}

	public synchronized float getCorrectionForLength(Chromosome c, int minDist, int maxDist) {

		if (! isFinalised) finalise();

		int startIndex = minDist/DISTANCE_GROUP_LENGTH;
		int endIndex = maxDist/DISTANCE_GROUP_LENGTH;

		if (startIndex < 0) {
			startIndex = 0;
		}

		float totalCorrection = 0;

		ChromosomeDataStore cds = readData.get(c);

		for (int index=startIndex;index<=endIndex;index++) {

			float thisCorrection = cds.getCorrectionForIndex(index);

			totalCorrection += thisCorrection;
		}


		float adjustedCorrection = totalCorrection/(((endIndex-startIndex)+1));

		//		if (adjustedCorrection < cds.getCorrectionForIndex(c.length()/DISTANCE_GROUP_LENGTH) || adjustedCorrection > cds.getCorrectionForIndex(0)) {
		//			throw new IllegalArgumentException("Correction "+adjustedCorrection+" was out of bounds of actual corrections indices were "+startIndex+" and "+endIndex+" total correction was "+totalCorrection);
		//		}

		return adjustedCorrection;
	}

	/**
	 * This call optimises the data structure from a flexible structure
	 * which can accept more data, to a fixed structure optimised for
	 * size and speed of access.  If required it can also cache the data
	 * to disk.
	 * 
	 * This call should only be made by DataParsers who know that no
	 * more data will be added.
	 */	
	public synchronized void finalise () {

		if (isFinalised) return;

		// To make querying the data more efficient we're going to convert
		// all of the vectors in our data structure into SequenceRead arrays
		// which are sorted by start position.  This means that subsequent
		// access will be a lot more efficient.

		long finaliseStartTime = System.currentTimeMillis();
		Enumeration<Chromosome> e = readData.keys();

		chromosomesStillToFinalise = new ThreadSafeIntCounter();

		while (e.hasMoreElements()) {

			while (chromosomesStillToFinalise.value() >= MAX_CONCURRENT_FINALISE) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException ex) {}
			}

			Chromosome c = e.nextElement();
			chromosomesStillToFinalise.increment();
			readData.get(c).finalise();
		}

		// Now we need to wait around for the last chromosome to finish
		// processing

		while (chromosomesStillToFinalise.value() > 0) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException ex) {}
		}

		isFinalised = true;

		long finaliseEndTime = System.currentTimeMillis();


		// Finally, now that we've sent all of the correct data up to the superclass
		// we can let that finalise itself.

		long superSubEndTime = System.currentTimeMillis();
		super.finalise();

		long superFinaliseEndTime = System.currentTimeMillis();

		System.err.println("HiC finalise="+((finaliseEndTime-finaliseStartTime)/1000d)+" super submit="+((superSubEndTime-finaliseEndTime)/1000d)+" super finalise="+((superFinaliseEndTime-superSubEndTime)/1000d));



	}


	public void run () {

		// We need to delete any cache files we're still holding

		// Clear out the DataSet
		super.run();

		// Now clean up our own mess
		Enumeration<Chromosome> e = readData.keys();
		while (e.hasMoreElements()) {
			Chromosome c = e.nextElement();

			File f = readData.get(c).tempFile;
			if (f != null) {
				if (!f.delete()) System.err.println("Failed to delete cache file "+f.getAbsolutePath());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore#getCisCountForChromosome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public int getCisCountForChromosome(Chromosome c) {
		if (cisChromosomeCounts.containsKey(c)) {
			return cisChromosomeCounts.get(c).value();
		}
		return 0;
	}
	
	/*
	 * (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore#getTransCountForChromosome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public int getTransCountForChromosome(Chromosome c) {
		if (transChromosomeCounts.containsKey(c)) {
			return transChromosomeCounts.get(c).value();
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore#getCisCount()
	 */
	public long getCisCount() {
		return cisCount;
	}

	/*
	 * (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore#getTransCount()
	 */
	public long getTransCount() {
		return transCount;
	}
	
	


	/**
	 * The Class ChromosomeDataStore.
	 */
	private class ChromosomeDataStore implements Runnable {

		public HiCHitCollection hitCollection;

		/** The temp file. */
		public File tempFile = null;

		/** The length based counts **/
		public float [] distanceCorrections;

		public ChromosomeDataStore (Chromosome c) {
			hitCollection = new HiCHitCollection(c.name());
			distanceCorrections = new float[getIndexForLength(c.length())+1];
		}

		private int getIndexForLength (int length) {
			// Work out which index this length represents
			int index = length/DISTANCE_GROUP_LENGTH;

			return index;
		}


		public void finalise () {
			Thread t = new Thread(this);
			t.start();
		}

		public void run() {

			// This method is only run when the store is being finalised.  It allows
			// us to process all of the chromosomes for a data store in parallel
			// which is quicker given that the processing is constrained by CPU

			//			long calcStartTime = System.currentTimeMillis();

			/*
			 * Sorting the hit collection is *slow* but we need to do this for normal
			 * datasets.  If we've been passed sorted data then we can omit this step
			 * and save ourselves a big old chunk of time.
			 */
			if (needToSort) {
				hitCollection.sortCollection();
			}
			else {
				// We still need to trim the long vectors we've used to store data.  This
				// happens implicitly when sorting, but here we need to be explicit.
				hitCollection.trim();
			}

			if (removeDuplicates != DataSet.DUPLICATES_REMOVE_NO) {
				hitCollection.deduplicateCollection();
			}


			// We now need to work out the distance correction counts so we can 
			// work out distance correction factors.

			int [] distanceCounts = new int[distanceCorrections.length];

			// We need to do distance counts for cis chromosomes only, but we need to get total
			// counts for all chromosomes, so we need to work our way through the whole lot

			String [] chromosomeNames = hitCollection.getChromosomeNamesWithHits();

			// We keep local counts here so we only have to do one update of the
			// synchronised counters

			int totalReads = 0;
			int forwardReads = 0;
			int reverseReads = 0;
			int unknownReads = 0;

			long readLengths = 0;
	
			for (int c=0;c<chromosomeNames.length;c++) {

				long [] sourceReads = hitCollection.getSourcePositionsForChromosome(chromosomeNames[c]);
				long [] hitReads = null;
				
				boolean sameChromosome = chromosomeNames[c].equals(hitCollection.getSourceChromosomeName());

				if (sameChromosome) {
					hitReads = hitCollection.getHitPositionsForChromosome(chromosomeNames[c]);
				}

				for (int i=0;i<sourceReads.length;i++) {
					if (sameChromosome) {
						int distance = SequenceRead.fragmentLength(sourceReads[i],hitReads[i]);
						++distanceCounts[getIndexForLength(distance)];
					}

					minMaxLength.addValue(SequenceRead.length(sourceReads[i]));

					// Add this length to the total
					readLengths += SequenceRead.length(sourceReads[i]);

					// Increment the appropriate counts
					totalReads++;;
					if (SequenceRead.strand(sourceReads[i])==Location.FORWARD) {
						forwardReads++;
					}
					else if (SequenceRead.strand(sourceReads[i])==Location.REVERSE) {
						reverseReads++;
					}
					else {
						unknownReads++;
					}

				}

			}

			System.err.println("Found "+totalReads+" reads on "+hitCollection.getSourceChromosomeName());

			// Now update the synchronized counters
			totalReadCount.incrementBy(totalReads);
			forwardReadCount.incrementBy(forwardReads);
			reverseReadCount.incrementBy(reverseReads);
			unknownReadCount.incrementBy(unknownReads);

			totalReadLength.incrementBy(readLengths);

			// We now need to correct for the number of possible distance combinations
			// so we get an averaged value for each distance

			// Start by making up a probability matrix for randomly placed reads 
			// on the chromosome

			float [] randomReadDistribution = new float[distanceCounts.length];
			int totalCategoryCount = 0;

			for (int i=0;i<=distanceCounts.length;i++) {
				totalCategoryCount += i;
			}

			for (int i=0;i<distanceCounts.length;i++) {
				randomReadDistribution[i] = ((float)(distanceCounts.length-i))/totalCategoryCount;
			}

			// Now work out the average number of interactions we expect to observe 
			// for any distance pair.

			int totalObservations  = 0;
			for (int i=0;i<distanceCounts.length;i++) {
				totalObservations += distanceCounts[i];
			}



			//			System.err.println("Total counts = "+totalObservations+" totalCategories="+totalCategoryCount+" counts per category="+averageObservationsPerCategory);

			// Now we work out a specific correction factor for the actual data in this
			// dataset.  We need to compare the actual observations we get with the 
			// expected ones from the theoretical distribution.

			float lastDecentCorrection = -1;

			for (int i=0;i<distanceCounts.length;i++) {

				// Work out the proportion of reads which actually fell into this size range
				float observedProportion = ((float)distanceCounts[i])/totalObservations;

				// Work out the ratio to what we should have seen from a random distribution
				float distanceCorrection = observedProportion/randomReadDistribution[i];

				// If we don't have enough data to assign a sensible value then use the last
				// group where we did.
				if ((distanceCounts[i] < 10 ) && lastDecentCorrection > 0) {
					// We don't really have enough data to be able to correct this
					// distance, so just apply the last valid correction factor
					distanceCorrection = lastDecentCorrection;
				}
				else {
					lastDecentCorrection = distanceCorrection;
				}

				distanceCorrections[i] = distanceCorrection;

				//				if (chromosome.name().equals("1")  && i % 50 == 0) {	
				//					System.err.println("Correction for chr"+chromosome.name()+" distance "+(i*DISTANCE_GROUP_LENGTH)+" is "+distanceCorrection+" absolute is "+distanceCounts[i]);
				//				}
			}

			// Work out the cached values for total length,count and for/rev/unknown counts

			try {

				//					System.err.println("Started writing cache file for "+chromosome.name());

				//					long cacheWriteStart = System.currentTimeMillis();
				tempFile = File.createTempFile("seqmonk_data_hic", ".temp", SeqMonkPreferences.getInstance().tempDirectory());
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
				oos.writeObject(hitCollection);
				oos.close();
				//					long cacheWriteEnd = System.currentTimeMillis();

				//					System.err.println("Time for "+hitCollection.getSourceChromosomeName()+" calc="+((cacheWriteStart-calcStartTime)/1000d)+" cache="+((cacheWriteEnd-cacheWriteStart)/1000d));

				hitCollection = null;
				//					System.err.println("Cache file written for "+chromosome.name());

			}
			catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}

			chromosomesStillToFinalise.decrement();

		}

		public float getCorrectionForIndex (int index) {
			if (index > distanceCorrections.length-1) {
				index = distanceCorrections.length-1;
			}
			return distanceCorrections[index];
		}
	}

}
