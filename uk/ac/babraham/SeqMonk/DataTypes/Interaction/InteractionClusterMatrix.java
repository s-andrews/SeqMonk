/**
 * Copyright 2012- 21 Simon Andrews
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

import java.util.Iterator;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterDataSource;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

public class InteractionClusterMatrix implements ClusterDataSource, Runnable, Cancellable {

	private InteractionProbePair [] interactions;
	private int probeCount;
	private float [][] correlationMatrix;
	private boolean cancel = false;
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	
	public InteractionClusterMatrix (InteractionProbePair [] interactions, int probeCount) {
		this.interactions = interactions;
		this.probeCount = probeCount;
	}
	
	public float [][] correlationMatix () {
		return correlationMatrix;
	}
	
	public void startCorrelating () {
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
	
	public void cancel () {
		cancel = true;
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
	protected void progressComplete() {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressComplete("interaction_cluster_matrix", this);
		}
	}

	
	public int getProbeCount() {
		return correlationMatrix.length;
	}

	public float calculateClusterValue(Integer[] ind1, Integer[] ind2) {
		float total = 0;
		int count = 0;

		for (int i=0;i<ind1.length;i++) {
			for (int j=0;j<ind2.length;j++) {

				if (ind1[i] < ind2[j]) {
					total += correlationMatrix[ind1[i]][ind2[j]];
				}
				else {
					total += correlationMatrix[ind2[j]][ind1[i]];
				}
				++count;
			}
		}

		return total/count;
	}

	public void run() {
		progressUpdated("Making correlation matrix", 0, 1);
		boolean [][] booleanInteractions = new boolean [probeCount][probeCount];

		for (int i=0;i<interactions.length;i++) {
			if (cancel) {
				progressCancelled();
				cancel = false;
				return;
			}
			booleanInteractions[interactions[i].probe1Index()][interactions[i].probe2Index()] = true;
			booleanInteractions[interactions[i].probe2Index()][interactions[i].probe1Index()] = true;
		}

		correlationMatrix = new float[probeCount][probeCount];

		for (int p1=0;p1<probeCount;p1++) {
			if (p1%100 == 0) {
				progressUpdated("Making correlation matrix", p1, booleanInteractions.length);				
			}

			if (cancel) {
				progressCancelled();
				return;
			}
			for (int p2=p1+1;p2<probeCount;p2++) {
				correlationMatrix[p1][p2] = calculateCorrelation(booleanInteractions[p1],booleanInteractions[p2]);
				correlationMatrix[p2][p1] = correlationMatrix[p1][p2];
				
			}
		}
		
		interactions = null;
		
		progressComplete();

	}
	
	private float calculateCorrelation (boolean [] b1, boolean [] b2) {

		float n11 = 0;
		float n10 = 0;
		float n01 = 0;
		float n00 = 0;
		float b1n1 = 0;
		float b1n0 = 0;
		float b2n1 = 0;
		float b2n0 = 0;

		for (int i=0;i<b1.length;i++) {
			if (b1[i]) {
				b1n1++;
				if (b2[i]) {
					b2n1++;
					n11++;
				}
				else {
					b2n0++;
					n10++;
				}
			}
			else {
				b1n0++;
				if (b2[i]) {
					b2n1++;
					n01++;
				}
				else {
					b2n0++;
					n00++;
				}
			}

		}


		float numerator = (float)((n11*n00)-(n10*n01));
		float denominator = (float)(Math.sqrt(b1n1*b1n0*b2n1*b2n0));
		
		
		float correlation = 0;
		
		if (denominator > 0) {
			correlation = numerator/denominator;
		}

//		System.err.println("Counts are "+n11+","+n10+","+n01+","+n00+","+b1n1+","+b1n0+","+b2n1+","+b2n0+"="+correlation);

		return correlation;
	}

	public float minClusterValue() {
		return -1;
	}

	public float maxClusterValue() {
		return 1;
	}

}
