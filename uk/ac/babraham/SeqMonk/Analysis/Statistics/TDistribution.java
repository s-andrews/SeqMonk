/**
 * Copyright Copyright 2010-18 Simon Andrews
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

/**
 * The Class TDistribution provides an implementation of the
 * statistical T-distribution.
 */
public class TDistribution {

	// This code is based on the perl Statistics::Distributions
	// module.  Blame them for any errors in the algorithms, but
	// blame me if it's coded wrong!
	
	/**
	 * T prob.
	 * 
	 * @param freedom the freedom
	 * @param tValue the t value
	 * @return the double
	 */
	public static double tProb (int freedom, double tValue) {
		
		// This bit is a bug in the original Perl code.  It breaks if
		// given a negative tValue.  Since the distribution is symmetrical
		// then we can just change the sign and move on.
		if (tValue < 0)
			tValue = 0-tValue;
		
		double w = Math.atan2((tValue/Math.sqrt(freedom)),1);
		double z = Math.pow(Math.cos(w),2);
		double y = 1;
		
		double a;
		double b;
		
		for (int i=freedom-2;i>=2;i-=2) {
			y = (double)1+(i-1) / (double)i*z*y;
		}
		
		if (freedom%2 == 0) {
			a = Math.sin(w)/2;
			b = 0.5;
		}
		else {
			a = (freedom == 1) ? 0 : Math.sin(w) * Math.cos(w)/Math.PI;
			b = 0.5 + w / Math.PI;
		}
				
		double p = 1-b-a*y;
		if (p < 0) {
//			System.err.println(p+" was less than 0");
			return 0;
		}
		else {
			// This p-value is for a 1-tailed test, we always want
			// a 2-tailed result so we must double it.
			return p*2;
		}
	}
	
	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main (String [] args) {
		System.out.println(TDistribution.tProb(5,1.8184));
	}
}
