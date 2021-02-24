/**
 * Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Filters.CorrelationCluster;

import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Correlation.PearsonCorrelation;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

public class CorrelationCluster implements Comparable<CorrelationCluster> {

	/**
	 * This class can store a set of probes which are all correlated in a set of data stores
	 * 
	 */
	
	private Vector<Probe>probes = new Vector<Probe>();
	private DataStore [] stores;
	
	private double [] values1;
	private double [] values2;
	
	
	public CorrelationCluster (DataStore [] stores, Probe p) {
		this.stores = stores;
		values1 = new double[stores.length];
		values2 = new double[stores.length];
		probes.add(p);
	}
	
	public int size () {
		return probes.size();
	}
	
	public Probe [] getProbes () {
		return probes.toArray(new Probe[0]);
	}
	
	public void addProbe (Probe p) {
		probes.add(p);
	}
	
		
	public double minRValue (Probe p, double cutoff) {	
		
		// We test this probe against all of the probes in our
		// set and if it matches against all of them we keep it
		
		double minR = 1;
		
		// First populate the new probes data
		
		try {
			for (int s=0;s<stores.length;s++) {
				values1[s] = stores[s].getValueForProbe(p);
			}
			
			Enumeration<Probe> ep = probes.elements();
			
			while (ep.hasMoreElements()) {
				Probe p2 = ep.nextElement();

				for (int s=0;s<stores.length;s++) {
					values2[s] = stores[s].getValueForProbe(p2);
				}

				double r = PearsonCorrelation.calculateCorrelation(values1, values2);
				
				if (r < minR) {
					minR = r;
				}

				// We can stop as soon as we pass the cutoff since
				// we're not keeping it anyway
				if (minR < cutoff) {
					return minR;
				}
			}
			
			return minR;
			
		}
		catch (SeqMonkException e) {
			return 0;
		}
		
	}

	public int compareTo(CorrelationCluster o) {
		return o.size()-size();
	}
}
