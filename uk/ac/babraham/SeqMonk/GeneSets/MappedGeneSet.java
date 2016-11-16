/**
 * Copyright 2014-15 Laura Biggins
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
package uk.ac.babraham.SeqMonk.GeneSets;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

//import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

/** 
 * Used for storing a set of probes that have been matched against gene sets
 * 
 * @author bigginsl
 *
 */

public class MappedGeneSet{// implements Comparable<MappedGeneSet> {

	// The geneSet object
	private GeneSet geneSet;
	
	// The probes that match the genes in the geneSet
	private Probe [] probes;
	
	public double meanZScore;
	
	public String summaryStatsDescription; 
	
	// the z scores for all the probes
	public double [] zScores;
	
	private String name;
	
	//public int compareTo(MappedGeneSet z) {
	//	return Double.compare(Math.abs(z.meanZScore),Math.abs(meanZScore));
	//}
	
	public MappedGeneSet(Probe[] probes, GeneSet geneSet){
		
		this.geneSet = geneSet;
		this.probes = probes;
		this.name = geneSet.geneSetName();
	}
	
	public MappedGeneSet(ProbeList probeList){
		
		this.probes = probeList.getAllProbes();	
		this.name = probeList.name();
	}	
	
	public String name(){
		
		return name;
	}
	
	public GeneSet getGeneSet(){
		return geneSet;
	}
	
	public Probe [] getProbes(){
		return probes;
	}			
}
