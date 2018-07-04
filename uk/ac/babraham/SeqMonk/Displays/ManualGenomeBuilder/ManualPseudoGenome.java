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
package uk.ac.babraham.SeqMonk.Displays.ManualGenomeBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

public class ManualPseudoGenome {

	private int numberOfChromosomes;
	private Vector<ManualGenomeChromosome[]> scaffoldsInPseudoChromosomes = new Vector<ManualGenomeChromosome[]>();
	
	public ManualPseudoGenome (int numberOfChromosomes, ManualGenome genome) {

		this.numberOfChromosomes = numberOfChromosomes;
		
		// First get all of the chromosomes and get a total genome length
		ManualGenomeChromosome [] chrs = genome.getChromosomes();
		
		long totalLength = chrs.length*500; // We're going to put 500bp gap between scaffolds
		
		for (int i=0;i<chrs.length;i++) {
			totalLength += chrs[i].length();
		}
		
		int targetLength = 1+ (int)(totalLength / numberOfChromosomes);
		
		// Now we can go through assigning chromosomes to pseudo chromosomes
		
		int currentPseudoNumber = 1;
		int currentPseudoLength = 0;
		int currentPseudoScaffoldCount = 0;
		Vector<ManualGenomeChromosome> chrsInThisPseudoChromsome = new Vector<ManualGenomeChromosome>();
		
		for (int i=0;i<chrs.length;i++) {
			
			if (currentPseudoLength+chrs[i].length() > targetLength && currentPseudoScaffoldCount > 0) {
				// Start a new pseudo chromosome
				currentPseudoNumber++;
				currentPseudoLength = 0;
				currentPseudoScaffoldCount = 0;
				scaffoldsInPseudoChromosomes.add(chrsInThisPseudoChromsome.toArray(new ManualGenomeChromosome[0]));
				chrsInThisPseudoChromsome.clear();
			}
			
			chrs[i].setPseudoChromosome("pseudo"+currentPseudoNumber, currentPseudoLength);
			currentPseudoLength += 100+chrs[i].length();
			currentPseudoScaffoldCount++;
			chrsInThisPseudoChromsome.add(chrs[i]);
			
		}
		if (chrsInThisPseudoChromsome.size()>0) {
			scaffoldsInPseudoChromosomes.add(chrsInThisPseudoChromsome.toArray(new ManualGenomeChromosome[0]));
		}
	}
	
	public int numberOfChromosomes () {
		return numberOfChromosomes;
	}
	
	public void writeChrList (PrintWriter pr) {
//		System.err.println("Writing out chrlist");
		for (int i=0;i<scaffoldsInPseudoChromosomes.size();i++) {
			int pseudoLength = (scaffoldsInPseudoChromosomes.elementAt(i)[scaffoldsInPseudoChromosomes.elementAt(i).length-1].pseudoChromosomeOffset()+scaffoldsInPseudoChromosomes.elementAt(i)[scaffoldsInPseudoChromosomes.elementAt(i).length-1].length());
//			System.err.println("Writing out "+scaffoldsInPseudoChromosomes.elementAt(i)[0].pseudoChromosomeName()+"\t"+pseudoLength);
			pr.println(scaffoldsInPseudoChromosomes.elementAt(i)[0].pseudoChromosomeName()+"\t"+pseudoLength);
		}
	}	
	
	public void writeScaffoldAlises(File baseFolder) throws IOException {
		PrintWriter pr = new PrintWriter(new File(baseFolder+"/aliases.txt"));
		PrintWriter gff = new PrintWriter(new File(baseFolder+"/scaffolds.gff"));
		
		for (int i=0;i<scaffoldsInPseudoChromosomes.size();i++) {
			for (int j=0;j<scaffoldsInPseudoChromosomes.elementAt(i).length;j++) {
				ManualGenomeChromosome c = scaffoldsInPseudoChromosomes.elementAt(i)[j];
				
				pr.println(c.name()+"\t"+c.pseudoChromosomeName()+"\t"+c.pseudoChromosomeOffset());
				
				char strand = '+';
				if (j%2==0) strand = '-';
				
				gff.println(c.pseudoChromosomeName()+"\t"+"seqmonk\tscaffold\t"+(c.pseudoChromosomeOffset()+1)+"\t"+(c.pseudoChromosomeOffset()+c.length()+"\t.\t"+strand+"\t.\tname="+c.name()));
			}
		}
		
		pr.close();
		gff.close();
	}
	
}
