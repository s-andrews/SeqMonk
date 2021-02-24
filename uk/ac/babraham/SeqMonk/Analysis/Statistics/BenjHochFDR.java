/**
 * Copyright Copyright 2010- 21 Simon Andrews
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
 * The Class BenjHochFDR provides false discovery rate correction
 * to a set of calculated p-values.
 */
public class BenjHochFDR {

	/**
	 * Calculate q values.  The t test values which are provided
	 * as input are modified in place.
	 * 
	 * @param tTestValues the t test values
	 */
	public static void calculateQValues (ProbeTTestValue [] tTestValues) {
		Arrays.sort(tTestValues);
		
		for (int i=0;i<tTestValues.length;i++) {
			tTestValues[i].q = tTestValues[i].p * ((double)(tTestValues.length)/(i+1));
			
			if (i>0 && tTestValues[i].q < tTestValues[i-1].q) {
				tTestValues[i].q = tTestValues[i-1].q;
			}
			
//			System.out.println("P-value "+tTestValues[i].p+" at index "+i+" with total length "+tTestValues.length+" converted to "+tTestValues[i].q);
		}
		
	}
	
	/**
	 * Calculate q values.  The t test values which are provided
	 * as input are modified in place.
	 * 
	 * @param tTestValues the t test values
	 */
	public static void calculateQValues (IndexTTestValue [] tTestValues) {
		Arrays.sort(tTestValues);
		
		for (int i=0;i<tTestValues.length;i++) {
			tTestValues[i].q = tTestValues[i].p * ((double)(tTestValues.length)/(i+1));

			if (i>0 && tTestValues[i].q < tTestValues[i-1].q) {
				tTestValues[i].q = tTestValues[i-1].q;
			}

			
			//			System.out.println("P-value "+tTestValues[i].p+" at index "+i+" with total length "+tTestValues.length+" converted to "+tTestValues[i].q);
		}
		
	}

	/**
	 * Calculate q values.  The t test values which are provided
	 * as input are modified in place.
	 * 
	 * @param tTestValues the t test values
	 */
	public static void calculateQValues (ProbeGroupTTestValue [] tTestValues) {
		Arrays.sort(tTestValues);
		
		for (int i=0;i<tTestValues.length;i++) {
			tTestValues[i].q = tTestValues[i].p * ((double)(tTestValues.length)/(i+1));

			if (i>0 && tTestValues[i].q < tTestValues[i-1].q) {
				tTestValues[i].q = tTestValues[i-1].q;
			}

			
			//			System.out.println("P-value "+tTestValues[i].p+" at index "+i+" with total length "+tTestValues.length+" converted to "+tTestValues[i].q);
		}
		
	}

	/**
	 * Calculate q values.  The t test values which are provided
	 * as input are modified in place.
	 * 
	 * @param tTestValues the t test values
	 */
	public static void calculateQValues (MappedGeneSetTTestValue [] tTestValues) {
		Arrays.sort(tTestValues);
		
		for (int i=0;i<tTestValues.length;i++) {
			tTestValues[i].q = tTestValues[i].p * ((double)(tTestValues.length)/(i+1));
			if (i>0 && tTestValues[i].q < tTestValues[i-1].q) {
				tTestValues[i].q = tTestValues[i-1].q;
			}

			if(i % 500 == 0){
			System.out.println("P-value "+tTestValues[i].p+" at index "+i+" with total length "+tTestValues.length+" converted to "+tTestValues[i].q);
			}
		}
	}
}
