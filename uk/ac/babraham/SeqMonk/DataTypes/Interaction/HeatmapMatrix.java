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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.HotColdColourGradient;

public class HeatmapMatrix implements Runnable, Cancellable {

	// This limits how many interactions we will ever observe.  There is an
	// absolute limit at Integer.MAX_VALUE but the practical limit seems to
	// be lower than that.
	//
	// If we're being clever we might be able to tweak the interaction threshold
	// as we're going along so that we stay under this limit, but we haven't
	// done that yet.
	private static final int MAX_INTERACTIONS = Integer.MAX_VALUE;

	// These constants are used to show which colour scheme is being used.
	public static final int COLOUR_BY_OBS_EXP = 2232;
	public static final int COLOUR_BY_P_VALUE = 2233;
	public static final int COLOUR_BY_INTERACTIONS = 2234;
	public static final int COLOUR_BY_QUANTITATION = 2235;
	
	private int currentColourSetting = COLOUR_BY_OBS_EXP;
	
	// This holds the type of gradient we want to use
	private static ColourGradient colourGradient = new HotColdColourGradient();
	
	private HashSet<Probe> probeFilterList = null;
	
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;
	private ProbeWithIndex [] probes;
	private ProbeList [] probeLists;
	private HiCDataStore dataSet;

	// Variables used to pre-filter the interactions to remove those
	// we're never going to be interested in
	private int initialMinDistance;
	private int initialMaxDistance;
	private double initialMinStrength;
	private double initialMaxSignificance;
	private int initialMinAbsolute;

	// Current filter options
	private int currentMinDistance;
	private int currentMaxDistance;
	private double currentMinStrength;
	private double currentMaxSignificance;
	private double currentMinDifference;
	private int currentMinAbsolute;

	private InteractionProbePair [] interactions;
	private InteractionProbePair [] filteredInteractions;

	// If we're correcting for physical linkage then there's
	// a flag to tell us to do this
	private boolean correctLinkage;
	private double maxValue;

	// Used when the results in the matrix are clustered
	private ClusterPair cluster;
	private float currentClusterRValue = 0f;	

	private Vector<HeatmapMatrixListener> optionListeners = new Vector<HeatmapMatrixListener>();


	public HeatmapMatrix (HiCDataStore dataSet , ProbeList [] probeLists, Genome genome, int minDistance, int maxDistance, double minStrength, double maxSignificance, int minAbsolute, boolean correctLinkage) {

		this.probeLists = probeLists;
		this.correctLinkage = correctLinkage;

//		if (this.correctLinkage) System.err.println("Correcting linkage");

		Vector<ProbeWithIndex> indexedProbes = new Vector<HeatmapMatrix.ProbeWithIndex>();

		int count = 0;
		for (int l=0;l<probeLists.length;l++) {
			Probe [] theseProbes = probeLists[l].getAllProbes();
			for (int p=0;p<theseProbes.length;p++) {
				indexedProbes.add(new ProbeWithIndex(theseProbes[p], count));
				count++;
			}
		}

		this.probes = indexedProbes.toArray(new ProbeWithIndex[0]);

		Arrays.sort(this.probes);		

		this.dataSet = dataSet;
		initialMinDistance = minDistance;
		initialMaxDistance = maxDistance;
		initialMinStrength = minStrength;
		initialMaxSignificance = maxSignificance;
		initialMinAbsolute = minAbsolute;

		currentMinDistance = initialMinDistance;
		currentMaxDistance = initialMaxDistance;
		currentMinStrength = initialMinStrength;
		currentMaxSignificance = initialMaxSignificance;
		currentMinAbsolute = initialMinAbsolute;
	}

	public ProbeList createProbeListFromCurrentInteractions () {
		ProbeList commonList = findCommonProbeListParent();

		// TODO: Get a better name
		ProbeList allProbesList = new ProbeList(commonList, "Filtered HiC hits", "HiC hits"+commonList, new String[0]);

		HashSet<Probe>allClusterProbes = new HashSet<Probe>();

		// Work our way through the interactions to get the subset which are active
		InteractionProbePair [] exportedInteractions = filteredInteractions();
		
		for (int i=0;i<exportedInteractions.length;i++){
			if (! allClusterProbes.contains(exportedInteractions[i].probe1())) {
				allProbesList.addProbe(exportedInteractions[i].probe1(),null);
				allClusterProbes.add(exportedInteractions[i].probe1());
			}
			if (! allClusterProbes.contains(exportedInteractions[i].probe2())) {
				allProbesList.addProbe(exportedInteractions[i].probe2(),null);
				allClusterProbes.add(exportedInteractions[i].probe2());
			}
		}
		
		return allProbesList;

	}
	
	public ProbeList createProbeListsFromClusters (int minClusterSize, int startIndex, int endIndex) {
		if (cluster == null) return null;

		ClusterPair [] connectedClusters = cluster.getConnectedClusters(currentClusterRValue);

		ProbeList commonList = findCommonProbeListParent();

		ProbeList allClusterList = new ProbeList(commonList, "HiC Clusters", "HiC Clusters with R > "+commonList, new String[0]);

		HashSet<Probe>allClusterProbes = new HashSet<Probe>();

		// Now we need to work our way through the connected clusters
		// to make the appropriate sub-lists

		// Make up the same initial list of probes as before
		Vector<Probe> originallyOrderedProbes = new Vector<Probe>();

		for (int l=0;l<probeLists.length;l++) {
			Probe [] theseProbes = probeLists[l].getAllProbes();
			for (int p=0;p<theseProbes.length;p++) {
				originallyOrderedProbes.add(theseProbes[p]);
			}
		}

		int currentPosition = 0;
		for (int subListIndex = 0;subListIndex < connectedClusters.length;subListIndex++) {

			Integer [] indices = connectedClusters[subListIndex].getAllIndices();
			currentPosition += indices.length;

			if (currentPosition-indices.length < startIndex) continue;
			if (currentPosition > endIndex) break;

			// We can immediately discard any lists which start off smaller than our limit.
			// We may get rid of the list later if there are duplicates in it.
			if (indices.length < minClusterSize) continue;

			Probe [] theseProbes = new Probe [indices.length];

			for (int i=0;i<theseProbes.length;i++) {
				theseProbes[i] = originallyOrderedProbes.elementAt(indices[i]);
			}

			Arrays.sort(theseProbes);

			// Now find the non-redundant count
			int nonRedCount = 1;

			for (int i=1;i<theseProbes.length;i++) {
				if (theseProbes[i] != theseProbes[i-1]) {
					nonRedCount++;
				}
			}

			if (nonRedCount < minClusterSize) continue; // There aren't enough different probes to keep this set.

			ProbeList thisList = new ProbeList(allClusterList, "Cluster "+(subListIndex+1), "HiC cluster list number "+(subListIndex+1), new String [] {"R-value"});

			float rValue = connectedClusters[subListIndex].rValue();

			thisList.addProbe(theseProbes[0], new float[]{rValue});
			if (! allClusterProbes.contains(theseProbes[0])) {
				allClusterList.addProbe(theseProbes[0],null);
				allClusterProbes.add(theseProbes[0]);
			}

			for (int i=1;i<theseProbes.length;i++) {
				if (theseProbes[i] == theseProbes[i-1]) continue;
				thisList.addProbe(theseProbes[i], new float[]{rValue});
				if (allClusterProbes.contains(theseProbes[i])) {
					continue;
				}
				allClusterList.addProbe(theseProbes[i],null);
				allClusterProbes.add(theseProbes[i]);
			}
		}

		return allClusterList;

	}
	
	public void setProbeFilterList (ProbeList list) {
		
		filteredInteractions = null;
		
		if (list == null) probeFilterList = null;
		else {
			probeFilterList = new HashSet<Probe>();
			Probe [] probes = list.getAllProbes();
			for (int p=0;p<probes.length;p++) {
				probeFilterList.add(probes[p]);
			}
		}
		
		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newProbeFilterList(list);
		}
	
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ProbeList findCommonProbeListParent () {
		if (probeLists.length == 1) {
			return probeLists[0];
		}
		else {
			HashSet [] parents = new HashSet [probeLists.length-1];
			for (int i=1;i<probeLists.length;i++) {
				parents[i-1] = new HashSet<ProbeList>();
				ProbeList currentList = probeLists[i];
				parents[i-1].add(currentList);
				while (! (currentList instanceof ProbeSet)) {
					parents[i-1].add(currentList.parent());
					currentList = currentList.parent();
				}
			}

			// Now we go through the first list to find the common parent
			ProbeList firstList = probeLists[0];
			boolean notFound = false;
			while (true) {
				notFound = false;
				for (int i=0;i<parents.length;i++) {
					if (! parents[i].contains(firstList)) {
						notFound = true;
					}
				}
				if (notFound) {
					firstList = firstList.parent();
				}
				else {
					return firstList;
				}
			}
		}

	}

	public HiCDataStore dataStore () {
		return dataSet;
	}

	public void setMinDistance (int minDistance) {
		if (minDistance < initialMinDistance) minDistance = initialMinDistance;

		currentMinDistance = minDistance;

		filteredInteractions = null;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newMinDistanceValue(minDistance);
		}
	}

	public void setMaxDistance (int maxDistance) {
		if (initialMaxDistance != 0 && maxDistance > initialMinDistance) maxDistance = initialMaxDistance;

		currentMaxDistance = maxDistance;

		filteredInteractions = null;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newMaxDistanceValue(maxDistance);
		}
	}

	public void setMinStrength (double minStrength) {
		if (minStrength < initialMinStrength) minStrength = initialMinStrength;

		currentMinStrength = minStrength;

		filteredInteractions = null;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newMinStrengthValue(minStrength);
		}

	}

	public void setMinDifference (double difference) {
		this.currentMinDifference = difference;

		filteredInteractions = null;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newMinDifferenceValue(difference);
		}

	}

	public void setMinAbsolute (int absolute) {
		this.currentMinAbsolute = absolute;

		filteredInteractions = null;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newMinAbsoluteValue(absolute);
		}
	}
	
	public void setColour (int colour) {
		if (colour == COLOUR_BY_INTERACTIONS  || colour == COLOUR_BY_OBS_EXP || colour == COLOUR_BY_P_VALUE || colour == COLOUR_BY_QUANTITATION) {
			currentColourSetting = colour;
		}
		
		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newColourSetting(colour);
		}

	}
	
	public int currentColourSetting () {
		return currentColourSetting;
	}
	
	public void setColourGradient (ColourGradient gradient) {
		HeatmapMatrix.colourGradient = gradient;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newColourGradient(gradient);
		}

	}
	
	public ColourGradient colourGradient () {
		return colourGradient;
	}

	public int initialMinAbsolute () {
		return initialMinAbsolute;
	}

	public int currentMinAbsolute () {
		return currentMinAbsolute;
	}


	public double minDifference () {
		return currentMinDifference;
	}

	public void setMaxSignificance (double maxSignficance) {		
		filteredInteractions = null;

		if (maxSignficance > initialMaxSignificance) maxSignficance = initialMaxSignificance;
		currentMaxSignificance = maxSignficance;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newMaxSignificanceValue(maxSignficance);
		}

	}


	public int initialMinDistance () {
		return initialMinDistance;
	}

	public int initialMaxDistance () {
		return initialMaxDistance;
	}

	public double initialMaxSignificance () {
		return initialMaxSignificance;
	}

	public double initialMinStrength () {
		return initialMinStrength;
	}

	public int currentMinDistance () {
		return currentMinDistance;
	}

	public int currentMaxDistance () {
		return currentMaxDistance;
	}

	public double currentMaxSignficance () {
		return currentMaxSignificance;
	}

	public double currentMinStrength () {
		return currentMinStrength;
	}

	public void setCluster (ClusterPair cluster) {
		this.cluster = cluster;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newCluster(cluster);
		}
	}

	public void setClusterRValue (float rValue) {
		this.currentClusterRValue = rValue;

		Enumeration<HeatmapMatrixListener> en = optionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().newClusterRValue(currentClusterRValue);
		}
	}

	public ClusterPair cluster () {
		return cluster;
	}

	public float currentClusterRValue () {
		return currentClusterRValue;
	}

	private boolean passesCurrentFilters (InteractionProbePair p) {

		// If there's a probe filter list check against that
		if (probeFilterList != null) {
			if (! (probeFilterList.contains(p.probe1()) || probeFilterList.contains(p.probe2())))
				return false;
		}
		
		// Check against the current strength filter
		if (p.strength() < currentMinStrength) return false;

		// Work out the distance between them
		if (p.sameChromosome()) {
			if (p.distance()<currentMinDistance) return false;

			if (currentMaxDistance != 0) {
				if (p.distance()>currentMaxDistance) return false;
			}
		}
		else {
			// If there's any filter for max distance then remove all trans
			if (currentMaxDistance != 0) return false;
		}

		// Check the significance
		if (currentMaxSignificance < 1 && p.signficance() > currentMaxSignificance) return false;

		// Check the absolute count
		if (p.absolute() < currentMinAbsolute) return false;

		return true;
	}

	public void addProgressListener (ProgressListener l) {
		if (l != null && ! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeProgressListener (ProgressListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	public void addOptionListener (HeatmapMatrixListener l) {
		if (l != null && ! optionListeners.contains(l)) {
			optionListeners.add(l);
		}
	}

	public void removeOptionListener (HeatmapMatrixListener l) {
		if (l != null && optionListeners.contains(l)) {
			optionListeners.remove(l);
		}
	}

	public int probeCount () {
		return probes.length;
	}

	public double maxValue () {
		if (maxValue < 100) {
			return maxValue;
		}
		else {
			return 100;
		}
	}

	public void startCalculating () {
		Thread t = new Thread(this);
		t.start();
	}

	public void cancel () {
		cancel = true;
	}

	public InteractionProbePair [] interactions () {
		return interactions;
	}

	public InteractionProbePair [] filteredInteractions () {
		if (filteredInteractions == null) {
			Vector<InteractionProbePair> tempInteractions = new Vector<InteractionProbePair>();

			for (int i=0;i<interactions.length;i++) {

				if (passesCurrentFilters(interactions[i])) {
					tempInteractions.add(interactions[i]);
				}
			}

			filteredInteractions = tempInteractions.toArray(new InteractionProbePair[0]);
		}

		return filteredInteractions;
	}

	public void run () {		

		// First calculate chromosome offsets so we can access the
		// relevant probes more quickly when we come to adding other
		// end data.

		Hashtable<String, Integer> offsets = new Hashtable<String, Integer>();

		offsets.put(probes[0].probe.chromosome().name(), 0);

		for (int i=1;i<probes.length;i++) {
			if (probes[i].probe.chromosome() != probes[i-1].probe.chromosome()) {
				offsets.put(probes[i].probe.chromosome().name(),i);
			}
		}


		// We also need an initial list of total counts for all of our probes
		// so we can do the O/E calculations.  We can incorporate into this 
		// the ability to correct for linkage.

		int [] totalCisCounts = new int[probes.length];
		int [] totalTransCounts = new int[probes.length];
		getTotalCounts(totalCisCounts,totalTransCounts);
	
		if (cancel) {
			Enumeration<ProgressListener> en2 = listeners.elements();
			while (en2.hasMoreElements()) {
				en2.nextElement().progressCancelled();
			}
			return;
		}

		// We'll make up an ever expanding list of interactions which we
		// care about
		Vector<InteractionProbePair>filteredInteractions = new Vector<InteractionProbePair>();

		
		// We'll need to correct for the number of tests we perform, but we're also able
		// to skip a load of tests based on the initial filters we supplied (non-statistical ones)
		// so we're going to keep a record of how many valid tests we actually need to correct for.
		//
		// The worse case scenario is that we do every possible comparison (but only one way round).
		
		// After some testing this proved to be a bad idea.  We skipped so many tests where the 
		// interaction wasn't observed that we ended up hugely under-correcting our final p-values
		// and making a load of false positive predictions.  We can do this more correctly later, 
		// but for now we're either going to correct for every possible test, or for just every
		// possible cis test if we're excluding trans hits.
		
		long numberOfTestsPerformed = 0;
		
		if (initialMaxDistance > 0) {
			// We're just counting cis interactions
			int currentChrCount = 1;
			Chromosome currentChr = probes[0].probe.chromosome();
			for (int p=1;p<probes.length;p++) {
				if (probes[p].probe.chromosome() == currentChr) {
					++currentChrCount;
				}
				else {
					numberOfTestsPerformed += (currentChrCount*((long)currentChrCount-1))/2;
					currentChrCount = 1;
					currentChr = probes[p].probe.chromosome();
				}
			}
			numberOfTestsPerformed += (currentChrCount*((long)currentChrCount-1))/2;			
		}
		else {
			numberOfTestsPerformed = (probes.length*((long)probes.length-1))/2;
		}
		
		
		// Now we go through the probes getting the other end information and
		// populating our matrix.		

		for (int p=0;p<probes.length;p++) {

			if (p%100 == 0) {
				Enumeration<ProgressListener> en = listeners.elements();
				while (en.hasMoreElements()) {
					en.nextElement().progressUpdated("Processed "+p+" probes", p, probes.length);
				}

				if (cancel) {
					Enumeration<ProgressListener> en2 = listeners.elements();
					while (en2.hasMoreElements()) {
						en2.nextElement().progressCancelled();
					}
					return;
				}

			}
			
//			System.err.println("Getting interactions for "+probes[p].probe);

			// We temporarily store the interactions with this probe which means we
			// can make a decision about which ones we're keeping as we go along which
			// drastically reduces the amount we need to store.

			// This is going to be the data structure which holds the information
			// on the pairs of probes which have any interactions.  The key is the
			// index position of the pair (x+(y*no of probes), and the value is the
			// number of times this interaction was seen

			Hashtable<Long, Integer>interactionCounts = new Hashtable<Long, Integer>();

			HiCHitCollection hiCHits = dataSet.getHiCReadsForProbe(probes[p].probe);

			String [] chromosomeNames = hiCHits.getChromosomeNamesWithHits();

//			System.err.println("Found hits on "+chromosomeNames.length+" chromosomes");
			CHROM: for (int c=0;c<chromosomeNames.length;c++) {
				
				// Do a quick distance test to be able to ignore lots of reads
				
				// Skip all trans reads if there is a max distance set.
				if (initialMaxDistance > 0) {
					if (! probes[p].probe.chromosome().name().equals(chromosomeNames[c])) {
						continue;
					}
				}

				long [] hitReads = hiCHits.getHitPositionsForChromosome(chromosomeNames[c]);

				SequenceRead.sort(hitReads);
				
//				System.err.println("Found "+hitReads.length+" hits on chr "+chromosomeNames[c]);
				

				// Now we need to start adding the other end information to the matrix

				// We're going to start when we hit the probes for this chromosome
				if (! offsets.containsKey(chromosomeNames[c])) {
					// There are no probes on this chromosome.
//					System.err.println("No probes on chr "+chromosomeNames[c]+" skipping");
					continue;
				}
				int lastIndex = offsets.get(chromosomeNames[c]);
								
				READ: for (int o=0;o<hitReads.length;o++) {

					if (cancel) {
						Enumeration<ProgressListener> en = listeners.elements();
						while (en.hasMoreElements()) {
							en.nextElement().progressCancelled();
						}
						return;
					}


					// Check against the relevant reads
					int startIndex = lastIndex;

					for (int x=startIndex;x<probes.length;x++) {
						// Check to see if this read overlaps with this probe

						// Check that we're on the right chromosome
						if (!probes[x].probe.chromosome().name().equals(chromosomeNames[c])) {
//							System.err.println("Stopping searching at position "+x+" since we changed to chr "+probes[x].probe.chromosome());
							continue CHROM;
						}
						
						if (probes[x].probe.start() > SequenceRead.end(hitReads[o])) {
							// We've gone past where this could possibly be. We need to move on
							// to the next read.
							continue READ;
						}

						if (SequenceRead.overlaps(hitReads[o], probes[x].probe.packedPosition())) {
							// We overlap with this probe
//							System.err.println("Found hit to probe "+probes[x].probe);

							// For probe list probes we could have the same probe several times
							// so we can add the read to all of the probes which are the same
							// from this point.

							// We can skip over interactions where the matched index is less than our index
							// since we'll duplicate this later anyway.

							long interactionKey = p+(x*(long)probes.length);
							
							// Make an entry in the interaction dataset if there isn't one there already
							
							if (! interactionCounts.containsKey(interactionKey)) {

								//TODO: Removed this for debugging.  Need to put back.
//								if (interactionCounts.size() >= MAX_INTERACTIONS) {
//									continue READ;
//								}

								if (probes[p].index < probes[x].index) {
									interactionCounts.put(interactionKey, 0);
								}
//								else {
//									System.err.println("Skipping earlier probe hit");
//								}
							}
							
							
							// Add one to our counts for this interaction
							if (probes[p].index < probes[x].index) {
								interactionCounts.put(interactionKey , interactionCounts.get(interactionKey) + 1);
							}

							// Since we found our first hit here we can start looking from here next time.
							lastIndex = x;
							
							// Now we also add counts to any other probes which follow which are the same as 
							// this one.
							for (x=x+1;x<probes.length;x++) {
								if (probes[x].probe.chromosome() != probes[x-1].probe.chromosome()) {
									// We've reached the end for this chromosome
									break;
								}
								if (probes[x].probe == probes[x-1].probe  ||  SequenceRead.overlaps(hitReads[o], probes[x].probe.packedPosition())) {
									if (probes[p].index >= probes[x].index)	continue;
									interactionKey = p+(x*(long)probes.length);
									if (! interactionCounts.containsKey(interactionKey)) {

										if (interactionCounts.size() >= MAX_INTERACTIONS) {
											continue READ;
										}

										interactionCounts.put(interactionKey, 0);
									}
									interactionCounts.put(interactionKey , interactionCounts.get(interactionKey) + 1);
								}
								else {
									break;
								}
							}
							
							continue READ;
						} // End if overlaps
					} // End check each probe
				}// End each hit read
			} // End each hit chromosome


			// We can now go through the interactions we saw and decide if we
			// want to keep any of them.

			HiCInteractionStrengthCalculator strengthCalc = new HiCInteractionStrengthCalculator(dataSet, correctLinkage);

			Enumeration<Long> en = interactionCounts.keys();
			while (en.hasMoreElements()) {
				long index = en.nextElement();
				int absoluteValue = interactionCounts.get(index);

				if (absoluteValue < initialMinAbsolute) continue;

				ProbeWithIndex probe1 = probes[(int)(index%probes.length)];
				ProbeWithIndex probe2 = probes[(int)(index/probes.length)];

				// We calculate the obs/exp based on the total pair count 
				// and the relative counts at each end of the interaction

				// Do the interaction strength calculation
				strengthCalc.calculateInteraction(absoluteValue,totalCisCounts[probe1.index],totalTransCounts[probe1.index],totalCisCounts[probe2.index],totalTransCounts[probe2.index],probe1.probe,probe2.probe);

				
				// We're not counting this here any more.  We work out theoretical numbers at the top instead
				// ++numberOfTestsPerformed;	
				
				float obsExp = (float)strengthCalc.obsExp();
				float pValue = (float)strengthCalc.rawPValue();


				// Do some quick checks against our filters so we can reduce the number of
				// interaction objects we have to create.
				if (obsExp < initialMinStrength) continue;
				if (initialMaxSignificance < 1 && pValue > initialMaxSignificance) continue; // This isn't the final p-value check, but if the raw pvalue fails then the corrected value is never going to pass.

				InteractionProbePair interaction = new InteractionProbePair(probe1.probe, probe1.index, probe2.probe, probe2.index, obsExp, absoluteValue);
				interaction.setSignificance(pValue);

				// We check against the current list of filters
				if (passesCurrentFilters(interaction)) {

					// See if the strength of any of the interactions is bigger than our current max
					if (obsExp > maxValue) maxValue = obsExp;

					if (filteredInteractions.size() >= MAX_INTERACTIONS) {
						Enumeration<ProgressListener> en2 = listeners.elements();
						while (en2.hasMoreElements()) {
							en2.nextElement().progressWarningReceived(new SeqMonkException("More than "+MAX_INTERACTIONS+" interactions passed the filters. Showing as many as I can"));
						}
					}
					else {
						filteredInteractions.add(interaction);
					}
				}

			}
		}

		
		// Put the interactions which worked into an Array
		interactions = filteredInteractions.toArray(new InteractionProbePair[0]);

//		System.err.println("Found a set of "+interactions.length+" initially filtered interactions");

		// Apply multiple testing correction
		Arrays.sort(interactions, new Comparator<InteractionProbePair>() {
			public int compare(InteractionProbePair o1, InteractionProbePair o2) {
				return Float.compare(o1.signficance(), o2.signficance());
			}
		});
		
//		System.err.println("Number of tests performed is "+numberOfTestsPerformed);
		
		for (int i=0;i<interactions.length;i++) {
			interactions[i].setSignificance(interactions[i].signficance()*((float)(numberOfTestsPerformed)/(i+1)));
			
			// We can't allow corrected p-values to end up in a different order to the uncorrected ones
			if (i>0 && interactions[i].signficance() < interactions[i-1].signficance()) {
				interactions[i].setSignificance(interactions[i-1].signficance());
			}
		}
		
		// Now re-filter the interactions to get those which finally pass the p-value filter
		filteredInteractions.clear();
		
		for (int i=0;i<interactions.length;i++) {
			if (initialMaxSignificance >=1 || interactions[i].signficance()<initialMaxSignificance) {
				filteredInteractions.add(interactions[i]);
			}
			else {
				break;
			}
		}

		// Put the interactions which worked into an Array
		interactions = filteredInteractions.toArray(new InteractionProbePair[0]);
		
//		System.err.println("Found a set of "+interactions.length+" interactions after multiple testing correction");

		// We've processed all of the reads and the matrix is complete.  We can therefore
		// draw the plot
		Enumeration<ProgressListener> en2 = listeners.elements();
		while (en2.hasMoreElements()) {
			en2.nextElement().progressComplete("heatmap", null);
		}


	}
	
	private void getTotalCounts (int [] cisCounts,int [] transCounts) {
		
		for (int p=0;p<probes.length;p++) {

			if (p%100 == 0) {
				Enumeration<ProgressListener> en = listeners.elements();
				while (en.hasMoreElements()) {
					en.nextElement().progressUpdated("Getting probe total counts", p, probes.length);
				}

				if (cancel) {
					return;
				}

			}
			
			HiCHitCollection hits = dataSet.getHiCReadsForProbe(probes[p].probe);

			String [] names = hits.getChromosomeNamesWithHits();
			for (int c=0;c<names.length;c++) {
				if (names[c].equals(probes[p].probe.chromosome().name())) {
					cisCounts[p] = hits.getSourcePositionsForChromosome(names[c]).length;
				}
				else {
					transCounts[p] += hits.getSourcePositionsForChromosome(names[c]).length;					
				}
			}
			
		}

	}

	private class ProbeWithIndex implements Comparable<ProbeWithIndex> {
		Probe probe;
		int index;

		public ProbeWithIndex (Probe probe, int index) {
			this.probe = probe;
			this.index = index;
		}

		public int compareTo(ProbeWithIndex o) {
			return probe.compareTo(o.probe);
		}

	}

}
