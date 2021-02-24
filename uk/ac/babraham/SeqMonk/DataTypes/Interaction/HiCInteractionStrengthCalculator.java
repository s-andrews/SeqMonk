/**
 * Copyright 2012- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataTypes.Interaction;

import org.apache.commons.math3.distribution.BinomialDistribution;

import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

public class HiCInteractionStrengthCalculator {

	private HiCDataStore dataSet;
	private boolean correctLinkage;
	
	// These variables are stored internally to allow the user to access the results
	// of the last interaction they calculated.
	private boolean isCis;
	private double probabilityOfCrossover;
	private double rawPValue;
	private double obsExp;
	
	
	public HiCInteractionStrengthCalculator (HiCDataStore dataSet, boolean correctLinkage) {
		this.dataSet = dataSet;
		this.correctLinkage = correctLinkage;
	}
	
	public void calculateInteraction (int interactionCount, int probe1CisCount, int probe1TransCount, int probe2CisCount, int probe2TransCount, Probe probe1, Probe probe2) {
		
//		System.err.println("Calculating interaction between "+probe1+" and "+probe2+" counts are "+probe1CisCount+","+probe1TransCount+","+probe2CisCount+","+probe2TransCount+","+interactionCount);
		
		isCis = probe1.chromosome().equals(probe2.chromosome());
		
		// Quick sanity check
		if (probe1CisCount+probe1TransCount == 0 || probe2CisCount+probe2TransCount == 0 || (isCis && dataSet.getCisCount() == 0) || ((!isCis) && dataSet.getTransCount() == 0)) {

			System.err.println("Bailing from comparison as there's no data");
			rawPValue = 1;
			probabilityOfCrossover = 0;
			obsExp = 1;
			return;
		}
		
		
		probabilityOfCrossover = getLikelihoodOfInteraction(interactionCount, probe1CisCount, probe1TransCount, probe2CisCount, probe2TransCount, probe1, probe2);
//		System.err.println("Crossover probability is "+probabilityOfCrossover);
		obsExp = getObsExp(interactionCount, probe1CisCount, probe1TransCount, probe2CisCount, probe2TransCount, probe1, probe2);
//		System.err.println("Obs/Exp is "+obsExp);
		rawPValue = getPValue(interactionCount, probe1CisCount, probe1TransCount, probe2CisCount, probe2TransCount, probe1, probe2);
//		System.err.println("P value is "+rawPValue);


		
	}
	
	/*
	 * These methods can be used by the user to read the results from the last interaction analysed.
	 */
	
	public double obsExp() {
		return obsExp;
	}
	
	public double rawPValue () {
		return rawPValue;
	}

	
	
	private double getLikelihoodOfInteraction (int interactionCount, int probe1CisCount, int probe1TransCount, int probe2CisCount, int probe2TransCount, Probe probe1, Probe probe2) {

		// We work out the expected frequency separately for cis and trans hits
		// since some datasets will have had trans removed all together, and
		// all datasets will have a completely variable cis/trans ratio
		
		double expected;
		
		if (isCis) {
						
			expected = probe2CisCount/(double)dataSet.getCisCountForChromosome(probe1.chromosome());
			
//			System.err.println("Proportion of cis read on chr "+probe2.chromosome()+" falling into probe 2 is "+expected);
			
			// If we're correcting linkage then work out the
			// appropriate correction factor.  We work out all of the 10kb 
			// chunks covered by this pair of probes and take the average of
			// these.
			
			int shortestDist = 0;
			int longestDist = 0;
			
			if (correctLinkage) {
				
				// Work out the range of distances covered by these probes
				int lowStart = Math.min(probe1.start(),probe2.start());
				int highStart = Math.max(probe1.start(),probe2.start());
				int lowEnd = Math.min(probe1.end(),probe2.end());
				int highEnd = Math.max(probe1.end(),probe2.end());
				
				shortestDist = highStart - lowEnd;
				longestDist = highEnd - lowStart;
				
				float correction = dataSet.getCorrectionForLength(probe1.chromosome(), shortestDist, longestDist);
				
//				System.err.println("Correcting "+shortestDist+" to "+longestDist+" gave correction of "+correction);
				
				expected *= correction;
							
			}
			
		}
		
		else {
			// We need to work out the chances of a trans hit coming up
			// anywhere			
			
			
			expected = probe2TransCount/(double)(dataSet.getTransCount()-dataSet.getTransCountForChromosome(probe1.chromosome()));

//			System.err.println("Proportion of trans reads falling into probe 2 is "+expected);

		}
		
		// After distance correction it's possible that we end up with a silly probability above 1.  Let's not do that.
		if (expected > 1) {
			expected = 1;
		}
		
		return expected;
	
	}
	
	
	private double getObsExp (int interactionCount, int probe1CisCount, int probe1TransCount, int probe2CisCount, int probe2TransCount, Probe probe1, Probe probe2) {
		
		if (probe1.chromosome().equals(probe2.chromosome())) {
									
			return interactionCount/(probe1CisCount*probabilityOfCrossover);
			
		}
		
		else {
			return interactionCount/(probe1TransCount*probabilityOfCrossover);
		}

	}
	
	private double getPValue (int interactionCount, int probe1CisCount, int probe1TransCount, int probe2CisCount, int probe2TransCount, Probe probe1, Probe probe2) {
		
		if (probe1.chromosome().equals(probe2.chromosome())) {
									
			BinomialDistribution bd = new BinomialDistribution(probe1CisCount, probabilityOfCrossover);
			
//			System.err.println("Significance is "+(1-bd.cumulativeProbability(interactionCount)));
			
			// Since the distribution gives us the probability of getting an interaction higher
			// than the one we observed we need to subtract 1 from it to get the probability of
			// this level of interaction or higher.
			return 1-bd.cumulativeProbability(interactionCount-1);
			
		}
		
		else {
			// We need to work out the chances of a trans hit coming up
			// anywhere

			BinomialDistribution bd = new BinomialDistribution(probe1TransCount, probabilityOfCrossover);
			
//			System.err.println("Significance is "+(1-bd.cumulativeProbability(interactionCount)));

			// Since the distribution gives us the probability of getting an interaction higher
			// than the one we observed we need to subtract 1 from it to get the probability of
			// this level of interaction or higher.
			return 1-bd.cumulativeProbability(interactionCount-1);
		}
			
	}

	
}
