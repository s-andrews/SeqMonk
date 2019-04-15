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
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;
import uk.ac.babraham.SeqMonk.Utilities.ThreadSafeIntCounter;
import uk.ac.babraham.SeqMonk.Utilities.ThreadSafeLongCounter;
import uk.ac.babraham.SeqMonk.Utilities.ThreadSafeMinMax;


/**
 * A DataSet represents a set of reads coming from a single source
 * (usually a file).  It is able to store and retrieve reads in a
 * very efficient manner.  If the user has requested that data be
 * cached the DataSet is also responsible for saving and loading
 * this data.
 */
public class DataSet extends DataStore implements Runnable {


	// These are a set of flags which say how we need to treat duplicates
	public static final int DUPLICATES_REMOVE_NO = 5715;
	public static final int DUPLICATES_REMOVE_START = 5716;
	public static final int DUPLICATES_REMOVE_END = 5717;
	public static final int DUPLICATES_REMOVE_START_END = 5718;


	// I've tried using a HashMap and a linked list instead of 
	// a hashtable and a vector but they proved to be slower and
	// use more memory.

	private Hashtable<Chromosome, ChromosomeDataStore> readData = new Hashtable<Chromosome, ChromosomeDataStore>();

	/** The original file name - can't be changed by the user */
	private String fileName;
	
	/** These are the import options used **/
	private String importOptions;

	/** A flag to say if we've optimised this dataset */
	private boolean isFinalised = false;

	/** A flag which is set as soon as any unsorted data is added to the data set */
	private boolean needsSorting = false;

	/** This count allows us to keep track of the progress of finalisation for the individual chromosomes */
	private ThreadSafeIntCounter chromosomesStillToFinalise;

	/** A flag to say if we should remove duplicates when finalising */
	private int removeDuplicates = DUPLICATES_REMOVE_NO;

	/** We cache the total read count to save having to reload
	 * every chromosome just to get the read count
	 */
	protected ThreadSafeLongCounter totalReadCount = new ThreadSafeLongCounter();

	/** We cache the forward read count to save having to reload
	 * every chromosome just to get the read count
	 */
	protected ThreadSafeLongCounter forwardReadCount = new ThreadSafeLongCounter();

	/**
	 * We cache the min and max read lengths so we can quickly access these
	 * for some anlayses without having to go through the whole dataset to
	 * get them
	 */
	protected ThreadSafeMinMax minMaxLength = new ThreadSafeMinMax();


	/** We cache the reverse read count to save having to reload
	 * every chromosome just to get the read count
	 */
	protected ThreadSafeLongCounter reverseReadCount = new ThreadSafeLongCounter();


	/** We cache the unknown read count to save having to reload
	 * every chromosome just to get the read count
	 */
	protected ThreadSafeIntCounter unknownReadCount = new ThreadSafeIntCounter();


	/** The total read length. */
	protected ThreadSafeLongCounter totalReadLength = new ThreadSafeLongCounter();

	// These are cached values used when we're saving excess data to temp files
	/** The last cached chromosome. */
	private Chromosome lastCachedChromosome = null;

	/** The reads last loaded from the cache */
	private ReadsWithCounts lastCachedReads = null;


	/** 
	 * This variable controls how many thread we allow to finalise at the same time.
	 * 
	 * We'll make as many threads as we have CPUs up to a limit of 6, above which we're 
	 * likely to hurt the throughput as we just thrash the underlying disks.
	 * 
	 * */
	private static final int MAX_CONCURRENT_FINALISE = Math.min(Runtime.getRuntime().availableProcessors(), 6);


	/** The last index at which a read was found */
	private int lastIndex = 0;

	private long lastProbeLocation = 0;

	/**
	 * Instantiates a new data set.
	 * 
	 * @param name The initial value for the user changeable name
	 * @param fileName The name of the data source  - which can't be changed by the user
	 */
	public DataSet (String name, String fileName, int removeDuplicates, String importOptions) {
		super(name);
		this.fileName = fileName;
		this.removeDuplicates = removeDuplicates;
		this.importOptions = importOptions;

		// We need to set a shutdown hook to delete any cache files we hold
		Runtime.getRuntime().addShutdownHook(new Thread(this));
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#setName(java.lang.String)
	 */
	public void setName (String name) {
		String oldName = this.name();
		super.setName(name);
		if (collection() != null) {
			collection().dataSetRenamed(this,oldName);
		}
	}

	protected int removeDuplicates () {
		return removeDuplicates;
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
	}


	public void addData (Chromosome chr, long read) throws SeqMonkException {
		addData(chr, read, false);
	}

	public void addData (Chromosome chr, long read, int count) throws SeqMonkException {
		addData(chr, read, count, false);
	}



	/**
	 * Adds more data to this set.
	 * 
	 * @param chr The chromosome to which data will be added
	 * @param read The data to add
	 * @throws SeqMonkException if this DataSet has been finalised.
	 */
	public void addData (Chromosome chr, long read, int count, boolean skipSorting) throws SeqMonkException {

		if (isFinalised) {
			throw new SeqMonkException("This data set is finalised.  No more data can be added");
		}

		if (count == 0) return;

		if (readData.containsKey(chr)) {
			readData.get(chr).addRead(read,count);
		}
		else {
			ChromosomeDataStore cds = new ChromosomeDataStore();
			cds.addRead(read,count);
			readData.put(chr,cds);
		}

		if (!skipSorting) needsSorting = true;
	}

	public void addData (Chromosome chr, long read, boolean skipSorting) throws SeqMonkException {
		addData(chr,read,1,skipSorting);
	}

	/**
	 * Gets the original data source name for this DataSet - usually
	 * the name of the file from which it was parsed.
	 * 
	 * @return the file name
	 */
	public String fileName () {
		return fileName;
	}
	
	/**
	 * Returns a string describing the options used during data import
	 * 
	 * @return
	 */
	public String importOptions () {
		return importOptions;
	}
	

	/**
	 * A quick check to see if any data overlaps with a probe
	 * 
	 * @param p the probe to check
	 * @return true, if at leas one read overlaps with this probe
	 */
	public boolean containsReadForProbe(Probe p) {

		if (! isFinalised) finalise();

		long [] allReads = getReadsForChromosome(p.chromosome()).reads;

		if (allReads.length == 0) return false;

		int startPos;

		// Use the cached position if we're on the same chromosome
		// and this probe position is higher than the last one we
		// fetched.

		if (lastCachedChromosome != null && p.chromosome() == lastCachedChromosome && (lastProbeLocation == 0 || SequenceRead.compare(p.packedPosition(), lastProbeLocation)>=0)) {
			startPos = lastIndex;
			//			System.out.println("Using cached start pos "+lastIndex);
		}

		// If we're on the same chromosome then we'll simply backtrack until we're far
		// enough back that we can't have missed even the longest read in the set.
		else if (lastCachedChromosome != null && p.chromosome() == lastCachedChromosome) {

			//			System.out.println("Last chr="+lastCachedChromosome+" this chr="+p.chromosome()+" lastProbeLocation="+lastProbeLocation+" diff="+SequenceRead.compare(p.packedPosition(), lastProbeLocation));

			int longestRead = getMaxReadLength();

			for (;lastIndex >0;lastIndex--) {
				if (p.start()-SequenceRead.start(allReads[lastIndex]) > longestRead) {
					break;
				}
			}

			//			System.out.println("Starting from index "+lastIndex+" which starts at "+SequenceRead.start(allReads[lastIndex])+" for "+p.start()+" when max length is "+longestRead);

			startPos = lastIndex;

		}


		// If we can't cache then start from the beginning.  It's not worth
		// the hassle of trying to guess starting positions
		else {
			startPos = 0;
			lastIndex = 0;
			//			System.out.println("Starting from the beginning");
			//			System.out.println("Last chr="+lastCachedChromosome+" this chr="+p.chromosome()+" lastProbeLocation="+lastProbeLocation+" diff="+SequenceRead.compare(p.packedPosition(), lastProbeLocation));
		}

		lastProbeLocation = p.packedPosition();

		// We now go forward to see what we can find

		for (int i=startPos;i<allReads.length;i++) {
			// Reads come in order, so we can stop when we've seen enough.
			if (SequenceRead.start(allReads[i]) > p.end()) {
				return false;
			}

			if (SequenceRead.overlaps(allReads[i], p.packedPosition())) {
				// They overlap.
				lastIndex = i;
				return true;
			}
		}

		return false;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadsForProbe(uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe)
	 */
	public ReadsWithCounts getReadsWithCountsForProbe(Probe p) {

		if (! isFinalised) finalise();

		ReadsWithCounts allReads;

		loadCacheForChromosome(p.chromosome());

		// We take a copy of the arrays now so that we don't get a problem if something
		// else updates them whilst we're still working otherwise we get index errors.
		allReads = lastCachedReads;

		if (allReads.reads.length == 0) return new ReadsWithCounts(new long[0]);

		LongVector reads = new LongVector();		
		IntVector counts = new IntVector();

		int startPos;

		// Use the cached position if we're on the same chromosome
		// and this probe position is higher than the last one we
		// fetched.

		if (lastCachedChromosome != null && p.chromosome() == lastCachedChromosome && (lastProbeLocation == 0 || SequenceRead.compare(p.packedPosition(), lastProbeLocation)>=0)) {
			startPos = lastIndex;
			//			System.out.println("Using cached start pos "+lastIndex);
		}


		// If we're on the same chromosome then we'll simply backtrack until we're far
		// enough back that we can't have missed even the longest read in the set.
		else if (lastCachedChromosome != null && p.chromosome() == lastCachedChromosome) {

			//			System.out.println("Last chr="+lastCachedChromosome+" this chr="+p.chromosome()+" lastProbeLocation="+lastProbeLocation+" diff="+SequenceRead.compare(p.packedPosition(), lastProbeLocation));

			int longestRead = getMaxReadLength();

			for (;lastIndex >0;lastIndex--) {
				if (p.start()-SequenceRead.start(allReads.reads[lastIndex]) > longestRead) {
					break;
				}
			}

			//			System.out.println("Starting from index "+lastIndex+" which starts at "+SequenceRead.start(allReads[lastIndex])+" for "+p.start()+" when max length is "+longestRead);

			startPos = lastIndex;

		}

		// If we're on a different chromosome then start from the very beginning
		else {
			startPos = 0;
			lastIndex = 0;
			//			System.out.println("Starting from the beginning");
		}

		if (startPos <0) startPos = 0; // Can't see how this would happen, but we had a report showing this.

		lastProbeLocation = p.packedPosition();

		// We now go forward to see what we can find

		boolean cacheSet = false;

		for (int i=startPos;i<allReads.reads.length;i++) {
			// Reads come in order, so we can stop when we've seen enough.
			if (SequenceRead.start(allReads.reads[i]) > p.end()) {
				break;
			}

			if (SequenceRead.overlaps(allReads.reads[i], p.packedPosition())) {
				// They overlap.

				// If this is the first hit we've seen for this probe
				// then update the cache
				if (!cacheSet) {
					lastIndex = i;
					cacheSet = true;
				}
				reads.add(allReads.reads[i]);
				counts.add(allReads.counts[i]);
			}
		}

		return new ReadsWithCounts(reads.toArray(), counts.toArray());
	}
	
	public long [] getReadsForProbe (Probe p) {
		return getReadsWithCountsForProbe(p).expandReads();
	}


	private synchronized void loadCacheForChromosome (Chromosome c) {

		// Check if we need to reset which chromosome was loaded last.

		boolean needToUpdate = lastCachedChromosome == null || lastCachedChromosome != c;

		if (needToUpdate) {
			//			System.err.println("Cache miss for "+this.name()+" requested "+c+" but last cached was "+lastCachedChromosome);
			lastCachedChromosome = c;
			lastProbeLocation = 0;
			lastIndex = 0;

			if (SeqMonkApplication.getInstance() != null) {
				SeqMonkApplication.getInstance().cacheUsed();
			}

			// Check to see if we even have any data for this chromosome
			if (!readData.containsKey(c)) {
				lastCachedReads = new ReadsWithCounts(new long[0]);
			}

			else {
				// We need to reload the data from the temp files.  We have two files - one for the reads
				// and one for the counts.
				try {
					ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(readData.get(c).readsWithCountsTempFile)));
					lastCachedReads = (ReadsWithCounts)ois.readObject();
					ois.close();
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
		}		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadsForChromsome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public synchronized ReadsWithCounts getReadsForChromosome(Chromosome c) {

		if (! isFinalised) finalise();

		loadCacheForChromosome(c);

		if (readData.containsKey(c)) {


			// We used to have a check for whether we were caching, but that's gone
			// now because they don't have the option to not cache any more.

			return lastCachedReads;


		}
		else {
			lastCachedReads = new ReadsWithCounts(new long[0]);
			return lastCachedReads;
		}
	}




	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run () {
		// We need to delete any cache files we're still holding

		Enumeration<Chromosome> e = readData.keys();
		while (e.hasMoreElements()) {
			Chromosome c = e.nextElement();

			File f = readData.get(c).readsWithCountsTempFile;
			if (f != null) {
				if (!f.delete()) System.err.println("Failed to delete cache file "+f.getAbsolutePath());
			}


		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadCountForChromosome(uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome)
	 */
	public int getReadCountForChromosome(Chromosome c) {

		if (! isFinalised) finalise();

		if (readData.containsKey(c)) {
			return getReadsForChromosome(c).totalCount();
		}
		else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getTotalReadCount()
	 */
	public long getTotalReadCount() {

		if (! isFinalised) finalise();

		return totalReadCount.value();
	}

	public int getMaxReadLength() {
		return minMaxLength.max();
	}

	public int getMinReadLength() {
		return minMaxLength.min();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getReadCountForStrand(int strand)
	 */
	public long getReadCountForStrand (int strand) {

		if (! isFinalised) finalise();

		if (strand == Location.FORWARD) {
			return forwardReadCount.value();
		}
		else if (strand == Location.REVERSE) {
			return reverseReadCount.value();
		}
		else {
			return unknownReadCount.value();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataStore#getTotalReadLength()
	 */
	public long getTotalReadLength () {

		if (! isFinalised) finalise();

		return totalReadLength.value();
	}

	/**
	 * The Class ChromosomeDataStore.
	 */
	private class ChromosomeDataStore implements Runnable {

		/** A vector to hold the read positions whilst we load them. */
		private LongVector readVector = new LongVector();

		/** A vector to hold the read counts whilst we load them. */
		private IntVector countVector = new IntVector();

		/** The temp file in which this data will be saved in the cache folder */
		public File readsWithCountsTempFile = null;

		/** A cache of the last read position added so we know if we can just
		 * increment the count instead of adding a new entry
		 */
		private long lastReadAdded = Long.MIN_VALUE;

		public void addRead (long read, int count) {
						
			if (read == lastReadAdded) {
				countVector.increaseLastBy(count);
			}
			else {
				readVector.add(read);
				countVector.add(count);
				lastReadAdded = read;
			}
		}


		public void finalise () {
			Thread t = new Thread(this);
			t.start();
		}

		public void run() {
			// This method is only run when the store is being finalised.  It allows
			// us to process all of the chromosomes for a data store in parallel
			// which is quicker given that the processing is constrained by CPU

			
			// We take a local copy of these vectors so that nothing else can touch 
			// them
			LongVector originalReads = readVector;
			IntVector originalCounts = countVector;

			long [] reads = originalReads.toArray();
			int [] counts = originalCounts.toArray();

			if (reads.length != counts.length) {
				throw new IllegalStateException("Reads and counts vectors weren't the same length");
			}

			if (needsSorting) {
				//				System.err.println("Sorting unsorted reads");

				SequenceRead.sort(reads,counts);
			}
			originalReads.clear();
			originalCounts.clear();
			
			
			// Now that we have sorted data we should find out if we can compress this
			// any further by collapsing identical reads which were non-adjacent in the
			// original data.  This will only happen during an initial import but it 
			// will save us some memory.

			boolean foundDuplicates = false;

			for (int i=1;i<reads.length;i++) {
				if (reads[i] == reads[i-1]) {
					foundDuplicates = true;
					break;
				}
			}
			
			if (foundDuplicates) {

//				System.err.println("Compressing duplicate reads");

				// Go back through the data reconstructing the original reads and original counts

				long lastRead = Long.MIN_VALUE;

				for (int r=0;r<reads.length;r++) {
					if (reads[r] == lastRead) {
						originalCounts.increaseLastBy(counts[r]);
					}
					else {
						originalReads.add(reads[r]);
						originalCounts.add(counts[r]);
						lastRead = reads[r];
					}
				}

				reads = originalReads.toArray();
				originalReads.clear();

				counts = originalCounts.toArray();
				originalCounts.clear();

			}


			readVector = null;
			countVector = null;

			
			// Now we can perform deduplication if we need to.
			

			if (removeDuplicates == DUPLICATES_REMOVE_NO) {}

			else if (removeDuplicates == DUPLICATES_REMOVE_START_END) {
				
				// Deduplicating on start+end is easy since we just set all of the counts to be one
				counts = new int[reads.length];
				for (int i=0;i<counts.length;i++) counts[i] = 1;
			}

			else  {
				// We're going to have to deduplicate based on position.  For this we're going 
				// to need to set up 3 boolean arrays so we know if we've seen this position before.

				// We're also going to need to know the position of the last position in any read.

				boolean [] forwardPositions = null;
				boolean [] reversePositions = null;
				boolean [] unknownPositions = null;

				int maxPosition = 0;
				for (int r=0;r<reads.length;r++) {
					if (SequenceRead.end(reads[r]) > maxPosition) maxPosition = SequenceRead.end(reads[r]);
				}
				maxPosition++;

				// Now we can go through and do the deduplication
				for (int i=0;i<reads.length;i++) {

					// Make up an array if we need to
					if (SequenceRead.strand(reads[i]) == Location.FORWARD) {
						if (forwardPositions == null) forwardPositions = new boolean[maxPosition];
						int keyPosition;
						if (removeDuplicates == DUPLICATES_REMOVE_START) {
							keyPosition = SequenceRead.start(reads[i]);
						}
						else if (removeDuplicates == DUPLICATES_REMOVE_END) {
							keyPosition = SequenceRead.end(reads[i]);
						}
						else {
							throw new IllegalStateException("Unknown duplicate removal value "+removeDuplicates);
						}
						if (forwardPositions[keyPosition]) continue; // We've already used this
						else {
							originalReads.add(reads[i]);
							originalCounts.add(1);
							forwardPositions[keyPosition] = true;
						}
					}
					if (SequenceRead.strand(reads[i]) == Location.REVERSE) {
						if (reversePositions == null) reversePositions = new boolean[maxPosition];
						int keyPosition;
						if (removeDuplicates == DUPLICATES_REMOVE_START) {
							keyPosition = SequenceRead.end(reads[i]);
						}
						else if (removeDuplicates == DUPLICATES_REMOVE_END) {
							keyPosition = SequenceRead.start(reads[i]);
						}
						else {
							throw new IllegalStateException("Unknown duplicate removal value "+removeDuplicates);
						}
						if (reversePositions[keyPosition]) continue; // We've already used this
						else {
							originalReads.add(reads[i]);
							originalCounts.add(1);
							reversePositions[keyPosition] = true;	
						}

					}
					if (SequenceRead.strand(reads[i]) == Location.UNKNOWN) {
						if (unknownPositions == null) unknownPositions = new boolean[maxPosition];
						int keyPosition;
						if (removeDuplicates == DUPLICATES_REMOVE_START) {
							keyPosition = SequenceRead.start(reads[i]);
						}
						else if (removeDuplicates == DUPLICATES_REMOVE_END) {
							keyPosition = SequenceRead.end(reads[i]);
						}
						else {
							throw new IllegalStateException("Unknown duplicate removal value "+removeDuplicates);
						}
						if (unknownPositions[keyPosition]) continue; // We've already used this
						else {
							originalReads.add(reads[i]);
							originalCounts.add(1);
							unknownPositions[keyPosition] = true;
						}
					}
				}

				reads = originalReads.toArray();
				originalReads.clear();

				counts = originalCounts.toArray();
				originalCounts.clear();

			} // End of deduplication section.



			// Work out the cached values for total length,count and for/rev/unknown counts

			// We keep local counts here so we only have to do one update of the
			// synchronised counters

			int totalReads = 0;
			int forwardReads = 0;
			int reverseReads = 0;
			int unknownReads = 0;

			long readLengths = 0;

			int localMinLength = 0;
			int localMaxLength = 0;

			for (int i=0;i<reads.length;i++) {

				// This is really slow when lots of datasets are doing this
				// at the same time.  Instead we can keep a local cache of
				// min max values and just send the extreme values to the 
				// main set at the end.
				//
				// minMaxLength.addValue(SequenceRead.length(reads[i]));

				if (i==0 || SequenceRead.length(reads[i]) < localMinLength) localMinLength = SequenceRead.length(reads[i]);
				if (i==0 || SequenceRead.length(reads[i]) > localMaxLength) localMaxLength = SequenceRead.length(reads[i]);

				// Add this length to the total
				readLengths += SequenceRead.length(reads[i])*counts[i];

				// Increment the appropriate counts
				totalReads += counts[i];;
				if (SequenceRead.strand(reads[i])==Location.FORWARD) {
					forwardReads += counts[i];
				}
				else if (SequenceRead.strand(reads[i])==Location.REVERSE) {
					reverseReads += counts[i];
				}
				else {
					unknownReads += counts[i];
				}
			}

			// Now update the min/max synchronized lengths
			minMaxLength.addValue(localMinLength);
			minMaxLength.addValue(localMaxLength);

			// Now update the syncrhonized counters
			totalReadCount.incrementBy(totalReads);
			forwardReadCount.incrementBy(forwardReads);
			reverseReadCount.incrementBy(reverseReads);
			unknownReadCount.incrementBy(unknownReads);

			totalReadLength.incrementBy(readLengths);

			try {
				readsWithCountsTempFile = File.createTempFile("seqmonk_read_set", ".temp", SeqMonkPreferences.getInstance().tempDirectory());
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(readsWithCountsTempFile)));
				oos.writeObject(new ReadsWithCounts(reads,counts));
				oos.close();
			}
			catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}

			chromosomesStillToFinalise.decrement();

		}
	}

}
