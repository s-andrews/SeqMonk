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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.PairedDataSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.MultiGenome;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * This data parser is not used to read SeqMonk projects being opened, but
 * is a way in which data from one SeqMonk project can be imported into
 * another one.  It is a standard DataParser, except that it pauses after it
 * has read the list of samples to ask which ones the user wants to import.
 */
public class SeqMonkReimportParser extends DataParser {

	/** The Constant MAX_DATA_VERSION says what is the highest
	 * version of the SeqMonk file format this parser can understand.
	 * If the file to be loaded has a version higher than this then
	 * the parser won't attempt to load it. */
	public static final int MAX_DATA_VERSION = 17;

	private BufferedReader br;
	private int thisDataVersion = -1;
	private Genome genome;

	/**
	 * Instantiates a new seq monk parser.
	 * 
	 * @param application The application which we're loading this file into
	 */
	public SeqMonkReimportParser (DataCollection collection) {
		super(collection);
		genome = collection.genome();
	}	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		File [] files = getFiles();
		Vector<DataSet>allDataSets = new Vector<DataSet>();

		for (int f=0;f<files.length;f++) {

			progressUpdated("Scanning File "+files[f].getName(), f, files.length);

			FileInputStream fis = null;

			try {
				fis = new FileInputStream(files[f]);
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(fis)));
			}
			catch (IOException ioe) {

				try {
					if (fis != null) {
						fis.close();
					}
					br = new BufferedReader(new FileReader(files[f]));
				} 
				catch (IOException ex) {
					progressExceptionReceived(ex);
					return;
				}
			}

			try {
				String line;
				String [] sections;

				while ((line = br.readLine()) != null) {

					sections = line.split("\\t");

					// Now we look where to send this...
					if (sections[0].equals("SeqMonk Data Version")) {
						parseDataVersion(sections);
					}				
					else if (sections[0].equals("Samples")) {
						DataSet [] dataSets = parseSamples(sections);
						for (int i=0;i<dataSets.length;i++) {
							allDataSets.add(dataSets[i]);
						}

						// Once we've parsed the samples we don't care about anything else.
						break;

					}
					else if (sections[0].equals("Genome")) {
						try {
							parseGenome(sections);
						}
						catch (SeqMonkException sme) {
							progressWarningReceived(sme);
							break;
						}

					}
				}

				// We're finished with the file
				br.close();

			}
			catch (Exception ex) {
				progressExceptionReceived(ex);
				try {
					br.close();
				} 
				catch (IOException e1) {
					throw new IllegalStateException(e1);
				}
				return;
			}

		}

		processingFinished(allDataSets.toArray(new DataSet[0]));
	}	


	/**
	 * Parses the data version.
	 * 
	 * @param sections data version line split on tabs
	 * @throws SeqMonkException
	 */
	private void parseDataVersion (String [] sections) throws SeqMonkException {
		if (sections.length != 2) {
			throw new SeqMonkException("Data Version line didn't contain 2 sections");
		}

		thisDataVersion = Integer.parseInt(sections[1]);

		if (thisDataVersion > MAX_DATA_VERSION) {
			throw new SeqMonkException("This data file needs a newer verison of SeqMonk to read it.");
		}
	}


	/**
	 * Parses the list of samples.
	 * 
	 * @param sections The tab split initial samples line
	 * @throws SeqMonkException
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private DataSet [] parseSamples (String [] sections) throws SeqMonkException, IOException {
		if (sections.length != 2) {
			throw new SeqMonkException("Samples line didn't contain 2 sections");
		}
		if (! sections[0].equals("Samples")){
			throw new SeqMonkException("Couldn't find expected samples line");
		}

		int n = Integer.parseInt(sections[1]);

		// We need to keep the Data Sets around to add data to later.

		// We're going to start all of these datasets off, but only collect data for some
		// of them.
		DataSet [] dataSets = new DataSet[n];

		for (int i=0;i<n;i++) {
			sections = br.readLine().split("\\t");
			// Originally there was only one section (the DataSet name).  Then
			// there were two names, a user supplied name and the original
			// imported file name.  Now there are 3 sections where the third section
			// indicates the type of dataset.  The only unusual one is a HiC dataset
			// anything else is assumed to be a normal dataset.
			if (sections.length == 1) {
				dataSets[i] = new DataSet(sections[0],"Not known",DataSet.DUPLICATES_REMOVE_NO);
			}
			else if (sections.length == 2) {
				dataSets[i] = new DataSet(sections[0],sections[1],DataSet.DUPLICATES_REMOVE_NO);
			}
			else if (sections.length == 3) {
				if (sections[2].equals("HiC")) {
					dataSets[i] = new PairedDataSet(sections[0],sections[1],DataSet.DUPLICATES_REMOVE_NO,0,false);					
				}
				else {
					dataSets[i] = new DataSet(sections[0],sections[1],DataSet.DUPLICATES_REMOVE_NO);
				}
			}
		}

		// At this point we need to know which of the datasets we're actually going
		// to import into the current project

		DataSetSelector selector = new DataSetSelector(dataSets);
		boolean [] dataSetsToParse = new boolean [dataSets.length];
		int lastDataSetToParse = 0;


		for (int i=0;i<dataSetsToParse.length;i++) {
			dataSetsToParse[i] = false;
		}

		int [] selectedIndices = selector.getSelectedIndices();

		for (int i=0;i<selectedIndices.length;i++) {
			dataSetsToParse[selectedIndices[i]] = true;
			if (selectedIndices[i]>lastDataSetToParse)
				lastDataSetToParse = selectedIndices[i];
		}


		// Now we can go through the rest of the sets and add data where appropriate

		// Immediately after the list of samples comes the lists of reads
		String line;


		// Iterate through the number of samples
		for (int i=0;i<n;i++) {


			if (i > lastDataSetToParse) {
				break; // No sense skipping through a load of data we're not going to import.
			}


			if (dataSetsToParse[i]) {
				progressUpdated("Reading data for "+dataSets[i].name(),i*10,n*10);
			}
			else {
				progressUpdated("Skipping "+dataSets[i].name(),i*10,n*10);				
			}


			// The first line is 
			line = br.readLine();
			sections = line.split("\t");
			if (sections.length != 2) {
				throw new SeqMonkException("Read line "+i+" didn't contain 2 sections");
			}
			int readCount = Integer.parseInt(sections[0]);

			// In versions prior to 7 we encoded everything on every line separately
			if (thisDataVersion < 7) {

				for (int r=0;r<readCount;r++) {

					if ((r % (1 +(readCount/10))) == 0) {
						if (dataSetsToParse[i]) {
							progressUpdated("Reading data for "+dataSets[i].name(),i*10+(r / (1 + (readCount/10))),n*10);
						}
						else {
							progressUpdated("Skipping "+dataSets[i].name(),i*10+(r / (1 + (readCount/10))),n*10);							
						}
					}


					line = br.readLine();
					if (line == null) {
						throw new SeqMonkException("Ran out of data whilst parsing reads for sample "+i);					
					}
					if (dataSetsToParse[i]) {
						sections = line.split("\t");
						Chromosome c;
						try {
							c = genome.getChromosome(sections[0]).chromosome();
						}
						catch (IllegalArgumentException sme) {
							progressWarningReceived(new SeqMonkException("Read from sample "+i+" could not be mapped to chromosome '"+sections[0]+"'"));
							continue;
						}

						int start = Integer.parseInt(sections[1]);
						int end = Integer.parseInt(sections[2]);
						int strand = Integer.parseInt(sections[3]);

						try {
							dataSets[i].addData(c,SequenceRead.packPosition(start, end, strand));
						}
						catch (SeqMonkException ex) {
							progressWarningReceived(ex);
							continue;
						}
					}
				}
			}

			else if (dataSets[i] instanceof PairedDataSet) {
				// Paired Data sets have a different format, with a packed position for
				// the first read, then a chromosome name and packed position for the second
				// read.


				// From v13 onwards we started entering HiC data pairs in both directions so
				// the data would be able to be read back in without sorting it.
				boolean hicDataIsPresorted = thisDataVersion >= 13;

				while (true) {
					// The first line should be the chromosome and a number of reads
					line = br.readLine();

					if (line == null) {
						throw new SeqMonkException("Ran out of data whilst parsing reads for sample "+i);					
					}

					if (dataSetsToParse[i]) {
						// A blank line indicates the end of the sample
						if (line.length() == 0) break;

						sections = line.split("\\t");

						Chromosome c = genome.getChromosome(sections[0]).chromosome();
						//					progressUpdated("Reading data for "+dataSets[i].name(),i*application.dataCollection().genome().getChromosomeCount()+seenChromosomeCount,n*application.dataCollection().genome().getChromosomeCount());


						int chrReadCount = Integer.parseInt(sections[1]);

						//						System.err.println("Trying to parse "+chrReadCount+" from chr "+c.name());

						for (int r=0;r<chrReadCount;r++) {

							line = br.readLine();
							if (line == null) {
								throw new SeqMonkException("Ran out of data whilst parsing reads for sample "+i);					
							}

							/*
							 * We used to have a split("\t") here, but this turned out to be the bottleneck
							 * which hugely restricted the speed at which the data could be read.  Switching
							 * for a set of index calls and substring makes this *way* faster.
							 */

							long packedPosition = Long.parseLong(line.substring(0, line.indexOf('\t')));

							Chromosome c2 = genome.getChromosome(line.substring(line.indexOf('\t'),line.lastIndexOf(('\t')))).chromosome();

							long packedPosition2 = Long.parseLong(line.substring(line.lastIndexOf('\t')+1,line.length()));


							try {
								dataSets[i].addData(c,packedPosition,hicDataIsPresorted);
								dataSets[i].addData(c2,packedPosition2,hicDataIsPresorted);
							}
							catch (SeqMonkException ex) {
								progressWarningReceived(ex);
								continue;				
							}
						}
					}

				}
			}


			else {
				// In versions after 7 we split the section up into chromosomes
				// so we don't put the chromosome on every line, and we put out
				// the packed double value rather than the individual start, end
				// and strand

				// As of version 12 we collapse repeated reads into one line with
				// a count after it, so we need to check for this.

				// We keep count of reads processed to update the progress listeners
				int readsRead = 0;

				while (true) {
					// The first line should be the chromosome and a number of reads
					line = br.readLine();

					if (line == null) {
						throw new SeqMonkException("Ran out of data whilst parsing reads for sample "+i);					
					}

					// A blank line indicates the end of the sample
					if (line.length() == 0) break;

					if (dataSetsToParse[i]) {
						sections = line.split("\\t");

						// We don't try to capture this exception since we can't then process
						// any of the reads which follow.
						Chromosome c = genome.getChromosome(sections[0]).chromosome();

						int chrReadCount = Integer.parseInt(sections[1]);

						int tabIndexPosition = 0;
						for (int r=0;r<chrReadCount;r++) {

							readsRead++;
							if ((readsRead % (1 +(readCount/10))) == 0) {
								if (dataSetsToParse[i]) {
									progressUpdated("Reading data for "+dataSets[i].name(),i*10+(readsRead / (1 + (readCount/10))),n*10);
								}
								else {
									progressUpdated("Skipping "+dataSets[i].name(),i*10+(readsRead / (1 + (readCount/10))),n*10);
								}
							}


							line = br.readLine();
							if (line == null) {
								throw new SeqMonkException("Ran out of data whilst parsing reads for sample "+i);					
							}

							long packedPosition;
							try {	
								if (line.indexOf("\t") != -1) {
									// We have an index with a count
									sections = line.split("\t");

									tabIndexPosition = line.indexOf("\t");
									packedPosition = Long.parseLong(line.substring(0,tabIndexPosition));
									int count = Integer.parseInt(line.substring(tabIndexPosition+1));

									//TODO: Does this break the progress bar sometimes?
									r+= (count -1);
									readsRead += (count-1);

									for (int x=1;x<=count;x++) {
										dataSets[i].addData(c,packedPosition);								
									}
								}

								else {
									packedPosition = Long.parseLong(line);
									dataSets[i].addData(c,packedPosition);
								}
							}
							catch (SeqMonkException ex) {
								progressWarningReceived(ex);
							}

						}

					}
				}

			}

			if (dataSetsToParse[i]) {
				dataSets[i].finalise();
			}

		}
		// Finally we need to make up an array of just the datasets we actually read
		Vector<DataSet>keepers = new Vector<DataSet>();
		for (int i=0;i<dataSets.length;i++) {
			if (dataSetsToParse[i]) {
				keepers.add(dataSets[i]);
			}
		}

		return keepers.toArray(new DataSet[0]);

	}


	/**
	 * Parses the genome line.
	 * 
	 * @param sections The tab split sections from the genome line
	 * @throws SeqMonkException
	 */
	private void parseGenome (String [] sections) throws SeqMonkException {
		if (sections.length != 3) {
			throw new SeqMonkException("Genome line didn't contain 3 sections");
		}
		if (! sections[0].equals("Genome")){
			throw new SeqMonkException("First line of file was not the genome description");
		}

		// We can have multi-genome projects now, but the simple string match we do
		// here should still work under these circumstances.  We could try to do better
		// and say that if any of the individual sections match then we should allow the
		// cross import since that should also work.
		
		
		
		if (!(genome.species().equals(sections[1]) && genome.assembly().equals(sections[2]))) {
			
			// We give them a chance to redeem themselves if this is a multi-genome and the
			// incoming file is one of the sub-genomes
			if (genome instanceof MultiGenome) {

				Genome [] subGenomes = ((MultiGenome)genome).getSubGenomes();
				
				for (int s=0;s<subGenomes.length;s++) {
										
					if (subGenomes[s].species().equals(sections[1]) && subGenomes[s].assembly().equals(sections[2])) {
						// That's close enough
						return;
					}
				}
				
				
			}
			
			throw new SeqMonkException("Genome versions didn't match between SeqMonk files ("+sections[1]+" "+sections[2]+" vs "+genome.species()+" "+genome.assembly()+")");
		}	
	}




	public String description() {
		return "Reads in data sets from one SeqMonk project into another";
	}



	public JPanel getOptionsPanel() {
		return null;
	}



	public boolean hasOptionsPanel() {
		return false;
	}



	public String name() {
		return "SeqMonk reimport parser";
	}


	public boolean readyToParse() {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getFileFilter()
	 */
	public FileFilter getFileFilter () {
		return new FileFilter() {

			public String getDescription() {
				return "SeqMonk Files";
			}

			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(".smk")) {
					return true;
				}
				else {
					return false;
				}
			}

		};
	}


	private class DataSetSelector extends JDialog {

		private JList list;

		public DataSetSelector (DataSet [] dataSets) {
			super(SeqMonkApplication.getInstance(),"Select DataSets to import");
			setModal(true);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

			list = new JList(dataSets);
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(new JScrollPane(list),BorderLayout.CENTER);

			JButton importButton = new JButton("Import");
			importButton.addActionListener(new ActionListener() {	
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					dispose();
				}
			});

			getContentPane().add(importButton,BorderLayout.SOUTH);

			setSize(400,400);
			setLocationRelativeTo(SeqMonkApplication.getInstance());
			setVisible(true);		
		}

		public int [] getSelectedIndices () {
			return list.getSelectedIndices();
		}
	}

}
