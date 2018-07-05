/**
 * Copyright Copyright 2010-18 Simon Andrews
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
 * Parses data files in the UCSC BEDPE format.
 * 
 * https://bedtools.readthedocs.io/en/latest/content/general-usage.html
 */
public class BedPEFileParser extends DataParser {
		
	private DataParserOptionsPanel prefs = new DataParserOptionsPanel(true, false, false,false);
	
	/**
	 * Instantiates a new bed file parser.
	 * 
	 * @param collection The dataCollection into which data will be put.
	 */
	public BedPEFileParser(DataCollection collection) {
		super(collection);
		prefs.forcePairedEnd();
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
					newData[f] = new PairedDataSet(probeFiles[f].getName(),probeFiles[f].getCanonicalPath(),prefs.removeDuplicates(),prefs.getImportOptionsDescription(),prefs.hiCDistance(),prefs.hiCIgnoreTrans());					
				}
				else {
					newData[f] = new DataSet(probeFiles[f].getName(),probeFiles[f].getCanonicalPath(),prefs.removeDuplicates(),prefs.getImportOptionsDescription());
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
					
//					System.err.println(line);
//					Thread.sleep(200);
					
					++lineCount;
					if (lineCount%100000 == 0) {
						progressUpdated("Read "+lineCount+" lines from "+probeFiles[f].getName(),f,probeFiles.length);
					}
					String [] sections = line.split("\t");
					
					/*
					 * The BEDPE file fileds are:
					 *    1. chrom1 - The name of the chromosome (e.g. chr3, chrY, chr2_random)
					 *    2. chromStart1 - The starting position (ZERO indexed)
					 *    3. chromEnd1 - The ending position (ZERO indexed and not included in the feature)
					 *    4. chrom2 - The name of the second chromosome
					 *    5. start2 - The second starting position (zero indexed)
					 *    6. end2 - The second end position
					 *    7. name - The featrure name - not used by this parser
					 *    					 *    
					 *    There are  9 additional optional BED fields of which we only care about #6 they are:

					 *    8.  score
					 *    9.  strand1 - Defines the strand - either '+' or '-'.
					 *    10. strand2 - The strand for the second read
					 *    ..and then some more.
					 *    
					 *    All optional fields must be present, up to the last one used.  IE
					 *    if stand is present then name and score must also be present.  I've
					 *    seen files where this isn't the case, but we'll code to the spec for
					 *    now.
					 *    
					 *    We will use fields 1,2,3,4,5,6 and optionally 9 and 10
					 *    
					 */
					
					// Check to see if we've got enough data to work with
					if (sections.length < 6) {
						progressWarningReceived(new SeqMonkException("Not enough data from line '"+line+"'"));
						continue; // Skip this line...						
					}
						
					
					// Do a quick check.  If either Chr1 or Chr2 is "." then it's not mapped and we want to
					// move on quickly.
					if (sections[0].equals(".") || sections[3].equals(".")) continue;
					
					int strand1;
					int start1;
					int end1;
					
					int strand2;
					int start2;
					int end2;
					
					try {
						
						// The start is zero indexed so we need to add 1 to get genomic positions
						start1 = Integer.parseInt(sections[1])+1;
						start2 = Integer.parseInt(sections[4])+1;
						
						// The end is zero indexed, but not included in the feature position so
						// we need to add one to get genomic coordinates, but subtract one to not
						// include the final base.
						end1 = Integer.parseInt(sections[2]);
						end2 = Integer.parseInt(sections[5]);
						
						// End must always be later than start
						if (start1 > end1) {
							progressWarningReceived(new SeqMonkException("End position1 "+end1+" was lower than start position "+start1));
							int temp = start1;
							start1 = end1;
							end1 = temp;
						}

						if (start2 > end2) {
							progressWarningReceived(new SeqMonkException("End position2 "+end2+" was lower than start position "+start2));
							int temp = start2;
							start2 = end2;
							end2 = temp;
						}

						
						if (sections.length >= 10) {
							if (sections[8].equals("+")) {
								strand1 = Location.FORWARD;
							}
							else if (sections[8].equals("-")) {
								strand1 = Location.REVERSE;
							}
							else if (sections[8].equals(".")) {
								strand1 = Location.UNKNOWN;
							}
							else {
								progressWarningReceived(new SeqMonkException("Unknown strand character '"+sections[8]+"' marked as unknown strand"));
								strand1 = Location.UNKNOWN;
							}
							
							if (sections[9].equals("+")) {
								strand2 = Location.FORWARD;
							}
							else if (sections[9].equals("-")) {
								strand2 = Location.REVERSE;
							}
							else if (sections[9].equals(".")) {
								strand2 = Location.UNKNOWN;
							}
							else {
								progressWarningReceived(new SeqMonkException("Unknown strand character '"+sections[9]+"' marked as unknown strand"));
								strand2 = Location.UNKNOWN;
							}

							
							
							
//							if (extendBy > 0) {
//								if (strand==Location.REVERSE) {
//									start -=extendBy;
//								}
//								else if (strand==Location.FORWARD) {
//									end+=extendBy;
//								}
//							}
						}
						else {
							strand1 = Location.UNKNOWN;
							strand2 = Location.UNKNOWN;
						}
					}
					catch (NumberFormatException e) {
						progressWarningReceived(new SeqMonkException("Location "+sections[0]+"-"+sections[1]+" was not an integer"));
						continue;
					}
					try {
						ChromosomeWithOffset c1 = dataCollection().genome().getChromosome(sections[0]);
						// We also don't allow readings which are beyond the end of the chromosome
						start1 = c1.position(start1);
						end1 = c1.position(end1);
						if (end1 > c1.chromosome().length()) {
							int overrun = end1 - c1.chromosome().length();
							progressWarningReceived(new SeqMonkException("Reading position "+end1+" was "+overrun+"bp beyond the end of chr"+c1.chromosome().name()+" ("+c1.chromosome().length()+")"));
							continue;
						}

						
						ChromosomeWithOffset c2 = dataCollection().genome().getChromosome(sections[3]);
						// We also don't allow readings which are beyond the end of the chromosome
						start2 = c2.position(start2);
						end2 = c2.position(end2);
						if (end2 > c2.chromosome().length()) {
							int overrun = end2 - c2.chromosome().length();
							progressWarningReceived(new SeqMonkException("Reading position "+end2+" was "+overrun+"bp beyond the end of chr"+c2.chromosome().name()+" ("+c2.chromosome().length()+")"));
							continue;
						}

						
						
						// We can now make the new reading
						
						// If this is HiC then we just make reads for each of the two ends and
						// add them.  There's nothing clever to do.
						if (prefs.isHiC()) {
							long read1 = SequenceRead.packPosition(start1,end1,strand1);
							newData[f].addData(c1.chromosome(),read1);

							long read2 = SequenceRead.packPosition(start2,end2,strand2);
							newData[f].addData(c2.chromosome(),read2);
						
						}
						else {
							// If this is normal data then we need to work out if the two
							// reads we've been given are actually compatible
							
							// If they're on different chromosomes then we kick them out
							if (!c1.chromosome().name().equals(c2.chromosome().name())) {
								progressWarningReceived(new SeqMonkException("Paried reads were on different chromosomes - discarding"));
								continue;
							}
							

							// If they're in incompatible directions we kick them out
							
							if (strand1 == Location.FORWARD && strand2 != Location.REVERSE) {
								progressWarningReceived(new SeqMonkException("Invalid strand orientation - discarding"));
								continue;
							}

							if (strand1 == Location.REVERSE && strand2 != Location.FORWARD) {
								progressWarningReceived(new SeqMonkException("Invalid strand orientation - discarding"));
								continue;
							}

							// If they're too far apart we kick them out							
							int start = 1;
							int end = 0;
							
							// We take the strand from read1
							int strand = strand1;
							
							if (strand == Location.FORWARD) {
								start = start1;
								end = end2;
							}
							
							else if (strand == Location.REVERSE) {
								start = start2;
								end = end1;
							}

							else if (strand == Location.UNKNOWN) {
								start = Math.min(start1, start2);
								end = Math.max(end1, end2);
							}

							if (end <= start) {
								progressWarningReceived(new SeqMonkException("Incorrectly oriented reads - discarding"));
								continue;
							}
							
							
							if ((end - start)+1 > prefs.pairDistanceCutoff()) {
								progressWarningReceived(new SeqMonkException("Distance between reads too great ("+(((end-start)+1)-prefs.pairDistanceCutoff())+")"));
								continue;
							}
							
							
							long read = SequenceRead.packPosition(start,end,strand);
							newData[f].addData(c1.chromosome(),read);
							
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
		return "BEDPE File Importer";
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
				return (pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".bed") || pathname.getName().toLowerCase().endsWith(".bed.gz") || pathname.getName().toLowerCase().endsWith(".bedpe") || pathname.getName().toLowerCase().endsWith(".bedpe.gz"));
			}

			public String getDescription() {
				return "BEDPE Files";
			}
		};
	}
	
}
