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
package uk.ac.babraham.SeqMonk.Analysis.Correlation;

import uk.ac.babraham.SeqMonk.SeqMonkException;

/**
 * A Class to calculate the Pearson Correlation.
 */
public class PearsonCorrelation {

	
	/**
	 * Calculate correlation.
	 * 
	 * @param data1 the first dataset
	 * @param data2 the second dataset
	 * @return the Pearson r-value
	 * @throws SeqMonkException if the two datasets don't have the same number of points in them.
	 */
	public static float calculateCorrelation (int [] data1, int [] data2) throws SeqMonkException {

		double [] d1 = new double[data1.length];
		double [] d2 = new double[data2.length];
		for (int i=0;i<data1.length;i++)d1[i] = data1[i];
		for (int i=0;i<data2.length;i++)d2[i] = data2[i];
		
		return calculateCorrelation(d1, d2);
		
	}
	/**
	 * Calculate correlation.
	 * 
	 * @param data1 the first dataset
	 * @param data2 the second dataset
	 * @return the Pearson r-value
	 * @throws SeqMonkException if the two datasets don't have the same number of points in them.
	 */
	public static float calculateCorrelation (double [] data1, double [] data2) throws SeqMonkException {
	
		if (data1.length != data2.length) {
			throw new SeqMonkException("Data sets must be the same length when calculating correlation");
		}
		
		double sum12 = 0;
		double sum1 = 0;
		double sum2 = 0;
		double sum1square = 0;
		double sum2square =0;

		int usableCount = 0;
		
		for (int i=0;i<data1.length;i++) {
			if (Double.isNaN(data1[i]) || Double.isNaN(data2[i])) {
				continue;
			}
			++usableCount;

			sum12 += data1[i]*data2[i];
			sum1 += data1[i];
			sum2 += data2[i];
			sum1square += data1[i]*data1[i];
			sum2square += data2[i]*data2[i];
		}
		
		double top = sum12 - ((sum1*sum2)/usableCount);
		double bottomRight = sum2square - ((sum2*sum2)/usableCount);
		double bottomLeft = sum1square - ((sum1*sum1)/usableCount);
		double bottom = Math.sqrt(bottomLeft * bottomRight);
		
		
		return (float)(top/bottom);
	}
	
	/**
	 * Calculate correlation.
	 * 
	 * @param data1 the first dataset
	 * @param data2 the second dataset
	 * @return the Pearson r-value
	 * @throws SeqMonkException if the two datasets don't have the same number of points in them.
	 */
	public static float calculateCorrelation (float [] data1, float [] data2) throws SeqMonkException {
	
		if (data1.length != data2.length) {
			throw new SeqMonkException("Data sets must be the same length when calculating correlation");
		}
		
		int usableCount = 0;
		
		double sum12 = 0;
		double sum1 = 0;
		double sum2 = 0;
		double sum1square = 0;
		double sum2square =0;
		
		for (int i=0;i<data1.length;i++) {
			if (Float.isNaN(data1[i]) || Float.isNaN(data2[i])) {
				continue;
			}
			++usableCount;
			sum12 += data1[i]*data2[i];
			sum1 += data1[i];
			sum2 += data2[i];
			sum1square += data1[i]*data1[i];
			sum2square += data2[i]*data2[i];
		}
		
		double top = sum12 - ((sum1*sum2)/usableCount);
		double bottomRight = sum2square - ((sum2*sum2)/usableCount);
		double bottomLeft = sum1square - ((sum1*sum1)/usableCount);
		double bottom = (float)Math.sqrt(bottomLeft * bottomRight);
		
		
		return (float)(top/bottom);
	}
	
	
		
}
