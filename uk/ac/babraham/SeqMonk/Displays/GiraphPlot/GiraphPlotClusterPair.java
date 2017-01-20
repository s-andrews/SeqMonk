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
package uk.ac.babraham.SeqMonk.Displays.GiraphPlot;
import java.util.Vector;

public class GiraphPlotClusterPair {

	private GiraphPlotClusterPair pair1 = null;
	private GiraphPlotClusterPair pair2 = null;
	//private ProbeList probelist1 = null;
	private Integer probeListIndex = null;
	private float rValue;
	
	/** This is used when setting up the initial GiraphPlotClusterPairs and forms the bottom level of the tiers */ 
	//public GiraphPlotClusterPair (ProbeList sinprobelisteProbeList) {
	public GiraphPlotClusterPair (Integer x) {	
		//this.probelist1 = sinprobelisteProbeList;
		this.probeListIndex = x;
		this.rValue = 1;
	}
	
	public GiraphPlotClusterPair (GiraphPlotClusterPair pair1, GiraphPlotClusterPair pair2, float rValue) {
		this.pair1 = pair1;
		this.pair2 = pair2;
		this.rValue = rValue;
	}
	
	protected GiraphPlotClusterPair pair1 () {
		return pair1;
	}
	
	protected GiraphPlotClusterPair pair2 () {
		return pair2;
	}
	
	protected Integer probeListIndex () {
		return probeListIndex;
	}

	public float rValue () {
		return rValue;
	}
			
	/**
	 * method to obtain a vector of cluster pairs for a given r value  
	 */
	 /** this should iterate through all the cluster pairs, getting all the genelists and
	  * cluster pairs. probelist has to be passed in as a parameter because it keeps getting added to each time the
	  * method is run. It has to return probelist as a vector so it can iterate.
	  * If we want to turn it into an array afterwards that can be done outside of the method.
	  * 
	  */
	 private Vector<GiraphPlotClusterPair> getGiraphPlotClusterPairs(Vector<GiraphPlotClusterPair> cp, GiraphPlotClusterPair inputGiraphPlotClusterPair, float rValueCutoff){
			 
		 if(inputGiraphPlotClusterPair.rValue() >= rValueCutoff){
			 cp.add(inputGiraphPlotClusterPair);				 
		 }
		
		 else{
			 getGiraphPlotClusterPairs(cp, inputGiraphPlotClusterPair.pair1(), rValueCutoff);
			 getGiraphPlotClusterPairs(cp, inputGiraphPlotClusterPair.pair2(), rValueCutoff);
		 }
		 return cp;
	 }
	 
	 /**
	  * returns all gene lists that are in a cluster pair
	  * @param probelist
	  * @param cp
	  * @return
	  */
	 protected Vector<Integer> getAllProbeListIndicesInGiraphPlotClusterPair(Vector<Integer> probeListIndices, GiraphPlotClusterPair cp){
		 
		 if(cp.probeListIndex() != null){
			 probeListIndices.add(cp.probeListIndex());
		 }
		 // hopefully this won't just go off down one path and not get all the gene lists
		 else{
			 getAllProbeListIndicesInGiraphPlotClusterPair(probeListIndices, cp.pair1());
			 getAllProbeListIndicesInGiraphPlotClusterPair(probeListIndices, cp.pair2());
		 }
		 return probeListIndices;
	 }	 
	 
	 
	 /**
	  * This returns an array of probeList indices.
	  * @param rValueCutoff
	  * @return
	  */
	 public Integer[][] getClusters(float rValueCutoff){		 
		 
		 // we need a new vector of cluster pairs which all have the right rValue
		Vector<GiraphPlotClusterPair> GiraphPlotClusterPairsCorrectRValue = getGiraphPlotClusterPairs(new Vector<GiraphPlotClusterPair>(), this, rValueCutoff);
		 
		Vector<Vector<Integer>> vecIndices = new Vector<Vector<Integer>>(); 
		 
		for (int i = 0; i < GiraphPlotClusterPairsCorrectRValue.size(); i++){
			 
			if(getAllProbeListIndicesInGiraphPlotClusterPair(new Vector<Integer>(), GiraphPlotClusterPairsCorrectRValue.get(i)).size() > 0){

				vecIndices.add(getAllProbeListIndicesInGiraphPlotClusterPair(new Vector<Integer>(), GiraphPlotClusterPairsCorrectRValue.get(i)));
			}	 
		}
		 
		Integer [][] indices = new Integer[vecIndices.size()][];
		for (int i = 0; i < vecIndices.size(); i++){
			indices[i] = vecIndices.get(i).toArray(new Integer[0]);	 			
		}	
	//	System.out.println("no of clusters from valid ProbeLists for rValue " + rValueCutoff + "  =  " + ProbeList.length);
		 
		return indices;
	}	 
	
	 		 
}

