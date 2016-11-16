/**
 * Copyright Copyright 2010-15 Simon Andrews
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

/**
 * Some static methods for basic statistical calcuations.
 */
public class SimpleStats {

	/**
	 * Mean.
	 * 
	 * @param values the values
	 * @return the mean
	 */
	public static double mean (double [] values) {
		
		if (values.length == 0) return 0;
		
		double mean = 0;
		for (int i=0;i<values.length;i++) {
			mean += values[i];
		}
		
		mean /= values.length;
		
		return mean;
		
	}
	
	/**
	 * Mean.
	 * 
	 * @param values the values
	 * @return the mean
	 */
	public static float mean (float [] values) {
		
		if (values.length == 0) return 0;
		
		double mean = 0;
		int count = 0;
		for (int i=0;i<values.length;i++) {
			if (Float.isInfinite(values[i]) || Float.isNaN(values[i])) continue;
			mean += values[i];
			count++;
		}
		
		mean /= count;
		
		return (float)mean;
		
	}
	
	/**
	 * Median.
	 * 
	 * @param values the values
	 * @return the median
	 */
	public static double median (double [] values) {
		
		if (values.length == 0) return 0;
		
		Arrays.sort(values);
		
		
		if (values.length %2 == 0) {
			double median = values[values.length/2];
			median += values[(values.length+1)/2];
			median /=2;
			return median;
		}
		else {
			return values[values.length/2];
		}
	}
	
	/**
	 * Median.
	 * 
	 * @param values the values
	 * @return the median
	 */
	public static float median (float [] values) {
		
		if (values.length == 0) return 0;
		
		Arrays.sort(values);
		
		int effectiveLength = values.length;
		if (Float.isNaN(values[values.length-1])) {
			for (int i=values.length-1;i>=0;i--) {
				if (!Float.isNaN(values[i])) {
					effectiveLength = i+1;
					break;
				}
			}
		}
		
		
		if (effectiveLength %2 == 0) {
			float median = values[effectiveLength/2];
			median += values[(effectiveLength+1)/2];
			median /=2;
			return median;
		}
		else {
			return values[effectiveLength/2];
		}
	}
	
	/**
	 * Median.
	 * 
	 * @param values the values
	 * @return the median
	 */
	public static float median (int [] values) {
		
		if (values.length == 0) return 0;
		
		Arrays.sort(values);
		
		if (values.length %2 == 0) {
			float median = values[values.length/2];
			median += values[(values.length+1)/2];
			median /=2;
			return median;
		}
		else {
			return values[values.length/2];
		}
	}
	
	/**
	 * Stdev. Will return 0 for sets with less than 2 values.
	 * 
	 * @param values the values
	 * @param mean The mean
	 * @return the stdev
	 */
	public static double stdev (double [] values, double mean) {
		
		// We don't want infinite values so we provide silly answers
		// to silly questions.
		if (values.length < 2) return 0;
		
		double stdev = 0;
		
		for (int i=0;i<values.length;i++) {
			stdev += Math.pow(values[i]-mean, 2);
		}
		
		stdev /= values.length-1;

		stdev = Math.sqrt(stdev);
		
		return stdev;
	}
	
	/**
	 * Stdev.  Will return 0 for sets with < 2 values
	 * 
	 * @param values the values
	 * @return the stdev
	 */
	public static float stdev (float [] values) {
		float mean = mean(values);
		return stdev(values,mean);
	}
	
	
	/**
	 * Stdev. Will return 0 for sets with < 2 values
	 * 
	 * @param values the values
	 * @param mean The mean
	 * @return the stdev
	 */
	public static float stdev (float [] values, float mean) {
		
		// We don't want infinite values so we provide silly answers
		// to silly questions.
		if (values.length < 2) return 0;
		
		float stdev = 0;
		
		for (int i=0;i<values.length;i++) {
			stdev += Math.pow(values[i]-mean, 2);
		}
		
		stdev /= values.length-1;

		stdev = (float)Math.sqrt(stdev);
		
		return stdev;
	}
	
	/**
	 * Stdev.  Will return 0 for sets with < 2 values
	 * 
	 * @param values the values
	 * @return the stdev
	 */
	public static double stdev (double [] values) {
		double mean = mean(values);
		return stdev(values,mean);
	}
	
	
	public static double max (double [] values) {
		if (values.length == 0) return 0;
		double max = values[0];
		for (int i=1;i<values.length;i++) {
			if (values[i]>max) max=values[i];
		}
		return max;
	}
	
	public static double percentile (double [] values, int percentile) {
		
		double [] sortedValues = Arrays.copyOf(values,values.length);
		Arrays.sort(sortedValues);
		
		int position = ((values.length-1)*percentile)/100;
		
		return sortedValues[position];
		
	}

	public static float percentile (float [] values, int percentile) {
		
		float [] sortedValues = Arrays.copyOf(values,values.length);
		Arrays.sort(sortedValues);
		
		int position = ((values.length-1)*percentile)/100;
		
		return sortedValues[position];
		
	}

	
}
