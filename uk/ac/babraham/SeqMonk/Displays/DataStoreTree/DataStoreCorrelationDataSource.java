/**
 * Copyright 2016-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.DataStoreTree;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Correlation.PearsonCorrelation;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterDataSource;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

public class DataStoreCorrelationDataSource implements ClusterDataSource {

	private Probe [] probes;
	private DataStore [] stores;

	private float [] probeValues1;
	private float [] probeValues2;

	private float [][] rValueMatrix;

	public DataStoreCorrelationDataSource (Probe [] probes, DataStore [] stores) {
		this.probes = probes;
		this.stores = stores;
		probeValues1 = new float[probes.length];
		probeValues2 = new float[probes.length];

		rValueMatrix = new float[stores.length][stores.length];
		for (int s=0;s<stores.length;s++) {
			for (int s2=0;s2<stores.length;s2++) {
				rValueMatrix[s][s2] = Float.NaN;
			}
		}

	}


	public int getProbeCount() {
		// Since we're clustering stores here we need to give them the total number of stores.
		return stores.length;
	}

	public float calculateClusterValue(Integer[] indices1, Integer[] indices2) {

		// We need to calculate the R value across the two sets of stores
		// To do this we simply get the average R value for all pairwise
		// comparisons within those stores.
		try {

			float rValueSum = 0;
			int rValueCount = 0;

			for (int i=0;i<indices1.length;i++) {
				for (int j=0;j<indices2.length;j++) {
					if (Float.isNaN(rValueMatrix[indices1[i]][indices2[j]])) {
						correlateIndexPair(indices1[i], indices2[j]);
					}

					if (Float.isNaN(rValueMatrix[indices1[i]][indices2[j]])) continue; // We can't calculate this correlation for some reason

					rValueSum += rValueMatrix[indices1[i]][indices2[j]];
					rValueCount++;
				}
			}

			if (rValueCount == 0) {
				// There are no valid correlations
				return 0;
			}

			return (rValueSum/rValueCount);

		}
		catch (SeqMonkException sme) {
			return -1;
		}

	}

	private void correlateIndexPair (int index1, int index2) throws SeqMonkException {

		for (int p=0;p<probes.length;p++) {

			probeValues1[p] = stores[index1].getValueForProbe(probes[p]);
			probeValues2[p] = stores[index2].getValueForProbe(probes[p]);

		}
		float rValue = PearsonCorrelation.calculateCorrelation(probeValues1, probeValues2);
		rValueMatrix[index1][index2] = rValue;
		rValueMatrix[index2][index1] = rValue;
	}


	public float minClusterValue() {
		return -1;
	}

	public float maxClusterValue() {
		return 1;
	}

}
