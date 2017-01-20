/**
 * Copyright 2014-17 Laura Biggins
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/** 
 * Contains all of the GeneSet objects.
 * 
 * @author bigginsl
 *
 */


public class GeneSetCollection {
	
	private GeneSet [] geneSets;
	
	public GeneSetCollection(GeneSet [] geneSets){
		
		this.geneSets = geneSets;
		
	}
	
	public GeneSet [] getGeneSets(){
		return geneSets;
	}
	
	/** To get all of the gene sets associated with one probe list. 
	 * 
	 * This needs optimising, the matching takes too long.
	 * */
	public MappedGeneSet [] getMappedGeneSets (Probe [] probes) {	
		
		System.err.println("At start of getMappedGeneSets method: " + new Timestamp(new java.util.Date().getTime()));
		
		// This will be the subset of all the geneSets that the probes map to.
		ArrayList <MappedGeneSet> mappedGeneSetArrayList = new ArrayList<MappedGeneSet>();
	
		
		Hashtable<String, Probe>probeLookupTable = new Hashtable<String, Probe>();
		
		// Let's create stripped probenames here rather than doing it many many times in the matching loop. 
		for (int p=0;p<probes.length;p++) {
			String probeName = probes[p].name();
			
			// try and remove extra text
			//probeName = probeName.replaceAll(probes[p].locationString(),"");
			
			probeName = probeName.replaceFirst("_upstream$", "").replaceAll("_downstream$", "").replaceAll("_gene$", "");
			probeName = probeName.replaceAll("-\\d\\d\\d$", "");
			
			
			//System.err.println("stripped probename = " + probeName);
			probeName = probeName.toUpperCase();
			probeLookupTable.put(probeName,probes[p]);
		}	
		
		
		// we don't want duplicates of geneSet objects within a probe list, so we're iterating through the geneSets
		// then through the probes
		for (int gs=0; gs<geneSets.length; gs++){
			
			// the probes that match
			ArrayList <Probe> probeArrayList = new ArrayList<Probe>();
			
			// the gene names in the gene set
			String [] geneNames = geneSets[gs].geneNames();
			
			// Let's not bother if there are less than 3 genes in a category
			if (geneNames.length < 3){
				continue;
			}
			
			// iterate through each gene in the geneSet
			for (int g=0; g<geneNames.length; g++){
				
				if (probeLookupTable.containsKey(geneNames[g])) {
					probeArrayList.add(probeLookupTable.get(geneNames[g]));
				}				
			}
			// create the mappedGeneSet - there's not much point in having a category that only has one gene in it. Might actually want to increase the minimum number of genes.
			if(probeArrayList.size() > 2){
				
				Probe [] matchedProbes = probeArrayList.toArray(new Probe[0]);
				MappedGeneSet mappedGS = new MappedGeneSet(matchedProbes, geneSets[gs]); 	
				mappedGeneSetArrayList.add(mappedGS);
			}
						
		}
		System.err.println("At end of getMappedGeneSets method in calculateCoordinates: " + new Timestamp(new java.util.Date().getTime()));
		// does size = 0 when there's nothing in it?
		if (mappedGeneSetArrayList.size()==0){
			return null;
		}
		else{
			MappedGeneSet [] mappedGeneSetArray = mappedGeneSetArrayList.toArray(new MappedGeneSet[0]);
			return(mappedGeneSetArray);
		}	
		
		
	}	
}
