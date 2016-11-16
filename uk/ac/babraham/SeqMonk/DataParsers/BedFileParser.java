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
import uk.ac.babraham.SeqMonk.DataTypes.PairedDataSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;

/**
 * Parses data files in the UCSC BED format.
 * 
 * http://genome.ucsc.edu/FAQ/FAQformat.html#format1
 */
public class BedFileParser extends DataParser {
		
	private DataParserOptionsPanel prefs = new DataParserOptionsPanel(false, false, false,false);
	
	/**
	 * Instantiates a new bed file parser.
	 * 
	 * @param collection The dataCollection into which data will be put.
	 */
	public BedFileParser(DataCollection collection) {
		super(collection);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
	
//		System.err.println("Started parsing BED files");
		
		int extendBy = prefs.extendReads();
		
		try {
			
			File [] probeFiles = getFiles();
			DataSet [] newData = new DataSet[probeFiles.length];
			
			for (int f=0;f<probeFiles.length;f++) {
				BufferedReader br;
				
				if (probeFiles[f].getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(probeFiles[f]))));	
				}
				else {
					br = new BufferedReader(new FileReader(probeFiles[f]));
				}
				
				String line;
	
				if (prefs.isHiC()) {
					newData[f] = new PairedDataSet(probeFiles[f].getName(),probeFiles[f].getCanonicalPath(),prefs.removeDuplicates(),prefs.hiCDistance(),prefs.hiCIgnoreTrans());					
				}
				else {
					newData[f] = new DataSet(probeFiles[f].getName(),probeFiles[f].getCanonicalPath(),prefs.removeDuplicates());
				}
								
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
						progressUpdated("Read "+lineCount+" lines from "+probeFiles[f].getName(),f,probeFiles.length);
					}
					String [] sections = line.split("\t");
					
					/*
					 * The BED file fileds are:
					 *    1. chrom - The name of the chromosome (e.g. chr3, chrY, chr2_random)
					 *    2. chromStart - The starting position (ZERO indexed)
					 *    3. chromEnd - The ending position (ZERO indexed and not included in the feature)
					 *    
					 *    There are  9 additional optional BED fields of which we only care about #6 they are:
					 *    4. name
					 *    5. score
					 *    6. strand - Defines the strand - either '+' or '-'.
					 *    ..and then some more.
					 *    
					 *    All optional fields must be present, up to the last one used.  IE
					 *    if stand is present then name and score must also be present.  I've
					 *    seen files where this isn't the case, but we'll code to the spec for
					 *    now.
					 *    
					 *    We will use fields 1,2,3 and optionally 6
					 *    
					 */
					
					// Check to see if we've got enough data to work with
					if (sections.length < 3) {
						progressWarningReceived(new SeqMonkException("Not enough data from line '"+line+"'"));
						continue; // Skip this line...						
					}
						
					int strand;
					int start;
					int end;
					
					try {
						
						// The start is zero indexed so we need to add 1 to get genomic positions
						start = Integer.parseInt(sections[1])+1;
						
						// The end is zero indexed, but not included in the feature position so
						// we need to add one to get genomic coordinates, but subtract one to not
						// include the final base.
						end = Integer.parseInt(sections[2]);
						
						// End must always be later than start
						if (start > end) {
							progressWarningReceived(new SeqMonkException("End position "+end+" was lower than start position "+start));
							int temp = start;
							start = end;
							end = temp;
						}
						
						if (sections.length >= 6) {
							if (sections[5].equals("+")) {
								strand = Location.FORWARD;
							}
							else if (sections[5].equals("-")) {
								strand = Location.REVERSE;
							}
							else {
								progressWarningReceived(new SeqMonkException("Unknown strand character '"+sections[5]+"' marked as unknown strand"));
								strand = Location.UNKNOWN;
							}
							
							if (extendBy > 0) {
								if (strand==Location.REVERSE) {
									start -=extendBy;
								}
								else if (strand==Location.FORWARD) {
									end+=extendBy;
								}
							}
						}
						else {
							strand = Location.UNKNOWN;
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
		
						// We can now make the new reading
						long read = SequenceRead.packPosition(start,end,strand);
						newData[f].addData(c.chromosome(),read);
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
				progressUpdated("Caching data from "+probeFiles[f].getName(), f, probeFiles.length);
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
		return "Imports BED format files";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#hasOptionsPanel()
	 */
	public boolean hasOptionsPanel () {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getOptionsPanel()
	 */
	public JPanel getOptionsPanel() {
		return prefs;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#name()
	 */
	public String name() {
		return "BED File Importer";
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
				return (pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".bed") || pathname.getName().toLowerCase().endsWith(".bed.gz"));
			}

			public String getDescription() {
				return "BED Files";
			}
		};
	}
	
}
