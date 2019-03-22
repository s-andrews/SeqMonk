/**
 * Copyright Copyright 2017-19 Simon Andrews
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

package uk.ac.babraham.SeqMonk.Analysis.Statistics;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.math3.distribution.NormalDistribution;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Displays.VariancePlot.VariancePlotPanel;

public class SmoothedVarianceDataset {

	private float [] smoothedVariances;
	private float [] values;
	private float [] varianceValues;
	private Integer [] indices;
	private int [] revIndices;
	
	private double [] currentDiffSet = new double[0];
	
	
	public SmoothedVarianceDataset (ReplicateSet r, Probe [] probes, int varianceMeasure, int windowSize) {
		
		smoothedVariances = new float[probes.length];
		
		// We need to get the sorted indices for the values
		
		values = new float [probes.length];
		varianceValues = new float [probes.length];
		indices = new Integer[probes.length];
		
		for (int p=0;p<probes.length;p++) {
			indices[p] = p;
			try {
				values[p] = r.getValueForProbeExcludingUnmeasured(probes[p]);
				
				if (Float.isNaN(values[p])) {
					throw new IllegalStateException("Can't calculate variances from NaN values");
				}
				
				try {
					switch (varianceMeasure) {
						case (VariancePlotPanel.VARIANCE_COEF): varianceValues[p] = r.getCoefVarForProbe(probes[p]);break;
						case (VariancePlotPanel.VARIANCE_QUARTILE_DISP): varianceValues[p] = r.getQuartileCoefDispForProbe(probes[p]);break;
						case (VariancePlotPanel.VARIANCE_SEM): varianceValues[p] = r.getSEMForProbe(probes[p]);break;
						case (VariancePlotPanel.VARIANCE_STDEV): varianceValues[p] = r.getStDevForProbe(probes[p]);break;
						case (VariancePlotPanel.VARIANCE_NUMBER_UNMEASURED) : varianceValues[p] = r.getUnmeasuredCountForProbe(probes[p]); break;
						default : throw new IllegalStateException("Didn't expect variance type "+varianceMeasure);
					}
				}
				catch (SeqMonkException sme) {
					varianceValues[p] = 0;
				}	
			}
			catch (SeqMonkException e) {}
		}
		
		// Now we sort the indices by the values
		Arrays.sort(indices,new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				return Float.compare(values[o1], values[o2]);
			}
		});
		
		// We need to reverse the sense of the indices, so that we can look up the sorted
		// position of each probe
		
		revIndices = new int [indices.length];
		
		for (int i=0;i<indices.length;i++) {
			revIndices[indices[i]] = i;
		}
		
		// Now we can step through the probes to work out the smoothed values for each one
		
		for (int p=0;p<probes.length;p++) {
			
			int sortedIndex = revIndices[p];
			
			int startingIndex = sortedIndex - (windowSize/2);
			int endingIndex = startingIndex + windowSize;
			
//			System.err.println("Index range is "+startingIndex+" to "+endingIndex);
			
			if (startingIndex < 0) startingIndex = 0;
			if (endingIndex >= probes.length) endingIndex = probes.length-1;
			
			double total = 0;
			int count = 0;
			
			for (int i=startingIndex; i<=endingIndex; i++) {
				total += varianceValues[indices[i]];			
				count++;
			}
			
			if (count > 0) {
				smoothedVariances[p] = (float)(total/count);
			}
			
		}
		
	}
	
	public float [] orderedValues () {
	
		float [] returnValues = new float[values.length];
		for (int i=0;i<indices.length;i++) {
			returnValues[i] = values[indices[i]];
		}

		return returnValues;
	
	}

	
	public float [] orderedSmoothedVariances () {
		
		float [] returnValues = new float[values.length];
		for (int i=0;i<indices.length;i++) {
			returnValues[i] = smoothedVariances[indices[i]];
		}

		return returnValues;
	
	}

	
	public float getSmoothedValueForX (float x) {
		
		// We should probably look to optimise this somehow, but lets take a simple approach to start with!
		
		// Do some sanity checks
		if (Float.isNaN(x) || Float.isInfinite(x)) {
			return smoothedVariances[0];
		}
		
		if (x < values[indices[0]]) return smoothedVariances[indices[0]];
		if (x > values[indices[values.length-1]]) return smoothedVariances[indices[values.length-1]];
		
		// Take the naive view that the points are equally spaced and guess the best place to start
		double proportion = (x-values[indices[0]])/(values[indices[values.length-1]]-values[indices[0]]);
		
		int startingIndex = (int)((values.length-1) * proportion);
		
		if (values[indices[startingIndex]] == x) return smoothedVariances[indices[startingIndex]];
		
		if (values[indices[startingIndex]]> x) {
			// We need to go down until we go past
			
			for (int i=startingIndex;i>=0;i--) {
				if (values[indices[i]] <= x) {
//					System.err.println("From x="+x+" Found y="+smoothedValues[i]+" xf="+xValues[i]+" low="+xValues[0]+" high="+xValues[xValues.length-1]+" guess=index "+startingIndex+" = "+xValues[startingIndex]);
					return smoothedVariances[indices[i]];
				}
			}
			
		}
		else {
			// We need to go up until we go past

			for (int i=startingIndex;i<values.length;i++) {
				if (values[indices[i]] >= x) {
//					System.err.println("From x="+x+" Found y="+smoothedValues[i]+" xf="+xValues[i]+" low="+xValues[0]+" high="+xValues[xValues.length-1]+" guess=index "+startingIndex+" = "+xValues[startingIndex]);
					return smoothedVariances[indices[i]];
				}
			}

		}
		
		throw new IllegalStateException("Didn't find a match for "+x+" low="+values[indices[0]]+" high="+values[indices[values.length-1]]+" guess=index "+startingIndex+" = "+values[indices[startingIndex]]);
		
	}
	
	
	
	public float averageDeviationFromSmoothed () {
		double total = 0;
		int count = 0;
		for (int i=0;i<varianceValues.length;i++) {
			if (! (Float.isInfinite(varianceValues[i]) || Float.isNaN(varianceValues[i]) || Float.isNaN(smoothedVariances[i]) || Float.isInfinite(smoothedVariances[i]))) {
				total += Math.abs(varianceValues[i]-smoothedVariances[i]);
				++count;
			}
		}
		
		return (float)(total/count);
	}
	
	public float getSmoothedValueForIndex (int i) {
		return smoothedVariances[i];
	}
	
	public float getDifferenceForIndex (int i) {
		return varianceValues[i]-smoothedVariances[i];
	}
	
	
	public double getIntenstiyPValueForIndex (int p, int probesPerSet) {
		
//		System.err.println("Getting p-value for index "+p);
		
		int sortedIndex = revIndices[p];
		
//		System.err.println("Sorted index is "+sortedIndex);
		
		int startingIndex = sortedIndex - (probesPerSet/2);
		int endingIndex = startingIndex + probesPerSet;
		
		if (startingIndex < 0) startingIndex = 0;
		if (endingIndex >= revIndices.length) endingIndex = revIndices.length-1;

//		System.err.println("Index range is "+startingIndex+" to "+endingIndex);
		
		if (currentDiffSet.length != (endingIndex-startingIndex)+1) {
			currentDiffSet = new double[(endingIndex-startingIndex)+1];
		}
		
//		System.err.println("Made array of size "+currentDiffSet.length);
		
		for (int i=startingIndex; i<=endingIndex; i++) {
			currentDiffSet[i-startingIndex] = varianceValues[indices[i]];
		}
		
//		System.err.println("Variance values assigned");

		double mean = SimpleStats.mean(currentDiffSet);
		double stdev = SimpleStats.stdev(currentDiffSet,mean);
	
		
//		System.err.println("Mean is "+mean+" stdev is "+stdev);
		
		if (stdev == 0) {
			return 1;
		}
			
		// Get the difference for this point
		double diff = varianceValues[p];
		
//		System.err.println("Value to test is "+diff);
					
		NormalDistribution nd = new NormalDistribution(mean, stdev);
			
		double significance;
			
		if (diff < mean) {
			significance = nd.cumulativeProbability(diff);
		}
		else {
			significance = 1-nd.cumulativeProbability(diff);
		}
		
//		System.err.println("P-value is "+significance);
		
		return significance;
		
	}
	
	
}
