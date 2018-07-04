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
package uk.ac.babraham.SeqMonk.DataTypes.Cluster;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

public class HierarchicalClusterSet implements Runnable, Cancellable {

	/**
	 * The interaction cluster set is a way of grouping together
	 * probes which show high degrees of correlation across a
	 * set of HiC interactions
	 */

	private ClusterDataSource data;
	private boolean cancel = false;
	private int cachedRValues = 1000;

	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();

	public HierarchicalClusterSet (ClusterDataSource data) {
		this.data = data;
		cachedRValues = data.getProbeCount();
	}

	public void startClustering () {
		Thread t = new Thread(this);
		t.start();
	}

	public void addListener (ProgressListener l) {
		if (l != null && ! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeListener (ProgressListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	/**
	 * Passes on Progress updated messages to all listeners
	 * 
	 * @param message The message to display
	 * @param current The current progress value
	 * @param total The progress value at completion
	 */
	protected void progressUpdated(String message, int current, int total) {

		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressUpdated(message, current, total);
		}
	}


	/**
	 * Passes on Progress cancelled message to all listeners
	 */
	protected void progressCancelled () {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressCancelled();
		}
	}

	/**
	 * Passes on Progress exception received message to all listeners
	 * 
	 * @param e The exception
	 */
	protected void progressExceptionReceived (Exception e) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressExceptionReceived(e);
		}
	}

	/**
	 * Passes on Filter finished message to all listeners
	 * 
	 * @param newList The newly created probe list
	 */
	protected void progressComplete(ClusterPair topLevelCluster) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressComplete("interaction_cluster", topLevelCluster);
		}
	}

	public void cancel() {
		cancel = true;
	}

	public void run() {

		progressUpdated("Clustering correlations", 0, 1);

		// First make a complete list of all probe indices in a single linked list
		LinkedList<ClusterPair> clusters = new LinkedList<ClusterPair>();
		for (int i=0;i<data.getProbeCount();i++) {
			clusters.add(new ClusterPair(i));
		}

		int numberOfClusters = clusters.size();

		// To save having to recalculate everything every time we're going to
		// keep hold of the top (up to cachedRValues) correlations and only do a full
		// recount if these run out.

		LinkedList<ClusterPair>bestCorrelations = new LinkedList<ClusterPair>();

		while (numberOfClusters > 1) {
			
			if ((data.getProbeCount()-numberOfClusters) % 1 == 0) {
				progressUpdated("Clustering with "+numberOfClusters+" clusters", data.getProbeCount()-numberOfClusters, data.getProbeCount());
			}

			if (cancel) {
				progressCancelled();
				return;
			}
			
			if (bestCorrelations.size() < 100) { // Law of diminishing returns...

//				System.err.println("Doing a full pass");

				// Do a full pass to get a new set of potentials
				int index1 = -1;

				ListIterator<ClusterPair> it1 = clusters.listIterator();

				while (it1.hasNext()) {
					index1++;
					ClusterPair pair1 = it1.next();
					ListIterator<ClusterPair> it2 = clusters.listIterator(index1+1);

					P2: while (it2.hasNext()) {

						if (cancel) {
							progressCancelled();
							cancel = false;
							return;
						}

						ClusterPair pair2 = it2.next();

						// Work out the correlation between pair1 and pair2
						// and compare to the current best correlation

						Integer [] indices1 = pair1.getAllIndices();
						Integer [] indices2 = pair2.getAllIndices();						

						float correlation = data.calculateClusterValue(indices1, indices2);

						//						System.err.println("There are "+indices1.length+" and "+indices2.length+" indices in this set correlation is "+correlation);

						if (bestCorrelations.isEmpty()) {
							// Add this and move on
							bestCorrelations.add(new ClusterPair(pair1,pair2,correlation));
							continue;
						}
						else if (bestCorrelations.size() < cachedRValues || correlation > bestCorrelations.getFirst().rValue()) {
							// We're adding this correlation into the set

							// If we're at our limit then remove the first one
							if (bestCorrelations.size() == cachedRValues) {
								bestCorrelations.removeFirst();
							}

							// Now work out where the new Cluster Pair needs to go
							ListIterator<ClusterPair> rValueIterator = bestCorrelations.listIterator();

							while (rValueIterator.hasNext()) {
								ClusterPair nextPair = rValueIterator.next();
								if (correlation < nextPair.rValue()) {
									// We need to insert before this value
									rValueIterator.previous();
									rValueIterator.add(new ClusterPair(pair1, pair2, correlation));
									
//									System.err.println("Added value "+correlation+" between "+nextPair.rValue()+" and "+rValueIterator.next().rValue());
									continue P2;
								}
							}

							// If we get here then this correlation must be better than anything currently
							// in the list
							bestCorrelations.add(new ClusterPair(pair1,pair2,correlation));
						}
					}
				}

//				// Do a sanity check on the sorting of the best list
//				Iterator<ClusterPair> sanity = bestCorrelations.iterator();
//				float lastR = 1;
//				while (sanity.hasNext()) {
//					float thisR = sanity.next().rValue();
//					if (thisR > lastR) {
//						throw new IllegalStateException("Best correlation list wasn't sorted properly "+lastR+" vs "+thisR);
//					}
//					lastR = thisR;
//				}
				
//				System.err.println("After full pass best correlation size is "+bestCorrelations.size()+" with lowest R "+bestCorrelations.getLast().rValue());
			}

			// The next best correlation is the head of the list

//			System.err.println("Taking top hit from list of "+bestCorrelations.size()+" r is "+bestCorrelations.getFirst().rValue());

			// Make the best grouping into a new cluster and remove the two originals from the set.
			ClusterPair bestPair = bestCorrelations.removeLast();

			// We need to remove the two clusters we've just joined from the original set
			ClusterPair pair1 = bestPair.pair1();
			clusters.remove(pair1);
			ClusterPair pair2 = bestPair.pair2();
			clusters.remove(pair2);

			// We also need to remove any other high scoring correlations which
			// contained pair1 or pair2
			Iterator<ClusterPair>corrTest = bestCorrelations.iterator();
			while(corrTest.hasNext()) {
				ClusterPair testPair = corrTest.next();
				if (testPair.pair1() == pair1 || testPair.pair2() == pair1 || testPair.pair1() == pair2 || testPair.pair2() == pair2) {
					corrTest.remove();
				}
			}

//			System.err.println("After removing other involved pairs from best correlations size is "+bestCorrelations.size());

			// Now we can add the new cluster to the set				
			clusters.add(bestPair);
			numberOfClusters--;

			// Finally we need to correlate this new pair with the rest of the set and
			// add any good hits to the bestCorrelations dataStructure.  There's no point
			// doing this if we have no values left in the existing list as we'll have to
			// do a complete rebuild
			
//			int newAddedValues = 0;
			
//			System.err.println("Comparing bestPair to "+clusters.size()+" clusters");
			
			if (! bestCorrelations.isEmpty()) {
				Iterator<ClusterPair> newCorr = clusters.iterator();

				Integer [] indices1 = bestPair.getAllIndices();

				C1: while (newCorr.hasNext()) {
					ClusterPair thisCorr = newCorr.next();
					if (bestPair == thisCorr) continue;

					Integer [] indices2 = thisCorr.getAllIndices();
					float correlation = data.calculateClusterValue(indices1, indices2);

					if (correlation > bestCorrelations.getFirst().rValue()) {
						// We're adding this correlation into the set
//						++newAddedValues;
						// If we're at our limit then remove the last one
						if (bestCorrelations.size() == cachedRValues) {
							bestCorrelations.removeFirst();
						}

						// Now work out where the new Cluster Pair needs to go
						ListIterator<ClusterPair> rValueIterator = bestCorrelations.listIterator();

						while (rValueIterator.hasNext()) {
							ClusterPair nextPair = rValueIterator.next();
							if (correlation < nextPair.rValue()) {
								// We need to insert before this value
								rValueIterator.previous();
								rValueIterator.add(new ClusterPair(bestPair,thisCorr,correlation));
								
//								System.err.println("Added value "+correlation+" between "+nextPair.rValue()+" and "+rValueIterator.next().rValue());

								continue C1;
							}
						}

						// If we get here then this correlation must be better than anything currently
						// in the list
						bestCorrelations.add(new ClusterPair(bestPair,thisCorr,correlation));

					}
				}
			}				

			// Do a sanity check on the sorting of the best list
//			Iterator<ClusterPair> sanity = bestCorrelations.iterator();
//			float lastR = 1;
//			while (sanity.hasNext()) {
//				float thisR = sanity.next().rValue();
//				if (thisR > lastR) {
//					throw new IllegalStateException("Best correlation list wasn't sorted properly "+lastR+" vs "+thisR);
//				}
//				lastR = thisR;
//			}
			
//			System.err.println("After adding in "+newAddedValues+" new correlations list size is "+bestCorrelations.size());
		}

		progressComplete(clusters.element());
	}


}
