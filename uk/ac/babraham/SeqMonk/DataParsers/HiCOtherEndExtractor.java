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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

/**
 * Extracts the other end reads from the currently visible set of HiC datasets
 * and makes them into new datasets
 */
public class HiCOtherEndExtractor extends DataParser {

	private HiCDataStore [] visibleHiCDataSets;
	private HiCOptionsPanel prefs;


	/**
	 * Instantiates a new active store parser.
	 * 
	 * @param data The dataCollection to which new data will be added and from which the active set will be taken
	 */
	public HiCOtherEndExtractor (DataCollection data, DataStore [] visibleStores) {
		super(data);

		prefs = new HiCOptionsPanel();
		
		// Extract out the HiC datasets
		Vector<HiCDataStore>keepers = new Vector<HiCDataStore>();
		for (int i=0;i<visibleStores.length;i++) {
			if (visibleStores[i] instanceof HiCDataStore && ((HiCDataStore)visibleStores[i]).isValidHiC()) {
				keepers.add((HiCDataStore)visibleStores[i]);
			}
		}

		visibleHiCDataSets = keepers.toArray(new HiCDataStore[0]);

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

		// We need to get the baits and other options from the preferences
		
		Probe [] baits = prefs.getBaits();
		boolean mergeIntoOne = prefs.mergeIntoOne();
		boolean excludeSelf = prefs.excludeSelf();
		
		DataSet [] newData;
		
		if (mergeIntoOne) {
			newData = new DataSet[visibleHiCDataSets.length];
		}
		else {
			newData = new DataSet[visibleHiCDataSets.length*baits.length];			
		}

		try {

			for (int d=0;d<visibleHiCDataSets.length;d++) {
				
				if (baits.length == 1) {
					System.err.println("Only one bait");
					newData[d] = new DataSet(prefs.prefix.getText()+"_"+visibleHiCDataSets[d].name(),"HiC other end of "+visibleHiCDataSets[d].name()+" for region "+baits[0].chromosome().name()+":"+baits[0].start()+"-"+baits[0].end(),DataSet.DUPLICATES_REMOVE_NO);
				}
				else if (mergeIntoOne){
					System.err.println("Multiple baits, but merging");
					newData[d] = new DataSet(prefs.prefix.getText()+"_"+visibleHiCDataSets[d].name(),"HiC other end of "+visibleHiCDataSets[d].name()+" for "+baits.length+" regions",DataSet.DUPLICATES_REMOVE_NO);
				}

				for (int b=0;b<baits.length;b++) {

					if (cancel) {
						progressCancelled();
						return;
					}

					progressUpdated("Getting other ends from "+visibleHiCDataSets[d].name()+" and "+baits[b].toString(),(d*baits.length)+b,visibleHiCDataSets.length*baits.length);
					if (!(baits.length == 1 || mergeIntoOne)) {
						newData[(d*baits.length)+b] = new DataSet(baits[b].toString()+"_"+visibleHiCDataSets[d].name(),"HiC other end of "+visibleHiCDataSets[d].name()+" for region "+baits[b].chromosome().name()+":"+baits[b].start()+"-"+baits[b].end(),DataSet.DUPLICATES_REMOVE_NO);						
					}

					HiCHitCollection hiCCollection = visibleHiCDataSets[d].getHiCReadsForProbe(baits[b]);

					String [] chromosomes = hiCCollection.getChromosomeNamesWithHits();

					for (int c=0;c<chromosomes.length;c++) {

						Chromosome chromosome = collection.genome().getChromosome(chromosomes[c]).chromosome();

						long [] reads = hiCCollection.getHitPositionsForChromosome(chromosomes[c]);

						for (int r=0;r<reads.length;r++) {

							if (cancel) {
								progressCancelled();
								return;
							}
							
							if (excludeSelf && baits[b].chromosome().name().equals(chromosomes[c]) && SequenceRead.overlaps(reads[r], baits[b].packedPosition())) {
								continue;
							}
							
							if (mergeIntoOne) {
								newData[d].addData(chromosome,reads[r]);								
							}
							else {
								newData[(d*baits.length)+b].addData(chromosome,reads[r]);
							}
						}
					}
					
					if (!mergeIntoOne) {
						// Cache the data in the new dataset
						progressUpdated("Caching data",(d*baits.length)+b , visibleHiCDataSets.length*baits.length);
						newData[(d*baits.length)+b].finalise();
					}
				}
				
				if (mergeIntoOne) {
					// Cache the data in the new dataset
					progressUpdated("Caching data",d , visibleHiCDataSets.length);
					newData[d].finalise();
				}
				
			}


			processingFinished(newData);
		}	

		catch (Exception ex) {
			progressExceptionReceived(ex);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#description()
	 */
	public String description() {
		return "Extracts other end information from HiC datasets";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getOptionsPanel()
	 */
	public JPanel getOptionsPanel() {
		if (prefs == null) {
			prefs = new HiCOptionsPanel();
		}
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
		return "HiC Other End import";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#readyToParse()
	 */
	public boolean readyToParse() {
		return true;
	}

	private class HiCOptionsPanel extends JPanel {

		private JTextField prefix;
		private JComboBox sourcesBox;
		private JCheckBox mergeIntoSingleLaneBox;
		private JCheckBox excludeSameFragmentBox;
		

		public HiCOptionsPanel () {

			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.weightx = 0.5;
			gbc.weighty = 0.5;
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			add(new JLabel("Bait Source"),gbc);

			gbc.gridx++;
			sourcesBox = new JComboBox(new String [] {"Current visible region","Active probe list"});
			add(sourcesBox,gbc);
			
			gbc.gridx=1;
			gbc.gridy++;
			
			add(new JLabel("Data Store Prefix"),gbc);

			gbc.gridx++;
			prefix = new JTextField("prefix",10);
			add(prefix,gbc);
						
			gbc.gridx=1;
			gbc.gridy++;
			
			add(new JLabel("Merge into single track"),gbc);

			gbc.gridx++;
			mergeIntoSingleLaneBox = new JCheckBox();
			mergeIntoSingleLaneBox.setEnabled(false);
			add(mergeIntoSingleLaneBox,gbc);
			
			gbc.gridx=1;
			gbc.gridy++;
			
			add(new JLabel("Exclude same bait ditags"),gbc);

			gbc.gridx++;
			excludeSameFragmentBox = new JCheckBox();
			add(excludeSameFragmentBox,gbc);
			
			sourcesBox.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent ae) {
					if (sourcesBox.getSelectedItem().equals("Current visible region")) {
						prefix.setEnabled(true);
						mergeIntoSingleLaneBox.setEnabled(false);
					}
					else {
						if (mergeIntoSingleLaneBox.isSelected()) {
							prefix.setEnabled(true);
						}
						else {
							prefix.setEnabled(false);
						}
						mergeIntoSingleLaneBox.setEnabled(true);
					}
					
				}
			});
			
			mergeIntoSingleLaneBox.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent ae) {

					if (sourcesBox.getSelectedItem().equals("Active probe list") && !mergeIntoSingleLaneBox.isSelected()) {
						prefix.setEnabled(false);
					}
					else {
						prefix.setEnabled(true);
					}
				}
			});
			
		}
		
		public Probe [] getBaits () {
			
			if (sourcesBox.getSelectedItem().equals("Active probe list")) {
				return collection.probeSet().getActiveList().getAllProbes();
			}
			else {
				return new Probe [] {new Probe(DisplayPreferences.getInstance().getCurrentChromosome(),DisplayPreferences.getInstance().getCurrentLocation())};
			}
		}
		
		public boolean mergeIntoOne () {
			if (sourcesBox.getSelectedItem().equals("Active probe list") && mergeIntoSingleLaneBox.isSelected()) {
				return true;
			}
			else {
				return false;
			}
		}

		public boolean excludeSelf () {
			return excludeSameFragmentBox.isSelected();
		}

		
		public Dimension getPreferredSize () {
			return new Dimension(200,100);
		}

	}

}
