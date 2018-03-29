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
 * The Class FDistribution provides an implementation of the standard
 * statistical F Distribution
 */
public class FDistribution {

	// This code is based on the perl Statistics::Distributions
	// module.  Blame them for any errors in the algorithms, but
	// blame me if it's coded wrong!
	
	/**
	 * F prob.
	 * 
	 * @param nf degrees of freedom in the numberator
	 * @param df degrees of freedom in the denominator
	 * @param fRatio the f ratio
	 * @return the F probability
	 */
	public static double fProb (int nf, int df, double fRatio) {
		
		double p=0;
		
		if (fRatio <=0) {
			p=1;
		}
		
		else if (df % 2 == 0) {
			double z = df / (df+nf*fRatio);
//			System.out.println("Z is"+z);
			double a = 1;
			for (int i=df-2;i>=2;i-=2) {
//				System.out.println("Before: a="+a+" i="+i);
				a=1+((nf+i-2d)/i)*z*a;
//				System.out.println("After: a="+a);
			}
			p = 1 - (Math.pow((1-z),(nf/2d))*a);
		}
		else if (nf % 2 == 0) {
			double z = nf *fRatio / (df+nf*fRatio);
//			System.out.println("Z is"+z);
			double a = 1;
			for (int i=nf-2;i>=2;i-=2) {
//				System.out.println("Before: a="+a+" i="+i);
				a=1+((df+i-2d)/i)*z*a;
//				System.out.println("After: a="+a);
			}
			p = Math.pow((1-z),(df/2d))*a;
		}
		else {
			double y =Math.atan2(Math.sqrt(nf*fRatio/df), 1);
//			System.out.println("Y is"+y);
			double z =Math.pow(Math.sin(y),2);
//			System.out.println("Z is"+z);
			double a =(nf==1) ? 0 : 1;
			for (int i=nf-2;i>=3;i -=2) {
//				System.out.println("Before: a="+a+" i="+i);				
				a=1+((df+i-2d)/i)*z*a;
//				System.out.println("After: a="+a);
			}
			double b=Math.PI;
			for (int i=2;i<=df-1;i+=2) {
//				System.out.println("Before: b="+b+" i="+i);				
				b *= (i-1d)/i;
//				System.out.println("After: b="+b);
			}
			double p1 = 2d/b*Math.sin(y)*Math.pow(Math.cos(y),df)*a;
//			System.out.println("P1="+p1);

			z=Math.pow(Math.cos(y),2);
//			System.out.println("Z="+z);

			a= (nf==1) ? 0:1;
			for (int i=df-2;i>=3;i-=2) {
//				System.out.println("Before: a="+a+" i="+i);				
				a= 1+(i-1d) / i*z*a;
//				System.out.println("After: a="+a);
			}
			double pOption = p1+1-2*y/Math.PI-2/Math.PI*Math.sin(y)*Math.cos(y)*a;
			
			if (pOption > 0) {
				p = pOption;
			}
			else {
				p=0;
			}
		}

//		System.out.println("P is "+p);
		return p;
	}
	
	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 */
	public static void main (String [] args) {
		System.out.println(fProb(9, 7, 0.625));
	}
}
