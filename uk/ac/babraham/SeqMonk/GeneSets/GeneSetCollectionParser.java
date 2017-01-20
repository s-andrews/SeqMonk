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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;

//import uk.ac.babraham.SeqMonk.SeqMonkException;
//import uk.ac.babraham.SeqMonk.Dialogs.CrashReporter;


/** 
 * Parses the file containing the ontology categories and the genes contained within each category and
 * creates a GeneSetInfo object.
 * 
 * @author bigginsl
 *
 */


public class GeneSetCollectionParser {

	public String filepath;
	private ArrayList <GeneSet> geneSetArrayList;
	private boolean cancel = false;
	private int minGenesInSet;
	private int maxGenesInSet;
	
	public GeneSetCollectionParser(int minGenesInSet, int maxGenesInSet){
		
		this.minGenesInSet = minGenesInSet;
		this.maxGenesInSet = maxGenesInSet;
		geneSetArrayList = new ArrayList<GeneSet>();		
	}
	
	// check whether the gene set info file can be found
	private boolean fileValid(String filepath){
		
		File f = new File(filepath);
		if(f.exists() && !f.isDirectory()) {
			return true;
		}
		else{
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "The gene set information file couldn't be found, please find a valid file to load.", "Couldn't load gene set file", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	/* run method which calls a function to choose a file for the gene set info.
	 * Then parseGeneSetInformation can be called.
	 * 
	 */
	
	public GeneSetCollection parseGeneSetInformation(String filepath) throws Exception{
		
		if (fileValid(filepath) == true){

			BufferedReader br = new BufferedReader(new FileReader(filepath));
			String line;	
			int totalCount = 0;
			int importCounter = 0;
			int largeGeneSets = 0;
			int smallGeneSets = 0;
			
			while((line = br.readLine()) != null){  
				
				// TODO: need to be able to cancel
				if (cancel) {
					//progressCancelled();
					br.close();
					return null;
				}

				if (line.trim().length() == 0) continue;  //Ignore blank lines
				if (line.startsWith("#")) continue; //Skip comments
												
				String [] splitLine = line.split("\t"); // should be tab-delimited
				
				if(splitLine.length < 3){
					// TODO: This should be a progress warning
					//System.err.println("Not enough data from line " + line);
				}
				else{
					
					totalCount++;
					
					// The first field contains the name and the source. At the moment we're just interested in the name.
					String[] nameInfo = splitLine[0].split("%"); 
					
					// The second field also contains name info so we want to ignore that.
					// The rest of the information (fields 3+) should just be the gene names.
					String [] genes = new String [splitLine.length - 2];
					
					if((genes.length > maxGenesInSet)){
						largeGeneSets++;
						continue;
					}
					else if (genes.length < minGenesInSet){ 
						smallGeneSets++;
						continue;
					}
					
					for (int i = 0; i< genes.length; i++){
						
						// convert to upper case to help with matching later on
						genes[i] = splitLine[i+2].toUpperCase();
					}
					
					geneSetArrayList.add(new GeneSet(nameInfo[0], genes));	
					importCounter++;
				}
				
			}
			br.close();
			
			GeneSet [] geneSetArray = geneSetArrayList.toArray(new GeneSet[0]);
			
			System.out.println("GeneSetCollectionParser: File loaded and parsed, " + importCounter + " gene sets were imported.");
			System.out.println(largeGeneSets + " gene sets were skipped as they contained more than " + maxGenesInSet + " genes.");
			System.out.println(smallGeneSets + " gene sets were skipped as they contained fewer than " + minGenesInSet + " genes.");
			
			if((largeGeneSets + smallGeneSets) > 0){	
				JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), totalCount + " gene sets in " + filepath + ".\n\n" + importCounter + " gene sets were imported.\n\n" 
						+ largeGeneSets + " gene sets contained more than " + maxGenesInSet + " genes.\n\n" + smallGeneSets + " gene sets contained fewer than " 
						+ minGenesInSet + " genes.", "Gene set import", JOptionPane.INFORMATION_MESSAGE);
			}
			
			return (new GeneSetCollection(geneSetArray));			
			
		}	
		else {
			return null;
		}
	}	
}	
