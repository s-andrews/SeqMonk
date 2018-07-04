/**
 * Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.AnnotationParsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;

/**
 * The Class GFFAnnotationParser reads sequence features from GFFv3 files
 */


public class GFF3AnnotationParser extends AnnotationParser {

	private String featurePrefix = "";

	
	/**
	 * Instantiates a new GFF annotation parser.
	 * 
	 * @param genome the genome
	 */
	public GFF3AnnotationParser (Genome genome) {
		super(genome);		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#requiresFile()
	 */
	@Override
	public boolean requiresFile() {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#fileFilter()
	 */
	public FileFilter fileFilter() {
		return new FileFilter() {
			
			public String getDescription() {
				return "GFF/GTF Files";
			}
			
			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(".gff") || f.getName().toLowerCase().endsWith(".gtf")) {
					return true;
				}
				return false;
			}
		};
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#name()
	 */
	public String name() {
		return "GFF/GTF Parser";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#parseAnnotation(java.io.File, uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome)
	 */
	public AnnotationSet [] parseAnnotation(File file, Genome genome) throws Exception {
		return parseAnnotation(file, genome, null);
	}
	public AnnotationSet [] parseAnnotation(File file, Genome genome, String prefix) throws Exception {
		
		System.err.println("Parsing "+file);
		
		if (prefix == null) {
			featurePrefix = JOptionPane.showInputDialog(SeqMonkApplication.getInstance(), "Feature prefix", "GFFv3/GTP Options", JOptionPane.QUESTION_MESSAGE);
		}
		else {
			featurePrefix = prefix;
		}
		
		if (featurePrefix == null) featurePrefix = "";
		
		Vector<AnnotationSet> annotationSets = new Vector<AnnotationSet>();

		AnnotationSet currentAnnotation = new AnnotationSet(genome, file.getName());
		annotationSets.add(currentAnnotation);
		
		Hashtable<String, FeatureGroup> groupedFeatures = new Hashtable<String, FeatureGroup>();
		
		BufferedReader br;

		if (file.getName().toLowerCase().endsWith(".gz")) {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));	
		}
		else {
			br = new BufferedReader(new FileReader(file));
		}

		String line;

		int count = 0;
		while ((line = br.readLine())!= null) {
			
			if (cancel) {
				progressCancelled();
				br.close();
				return null;
			}
			
			if (count % 1000 == 0) {
				progressUpdated("Read "+count+" lines from "+file.getName(), 0, 1);
			}
			
			if (count>1000000 && count%1000000 == 0) {
				progressUpdated("Caching...",0,1);
				currentAnnotation.finalise();
				currentAnnotation = new AnnotationSet(genome, file.getName()+"["+annotationSets.size()+"]");
				annotationSets.add(currentAnnotation);
			}

			
			++count;
			
			
			if (line.trim().length() == 0) continue;  //Ignore blank lines
			if (line.startsWith("#")) continue; //Skip comments
						
			String [] sections = line.split("\t");
			
			/*
			 * The GFFv3 file fields are:
			 *    1. name (which must be the chromosome here)
			 *    2. source (which we ignore)
			 *    3. feature type
			 *    4. start pos
			 *    5. end pos
			 *    6. score (which we ignore)
			 *    7. strand
			 *    8. frame (which we ignore)
			 *    9. attributes (structured field allowing us to group features together)
			 *    
			 */
			
			// Check to see if we've got enough data to work with
			if (sections.length < 7) {
				progressWarningReceived(new SeqMonkException("Not enough data from line '"+line+"'"));
				continue;
			}

			int strand;
			int start;
			int end;
			
			try {
				
				start = Integer.parseInt(sections[3]);
				end = Integer.parseInt(sections[4]);
				
				// End must always be later than start
				if (end < start) {
					int temp = start;
					start = end;
					end = temp;
				}
				
				if (sections.length >= 7) {
					if (sections[6].equals("+")) {
						strand = Location.FORWARD;
					}
					else if (sections[6].equals("-")) {
						strand = Location.REVERSE;
					}
					else {
						strand = Location.UNKNOWN;
					}
				}
				else {
					strand = Location.UNKNOWN;
				}
			}
			catch (NumberFormatException e) {
				progressWarningReceived(new SeqMonkException("Location "+sections[3]+"-"+sections[4]+" was not an integer"));
				continue;
			}
			
			ChromosomeWithOffset c;
			try {
				c = genome.getChromosome(sections[0]);
			}
			catch (IllegalArgumentException e) {
				progressWarningReceived(new SeqMonkException("Couldn't find a chromosome called "+sections[0]));
				continue;	
			}
			
			start = c.position(start);
			end = c.position(end);

			// We also don't allow readings which are beyond the end of the chromosome
			if (end > c.chromosome().length()) {
				int overrun = end - c.chromosome().length();
				progressWarningReceived(new SeqMonkException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")"));
				continue;
			}
						
			if (sections.length > 8 && sections[8].trim().length() > 0) {
				
				String [] attributes = sections[8].split(" *; *"); // Should check for escaped colons

				
				// Make up a data structure of the attributes we have
				Hashtable<String,Vector<String>> keyValuePairs = new Hashtable<String, Vector<String>>();
				
				for (int a=0;a<attributes.length;a++) {
					String [] keyValue = attributes[a].split("=",2); // Should check for escaped equals

					// See if we didn't get split
					if (keyValue.length == 1) {
						// This could be a GTF file which uses quoted values in space delimited fields
						keyValue = attributes[a].split(" \"");
						if (keyValue.length == 2) {
							// We need to remove the quote from the end of the value
							keyValue[1] = keyValue[1].substring(0, keyValue[1].length()-1);
//							System.out.println("Key='"+keyValue[0]+"' value='"+keyValue[1]+"'");
						}
					}
					
					if (keyValue.length == 2) {
						if (keyValuePairs.containsKey(keyValue[0])) {
							keyValuePairs.get(keyValue[0]).add(keyValue[1]);
						}
						else {
							Vector<String> newVector = new Vector<String>();
							newVector.add(keyValue[1]);
							keyValuePairs.put(keyValue[0], newVector);
						}
						
					}
					
					else {
						progressWarningReceived(new SeqMonkException("No key value delimiter in "+attributes[a]));
					}
					
				}
				
				// We now need to figure out what we're going to do with this feature.
				
				// If it's a GFFv3 file and this feature is a subfeature of another
				// type of feature then we need to simply add this as a sublocation
				// to the existing feature.  We only allow this for exon and CDS features
				// since mRNA has gene as a parent and we don't want to boot that
				
				if (keyValuePairs.containsKey("Parent")  && ! sections[2].equals("mRNA")) {
					
					// Features of a type get combined under their parent
					
					// We change exons to mRNA so we don't end up with spliced exon objects
					if (sections[2].equals("exon")) sections[2] = "mRNA";
					
					String [] parents = keyValuePairs.get("Parent").elementAt(0).split(",");
					
					for (int p=0;p<parents.length;p++) {

//						System.out.println("Adding feature "+sections[2]+" to GFFv3 parent "+parents[p]);

						if (!groupedFeatures.containsKey(sections[2]+"_"+parents[p])) {
							// Make a new feature to which we can add this
							Feature feature = new Feature(featurePrefix+sections[2],c.chromosome().name());
							groupedFeatures.put(sections[2]+"_"+parents[p], new FeatureGroup(feature, strand, feature.location()));
							Enumeration<String> en = keyValuePairs.keys();
							while (en.hasMoreElements()) {
								String key = en.nextElement();
								String [] values = keyValuePairs.get(key).toArray(new String[0]);
								for (int v=0;v<values.length;v++) {
									feature.addAttribute(key, values[v]);
								}
							}
						}	
						groupedFeatures.get(sections[2]+"_"+parents[p]).addSublocation(new Location(start, end, strand));
						
					}
				}
				
				
				// This could be a GTF file.  If so then we add the subfeature to the appropriate
				// parent feature
				else if (keyValuePairs.containsKey("transcript_id")) {

					if (sections[2].equals("exon")) sections[2] = "mRNA";
					
//					System.out.println("Adding feature "+sections[2]+" to GTF parent "+keyValuePairs.get("trancript_id").elementAt(0));

					if (! groupedFeatures.containsKey(sections[2]+"_"+keyValuePairs.get("transcript_id").elementAt(0))) {
						Feature feature = new Feature(featurePrefix+sections[2],c.chromosome().name());
						Enumeration<String> en = keyValuePairs.keys();
						while (en.hasMoreElements()) {
							String key = en.nextElement();
							String [] values = keyValuePairs.get(key).toArray(new String[0]);
							for (int v=0;v<values.length;v++) {
								feature.addAttribute(key, values[v]);
							}
						}
						
						
						groupedFeatures.put(sections[2]+"_"+keyValuePairs.get("transcript_id").elementAt(0), new FeatureGroup(feature, strand, feature.location()));
					}						
						
					groupedFeatures.get(sections[2]+"_"+keyValuePairs.get("transcript_id").elementAt(0)).addSublocation(new Location(start, end, strand));
				}

				else {
					// If we get here we're making a feature with attributes
				
					Feature feature = new Feature(featurePrefix+sections[2],c.chromosome().name());
					feature.setLocation(new Location(start,end,strand));
					Enumeration<String> en = keyValuePairs.keys();
					while (en.hasMoreElements()) {
						String key = en.nextElement();
						String [] values = keyValuePairs.get(key).toArray(new String[0]);
						for (int v=0;v<values.length;v++) {
							feature.addAttribute(key, values[v]);
						}
					}
					if (keyValuePairs.containsKey("ID")) {
						// This is a feature which may end up having subfeatures
						groupedFeatures.put(sections[2]+"_"+keyValuePairs.get("ID").elementAt(0), new FeatureGroup(feature, strand, feature.location()));

//						System.out.println("Making new entry for "+keyValuePairs.get("ID").elementAt(0));
				
					}
					else {
						// We can just add this to the annotation collection
						currentAnnotation.addFeature(feature);
					}
				}
				
			}
			else {
				// No group parameter to worry about
				Feature feature = new Feature(featurePrefix+sections[2],c.chromosome().name());
				feature.setLocation(new Location(start,end,strand));
				currentAnnotation.addFeature(feature);
			}
			
						
		}
		br.close();
		
		// Now go through the grouped features adding them to the annotation set
		
		Iterator<FeatureGroup> i = groupedFeatures.values().iterator();
		while (i.hasNext()) {
			Feature f = i.next().feature();
			currentAnnotation.addFeature(f);
		}
				
		return annotationSets.toArray(new AnnotationSet[0]);
	}

	/**
	 * The Class featureGroup.
	 */
	private class FeatureGroup {
		
		/** The feature. */
		private Feature feature;
		
		/** The sub locations. */
		private Vector<Location> subLocations = new Vector<Location>();
		
		/** The location */
		private Location location;
				
		/**
		 * Instantiates a new feature group.
		 * 
		 * @param feature the feature
		 * @param strand the strand
		 * @param location the location
		 */
		public FeatureGroup (Feature feature, int strand, Location location) {
			this.feature = feature;
			this.location = location;
		}
		
		/**
		 * Adds a sublocation.
		 * 
		 * @param location the location
		 */
		public void addSublocation (Location location) {
			subLocations.add(location);
		}
		
		/**
		 * Feature.
		 * 
		 * @return the feature
		 */
		public Feature feature () {
			try {
				if (subLocations.size() == 0) {
					feature.setLocation(location);					
				}
				else if (subLocations.size() == 1) {
					feature.setLocation(subLocations.elementAt(0));					
				}
				else {
					feature.setLocation(new SplitLocation(subLocations.toArray(new Location[0])));
				}
			} 
			catch (SeqMonkException e) {
				throw new IllegalStateException(e);
			}
			
			return feature;
		}
		
		
	}
	
}
