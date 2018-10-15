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
package uk.ac.babraham.SeqMonk.AnnotationParsers;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
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
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.MultiGenome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.SeqMonkFileFilter;

/**
 * This data parser is not used to read SeqMonk projects being opened, but
 * is a way in which data from one SeqMonk project can be imported into
 * another one.  It is a standard DataParser, except that it pauses after it
 * has read the list of samples to ask which ones the user wants to import.
 */
public class SeqMonkAnnotationReimportParser extends AnnotationParser {

	/** The Constant MAX_DATA_VERSION says what is the highest
	 * version of the SeqMonk file format this parser can understand.
	 * If the file to be loaded has a version higher than this then
	 * the parser won't attempt to load it. */
	public static final int MAX_DATA_VERSION = 18;

	private BufferedReader br;
	private int thisDataVersion = -1;

	/**
	 * Instantiates a new seq monk parser.
	 * 
	 * @param application The application which we're loading this file into
	 */
	public SeqMonkAnnotationReimportParser (Genome genome) {
		super(genome);
	}	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public AnnotationSet [] parseAnnotation (File file, Genome genome) throws Exception {


		progressUpdated("Scanning File "+file.getName(), 0, 1);

		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(fis)));
		} 
		catch (IOException ioe) {
			if (fis != null) {
				fis.close();
			}
			br = new BufferedReader(new FileReader(file));
		}


		String line;
		String [] sections;

		Vector<AnnotationSet> setsToKeep = new Vector<AnnotationSet>();

		while ((line = br.readLine()) != null) {

			if (cancel) {
				progressCancelled();
				return null;
			}
			
			sections = line.split("\\t");
			
			// Now we look where to send this...
			if (sections[0].equals("SeqMonk Data Version")) {
				parseDataVersion(sections);
			}				
			else if (sections[0].equals("Annotation")) {
				AnnotationSet as = parseAnnotation(sections, genome);
				
				setsToKeep.add(as);
				
				// Once we've parsed the samples we don't care about anything else.
//				break;

			}
		}

		// We're finished with the file
		br.close();


		return(setsToKeep.toArray(new AnnotationSet[0]));
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
	 * Parses an external set of annotations
	 * 
	 * @param sections The tab split initial annotation line
	 * @throws SeqMonkException
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private AnnotationSet parseAnnotation (String [] sections, Genome genome) throws SeqMonkException, IOException {

		if (sections.length != 3) {
			throw new SeqMonkException("Annotation line didn't contain 3 sections");
		}

		AnnotationSet set = new AnnotationSet(genome, sections[1]);

		int featureCount = Integer.parseInt(sections[2]);

		for (int i=0;i<featureCount;i++) {

			if (i%1000 == 0) {
				progressUpdated("Parsing annotation in "+set.name(), i, featureCount);
			}

			sections = br.readLine().split("\\t");
			Chromosome c;
			try {
				c = genome.getChromosome(sections[1]).chromosome();
			}
			catch (Exception sme) {
				progressWarningReceived(new SeqMonkException("Annotation feature could not be mapped to chromosome '"+sections[1]+"'"));
				continue;
			}
			Feature f = new Feature(sections[0], c.name());
			//TODO: Can we improve this to not use a Split Location each time?
			f.setLocation(new SplitLocation(sections[2]));

			for (int a=3;a+1<sections.length;a+=2) {
				f.addAttribute(sections[a], sections[a+1]);
			}

			set.addFeature(f);
		}

		set.finalise();

		return set;
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


	private class AnnotationSetSelector extends JDialog {

		private JList list;

		public AnnotationSetSelector (AnnotationSet [] annotationSets) {
			super(SeqMonkApplication.getInstance(),"Select AnnotationSets to import");
			setModal(true);
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

			list = new JList(annotationSets);
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


	@Override
	public FileFilter fileFilter() {
		return new SeqMonkFileFilter();
	}

	@Override
	public boolean requiresFile() {
		return true;
	}

}
