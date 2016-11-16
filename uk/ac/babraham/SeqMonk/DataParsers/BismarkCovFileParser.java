/**
 * Copyright Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataParsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;

/**
 * Parses data files in the UCSC BED format.
 * 
 * http://genome.ucsc.edu/FAQ/FAQformat.html#format1
 */
public class BismarkCovFileParser extends DataParser {
		
	private DataParserOptionsPanel prefs = new DataParserOptionsPanel(false, false, false,false);
	
	/**
	 * Instantiates a new bismark cov file parser.
	 * 
	 * @param collection The dataCollection into which data will be put.
	 */
	public BismarkCovFileParser(DataCollection collection) {
		super(collection);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
	
//		System.err.println("Started parsing cov files");
		
		try {
			
			File [] covFiles = getFiles();
			DataSet [] newData = new DataSet[covFiles.length];
			
			for (int f=0;f<covFiles.length;f++) {
				BufferedReader br;
				
				if (covFiles[f].getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(covFiles[f]))));	
				}
				else {
					br = new BufferedReader(new FileReader(covFiles[f]));
				}
				
				String line;
	
				newData[f] = new DataSet(covFiles[f].getName(),covFiles[f].getCanonicalPath(),prefs.removeDuplicates());
								
				int lineCount = 0;
				// Now process the file
				while ((line = br.readLine())!= null) {
					
					if (cancel) {
						br.close();
						progressCancelled();
						return;
					}
					
					if (line.trim().length() == 0) continue;  //Ignore blank lines
					
					++lineCount;
					if (lineCount%100000 == 0) {
						progressUpdated("Read "+lineCount+" lines from "+covFiles[f].getName(),f,covFiles.length);
					}
					String [] sections = line.split("\t");
					
					/*
					 * The COV file fileds are:
					 *    1. chrom - The name of the chromosome (e.g. chr3, chrY, chr2_random)
					 *    2. chromStart - The starting position (ZERO indexed)
					 *    3. chromEnd - The ending position (ZERO indexed and not included in the feature)
					 *    4. percentage methylation (not used)
					 *    5. number of methylated calls
					 *    6. number of unmethylated calls
					 *    
					 */
					
					// Check to see if we've got enough data to work with
					if (sections.length < 6) {
						progressWarningReceived(new SeqMonkException("Not enough data from line '"+line+"'"));
						continue; // Skip this line...						
					}
						
					int start;
					int end;
					int methCount;
					int unmethCount;
					
					try {
						
						start = Integer.parseInt(sections[1]);						
						end = Integer.parseInt(sections[2]);
						methCount = Integer.parseInt(sections[4]);						
						unmethCount = Integer.parseInt(sections[5]);
						
						// End must always be later than start
						if (start > end) {
							progressWarningReceived(new SeqMonkException("End position "+end+" was lower than start position "+start));
							int temp = start;
							start = end;
							end = temp;
						}
						
					}
					catch (NumberFormatException e) {
						progressWarningReceived(new SeqMonkException("Location "+sections[0]+"-"+sections[1]+" was not an integer"));
						continue;
					}
					try {
						ChromosomeWithOffset c = dataCollection().genome().getChromosome(sections[0]);
						// We also don't allow readings which are beyond the end of the chromosome
						start = c.position(start);
						end = c.position(end);
						if (end > c.chromosome().length()) {
							int overrun = end - c.chromosome().length();
							progressWarningReceived(new SeqMonkException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")"));
							continue;
						}
		
						// We can now make the new reads
						long methRead = SequenceRead.packPosition(start,end,Location.FORWARD);
						long unmethRead = SequenceRead.packPosition(start,end,Location.REVERSE);
						for (int i=0;i<methCount;i++) {
							newData[f].addData(c.chromosome(),methRead);
						}
						for (int i=0;i<unmethCount;i++) {
							newData[f].addData(c.chromosome(),unmethRead);
						}
					}
					catch (IllegalArgumentException iae) {
						progressWarningReceived(iae);
					}
					catch (SeqMonkException sme) {
						progressWarningReceived(sme);
						continue;
					}
	
				}
				
				// We're finished with the file.
				br.close();
								
				// Cache the data in the new dataset
				progressUpdated("Caching data from "+covFiles[f].getName(), f, covFiles.length);
				newData[f].finalise();

			}

			processingFinished(newData);

		}	

		catch (Exception ex) {
			progressExceptionReceived(ex);
			return;
		}
		
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#description()
	 */
	public String description() {
		return "Imports Bismark coverage files";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#hasOptionsPanel()
	 */
	public boolean hasOptionsPanel () {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getOptionsPanel()
	 */
	public JPanel getOptionsPanel() {
		return null;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#name()
	 */
	public String name() {
		return "Bismark Cov file parser";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#readyToParse()
	 */
	public boolean readyToParse() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getFileFilter()
	 */
	public FileFilter getFileFilter () {
		return new FileFilter() {
			public boolean accept(File pathname) {
				return (pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".cov") || pathname.getName().toLowerCase().endsWith(".cov.gz"));
			}

			public String getDescription() {
				return "COV Files";
			}
		};
	}
	
}
