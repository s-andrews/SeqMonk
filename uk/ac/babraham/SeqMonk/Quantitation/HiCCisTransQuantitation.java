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
package uk.ac.babraham.SeqMonk.Quantitation;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * A quantitation based on the ratio of cis to trans reads in each probe
 */
public class HiCCisTransQuantitation extends Quantitation {

	private JPanel optionPanel = null;

	/** Do we remove duplicate pairs */
	private boolean removeDuplicates;

	/** Do we want to ignore duplicated reads */
	private JCheckBox ignoreDuplicatesBox;

	private JCheckBox includeFarCisBox;
	private boolean includeFarCis;
	
	private JCheckBox correctPerChromosomeBox;
	private boolean correctPerChromosome;

	private JTextField farCisDistanceField;
	private int farCisDistance = 10000000;

	/** The stores we're going to quantitate. */
	private HiCDataStore [] data;


	public HiCCisTransQuantitation(SeqMonkApplication application) {
		super(application);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {

		Vector<HiCDataStore> hiCDataStores = new Vector<HiCDataStore>();
		for (int d=0;d<data.length;d++) {
			if (data[d] instanceof HiCDataStore && ((HiCDataStore)data[d]).isValidHiC()) {
				hiCDataStores.add((HiCDataStore)data[d]);
			}
		}

		this.data = hiCDataStores.toArray(new HiCDataStore[0]);

		removeDuplicates = ignoreDuplicatesBox.isSelected();

		includeFarCis = includeFarCisBox.isSelected();

		correctPerChromosome = correctPerChromosomeBox.isSelected();
		
		if (includeFarCis) {
			if (farCisDistanceField.getText().length()==0) {
				farCisDistance = 10000000;
			}
			else {
				farCisDistance = Integer.parseInt(farCisDistanceField.getText());
			}
			System.err.println("Far cis distance cutoff is "+farCisDistance);
		};

		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	public boolean requiresHiC () {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {

		if (optionPanel != null) {
			// We've done this already
			return optionPanel;
		}

		optionPanel = new JPanel();
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		optionPanel.add(new JLabel("Count duplicated reads only once"),gbc);

		gbc.gridx = 2;
		ignoreDuplicatesBox = new JCheckBox();
		ignoreDuplicatesBox.setSelected(false);
		optionPanel.add(ignoreDuplicatesBox,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Include far-cis in trans count"),gbc);

		gbc.gridx = 2;
		includeFarCisBox = new JCheckBox();
		includeFarCisBox.setSelected(false);
		includeFarCisBox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				farCisDistanceField.setEnabled(includeFarCisBox.isSelected());
			}
		});

		optionPanel.add(includeFarCisBox,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Far cis distance cutoff"),gbc);

		gbc.gridx = 2;
		farCisDistanceField = new JTextField(""+farCisDistance,7);
		farCisDistanceField.setEnabled(false);
		farCisDistanceField.addKeyListener(new NumberKeyListener(false, false));
		optionPanel.add(farCisDistanceField,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		optionPanel.add(new JLabel("Subtract median chromosome value"),gbc);

		gbc.gridx = 2;
		correctPerChromosomeBox = new JCheckBox();
		optionPanel.add(correctPerChromosomeBox,gbc);
		

		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}

	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("HiC Cis Trans Quantitation ");

		if (ignoreDuplicatesBox.isSelected()) {
			sb.append(" duplicates ignored");
		}

		if (includeFarCis) {
			sb.append(" far cis >");
			sb.append(farCisDistance);
			sb.append(" counted as trans");
		}

		return sb.toString();
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		Probe [] probes = application.dataCollection().probeSet().getAllProbes();


		for (int p=0;p<probes.length;p++) {

			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated(p, probes.length);

			for (int d=0;d<data.length;d++) {

				int cisCount = 0;
				int transCount = 0;

				HiCHitCollection hiCHits = data[d].getHiCReadsForProbe(probes[p]);

				String [] chromosomeNames = hiCHits.getChromosomeNamesWithHits();

				for (int c=0;c<chromosomeNames.length;c++) {

					long [] sourceReads = hiCHits.getSourcePositionsForChromosome(chromosomeNames[c]);
					long [] hitReads = hiCHits.getHitPositionsForChromosome(chromosomeNames[c]);

//					System.err.println("From '"+chromosomeNames[c]+"' found "+sourceReads.length+" hits compared to '"+probes[p].chromosome().name()+"'");
					                                         
					
					for (int r=0;r<sourceReads.length;r++) {

						// Check if we can ignore this one
						if (removeDuplicates) {
							if (r>0 && sourceReads[r]==sourceReads[r-1] && hitReads[r]==hitReads[r-1]) continue;
						}
						if (!chromosomeNames[c].equals(probes[p].chromosome().name())) {
							++transCount;
						}
						else {
							if (includeFarCis) {
								int distance = SequenceRead.fragmentLength(sourceReads[r], hitReads[r]);
								if (distance > farCisDistance){
									++transCount;
								}
								else {
									//									System.err.println("Distance was "+distance);
									++cisCount;
								}
							}
							else {
								++cisCount;
							}
						}
					}
				}

				float percentage = ((transCount*100f)/(cisCount+transCount));

				if (cisCount+transCount == 0) {
					percentage = 0;
				}

				// TODO: This is icky since the inheritance between HiCDataStore and DataStore
				// isn't properly sorted out.
				((DataStore)data[d]).setValueForProbe(probes[p], percentage);					

			}

		}
		
		if (correctPerChromosome) {
			Chromosome [] chrs = application.dataCollection().genome().getAllChromosomes();
			
			for (int c=0;c<chrs.length;c++) {
				Probe [] thisChrProbes = application.dataCollection().probeSet().getProbesForChromosome(chrs[c]);
				
				float [] thisChrValues = new float[thisChrProbes.length];
				
				for (int d=0;d<data.length;d++) {
					
					DataStore ds = (DataStore)data[d];
					
					for (int p=0;p<thisChrProbes.length;p++) {
						try {
							thisChrValues[p] = ds.getValueForProbe(thisChrProbes[p]);
						} catch (SeqMonkException e) {}
					}
					
					float median = SimpleStats.median(thisChrValues);
					
					for (int p=0;p<thisChrProbes.length;p++) {
						try {
							ds.setValueForProbe(thisChrProbes[p],ds.getValueForProbe(thisChrProbes[p])-median);
						} catch (SeqMonkException e) {}
					}	
				}
			}
		}

		quantitatonComplete();

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "HiC Trans/Cis Quantitation";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

}
