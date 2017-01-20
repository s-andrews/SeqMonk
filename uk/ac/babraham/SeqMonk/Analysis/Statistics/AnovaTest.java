/**
 * Copyright Copyright 2010-17 Simon Andrews
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

/**
 * The Class AnovaTest provides an implementation of an ANOVA statistical test.
 */
public class AnovaTest {

	/**
	 * Calculates an ANOVA p value.
	 * 
	 * @param data the data
	 * @return the p-value
	 * @throws SeqMonkException if there isn't enough data
	 */
	public static double calculatePValue (double [][] data) throws SeqMonkException {

		//Calculate sample means for each group and a grand mean for everything
		double [] sampleMeans = new double[data.length];
		double grandMean = 0;
		int grandCount = 0;
		
		for (int i=0;i<data.length;i++) {
			for (int j=0;j<data[i].length;j++) {
				grandMean+=data[i][j];
				sampleMeans[i]+=data[i][j];
				++grandCount;
			}
			sampleMeans[i] /= data[i].length;
//			System.out.println("Sample mean for group "+i+"="+sampleMeans[i]);
		}
		grandMean /= grandCount;
		
//		System.out.println("Grand mean="+grandMean+" from "+grandCount+" samples");
		
		double grandSumOfSquares = 0;
		
		for (int i=0;i<data.length;i++) {
			for (int j=0;j<data[i].length;j++) {
				grandSumOfSquares += Math.pow(data[i][j]-grandMean, 2);
			}	
		}
		
//		System.out.println("GSS="+grandSumOfSquares);
		
		double sumOfSqaresOfGroupMeans = 0;
		
		for (int i=0;i<sampleMeans.length;i++) {
			sumOfSqaresOfGroupMeans += data[i].length * Math.pow(sampleMeans[i]-grandMean,2);
		}
		
//		System.out.println("Sum of square means="+sumOfSqaresOfGroupMeans);
		
		double averageSumOfSquaresOfGroupMeans = sumOfSqaresOfGroupMeans/(sampleMeans.length -1);
		
//		System.out.println("Average sum of square means="+averageSumOfSquaresOfGroupMeans);
		
		double withinGroupVariability = grandSumOfSquares - sumOfSqaresOfGroupMeans;

//		System.out.println("Within Group Variability="+withinGroupVariability);
		
		double averageWithinGroupVariability = withinGroupVariability / (grandCount-data.length);
		
//		System.out.println("Average Within Group Variability="+averageWithinGroupVariability);

		
		double fRatio = averageSumOfSquaresOfGroupMeans/averageWithinGroupVariability;
		
//		System.out.println("F-ratio="+fRatio);
		
		double pValue = FDistribution.fProb(data.length-1, grandCount-data.length, fRatio);
		
//		System.out.println("P-value="+pValue);
		return pValue;
	}	
	
	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main (String [] args) {
//		double [][] data = new double [][] {
//			{0.64,1.52,0.98,0.17,0.75,1.48,1.18,0.33,1.42},
//			{1.37,1.23,0.26,0.47,0.42,0.64,0.32,0.65,0.43,0.67},
//			{0.43,0.70,0.79,0.89,0.24,0.25,1.01,0.77,0.47,0.47},
//			{0.46,0.65,0.41,0.81,1.20,1.08,0.34,1.98,1.39,1.12},
//			{3.14,2.78,1.04,2.78,0.82,1.65,0.49,0.97,1.39,3.24},
//		};
		
		double [][] data = new double [][] {
				{3,2,1,1,4},
				{5,2,4,2,3},
				{7,4,5,3,6},
		};

		
		try {
			calculatePValue(data);
		} catch (SeqMonkException e) {
			e.printStackTrace();
		}
		
	}
	
}
