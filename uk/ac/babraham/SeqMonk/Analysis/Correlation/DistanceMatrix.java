/**
 * Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Analysis.Correlation;

import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

public class DistanceMatrix implements Runnable, Cancellable {

	private DataStore [] stores;
	private Probe [] probes;
	private float [] matrix;
	private boolean cancel = false;
	private boolean calculationComplete = false;
	
	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();
	
	public DistanceMatrix (DataStore [] stores, Probe [] probes) {
	
		Vector<DataStore> validStores = new Vector<DataStore>();
		for (int s=0;s<stores.length;s++) {
			if (stores[s].isQuantitated()) {
				validStores.add(stores[s]);
			}
		}
		
		if (validStores.size() == 0) {
			throw new IllegalArgumentException("There were no quantitated stores");
		}
		
		this.stores = validStores.toArray(new DataStore[0]);
		
		if (probes.length < 3) {
			throw new IllegalArgumentException("At least 3 probes are needed to calculate a correlation");
		}
		this.probes = probes;
		
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
	
	public float getCorrelationForStores(DataStore store1, DataStore store2) throws SeqMonkException {
		
		if (! calculationComplete) {
			throw new SeqMonkException("Correlation calculation not yet complete");
		}
		
		int st1 = getIndexForStore(store1);
		int st2 = getIndexForStore(store2);
		
		if (st1 == -1 || st2 == -1) {
			throw new SeqMonkException("Attempted to get correlation for a store not in the distance matrix");
		}
		
		return getCorrelationForStoreIndices(st1, st2);
	}
	
	public float getCorrelationForStoreIndices (int st1, int st2) throws SeqMonkException {

		if (! calculationComplete) {
			throw new SeqMonkException("Correlation calculation not yet complete");
		}

		if (st1 >= stores.length || st2 >= stores.length) {
			throw new SeqMonkException("Store index was larger than the list of stores");
		}
		
		if (st2 < st1) {
			int temp = st2;
			st2 = st1;
			st1 = temp;
		}
		
		System.out.println("Retrieving information for "+st1+" and "+st2);
		if (st2 == st1) return 1;
		
		// Work out the position in the line up for these stores
		int count = -1;
		for (int s1=0;s1<stores.length;s1++) {
			for (int s2=s1+1;s2<stores.length;s2++) {
				count++;
//				System.out.println("Checking "+s1+" and "+s2+" with count "+count);
				if (s1 == st1 && s2 == st2) {
					return matrix[count];
				}
			}
		}
		
		return 0;		
	}
	
	private int getIndexForStore(DataStore store) {
		for (int i=0;i<stores.length;i++) {
			if (stores[i] == store) {
				return i;
			}
		}
		
		return -1;
	}
	
	public DataStore [] stores () {
		return stores;
	}

	public void run () {
		
		cancel = false;
		
		int count = 0;
		for (int s=stores.length-1;s>0;s--) {
			count += s;
		}
		
		matrix = new float[count];
		
		double [] f1 = new double[probes.length];
		double [] f2 = new double[probes.length];
		
		count = 0;
		try {
			for (int s1=0;s1<stores.length;s1++) {
				for (int s2=s1+1;s2<stores.length;s2++) {
					if (cancel) {
						Enumeration<ProgressListener>el = listeners.elements();
						while (el.hasMoreElements()) {
							el.nextElement().progressCancelled();
						}
						return;
					}
					for (int p=0;p<probes.length;p++) {
						f1[p] = stores[s1].getValueForProbe(probes[p]);
						f2[p] = stores[s2].getValueForProbe(probes[p]);
					}
					matrix[count] = PearsonCorrelation.calculateCorrelation(f1, f2);
					
//					System.out.println("Set R="+matrix[count]+" for "+stores[s1]+" vs "+stores[s2]);
					
					count++;
					
					Enumeration<ProgressListener>el = listeners.elements();
					while (el.hasMoreElements()) {
						el.nextElement().progressUpdated("Made "+count+" out of "+matrix.length+" comparisons", count, matrix.length);
					}
					
				}
			}
		}
		catch (SeqMonkException e) {
			Enumeration<ProgressListener>el = listeners.elements();
			while (el.hasMoreElements()) {
				el.nextElement().progressExceptionReceived(e);
			}
			return;
		}
		
		calculationComplete = true;
		
		Enumeration<ProgressListener>el = listeners.elements();
		while (el.hasMoreElements()) {
			el.nextElement().progressComplete("calculate_correlation", this);
		}

		
	}

	public void cancel() {
		cancel = true;
	}
	
	
}
