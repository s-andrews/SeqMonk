/**
 * Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Network.DownloadableGenomes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Date;
import java.util.Hashtable;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

public class DownloadableGenomeSet {

	private Hashtable <String, GenomeSpecies> seenSpecies;
	private Vector<GenomeSpecies> species = new Vector<GenomeSpecies>();
	
	public DownloadableGenomeSet () throws IOException {

		URL genomeIndexURL = new URL(SeqMonkPreferences.getInstance().getGenomeDownloadLocation()+"genome_index.txt");

		BufferedReader genomeIndexReader = new BufferedReader(new InputStreamReader(genomeIndexURL.openStream()));
		seenSpecies = new Hashtable<String, GenomeSpecies>();
		
		String indexLine = null;
		while ((indexLine = genomeIndexReader.readLine())!= null) {
			String [] sections = indexLine.split("\\t");
			if (sections.length < 4) {
				throw new IOException("Genome list file is corrupt.  Expected 4 sections on line '"+indexLine+"' but got "+sections.length);
			}
			if (!seenSpecies.containsKey(new String(sections[0]))) {
				GenomeSpecies newSpecies = new GenomeSpecies(sections[0]);
				species.add(newSpecies);
				seenSpecies.put(sections[0],newSpecies);
			}
			
			long epoch = Long.parseLong(sections[3]);
			Date date = new Date(epoch*1000); // Network date is in seconds.  Local date is in milliseconds.
			
//			System.out.println("For assembly "+sections[1]+" epoch was "+epoch+" and date was "+date);
			
			new GenomeAssembly(seenSpecies.get(sections[0]),sections[1],Integer.parseInt(sections[2]),date);

//			System.out.println("Found organism "+sections[0]+" and assembly "+sections[1]);
		}
	}
	
	public GenomeSpecies [] species () {
		return species.toArray(new GenomeSpecies[0]);
	}
	
	public GenomeAssembly [] findUpdateableGenomes () throws IOException {
		
		// We need to go through the installed genomes and see if we have an assembly
		// which is newer than the one which is installed.
		
		Vector<GenomeAssembly>updates = new Vector<GenomeAssembly>();
		
		File [] speciesFolders = SeqMonkPreferences.getInstance().getGenomeBase().listFiles();
		
		for (int s=0;s<speciesFolders.length;s++) {
			if (!speciesFolders[s].isDirectory()) continue;
						
			File [] assemblyFolders = speciesFolders[s].listFiles();
			
			// On windows we get a stupid situation if the user has set their genomes folder to
			// be their documents folder.  This contains special pseudo-directories (called 
			// junctions) for My Music, My Pictures etc.  These appear to java to be normal 
			// directories but if you move into them then you get to what appears to be an 
			// invalid location and you can't list files, which results in a crash.
			
			// We can therefore check for whether the results of listFiles is null and just skip
			// this.  This isn't great as we might miss some real failures, but there's no 
			// native way to explicitly check for junctions.

			if (assemblyFolders == null) continue;
			
			
			for (int a=0;a<assemblyFolders.length;a++) {
				if (!assemblyFolders[a].isDirectory()) continue;
				
				// Now find the latest modification time on any dat / gff / gtf / gtf3
				
				File [] datFiles = assemblyFolders[a].listFiles();
				
				long latestEpoch = 0;
				
				for (int d=0;d<datFiles.length;d++) {
					if (datFiles[d].getName().toLowerCase().endsWith(".dat") 
							|| datFiles[d].getName().toLowerCase().endsWith(".gff")
							|| datFiles[d].getName().toLowerCase().endsWith(".gff3")
							|| datFiles[d].getName().toLowerCase().endsWith(".gtf")
							) {
						if (datFiles[d].lastModified() > latestEpoch) {
							latestEpoch = datFiles[d].lastModified();
						}
					}
				}
				
				Date latestDate = new Date(latestEpoch);
				
				// Now see if there is an assembly in the downloadable genomes
				// which matches this one, and if it's newer than the one we
				// have installed.
				
				if (seenSpecies.containsKey(speciesFolders[s].getName())) {
					GenomeAssembly [] genomes = seenSpecies.get(speciesFolders[s].getName()).assemblies();
					
					for (int ga=0;ga<genomes.length;ga++) {
						if (genomes[ga].assembly().equals(assemblyFolders[a].getName())){
							// We have a match, but is it newer
							
							if (genomes[ga].date().after(latestDate)) {
								// We have an update to record.
								updates.add(genomes[ga]);
							}
//							else {
//								System.out.println("Local date for "+genomes[ga].assembly()+" is "+latestDate.toString()+" but network date is "+genomes[ga].date().toString());
//								
//							}
						}
					}
					
				}
				
			}
		}
		
		
		return updates.toArray(new GenomeAssembly[0]);
	}

	public String toString () {
		return "Downloadable Genomes";
	}
	
	public static void main (String [] args) {
		try {
			DownloadableGenomeSet dgs = new DownloadableGenomeSet();
			
			GenomeAssembly [] updates = dgs.findUpdateableGenomes();
			
			System.out.println ("There are "+updates.length+" genomes to update");
			
			for (int i=0;i<updates.length;i++) {
				System.out.println(updates[i].species().name()+"\t"+updates[i].assembly()+" from "+updates[i].date());
			}
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
}
