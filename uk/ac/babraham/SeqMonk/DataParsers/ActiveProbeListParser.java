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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Takes a Probe List and turns it into a DataSet
 */
public class ActiveProbeListParser extends DataParser {

	private ActiveDataParserOptionsPanel prefs;
	private ProbeList activeList;

	/**
	 * Instantiates a new active store parser.
	 * 
	 * @param data The dataCollection to which new data will be added and from which the active set will be taken
	 */
	public ActiveProbeListParser (SeqMonkApplication application) {
		super(application.dataCollection());
		activeList = application.dataCollection().probeSet().getActiveList();
		prefs = new ActiveDataParserOptionsPanel();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getFileFilter()
	 */
	public FileFilter getFileFilter () {
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		DataSet newData = processNormalDataStore(activeList);

		processingFinished(new DataSet [] {newData});
	}	


	private DataSet processNormalDataStore (ProbeList activeList) {

		int extendBy = prefs.extendReads();
		boolean reverse = prefs.reverseReads();
		
		boolean modifyStrand = false;
		
		int forcedStrand = 0;
		
		
		if (!prefs.strandOptionBox.getSelectedItem().equals("From probes")) {
			modifyStrand = true;

			if (prefs.strandOptionBox.getSelectedItem().equals("Forward")) {
				forcedStrand = Location.FORWARD;
			}
			else if (prefs.strandOptionBox.getSelectedItem().equals("Reverse")) {
				forcedStrand = Location.REVERSE;
			}
			else if (prefs.strandOptionBox.getSelectedItem().equals("Unknown")) {
				forcedStrand = Location.UNKNOWN;
			}
			else {
				throw new IllegalArgumentException("Unknown forced strand option "+prefs.strandOptionBox.getSelectedItem());
			}
		
		}
		
		
		
		

		DataSet newData = new DataSet(activeList.name(),"Reimported from "+activeList.name(),prefs.removeDuplicates());

		// Now process the data
		Chromosome [] chrs = dataCollection().genome().getAllChromosomes();

		for (int c=0;c<chrs.length;c++) {

			progressUpdated("Processing "+activeList.name()+" chr "+chrs[c].name(),c,chrs.length);

			Probe [] probes = activeList.getProbesForChromosome(chrs[c]);


			for (int r=0;r<probes.length;r++) {

				if (cancel) {
					progressCancelled();
					return null;
				}

				long read;

				int start = probes[r].start();
				int end = probes[r].end();

				int strand = probes[r].strand();
								
				if (reverse) {
					if (strand == Location.FORWARD) {
						strand = Location.REVERSE;
					}
					else if (strand == Location.REVERSE) {
						strand = Location.FORWARD;
					}

				}
								

				if (extendBy != 0) {
					
					// We now allow negative extensions to shorten reads
					if (strand == Location.FORWARD || strand == Location.UNKNOWN) {
						end += extendBy;
						if (end < start) end = start;
						
						
					}
					else if (strand == Location.REVERSE) {
						start -= extendBy;
						
						if (start > end) start = end;
					}
				}

				// We don't allow reads before the start of the chromosome
				if (start < 1) {
					int overrun = (0 - start)+1;
					progressWarningReceived(new SeqMonkException("Reading position "+start+" was "+overrun+"bp before the start of chr"+chrs[c].name()+" ("+chrs[c].length()+")"));
					continue;
				}

				// We also don't allow readings which are beyond the end of the chromosome
				if (end > chrs[c].length()) {
					int overrun = end - chrs[c].length();
					progressWarningReceived(new SeqMonkException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+chrs[c].name()+" ("+chrs[c].length()+")"));
					continue;
				}
				

				
				// Force the strand to what they specified if they want this.
				if (modifyStrand) {
					strand = forcedStrand;
				}


				// We can now make the new reading
				try {
					read = SequenceRead.packPosition(start,end,strand);
					newData.addData(chrs[c],read);
				}
				catch (SeqMonkException e) {
					progressWarningReceived(e);
					continue;
				}

			}
		}

		return newData;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#description()
	 */
	public String description() {
		return "Imports General Feature Format Files";
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
		return "Active Probe List Importer";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#readyToParse()
	 */
	public boolean readyToParse() {
		return true;
	}

	private class ActiveDataParserOptionsPanel extends JPanel {

		private JComboBox removeDuplicates;
		private JCheckBox reverseReads;
		private JTextField extendReads;
		private JComboBox strandOptionBox;

		public ActiveDataParserOptionsPanel () {
			setLayout(new BorderLayout());

			JPanel commonOptions = new JPanel();

			commonOptions.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx = 0.5;
			gbc.weighty = 0.5;
			gbc.fill=GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(5,5,5,5);

			commonOptions.add(new JLabel("Remove duplicate reads"),gbc);
			removeDuplicates = new JComboBox(new String [] {"No","Yes, based on start", "Yes, based on end", "Yes, start and end"});
			
			gbc.gridx=2;
			commonOptions.add(removeDuplicates,gbc);

			gbc.gridx=1;
			gbc.gridy++;
			

			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Strand of reads"),gbc);
			gbc.gridx=2;
			strandOptionBox = new JComboBox(new String [] {"From probes","Forward","Reverse","Unknown"});
			commonOptions.add(strandOptionBox,gbc);
			gbc.gridx=1;
			gbc.gridy++;
						


			commonOptions.add(new JLabel("Extend reads by (bp)"),gbc);
			gbc.gridx=2;
			extendReads = new JTextField(6);
			extendReads.addKeyListener(new NumberKeyListener(false, true));
			commonOptions.add(extendReads,gbc);

			
			gbc.gridx=1;
			gbc.gridy++;
			commonOptions.add(new JLabel("Reverse all reads"),gbc);
			gbc.gridx=2;
			reverseReads = new JCheckBox();
			commonOptions.add(reverseReads,gbc);
			gbc.gridx=1;
			gbc.gridy++;
						


			commonOptions.add(new JLabel("Extend reads by (bp)"),gbc);
			gbc.gridx=2;
			extendReads = new JTextField(6);
			extendReads.addKeyListener(new NumberKeyListener(false, true));
			commonOptions.add(extendReads,gbc);

			add(commonOptions,BorderLayout.NORTH);


		}


		public int removeDuplicates () {
			if (removeDuplicates.getSelectedItem().equals("No")) {
				return DataSet.DUPLICATES_REMOVE_NO;
			}
			else if (removeDuplicates.getSelectedItem().equals("Yes, based on start")) {
				return DataSet.DUPLICATES_REMOVE_START;
			}
			else if (removeDuplicates.getSelectedItem().equals("Yes, based on end")) {
				return DataSet.DUPLICATES_REMOVE_END;
			}
			else if (removeDuplicates.getSelectedItem().equals("Yes, start and end")) {
				return DataSet.DUPLICATES_REMOVE_START_END;
			}
			
			throw new IllegalStateException("Didn't understand duplicate string "+removeDuplicates.getSelectedItem());
		}


		public boolean reverseReads () {
			return reverseReads.isSelected();
		}

		public int extendReads () {
			if (extendReads == null || extendReads.getText().length()==0) {
				return 0;
			}
			return Integer.parseInt(extendReads.getText());
		}

		public Dimension getPreferredSize () {
			return new Dimension(300,150);
		}

	}
}
