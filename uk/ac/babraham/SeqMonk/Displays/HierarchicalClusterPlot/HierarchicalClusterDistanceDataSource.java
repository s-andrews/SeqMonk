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
package uk.ac.babraham.SeqMonk.Displays.HierarchicalClusterPlot;

import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterDataSource;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

public class HierarchicalClusterDistanceDataSource implements ClusterDataSource {

	private Probe [] probes;
	private DataStore [] stores;
	private boolean normalise;
	private boolean setValue = false;
	private float minValue = 0;
	private float maxValue = 1;

	public HierarchicalClusterDistanceDataSource (Probe [] probes, DataStore [] stores, boolean normalise) {
		// Only use stores which are actually quantitated
		Vector<DataStore> keptStores = new Vector<DataStore>();
		
		for (int d=0;d<stores.length;d++) {
			if (stores[d].isQuantitated()) {
				keptStores.add(stores[d]);
			}
		}

		
		this.probes = probes;
		this.stores = keptStores.toArray(new DataStore[0]);
		this.normalise = normalise;
	}

	public int getProbeCount() {
		return probes.length;
	}

	
	public float calculateClusterValue(Integer[] indices1, Integer[] indices2) {
		try {
			float [] average1 = new float [stores.length];
			for (int i=0;i<indices1.length;i++) {
				float [] localValues = new float[stores.length];
				for (int d=0;d<stores.length;d++) {
					localValues[d] = stores[d].getValueForProbe(probes[indices1[i]]);
				}
				float median = SimpleStats.mean(localValues);
				for (int v=0;v<localValues.length;v++) {
					if (normalise) {
						average1[v] += (localValues[v] - median);
					}
					average1[v] += localValues[v];
				}
			}
			float [] average2 = new float [stores.length];
			for (int i=0;i<indices2.length;i++) {
				float [] localValues = new float[stores.length];
				for (int d=0;d<stores.length;d++) {
					localValues[d] = stores[d].getValueForProbe(probes[indices2[i]]);
				}
				float median = SimpleStats.mean(localValues);
				for (int v=0;v<localValues.length;v++) {
					if (normalise) {
						average2[v] += (localValues[v] - median);
					}
					average2[v] += localValues[v];
				}
			}
			for (int d=0;d<stores.length;d++) {
				average1[d] /= indices1.length;
				average2[d] /= indices2.length;
			}
			
			// Now we want to calculate the absolute difference between each of the
			// values in averages1 and 2
			float absDiffs = 0;
			
			for (int a=0;a<stores.length;a++) {
				absDiffs += Math.abs(average1[a] - average2[a]);
			}

			// In this logic the highest values get joined, so we return 
			// the negative of the differences
			
			if (!setValue) {
				minValue = 0-absDiffs;
				maxValue = 0-absDiffs;
				setValue = true;
			}
			
			if (0-absDiffs < minValue) minValue = 0-absDiffs;
			if (0-absDiffs > maxValue) maxValue = 0-absDiffs;
			
			return 0-absDiffs;
		}
		catch (SeqMonkException sme) {
			throw new IllegalStateException(sme);
		}
	}

	public float minClusterValue() {
		return minValue;
	}

	public float maxClusterValue() {
		return maxValue;
	}

	
}
