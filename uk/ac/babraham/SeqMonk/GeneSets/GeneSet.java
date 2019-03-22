/**
 * Copyright 2014-19 Laura Biggins
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

/** 
 * Contains each line of the GMT file i.e. the ontology category and all the associated genes. 
 * 
 * @author bigginsl
 *
 */

public class GeneSet {
	
	// The identifier
	private String geneSetIdentifier;
	
	// The ontology category
	private String geneSetName;
	
	// The set of gene names belonging to the category
	private String [] geneNames;
	
	// Some gmt files have a description
	private String description;
	
	// create a gene set
	public GeneSet(String geneSetIdentifier, String geneSetName, String [] geneNames, String description){
		
		this.geneSetIdentifier = geneSetIdentifier;
		this.geneSetName = geneSetName;
		this.geneNames = geneNames;
		this.description = description;
		
	}
	
	// returns the gene set category
	public void setGeneSetDescription(String geneSetDescription){
		
		description = geneSetDescription;
	}
	
	// returns the gene set identifier
	public String geneSetIdentifier(){
		
		return geneSetIdentifier;
	}
	
	// returns the gene set name
	public String geneSetName(){
		
		return geneSetName;
	}
	
	// returns the gene set category
	public String geneSetDescription(){
		
		return description;
	}
	
	// returns the array of gene names 
	public String [] geneNames(){
		
		return geneNames;
	}

}
