/**
 * Copyright Copyright 2010-17 Simon Andrews
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
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.CoreAnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.MultiGenome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SingleGenome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * The Class GenomeParser loads all of the features for a CoreGenomeAnnotationSet.
 * It can either do a full parse of the original EMBL format files, or take
 * a shortcut if there are pre-cached object files present.
 */
public class GenomeParser implements Runnable {

	/** The listeners. */
	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();

	/** The genome. */
	private SingleGenome [] singleGenomes;

	/** The base location. */
	private File [] baseLocations;

	/** The current offset. */
	private int currentOffset = 0;

	/** The prefs. */
	private SeqMonkPreferences prefs = SeqMonkPreferences.getInstance();


	/**
	 * Parses the genome.
	 * 
	 * @param baseLocations the base location
	 */
	public void parseGenome (File [] baseLocations) {
		this.baseLocations = baseLocations;

		Thread t = new Thread(this);
		t.start();
	}

	/**
	 * Adds the progress listener.
	 * 
	 * @param pl the pl
	 */
	public void addProgressListener (ProgressListener pl) {
		if (pl != null && ! listeners.contains(pl))
			listeners.add(pl);
	}

	/**
	 * Removes the progress listener.
	 * 
	 * @param pl the pl
	 */
	public void removeProgressListener (ProgressListener pl) {
		if (pl != null && listeners.contains(pl))
			listeners.remove(pl);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			singleGenomes = new SingleGenome[baseLocations.length];
			for (int i=0;i<baseLocations.length;i++) {
				singleGenomes[i] = new SingleGenome(baseLocations[i]);				
			}

		} catch (SeqMonkException ex) {
			Enumeration<ProgressListener> en = listeners.elements();

			while (en.hasMoreElements()) {
				en.nextElement().progressExceptionReceived(ex);
				return;
			}
		}

		for (int g=0;g<singleGenomes.length;g++) {


			File cacheCompleteFile = new File(baseLocations[g].getAbsoluteFile()+"/cache/cache.complete");

			if (cacheCompleteFile.exists()) {

				boolean cacheFailed = false;

				try {
					// Check the version inside the cache.complete file
					BufferedReader br = new BufferedReader(new FileReader(cacheCompleteFile)); 
					String line = br.readLine();
					br.close();
					if (line == null || line.length() == 0) {
						// If there's no version in there then re-parse
						cacheFailed = true;
					}
					// We re-parse if the cache was made by a different version
					if (! SeqMonkApplication.VERSION.equals(line)) {
						System.err.println("Version mismatch between cache ('"+line+"') and current version ('"+SeqMonkApplication.VERSION+"') - reparsing");
						cacheFailed = true;
					}
				}
				catch (IOException ioe) {
					cacheFailed = true;
				}

				// Check to see if the .dat files have changed since the cache
				// file was saved

				File [] files = baseLocations[g].listFiles(new FileFilter() {

					public boolean accept(File f) {
						if (f.getName().toLowerCase().endsWith(".dat")  || f.getName().toLowerCase().endsWith(".gff") || f.getName().toLowerCase().endsWith(".gff3") || f.getName().toLowerCase().endsWith(".gtf")) {
							return true;
						}
						else {
							return false;
						}
					}

				});

				boolean datFilesUpdated = false;
				for (int f=0;f<files.length;f++) {
					if (files[f].lastModified() > cacheCompleteFile.lastModified()) {
						System.err.println("Modification on "+files[f]+" is newer than on "+cacheCompleteFile+" "+files[f].lastModified()+" vs "+cacheCompleteFile.lastModified());
						datFilesUpdated = true;
						break;
					}
				}

				if (cacheFailed || datFilesUpdated) {
					if (! cacheCompleteFile.delete()) {
						System.err.println("Failed to delete the existing cache.complete file");
					}
					//				System.err.println("Dat files updated - reparsing");
					parseGenomeFiles(singleGenomes[g],baseLocations[g]);
				}
				else {
					reloadCacheFiles(singleGenomes[g],baseLocations[g]);
				}
			}
			else {
				System.err.println("File '"+cacheCompleteFile+"' doesn't exist - reparsing");
				parseGenomeFiles(singleGenomes[g],baseLocations[g]);
			}

			File aliasesFile = new File(baseLocations[g].getAbsoluteFile()+"/aliases.txt");

			if (aliasesFile.exists()) {
				try {
					readAliases(aliasesFile,singleGenomes[g]);
				} 
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}

		}
		
		Genome genomeToReturn;
		
		if (singleGenomes.length == 1) {
			genomeToReturn = singleGenomes[0];
		}
		else {
			genomeToReturn = new MultiGenome(singleGenomes);
		}
		
		Enumeration<ProgressListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressComplete("load_genome", genomeToReturn);
		}

	}

	private void readAliases (File file, SingleGenome genome) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));

		String line;

		while ((line = br.readLine()) != null) {
			String [] splitLine = line.split("\t");

			if (splitLine.length < 2) {
				//				System.err.println("Skipping alias line "+line);
				continue;
			}

			try {
				if (splitLine.length >= 3) {
					genome.addAlias(splitLine[0], splitLine[1], Integer.parseInt(splitLine[2]));
				}
				else {
					genome.addAlias(splitLine[0], splitLine[1],0);
				}
			} 
			catch (Exception e) {
				br.close();
				throw new IllegalStateException(e);
			}
		}

		br.close();
	}


	private void reloadCacheFiles (SingleGenome genome, File baseLocation) {


		Enumeration<ProgressListener> el = listeners.elements();

		while (el.hasMoreElements()) {
			el.nextElement().progressUpdated("Reloading cache files",0,1);
		}


		CoreAnnotationSet coreAnnotation = new CoreAnnotationSet(genome);

		File cacheDir = new File(baseLocation.getAbsoluteFile()+"/cache/");

		// First we need to get the list of chromosomes and set those
		// up before we go on to add the actual feature sets.
		File chrListFile = new File(baseLocation.getAbsoluteFile()+"/cache/chr_list");

		try {
			BufferedReader br = new BufferedReader(new FileReader(chrListFile));

			String line;
			while ((line = br.readLine()) != null) {
				String [] chrLen = line.split("\\t");
				Chromosome c = genome.addChromosome(chrLen[0]);
				c.setLength(Integer.parseInt(chrLen[1]));

			}
			br.close();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}

		File [] cacheFiles = cacheDir.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.getName().toLowerCase().endsWith(".cache");
			}
		});

		for (int i=0;i<cacheFiles.length;i++) {
			// Update the listeners

			String name = cacheFiles[i].getName();
			name = name.replaceAll("\\.cache$", "");
			String [] chrType = name.split("%",2);
			if (chrType.length != 2) {
				throw new IllegalStateException("Cache name '"+name+"' didn't split into chr and type");
			}
			coreAnnotation.addPreCachedFile(chrType[1], chrType[0], cacheFiles[i]);


		}

		genome.annotationCollection().addAnnotationSets(new AnnotationSet [] {coreAnnotation});

	}


	private void parseGenomeFiles (SingleGenome genome, File baseLocation) {

		// We start by seeing whether there is a chr_list file in the top level
		// which defines the size and extent of the chromosomes
		try {
			parseChrListFile(genome,baseLocation);
		}
		catch (Exception ex) {
			Enumeration<ProgressListener> en = listeners.elements();

			while (en.hasMoreElements()) {
				en.nextElement().progressExceptionReceived(ex);
			}
			return;
		}		


		// We need a list of all of the .dat files inside the baseLocation
		File [] files = baseLocation.listFiles(new FileFilter() {

			public boolean accept(File f) {
				if (f.getName().toLowerCase().endsWith(".dat")) {
					return true;
				}
				else {
					return false;
				}
			}

		});

		AnnotationSet coreAnnotation = new CoreAnnotationSet(genome);

		for (int i=0;i<files.length;i++) {
			// Update the listeners
			Enumeration<ProgressListener> e = listeners.elements();

			while (e.hasMoreElements()) {
				e.nextElement().progressUpdated("Loading Genome File "+files[i].getName(),i,files.length);
			}
			try {
				processEMBLFile(files[i],coreAnnotation,genome);
			} 
			catch (Exception ex) {
				Enumeration<ProgressListener> en = listeners.elements();

				while (en.hasMoreElements()) {
					en.nextElement().progressExceptionReceived(ex);
				}
				return;
			}			
		}

		// Update the listeners
		Enumeration<ProgressListener> e = listeners.elements();

		while (e.hasMoreElements()) {
			e.nextElement().progressUpdated("Caching annotation data",1,1);
		}

		// Now do the same thing for gff files.

		// We need a list of all of the .gff/gtf files inside the baseLocation
		files = baseLocation.listFiles(new FileFilter() {

			public boolean accept(File f) {
				if (f.getName().toLowerCase().endsWith(".gff") || f.getName().toLowerCase().endsWith(".gtf") || f.getName().toLowerCase().endsWith(".gff3")) {
					return true;
				}
				else {
					return false;
				}
			}

		});

		GFF3AnnotationParser gffParser = new GFF3AnnotationParser(genome);

		for (int i=0;i<files.length;i++) {
			//			System.err.println("Parsing "+files[i]);
			// Update the listeners
			e = listeners.elements();

			while (e.hasMoreElements()) {
				e.nextElement().progressUpdated("Loading Genome File "+files[i].getName(),i,files.length);
			}
			try {
				AnnotationSet [] newSets = gffParser.parseAnnotation(files[i], genome,"");
				for (int s=0;s<newSets.length;s++) {
					Feature [] features = newSets[s].getAllFeatures();
					for (int f=0;f<features.length;f++) {
						coreAnnotation.addFeature(features[f]);
					}
				}
			} 
			catch (Exception ex) {
				Enumeration<ProgressListener> en = listeners.elements();

				while (en.hasMoreElements()) {
					en.nextElement().progressExceptionReceived(ex);
				}
				return;
			}			
		}

		// Update the listeners
		e = listeners.elements();

		while (e.hasMoreElements()) {
			e.nextElement().progressUpdated("Caching annotation data",1,1);
		}


		genome.annotationCollection().addAnnotationSets(new AnnotationSet[] {coreAnnotation});

		// Debugging - put out some stats
		//		System.err.println("Made genome with "+genome.getAllChromosomes().length+" chromosomes");
		//		System.err.println("There are "+genome.annotationCollection().listAvailableFeatureTypes().length+" different feature types");


	}

	protected void progressWarningReceived (Exception e) {
		Enumeration<ProgressListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressWarningReceived(e);
		}
	}

	private void parseChrListFile(SingleGenome genome, File baseLocation) throws Exception {
		File chrListFile = new File(baseLocation.getAbsolutePath()+"/chr_list");
		if (chrListFile.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(chrListFile));
			String line;

			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");
				Chromosome c = genome.addChromosome(sections[0]);
				c.setLength(Integer.parseInt(sections[1]));
			}

			br.close();

			// If we've loaded the chromosome list we also need to check at this point
			// whether there are any aliases defined since these might be used by the 
			// dat or gff files.  We will end up re-adding these aliases a bit later
			// which is unfortunate but won't slow things down much so it's not too bad.

			File aliasesFile = new File(baseLocation.getAbsoluteFile()+"/aliases.txt");

			if (aliasesFile.exists()) {
				try {
					readAliases(aliasesFile,genome);
				} 
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}

		}


	}

	/**
	 * Process embl file.
	 * 
	 * @param f the f
	 * @param annotation the annotation
	 * @throws Exception the exception
	 */
	private void processEMBLFile (File f, AnnotationSet annotation, SingleGenome genome) throws Exception {

		BufferedReader br = new BufferedReader(new FileReader(f));

		Chromosome c = null;
		// We need to find and read the accession line to find out
		// which chromosome and location we're dealing with.

		// Each physical file can contain more than one EMBL file.  We 
		// need to account for this in our processing.

		while ((c = parseChromosome(br,genome)) != null) {
			String line;			
			// We can now skip through to the start of the feature table
			while ((line=br.readLine())!=null) {
				if (line.startsWith("FH") || line.startsWith("SQ")) {
					break;
				}
			}

			// We can now start reading the features one at a time by
			// concatonating them and then passing them on for processing
			StringBuffer currentAttribute = new StringBuffer();
			boolean skipping = true;
			Feature feature = null;
			while ((line=br.readLine())!=null) {

				//				System.err.println("Read line '"+line+"'");

				if (line.startsWith("XX") || line.startsWith("SQ") || line.startsWith("//")) {
					skipToEntryEnd(br);
					break;
				}

				if (line.length() < 18) continue; // Just a blank line.

				String type = line.substring(5,18).trim();
				//				System.out.println("Type is "+type);
				if (type.length()>0) {
					//We're at the start of a new feature.

					// Check whether we need to process the old feature
					if (skipping) {
						// We're either on the first feature, or we've
						// moving past this one
						skipping = false;
					}
					else {						
						// We need to process the last attribute from the
						// old feature
						processAttributeReturnSkip(currentAttribute.toString(), feature);
						annotation.addFeature(feature);
					}

					// We can check to see if we're bothering to load this type of feature
					if (prefs.loadAnnotation(type)) {
						//						System.err.println("Creating new feature of type "+type);
						feature = new Feature(type,c.name());
						currentAttribute=new StringBuffer("location=");
						currentAttribute.append(line.substring(21).trim());
						continue;
					}
					else {
						//						System.err.println("Skipping feature of type "+type);
						genome.addUnloadedFeatureType(type);
						skipping = true;
					}

				}

				if (skipping) continue;

				String data = line.substring(21).trim();

				if (data.startsWith("/")) {
					// We're at the start of a new attribute

					//Process the last attribute
					skipping = processAttributeReturnSkip(currentAttribute.toString(), feature);
					currentAttribute = new StringBuffer();
				}

				// Our default action is just to append onto the existing information

				// Descriptions which run on to multiple lines need a space adding
				// before the next lot of text.
				if (currentAttribute.indexOf("description=") >= 0) currentAttribute.append(" ");

				currentAttribute.append(data);

			}

			// We've finished, but we need to process the last feature
			// if there was one
			if (!skipping) {
				// We need to process the last attribute from the
				// old feature
				processAttributeReturnSkip(currentAttribute.toString(), feature);
				annotation.addFeature(feature);
			}
		}
		br.close();
	}

	/**
	 * Process attribute return skip.
	 * 
	 * @param attribute the attribute
	 * @param feature the feature
	 * @return true, if successful
	 * @throws SeqMonkException the seq monk exception
	 */
	private boolean processAttributeReturnSkip (String attribute, Feature feature) throws SeqMonkException {
		//		System.out.println("Adding feature - current attribute is "+attribute);
		String [] nameValue = attribute.split("=",2);

		// We used to insist on key value pairs, but the EMBL spec
		// allows a key without a value, so one value is OK.


		if (nameValue[0].equals("location")) {

			// A location has to have a value
			if (nameValue.length < 2) {
				throw new SeqMonkException("Location didn't have an '=' delimiter");
			}

			//			System.out.println("Location is "+nameValue[1]);
			//Check to see if this is a location we can support

			if (nameValue[1].indexOf(":")>=0) {
				// Some locations are given relative to other sequences
				// (where a feature splits across more than one sequence).
				// We can't handle this so we don't try.
				return true;
			}

			feature.setLocation(new SplitLocation(nameValue[1],currentOffset));
		}
		else {
			// All other attributes just get added to the feature
			if (nameValue.length == 2) {
				feature.addAttribute(nameValue[0], nameValue[1]);
			}
			else {
				feature.addAttribute(nameValue[0], null);
			}
			return false;
		}

		return false;
	}

	/**
	 * Parses the chromosome.
	 * 
	 * @param br the br
	 * @return the chromosome
	 * @throws SeqMonkException the seq monk exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private Chromosome parseChromosome (BufferedReader br, SingleGenome genome) throws SeqMonkException, IOException {
		String line;
		while ((line=br.readLine())!=null) {

			if (line.startsWith("AC")) {
				String [] sections = line.split(":");
				if (sections.length != 6) {
					// It's not a chromosome file.  We probably just want to
					// skip it and move onto the next entry
					progressWarningReceived(new SeqMonkException("AC line didn't have 6 sections '"+line+"'"));
					skipToEntryEnd(br);
					continue;
				}
				if (line.indexOf("supercontig")>=0) {
					// It's not a chromosome file.  We probably just want to
					// skip it and move onto the next entry
					skipToEntryEnd(br);
					continue;
				}

				// This will return the existing chromosome of this
				// name if it exists already, but will create a new
				// one if it doesn't.
				Chromosome c = genome.addChromosome(sections[2]);

				c.setLength(Integer.parseInt(sections[4]));

				// Since the positions of all features are given relative
				// to the current sequence we need to add the current
				// start position to all locations as an offset.
				currentOffset = Integer.parseInt(sections[3])-1;
				return c;
			}

			if (line.startsWith("//")) {
				throw new SeqMonkException("Couldn't find AC line");
			}
		}
		return null;
	}


	/**
	 * Skip to entry end.
	 * 
	 * @param br the br
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void skipToEntryEnd (BufferedReader br) throws IOException {
		String line;
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
		}

		while ((line=br.readLine())!=null) {
			if (line.startsWith("//"))
				return;
		}
	}

}