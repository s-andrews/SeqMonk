/**
 * Copyright 2014-15-15 Laura Biggins
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

/**
 *  Produces clusters using supplied coordinates. 
 * 
 * @author bigginsl
 *
 */

public class GiraphPlotCluster implements Runnable{ 
		
	private Vector<GiraphPlotClusterPair> clusterPairs;
		
	protected boolean clustersReady = false;
	
	private Integer [] probeListIndices;
	
	private float [] xCoordinates;
	private float [] yCoordinates;
	
	public GiraphPlotCluster(float [] xCoordinates, float [] yCoordinates){
		
		this.xCoordinates = xCoordinates;
		this.yCoordinates = yCoordinates;
		
		probeListIndices = new Integer[xCoordinates.length];
		
		clusterPairs = new Vector<GiraphPlotClusterPair>();
		
		/** make an array of indices and also the initial cluster pairs */
		for(int i=0; i<xCoordinates.length; i++){
			probeListIndices[i] = i;				
			clusterPairs.add(new GiraphPlotClusterPair((Integer)i));
		}
		
		Thread clusterThread = new Thread(this);
		clusterThread.start();
	}
	 
	protected GiraphPlotClusterPair clusterPair(){
		// this should be 1 as clustersReady is only set to true if so 
		if(clusterPairs.size() == 1){
			return (clusterPairs.get(0));
		}
		System.err.println("something went wrong with the clustering, we ended up with more than one cluster");
		return null;
	}
	
	public void run(){
		
//		java.util.Date date= new java.util.Date();
		//System.out.println("At start of run method in Cluster: " + new Timestamp(date.getTime()));
		
		findNextHighestCorrelation();
		
		if(clusterPairs.size() == 1){
			clustersReady = true;
		}
		else{
			// throw an error
			System.err.println("The clustering appears not to have completed, number of cluster pairs = " + clusterPairs.size());
		}
		//System.err.println("At end of run method in Cluster: " + new Timestamp(new java.util.Date().getTime()));
	}
			
	 /** this needs to iterate through all the cluster pairs
	  * 
	  * to start with it's going to check everything, even if it's been checked before. I don't know if there's an efficient way to set a flag to say
	  * if I've already done that exact comparison.  
	  */	 
	 private void findNextHighestCorrelation(){
		 
		 /** have lastMax variable so that if we hit on an r value that is the same as the last highest one found, we
		  * might as well just go with that than searching through the whole lot 
		  */
		 float lastMaxRValue = 0;
		
		 WHILE_LOOP: while (clusterPairs.size() > 1){ 
		 
			Float meanCorr = (float) 0;
			Integer [] probeListIndicesInClusterPair1;
			Integer [] probeListIndicesInClusterPair2;
			Float meanCorrTemp;
			int index1 = 0;
			int index2 = 1;	
			
			/**
			 * loop through comparing everything against everything in the clusterPairs vector 
			 */
			 for (int i = 0; i < (clusterPairs.size() -1); i++){					
				 
				 probeListIndicesInClusterPair1 = clusterPairs.get(i).getAllProbeListIndicesInGiraphPlotClusterPair(new Vector<Integer>(), clusterPairs.get(i)).toArray(new Integer[0]);
				 
				 for (int j = i+1; j < clusterPairs.size(); j++){
						
					probeListIndicesInClusterPair2 = clusterPairs.get(j).getAllProbeListIndicesInGiraphPlotClusterPair(new Vector<Integer>(), clusterPairs.get(j)).toArray(new Integer[0]);	
				
					float d = getMeanDistance(probeListIndicesInClusterPair1, probeListIndicesInClusterPair2);
					meanCorrTemp = 1-d;
					
					if(meanCorrTemp > meanCorr){						
						meanCorr = meanCorrTemp;
						index1 = i;
						index2 = j;								
						if((meanCorr == 1) || (meanCorr == lastMaxRValue)){
				
						/**
						 *  we're not going to find a higher r value so might as well use this one	
						 */
							makeNewClusterPair(index1, index2, meanCorr);
							lastMaxRValue = meanCorr;
							continue WHILE_LOOP;
						}						
					}
				}
			 }
			/**
			 * we've got to the end of this iteration of the while loop so we create a new ClusterPair with the two 
			 * cluster pairs that were the most highly correlated. If the highest correlation was 0, the indices would be 0 and 1 
			 * from when they were declared. This is fine, because these will be clustered together, added to the end of the vector,
			 * and removed from the front.   
			 */			 
			makeNewClusterPair(index1, index2, meanCorr);
			lastMaxRValue = meanCorr;	
			
		 }				 			
	 }
	 
	 private void makeNewClusterPair(int index1, int index2, float rValue){
		 		 
		 /** create the new cluster pair */
		 GiraphPlotClusterPair newClusterPair = new GiraphPlotClusterPair(clusterPairs.get(index1), clusterPairs.get(index2), rValue);
		 /** remove the old clusterPairs from the vector as they should now be held within the new ClusterPair object */		
		 removeClusterPairs(index1, index2);
		 //maybe this should be added to the front of the vector?
		 clusterPairs.add(newClusterPair); 
		
		// System.out.println("made new cluster pair with r value of " + rValue + ", cluster pair length is now " + clusterPairs.size());
	 }	 		  

	 // removes the 2 cluster pairs so that they can be added as one later on
	 private void removeClusterPairs(int index1, int index2){
		 // maybe index 1 is always less than index 2 and this if statement is useless?
		 if(index1 < index2){
			 clusterPairs.remove(index1);		
			 // this has to be -1 because index 1 has been removed
			 clusterPairs.remove(index2 -1);			 
		 }
		 else { 
			 clusterPairs.remove(index2);		
			 // this has to be -1 because index 2 has been removed
			 clusterPairs.remove(index1 -1);
		 }
	 }		
	 
	 /** This is going to calculate the clusters using the distances rather than the overlap
	  * to try and make the colours match what you actually see on the screen.
	  */
	private float getMeanDistance(Integer[] probelist1Indices, Integer[] probelist2Indices){	 
		 float distSum = 0;
		 
		 if(probelist1Indices.length == 1 && probelist2Indices.length == 1){
			 
			 return getDistanceBetween2points(probelist1Indices[0], probelist2Indices[0]);
		 }
		 
		 for (int i = 0; i < probelist1Indices.length; i++){
				
				float distTemp = 0;
				
				for (int j = 0; j < probelist2Indices.length; j++){

					// sum up all distances for probelist1Indices[i] against each ProbeList in probelist2Indices 
					distTemp = distTemp + getDistanceBetween2points(probelist1Indices[i], probelist2Indices[j]);					
				}
				// get mean
				distTemp = distTemp/probelist2Indices.length;			
				// sum up means
				distSum = distSum + distTemp;
			}	
			// calculate overall mean correlation
			return distSum/probelist1Indices.length;		 
	 }		 
	
	protected float getDistanceBetween2points(int i, int j){
		
		/** The difference in the x coordinates between gene list i and j */ 
		float distanceX = xCoordinates[i] - xCoordinates[j];
		
		/** The difference in the y coordinates between gene list i and j */
		float distanceY =  yCoordinates[i] - yCoordinates[j];
		 
		/** The distance between the 2 gene lists */
		float actualDistance = (float)Math.sqrt((distanceX * distanceX) + (distanceY * distanceY));
		
		return (actualDistance);
	}
}	

