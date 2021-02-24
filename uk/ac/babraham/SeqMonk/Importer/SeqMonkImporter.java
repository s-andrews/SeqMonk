/**
 * Copyright 2016- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Importer;

import java.io.File;
import java.io.FileNotFoundException;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.AnnotationParsers.GenomeParser;
import uk.ac.babraham.SeqMonk.DataParsers.BAMFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.BismarkCovFileParser;
import uk.ac.babraham.SeqMonk.DataParsers.DataParser;
import uk.ac.babraham.SeqMonk.DataParsers.DataParserOptionsPanel;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataWriters.SeqMonkDataWriter;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * This class is a non-interactive importer used to create new seqmonk projects
 * from a list of BAM files.
 * 
 * @author Andrewss
 *
 */

public class SeqMonkImporter implements ProgressListener {

	/**
	 * The arguments are:
	 * 
	 * 1 ) The genome to use (Species/Assembly)
	 * 2 ) The output file name
	 * 3 ) Whether to split for RNA-Seq (0 = auto, 1 = no, 2 = yes, 3 = yes+introns)
	 * 4 ) MapQ cutoff filter
	 * 5+) List of BAM files to import 
	 * 	
	 * @param args
	 */
	
	private boolean wait = false;
	private Genome genome;
	private DataCollection data;
	private int warningsCount;
	
	public SeqMonkImporter (String [] args) {
		
		String genomeString = args[0];
		
		// We need to turn whatever the user supplies into an absolute file path
		// otherwise later on when we call getParent on it we get a null value.
		// This step forces the extrapolation of a full system path which won't
		// have this problem.
		File outFile = new File(args[1]).getAbsoluteFile();
		int splitReads = Integer.parseInt(args[2]);
		int mapqCutoff = Integer.parseInt(args[3]);
		
		File [] files = new File[args.length-4];
		
		for (int i=0; i<files.length;i++) {
			files[i] = new File(args[i+4]);
		}
		
//		System.err.println("Genome = "+genomeString);
//		System.err.println("Outfile = "+args[1]);
//		System.err.println("Outfile path = "+outFile.getAbsolutePath());
//		System.err.println("Split Reads = "+splitReads);
//		System.err.println("MapqCutoff = "+mapqCutoff);
		
		SeqMonkPreferences prefs = SeqMonkPreferences.getInstance();
		
		// They may have defined a temp directory but not created it (happens a lot on clusters)
		// so we'll check this and try to make it if it's missing.
		
		File tempDir = SeqMonkPreferences.getInstance().tempDirectory();
		
		if (tempDir != null && !tempDir.exists()) {
			tempDir.mkdirs();
		}

		if (!tempDir.exists()) {
			System.err.println("ERROR: Cache directory "+tempDir.getAbsolutePath()+" doesn't exist and couldn't be created");
			System.exit(1);
		}
		
		System.err.println("Reading Genome");

		GenomeParser genomeParser = new GenomeParser();
		genomeParser.addProgressListener(this);
		wait = true;
		
		try {
			genomeParser.parseGenome(new File []{new File(prefs.getGenomeBase().getAbsolutePath()+"/"+genomeString)});
		}
		catch (FileNotFoundException fnfe) {
			System.err.println("Couldn't find genome for "+genomeString);
			fnfe.printStackTrace();
			System.exit(1);
		}

		while(wait) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		
		if (genome == null) {
			System.err.println("No genome loaded for some reason");
			System.exit(1);
		}
		
		data = new DataCollection(genome);
		
		System.err.println("Genome loaded for "+data.genome());

		System.err.println("Parsing Data");

		
		// We'll let them import either BAM files or coverage files
		// but we need to figure out which we've got.
				
		DataParser parser;
		
		boolean importBAM = true;
		
		if (files.length > 0 && (files[0].getName().toLowerCase().endsWith(".cov.gz") || files[0].getName().toLowerCase().endsWith(".cov"))) {
			importBAM = false;
		}

		if (importBAM) {
			System.err.println("Importing as BAM files");
			parser = new BAMFileParser(data);
		}
		else {
			System.err.println("Importing as Bismark coverage files");
			parser = new BismarkCovFileParser(data);
		}
		
		
		for (int f=0;f<files.length;f++) {
		
			System.err.println("Parsing "+files[f].getName());
			parser.setFiles(new File[]{files[f]});
			
			if (f==0 && importBAM) {
				// Fetching this will trigger the auto-configure of the settings
				// we only want to call this once so that we keep consistent settings
				// for all of the files we parse.  We also only call it if we're 
				// parsing BAM files.  There are no options to set if we're importing
				// coverage files.
				DataParserOptionsPanel options = (DataParserOptionsPanel)parser.getOptionsPanel();

				
				// If they've chosen to do auto-configure then we don't need to change the
				// splicing options.  If they've forced it then we do.
				if (splitReads == 1) {
					options.setSpliced(false);
				}
				else if (splitReads == 2) {
					options.setSpliced(true);
				}
				else if (splitReads == 3) {
					options.setSpliced(true);
					options.setIntrons(true);
				}
				
				
				// We'll assume that any positive MAPQ is a real cutoff.
				if (mapqCutoff >= 0) {
					options.setMinMappingQuality(mapqCutoff);
				}
			}
			
			parser.addProgressListener(this);
			wait = true;
			try {
				parser.parseData();
			} catch (SeqMonkException e) {
				e.printStackTrace();
				System.exit(1);
			}
		
			while(wait) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			}
			
			// See if there were any warnings
			if (warningsCount > 0) {
				System.err.println("There were "+warningsCount+" import warnings for "+files[f].getName());
				warningsCount = 0;
			}
			
		}
		
		SeqMonkDataWriter writer = new SeqMonkDataWriter();
		writer.addProgressListener(this);
		wait = true;
		writer.writeData(data, outFile);
		while (wait) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		
		System.err.println("All done");
	}
	
	
	public static void main(String[] args) {
		
//		for (int i=0;i<args.length;i++) {
//			System.err.println(""+(i+1)+":"+args[i]);
//		}
		
		
		new SeqMonkImporter(args);
	}

	@Override
	public void progressExceptionReceived(Exception e) {

		e.printStackTrace();
		System.exit(1);
	}

	public void progressWarningReceived(Exception e) {
		++warningsCount;
	}

	public void progressUpdated(String message, int current, int max) {}

	public void progressCancelled() {}

	public void progressComplete(String command, Object result) {
				
		if (command.equals("load_genome")) {
			genome = (Genome)result;
		}
	
		if (command.equals("datasets_loaded")) {
			DataSet [] newSets = (DataSet [])result;
			
			for (int i=0;i<newSets.length;i++) {
				data.addDataSet(newSets[i]);
			}
		}
		
		wait = false;
	}

}
