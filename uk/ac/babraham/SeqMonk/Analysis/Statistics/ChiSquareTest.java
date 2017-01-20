/**
 * Copyright 2013-17 Simon Andrews
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

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

public class ChiSquareTest {

	public static double chiSquarePvalue(int [][] table) {
		
		// We assume that all elements of table have the same number of elements in them
		int [] rowTotals = new int [table.length];
		int [] colTotals = new int [table[0].length];
		int grandTotal = 0;
		
		for (int i=0;i<table.length;i++) {
			for (int j=0;j<table[i].length;j++) {
				rowTotals[i] += table[i][j];
				colTotals[j] += table[i][j];
				grandTotal += table[i][j];
			}
		}
		
		double [] colProportions = new double [colTotals.length];
		for (int c=0;c<colTotals.length;c++) {
			colProportions[c] = ((double)colTotals[c])/grandTotal;
		}
		
		double chiSquare = 0;
		
		for (int r=0;r<table.length;r++) {
			for (int c=0;c<table[r].length;c++) {
				double expected = rowTotals[r]*colProportions[c];
				chiSquare += Math.pow(expected-table[r][c],2)/expected;
			}
		}
		
		ChiSquaredDistribution dist = new ChiSquaredDistribution((colTotals.length-1)*(rowTotals.length-1));
		
		return 1-dist.cumulativeProbability(chiSquare);
		
	}
	
}
