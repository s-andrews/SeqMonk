/**
 * Copyright 2009- 21 Simon Andrews
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

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The Class StarWars calculates and describes the elements of a single
 * StarWars plot.
 */
public class StarWars {

	/** The data. */
	private DataStore data;
	
	/** The ordered probes. */
	private Probe [] orderedProbes;
	
	/** The mean. */
	private float mean;
	
	/** The standard_error. */
	private float standard_error;
	
	/** The min value. */
	private float minValue = Float.NaN;
	
	/** The max value. */
	private float maxValue = Float.NaN;
		
	/**
	 * Instantiates a new star wars.
	 * 
	 * @param data the data
	 * @param probes the probes
	 * @throws SeqMonkException the seq monk exception
	 */
	public StarWars (DataStore data, ProbeList probes) throws SeqMonkException {
		this(data,probes,3);
	}
	
	/**
	 * Instantiates a new star wars.
	 * 
	 * @param data the data
	 * @param probes the probes
	 * @throws SeqMonkException the seq monk exception
	 */
	public StarWars (DataStore data, ProbeList probes, double stringency) throws SeqMonkException {
		this.data = data;

		orderProbes(probes);
		calculateRanges();
		
	}
	
	
	/**
	 * Calculate ranges.
	 * 
	 * @throws SeqMonkException the seq monk exception
	 */
	private void calculateRanges() throws SeqMonkException {

//		System.err.println("Starting to calculate ranges for "+data.name());
		
		// Get the mean
		int lengthForMean = 0;
		for (int i=0;i<orderedProbes.length;i++) {
			float value = data.getValueForProbe(orderedProbes[i]);
			if (Float.isNaN(value) || Float.isInfinite(value)) continue;
			mean += value;
			++lengthForMean;
		}
		mean /= lengthForMean;
		
		if (lengthForMean == 0) {
			mean = Float.NaN;
		}
		
//		System.err.println("Mean is "+mean);
		
		// Get the standard error of the mean
		standard_error = 0;
		for (int i=0;i<orderedProbes.length;i++) {
			float value = data.getValueForProbe(orderedProbes[i]);
			if (Float.isNaN(value) || Float.isInfinite(value)) continue;
			standard_error += Math.pow(value-mean, 2);
		}
		standard_error /= lengthForMean-1;
		standard_error /= Math.sqrt(standard_error);
		standard_error /= Math.sqrt(lengthForMean);
		
		// Can't calculate STDERR if we only have a single value.
		if (lengthForMean < 2) {
			standard_error = 0;
		}

//		System.err.println("Standard Error is "+standard_error);
		
		
		// Work out the highest and lowest values in the data.  Ignore any
		// infinite values which may be at the ends of the distribution.
		
		for (int i=0;i<orderedProbes.length;i++) {
			float value = data.getValueForProbe(orderedProbes[i]);
			if (Float.isNaN(value) || Float.isInfinite(value)) continue;
			minValue = data.getValueForProbe(orderedProbes[i]);
			break;
		}

		for (int i=orderedProbes.length-1;i>=0;i--) {
			float value = data.getValueForProbe(orderedProbes[i]);
			if (Float.isNaN(value) || Float.isInfinite(value)) continue;
			maxValue = data.getValueForProbe(orderedProbes[i]);
			break;
		}
		
//		System.err.println("Range is "+minValue+" to "+maxValue);
		
//		System.err.println("Finished calculating ranges for "+data.name());

	}
	
	/**
	 * Order probes.
	 * 
	 * @param probes the probes
	 */
	private void orderProbes (ProbeList probes) {
//		System.err.println("Starting to order probes for "+data.name());
		orderedProbes = probes.getAllProbes();
		
		Arrays.sort(orderedProbes,new ProbeValueComparator(data));
//		System.err.println("Finished ordering probes for "+data.name());
	}
	
	
	/**
	 * Mean.
	 * 
	 * @return the double
	 */
	public double mean () {
		return mean;
	}
	
	/**
	 * Standard Error.
	 * 
	 * @return the double
	 */
	public double standard_error () {
		return standard_error;
	}
	
	
	/**
	 * Min value.
	 * 
	 * @return the double
	 */
	public double minValue () {
		return minValue;
	}
	
	/**
	 * Max value.
	 * 
	 * @return the double
	 */
	public double maxValue () {
		return maxValue;
	}
	
	public double lowerBound () {
		return mean()-standard_error();
	}
	
	public double upperBound () {
		return mean()+standard_error();
	}
	
	
	/**
	 * The Class ProbeValueComparator.
	 */
	private class ProbeValueComparator implements Comparator<Probe> {
		
		/** The d. */
		DataStore d;
		
		/**
		 * Instantiates a new probe value comparator.
		 * 
		 * @param d the d
		 */
		public ProbeValueComparator (DataStore d) {
			this.d = d;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Probe p1, Probe p2) {
			try {
				return Double.compare(d.getValueForProbe(p1), d.getValueForProbe(p2));
			} 
			catch (SeqMonkException e) {
				return 0;
			}
		}
	}
	
	
}
