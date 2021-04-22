/**
 * Copyright Copyright 2010- 21 Simon Andrews
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
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceReadWithChromosome;
import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;

/**
 * Parses data files in the default Bowtie output format
 * 
 * http://bowtie-bio.sourceforge.net/manual.shtml#default-bowtie-output
 */
public class BowtieFileParser extends DataParser {
		
	// Extra options which can be set
	private boolean pairedEndImport = false;
	
	private int pairedEndDistance = 1000;
	
	private int extendBy = 0;
	
	private DataParserOptionsPanel prefs = new DataParserOptionsPanel(true, false, false,false,false);
	
	
	/**
	 * Instantiates a new bowtie file parser.
	 * 
	 * @param data The dataCollection into which data will be put.
	 */
	public BowtieFileParser (DataCollection data) {
		super(data);
	}

	/**
	 * Instantiates a new bowtie file parser.
	 * 
	 * @param data the data
	 * @param pairedEndImport the paired end import
	 * @param pairedEndDistanceCutoff the paired end distance cutoff
	 */
	public BowtieFileParser (DataCollection data, boolean pairedEndImport, int pairedEndDistanceCutoff) {
		super(data);
		this.pairedEndImport = pairedEndImport;
		pairedEndDistance = pairedEndDistanceCutoff;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		pairedEndImport = prefs.pairedEnd();
		pairedEndDistance = prefs.pairDistanceCutoff();
		if (!pairedEndImport) {
			extendBy = prefs.extendReads();
		}
		
		String [] sections1;
		String [] sections2 = null;
		
		File [] bowtieFiles = getFiles();
		DataSet [] newData = new DataSet[bowtieFiles.length];
		
		try {
			for (int f=0;f<bowtieFiles.length;f++) {

				BufferedReader br;

				if (bowtieFiles[f].getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(bowtieFiles[f]))));	
				}
				else {
					br = new BufferedReader(new FileReader(bowtieFiles[f]));
				}

				String line;
				String line2 = null;
	
				if (prefs.isHiC()) {
					newData[f] = new PairedDataSet(bowtieFiles[f].getName(),bowtieFiles[f].getCanonicalPath(),prefs.removeDuplicates(),prefs.getImportOptionsDescription(),prefs.hiCDistance(),prefs.hiCIgnoreTrans());					
				}
				else {
					newData[f] = new DataSet(bowtieFiles[f].getName(),bowtieFiles[f].getCanonicalPath(),prefs.removeDuplicates(),prefs.getImportOptionsDescription());
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
					
					if (pairedEndImport) {
						line2 = br.readLine();
						if (line2 == null) {
							++lineCount;
							progressWarningReceived(new SeqMonkException("Ran out of file when looking for paired end"));
							continue;
						}
					}
					
					if (lineCount%100000 == 0) {
						progressUpdated("Read "+lineCount+" lines from "+bowtieFiles[f].getName(),f,bowtieFiles.length);
					}
					sections1 = line.split("\t");
					
					if (pairedEndImport) {
						sections2 = line2.split("\t");
					}
					
					/*
					 * The Bowtie file fileds are:
					 *    
					 *    0. Read Name
					 *    1. Orientation (+/-)
					 *    2. Reference Sequence (chromosome)
					 *    3. Start position (0 based)
					 *    4. Read Sequence (Revcomp if hit is on - strand)
					 *    5. Quality String
					 *    6. Not used
					 *    7. Mismatch Descriptors
					 *    
					 *    Paired reads have results on consecutive lines with reads
					 *    named xxx/1 and xxx/2.
					 *    
					 *    There can be more than one hit for each read.  All hits for
					 *    a given read are found together in the file.
					 *    
					 */
					
					// Check to see if we've got enough data to work with
					if (sections1.length < 5) {
						progressWarningReceived(new SeqMonkException("Not enough data from line '"+line+"'"));
						continue; // Skip this line...						
					}
					
					if (pairedEndImport && sections2.length < 5) {
						progressWarningReceived(new SeqMonkException("Not enough data from line '"+line+"'"));
						continue; // Skip this line...						
					}

										
					try {	
						if (pairedEndImport) {
							SequenceReadWithChromosome read = getPairedEndRead(sections1,sections2);
							newData[f].addData(read.chromosome,read.read);
						}
						else {
							SequenceReadWithChromosome read = getSingleEndRead(sections1);
							newData[f].addData(read.chromosome,read.read);
						}
					}
					catch (SeqMonkException ex) {
						progressWarningReceived(ex);
					}
	
				}
				
				// We're finished with the file.
				br.close();
				
				// Cache the data in the new dataset
				progressUpdated("Caching data from "+bowtieFiles[f].getName(), f, bowtieFiles.length);
				newData[f].finalise();
			}
		}	

		catch (Exception ex) {
			progressExceptionReceived(ex);
			return;
		}
		
		processingFinished(newData);
	}
	
	/**
	 * Gets a single single end read.
	 * 
	 * @param sections The set of tab-split strings from the bowtie output
	 * @return The read which was read
	 * @throws SeqMonkException
	 */
	private SequenceReadWithChromosome getSingleEndRead (String [] sections) throws SeqMonkException {
		int strand;
		int start;
		int end;
		
		try {
			
			start = Integer.parseInt(sections[3])+1;
			end = start+(sections[4].length()-1);
						
			if (sections[1].equals("+")) {
				strand = Location.FORWARD;
			}
			else if (sections[1].equals("-")) {
				strand = Location.REVERSE;
			}
			else {
				strand = Location.UNKNOWN;
			}
			
			if (extendBy != 0) {
				if (strand == Location.FORWARD) {
					end += extendBy;
					if (end < start) {
						end = start;
					}
				}
				else if (strand == Location.REVERSE) {
					start -= extendBy;
					if (start > end) {
						start = end;
					}
				}
			}
		}
		catch (NumberFormatException e) {
			throw new SeqMonkException("Location "+sections[3]+" was not an integer");
		}
		
		ChromosomeWithOffset c;
		try {
			c = collection.genome().getChromosome(sections[2]);
		}
		catch (Exception iae) {
			throw new SeqMonkException(iae.getLocalizedMessage());
		}
		
		start = c.position(start);
		end = c.position(end);
		
		// We also don't allow readings which are beyond the end of the chromosome
		if (end > c.chromosome().length()) {
			int overrun = end-c.chromosome().length();
			throw new SeqMonkException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
		}
		if (start < 1) {
			throw new SeqMonkException("Reading position "+start+" was before the start of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");			
		}

		// We can now make the new reading
		SequenceReadWithChromosome read = new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,end,strand));

		return read;
	}
	
	/**
	 * Gets a paired end read.
	 * 
	 * @param sections1 The tab split bowtie output sections for the first read
	 * @param sections2 The tab split bowtie output sections for the second read
	 * @return The paired end read which was read
	 * @throws SeqMonkException
	 */
	private SequenceReadWithChromosome getPairedEndRead (String [] sections1, String [] sections2) throws SeqMonkException {

		// We can get the lines with read two first, in which case we'll reverse things
		
		boolean readsAreReversed = false;
		if (sections1[0].substring(sections1[0].length()-1).equals("2")) {
			readsAreReversed = true;
		}
		
		int strand;
		int start;
		int end;
	
		try {

			/*
			 * This convention isn't true in newer bowtie files so we can't rely on this any more.
			 */
			
//			if (! sections1[0].substring(0, sections1[0].length()-2).equals(sections2[0].substring(0, sections2[0].length()-2))) {
//				throw new SeqMonkException("Paired reads '"+sections1[0]+"' and '"+sections2[0]+"' did not match names");
//			}
			
			
			int read1start = Integer.parseInt(sections1[3])+1;
			int read1end = read1start+(sections1[4].length()-1);

			int read2start = Integer.parseInt(sections2[3])+1;
			int read2end = read2start+(sections2[4].length()-1);

			if (read1start < read2start) {
				start = read1start;
			}
			else {
				start = read2start;
			}
			
			if (read2end > read1end) {
				end = read2end;
			}
			else {
				end = read1end;
			}
									
			if (sections1[1].equals("+") && sections2[1].equals("-")) {
				if (readsAreReversed) {
					strand = Location.REVERSE;					
				}
				else {
					strand = Location.FORWARD;
				}
			}
			else if (sections1[1].equals("-") && sections2[1].equals("+")) {
				if (readsAreReversed) {
					strand = Location.FORWARD;					
				}
				else {
					strand = Location.REVERSE;
				}
			}
			else {
				strand = Location.UNKNOWN;
			}
		}
		catch (NumberFormatException e) {
			throw new SeqMonkException("Location "+sections1[3]+" or "+sections2[3]+" was not an integer");
		}
		
		if ((end - start)+1 > pairedEndDistance) {
			throw new SeqMonkException("Distance between ends "+((end - start)+1)+" was larger than cutoff ("+pairedEndDistance+")");
		}

		// If a separate chromosome is reported then the reads are on different
		// chromosomes and won't be reported.
		
		if (! sections1[2].equals(sections2[2])) {
			throw new SeqMonkException("Paried end read was on a different chromosome");
		}

		ChromosomeWithOffset c;
		
		try {
			c = dataCollection().genome().getChromosome(sections1[2]);
		}
		catch (Exception e) {
			throw new SeqMonkException(e.getLocalizedMessage());
		}
		
		start = c.position(start);
		end = c.position(end);
		
		// We also don't allow readings which are beyond the end of the chromosome
		if (end > c.chromosome().length()) {
			int overrun = end - c.chromosome().length();
			throw new SeqMonkException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");
		}
		if (start < 1) {
			throw new SeqMonkException("Reading position "+start+" was before the start of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")");			
		}


		// We can now make the new reading
		SequenceReadWithChromosome read = new SequenceReadWithChromosome(c.chromosome(),SequenceRead.packPosition(start,end,strand));

		return read;
	}
		
			

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#description()
	 */
	public String description() {
		return "Imports Data from standard Bowtie format files";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getOptionsPanel()
	 */
	public JPanel getOptionsPanel() {
		return prefs;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#hasOptionsPanel()
	 */
	public boolean hasOptionsPanel() {
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#name()
	 */
	public String name() {
		return "Bowtie File Importer";
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
			
			public String getDescription() {
				return "Bowtie Text Files";
			}
		
			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().contains("bowtie")) {
					return true;
				}
				else {
					return false;
				}
			}
		
		};
	}
	
}
