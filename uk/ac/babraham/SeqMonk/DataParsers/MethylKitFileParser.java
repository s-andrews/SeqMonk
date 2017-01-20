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
 * Parses data files for methylation data coming out of 
 * the R MethylKit package.
 */
public class MethylKitFileParser extends DataParser {
		
	private DataParserOptionsPanel prefs = new DataParserOptionsPanel(false, false, false,false);
	
	/**
	 * Instantiates a new QasR file parser.
	 * 
	 * @param collection The dataCollection into which data will be put.
	 */
	public MethylKitFileParser(DataCollection collection) {
		super(collection);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
	
//		System.err.println("Started parsing cov files");
		
		try {
			
			File [] methylKitFiles = getFiles();
			DataSet [] newData = new DataSet[methylKitFiles.length];
			
			for (int f=0;f<methylKitFiles.length;f++) {
				BufferedReader br;
				
				if (methylKitFiles[f].getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(methylKitFiles[f]))));	
				}
				else {
					br = new BufferedReader(new FileReader(methylKitFiles[f]));
				}
				
				String line;
	
				newData[f] = new DataSet(methylKitFiles[f].getName(),methylKitFiles[f].getCanonicalPath(),prefs.removeDuplicates());
								
				int lineCount = 0;
				// Now process the file
				while ((line = br.readLine())!= null) {
					
					if (cancel) {
						br.close();
						progressCancelled();
						return;
					}
					
					if (line.trim().length() == 0) continue;  //Ignore blank lines
					if (line.startsWith("#")) continue;       // In case it has comments
					if (line.startsWith("chrBase")) continue; // This is the start of the header
					
					++lineCount;
					if (lineCount%100000 == 0) {
						progressUpdated("Read "+lineCount+" lines from "+methylKitFiles[f].getName(),f,methylKitFiles.length);
					}
					String [] sections = line.split("\t");
					
					/*
					 * The MethylKot file fields are:
					 *    1. location - Combination of chr and pos which we're ignoring
					 *    2. chr - the chromosome with leading chr
					 *    3. position - the base position
					 *    4. strand - F or R to indicate strand
					 *    5. total count - integer count of total coverage
					 *    6. freqC - the percentage of total coverage which was C (meth)
					 *    7. freqT - the percentage of total coverage which was T (unmeth)
					 *    
					 *    It's not clear whether it's guaranteed that 6+7 === 100 but we'll not
					 *    assume that in case there are other bases found at that position.
					 *    
					 */
					
					// Check to see if we've got enough data to work with
					if (sections.length < 6) {
						progressWarningReceived(new SeqMonkException("Not enough data from line '"+line+"'"));
						continue; // Skip this line...						
					}
						
					int position;
					int totalCount;
					int methCount;
					int unmethCount;
					
					try {
						position = Integer.parseInt(sections[2]);
						totalCount = Integer.parseInt(sections[4]);
						methCount = Math.round((Float.parseFloat(sections[5])/100)*totalCount);						
						unmethCount = Math.round((Float.parseFloat(sections[6])/100)*totalCount);
					}
					catch (NumberFormatException e) {
						progressWarningReceived(new SeqMonkException("Failed to parse position and counts from "+line));
						continue;
					}
					
//					System.err.println("Pos="+position+" total="+totalCount+" meth="+methCount+" unmeth="+unmethCount);
					
					try {
						ChromosomeWithOffset c = dataCollection().genome().getChromosome(sections[1]);
						// We also don't allow readings which are beyond the end of the chromosome
						if (position > c.chromosome().length()) {
							int overrun = position - c.chromosome().length();
							progressWarningReceived(new SeqMonkException("Reading position "+position+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")"));
							continue;
						}
		
						// We can now make the new reads
						long methRead = SequenceRead.packPosition(position,position,Location.FORWARD);
						long unmethRead = SequenceRead.packPosition(position,position,Location.REVERSE);
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
				progressUpdated("Caching data from "+methylKitFiles[f].getName(), f, methylKitFiles.length);
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
		return "Imports MethylKit methylation files";
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
		return "MethylKit file parser";
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
				return true;
//				return (pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".cov") || pathname.getName().toLowerCase().endsWith(".cov.gz"));
			}

			public String getDescription() {
				return "MethylKit Files";
			}
		};
	}
	
}
