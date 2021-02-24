/**
 * Copyright 2009- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities;

import java.util.HashMap;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;

/**
 * Since different files will use varying conventions to represent a chromosome
 * name we use this class to try to interpret these different strings to correctly
 * extract the chromosome name.
 * 
 * Although you can instantiate this class directly it is normally used by calling
 * the getChromosome(String) method from the Genome class.
 */
public class ChromosomeNameTranslator {
	
	private Genome genome;

	/** A name map which stores the results of previously deciphered strings to speed future access */
	private HashMap<String, ChromosomeWithOffset> nameMap = new HashMap<String, ChromosomeWithOffset>();
	
	/** A name map optionally loaded from values provided by the user which we wouldn't figure out for ourselves */
	private HashMap<String, ChromosomeWithOffset> userNameMap = new HashMap<String, ChromosomeWithOffset>();
	
	/**
	 * Instantiates a new chromosome name translator.
	 * 
	 * @param genome The genome which provides the chromosome we can match against.
	 */
	public ChromosomeNameTranslator (Genome genome) {
		this.genome = genome;
	}
	
	/**
	 * Adds a manual entry to the name map.
	 * 
	 * @param userSuppliedName The name we want to map
	 * @param chromosome The chromosome associated with this name
	 * @throws SeqMonkException Thrown if this chromosome is not in the genome used to instantiate the class
	 */
	public void addNameMap (String userSuppliedName, Chromosome chromosome, int offset) {
		if (genome.hasChromosome(chromosome)) {
			ChromosomeWithOffset cwo = new ChromosomeWithOffset(chromosome, offset);
			userNameMap.put(userSuppliedName,cwo);
			nameMap.put(userSuppliedName,cwo);
		}
		else {
			throw new IllegalArgumentException("Couldn't find a chromosome called"+chromosome+"' in this genome");
		}
	}
		
	/**
	 * Tries to find a match for a name, but provides an option to not remember failures.
	 * This can be used when the genome is still in a state of flux (with new chromosomes
	 * being added) so we know that something which fails now may not necessarily fail
	 * later.  This shouldn't be used once the genome is completely loaded since performance
	 * will be adversely affected by not caching failures and rechecking them every time.
	 * 
	 * @param name The name to match
	 * @param rememberFailures Whether to cache names which couldn't be matched to a chromosome
	 * @return The chromosome we matched.
	 * @throws SeqMonkException If we couldn't match the name to any chromosome
	 */
	public ChromosomeWithOffset getChromosomeForName (String name, boolean rememberFailures) {
		
		/**
		 * This method can be used with any string which might be a chromosome
		 * name.  Several different methods will be tried to extract a valid
		 * chromosome name from the actual name provided.
		 * 
		 * If a name can't be matched to a real chromosome then a SeqMonk exception
		 * will be thrown.
		 */
		
		// See if we've already worked this out for this name
		if (nameMap.containsKey(name)) {
			ChromosomeWithOffset chr = nameMap.get(name);
			if (chr != null) {
				return chr;
			}
			throw new IllegalArgumentException("Couldn't extract a valid name from '"+name+"'");
		}

		
		// Ensembl chromsome files look like:
		// Homo_sapiens.GRCh37.55.dna.chromosome.1.fa
		
		String [] dotSections = name.split("\\.");
		if (dotSections.length == 7) {
			// Try it
			Chromosome chr = genome.getExactChromsomeNameMatch(dotSections[5]);
			if (chr != null) {
				nameMap.put(name, new ChromosomeWithOffset(chr, 0));
				return nameMap.get(name);
			}
		}
		
		// Try filenames next
		String filename = name.trim();
		if (filename.toLowerCase().endsWith(".txt")) {
			filename = filename.substring(0, filename.length()-4);
		}

		if (filename.toLowerCase().endsWith(".fa")) {
			filename = filename.substring(0, filename.length()-3);
		}
		if (filename.toLowerCase().startsWith("chromosome")) {
			filename = filename.substring(10);
		}
		if (filename.toLowerCase().startsWith("chr")) {
			filename = filename.substring(3);
		}
		filename = filename.trim();
				
		Chromosome chr = genome.getExactChromsomeNameMatch(filename);
		if (chr != null) {
			nameMap.put(name, new ChromosomeWithOffset(chr, 0));
			return nameMap.get(name);
		}

		chr = genome.getExactChromsomeNameMatch(filename.toUpperCase());
		if (chr != null) {
			nameMap.put(name, new ChromosomeWithOffset(chr, 0));
			return nameMap.get(name);			
		}

		chr = genome.getExactChromsomeNameMatch(filename.toLowerCase());
		if (chr != null) {
			nameMap.put(name, new ChromosomeWithOffset(chr, 0));
			return nameMap.get(name);			
		}

		// We could also try the first text before a space in the string
		String [] spaceSections = name.trim().split("\\s+");
		chr = genome.getExactChromsomeNameMatch(spaceSections[0]);
		if (chr != null) {
			nameMap.put(name,new ChromosomeWithOffset(chr, 0));
			return nameMap.get(name);
		}
		
		
		// If we get this far then all is lost and we have to give up.
		if (rememberFailures) {
			nameMap.put(name, null);
		}
		throw new IllegalArgumentException("Couldn't extract a valid name from '"+name+"'");
		
	}

}
