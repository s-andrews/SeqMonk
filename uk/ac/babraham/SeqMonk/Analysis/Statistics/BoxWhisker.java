/**
 * Copyright 2009-18 Simon Andrews
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
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The Class BoxWhisker calculates and describes the elements of a single
 * BoxWhisker plot.
 */
public class BoxWhisker {

	/** The data. */
	private DataStore data;
	
	/** The ordered probes. */
	private Probe [] orderedProbes;
	
	/** The stringency. */
	private double stringency;

	/** The median. */
	private double median;
	
	/** The upper quartile. */
	private double upperQuartile;
	
	/** The lower quartile. */
	private double lowerQuartile;
	
	/** The upper whisker. */
	private double upperWhisker;
	
	/** The lower whisker. */
	private double lowerWhisker;
	
	/** The min value. */
	private double minValue;
	
	/** The max value. */
	private double maxValue;
	
	/** The upper outliers. */
	private float [] upperOutliers = new float[0];
	
	/** The lower outliers. */
	private float [] lowerOutliers = new float[0];
	
	/** The upper probe outliers. */
	private Probe [] upperProbeOutliers = new Probe[0];
	
	/** The lower probe outliers. */
	private Probe [] lowerProbeOutliers = new Probe[0];
		
	/**
	 * Instantiates a new box whisker.
	 * 
	 * @param data the data
	 * @param probes the probes
	 * @throws SeqMonkException the seq monk exception
	 */
	public BoxWhisker (DataStore data, ProbeList probes) throws SeqMonkException {
		this(data,probes,3);
	}
	
	/**
	 * Instantiates a new box whisker.
	 * 
	 * @param data the data
	 * @param probes the probes
	 * @param stringency the stringency
	 * @throws SeqMonkException the seq monk exception
	 */
	public BoxWhisker (DataStore data, ProbeList probes, double stringency) throws SeqMonkException {
		this.data = data;
		this.stringency = stringency;

		orderProbes(probes);
		calculateRanges();
		
	}
	
	/**
	 * Sets the stringency.
	 * 
	 * @param stringency the new stringency
	 * @throws SeqMonkException the seq monk exception
	 */
	public void setStringency (double stringency) throws SeqMonkException {
		this.stringency = stringency;
		calculateRanges();
	}
	
	
	/**
	 * Calculate ranges.
	 * 
	 * @throws SeqMonkException the seq monk exception
	 */
	private void calculateRanges() throws SeqMonkException {

//		System.err.println("Starting to calculate ranges for "+data.name());
		
		// Get the median
		// Account for really small datasets
		if (orderedProbes.length < 3) {
			for (int i=0;i<orderedProbes.length;i++) {
				median += data.getValueForProbe(orderedProbes[i]);
			}
			median /= orderedProbes.length;
		}
		else if (orderedProbes.length % 2 == 1) {
			// We can just take the middle value
			median = data.getValueForProbe(orderedProbes[orderedProbes.length/2]);
		}
		else {
			median = data.getValueForProbe(orderedProbes[orderedProbes.length/2]);
			median += data.getValueForProbe(orderedProbes[1+(orderedProbes.length/2)]);
			median /=2;
		}
		
//		System.err.println("Median is "+median);
		
		// We'll just take the closest value for the quartiles.  In theory these
		// could end up as infinite values, but if more than a quarter of your
		// data is infinite then you're probably stuffed anyway so don't worry
		// about that.
		
		lowerQuartile = data.getValueForProbe(orderedProbes[orderedProbes.length/4]);
		upperQuartile = data.getValueForProbe(orderedProbes[(orderedProbes.length*3)/4]);
		
//		System.err.println("Quartile range is "+lowerQuartile+" to "+upperQuartile);
		
		// Work out the highest and lowest values in the data.  Ignore any
		// infinite values which may be at the ends of the distribution.
		
		for (int i=0;i<orderedProbes.length;i++) {
			if (data.getValueForProbe(orderedProbes[i]) != Float.NEGATIVE_INFINITY) {
				minValue = data.getValueForProbe(orderedProbes[i]);
				break;
			}
		}
		
		lowerWhisker = minValue;

		for (int i=orderedProbes.length-1;i>=0;i--) {
			if (data.getValueForProbe(orderedProbes[i]) != Float.POSITIVE_INFINITY) {
				maxValue = data.getValueForProbe(orderedProbes[i]);
				break;
			}
		}
		
		upperWhisker = maxValue;

		if (median+((upperQuartile-lowerQuartile)*stringency) < upperWhisker) {
			upperWhisker = median+((upperQuartile-lowerQuartile)*stringency);
		}
		if (median-((upperQuartile-lowerQuartile)*stringency) > lowerWhisker) {
			lowerWhisker = median-((upperQuartile-lowerQuartile)*stringency);
		}
		
//		System.err.println("Whisker range is "+lowerWhisker+" to "+upperWhisker);

		// Find any outliers
		for (int i=0;i<orderedProbes.length;i++) {
			if (data.getValueForProbe(orderedProbes[i]) >= lowerWhisker) {
				lowerOutliers = new float[i];
				lowerProbeOutliers = new Probe[i];
				for (int j=0;j<i;j++) {
					lowerOutliers[j] = data.getValueForProbe(orderedProbes[j]);
					lowerProbeOutliers[j] = orderedProbes[j];
				}
				break;
			}
		}
		
//		System.err.println("Low value is "+minValue+" there are "+lowerOutliers.length+" lower outliers");

		for (int i=0;i<orderedProbes.length;i++) {
			if (data.getValueForProbe(orderedProbes[orderedProbes.length-(i+1)]) <= upperWhisker) {
				upperOutliers = new float[i];
				upperProbeOutliers = new Probe[i];
				for (int j=0;j<i;j++) {
					upperOutliers[j] = data.getValueForProbe(orderedProbes[orderedProbes.length-(j+1)]);
					upperProbeOutliers[j] = orderedProbes[orderedProbes.length-(j+1)];
				}
				break;
			}
		}

//		System.err.println("High value is "+maxValue+" there are "+upperOutliers.length+" upper outliers");

		
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
		
		// Filter the list to remove any probes which have NaN values
		try {
			if (Float.isNaN(data.getValueForProbe(orderedProbes[orderedProbes.length-1]))) {
				Vector<Probe> keepers = new Vector<Probe>();
				
				for (int i=0;i<orderedProbes.length;i++) {
					if (Float.isNaN(data.getValueForProbe(orderedProbes[i]))) {
						break;
					}
					
					keepers.add(orderedProbes[i]);
				}
				
				orderedProbes = keepers.toArray(new Probe[0]);
				
			}
		}
		catch (SeqMonkException sme) {}
		
//		System.err.println("Finished ordering probes for "+data.name());
	}
	
	/**
	 * Median.
	 * 
	 * @return the double
	 */
	public double median () {
		return median;
	}
	
	/**
	 * Upper quartile.
	 * 
	 * @return the double
	 */
	public double upperQuartile () {
		return upperQuartile;
	}
	
	/**
	 * Lower quartile.
	 * 
	 * @return the double
	 */
	public double lowerQuartile () {
		return lowerQuartile;
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
	
	/**
	 * Lower whisker.
	 * 
	 * @return the double
	 */
	public double lowerWhisker () {
		return lowerWhisker;
	}
	
	/**
	 * Upper whisker.
	 * 
	 * @return the double
	 */
	public double upperWhisker () {
		return upperWhisker;
	}
	
	/**
	 * Upper outliers.
	 * 
	 * @return the float[]
	 */
	public float [] upperOutliers () {
		return upperOutliers;
	}
	
	/**
	 * Lower outliers.
	 * 
	 * @return the float[]
	 */
	public float [] lowerOutliers () {
		return lowerOutliers;
	}
	
	/**
	 * Upper probe outliers.
	 * 
	 * @return the probe[]
	 */
	public Probe [] upperProbeOutliers () {
		return upperProbeOutliers;
	}
	
	/**
	 * Lower probe outliers.
	 * 
	 * @return the probe[]
	 */
	public Probe [] lowerProbeOutliers () {
		return lowerProbeOutliers;
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
