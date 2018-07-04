/**
 * Copyright 2013-18 Simon Andrews
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

import uk.ac.babraham.SeqMonk.SeqMonkException;

public class TTest {

	/**
	 * Calculate p value.
	 * 
	 * @param values1 the first data set
	 * @param values2 the second data set
	 * @return the p-value
	 * @throws SeqMonkException if there isn't enough data
	 */
	public static double calculatePValue (double [] values1, double [] values2) throws SeqMonkException {

		if (values1 == null  || values2 == null) {
			throw new SeqMonkException ("Lists of values for t-test cannot be null");
		}
		if (values1.length < 2 || values2.length < 2) {
			throw new SeqMonkException("At least two values are required in each set for a t-test");
		}

		double mean1 = 0;
		for (int i=0;i<values1.length;i++){
			mean1 += values1[i];
		}
		mean1 /= values1.length;
		
		double var1 = 0;
		for (int i=0;i<values1.length;i++) {
			var1 += Math.pow((values1[i]-mean1),2);
		}
		var1 /= (values1.length-1);

		double mean2 = 0;
		for (int i=0;i<values2.length;i++){
			mean2 += values2[i];
		}
		mean2 /= values2.length;
		
		double var2 = 0;
		for (int i=0;i<values2.length;i++) {
			var2 += Math.pow((values2[i]-mean2),2);
		}
		var2 /= (values2.length-1);

		double tValue = ((mean1-mean2)/Math.sqrt((var1/values1.length)+(var2/values2.length)));
				
		if (tValue < 0)
			tValue = 0-tValue;

		double pValue = TDistribution.tProb((values1.length+values2.length)-2,tValue);

		return pValue;
	}
	
	
	public static double calculatePValue (double mean1, double mean2, double sd1, double sd2, int n1, int n2) {
		
		double var1 = sd1*sd1;
		
		double var2 = sd2*sd2;

		double tValue = ((mean1-mean2)/Math.sqrt((var1/n1)+(var2/n2)));
				
		if (tValue < 0)
			tValue = 0-tValue;

		double pValue = TDistribution.tProb((n1+n2)-2,tValue);

		return pValue;
	}
	
	
	
	/**
	 * Calculate p value.
	 * 
	 * @param values the values
	 * @param testBase the static value to compare against
	 * @return the raw p-value
	 * @throws SeqMonkException if not enough data is found.
	 */
	public static double calculatePValue (double [] values, double testBase) throws SeqMonkException {

		if (values == null) {
			throw new SeqMonkException ("List of values for t-test cannot be null");
		}
		if (values.length < 2) {
			throw new SeqMonkException("At least two values are required for a t-test");
		}
		double mean =0;
		for (int i=0;i<values.length;i++){
			mean += values[i];
		}
		mean /= values.length;
		
		double stdev = 0;
		for (int i=0;i<values.length;i++) {
			stdev += Math.pow((values[i]-mean),2);
		}
		stdev /= (values.length-1);
		stdev = Math.sqrt(stdev);
		
		double sem = stdev / (Math.sqrt(values.length));
		
//		System.out.println("SEM="+sem+" Mean="+mean+" Test="+testBase);
		
		double tValue = (mean-testBase)/sem;
		
		double pValue = TDistribution.tProb(values.length-1,tValue);
		
		return pValue;
	}
	
}
