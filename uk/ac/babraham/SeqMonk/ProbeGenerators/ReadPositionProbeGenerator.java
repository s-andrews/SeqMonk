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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * The Class ReadPositionProbeGenerator makes probes over every unique position
 * of reads in the data
 */
public class ReadPositionProbeGenerator extends ProbeGenerator implements Runnable, KeyListener, ListSelectionListener {

	/** The options panel. */
	private JPanel optionPanel = null;

	private JTextField minCountField;
	private JTextField readsPerWindowField;
	private JCheckBox designWithinExistingBox;
	private JComboBox limitRegionBox;
	private JCheckBox ignoreStrandBox;
	private JList storesList;
	private DataStore [] selectedStores;
	private int minCount = 1;
	private int readsPerWindow = 1;
	private boolean limitWithinRegion = false;
	private boolean ignoreStrand;
	private JComboBox readStrandTypeBox;
	private ReadStrandType readStrandType = null;


	/**
	 * Instantiates a new contig probe generator.
	 * 
	 * @param collection The dataCollection to use for generation
	 */
	public ReadPositionProbeGenerator(DataCollection collection) {
		super(collection);
	}

	public boolean requiresExistingProbeSet () {
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {

		readStrandType = (ReadStrandType)readStrandTypeBox.getSelectedItem();
		readStrandType.setIgnoreDuplicates(false);

		ignoreStrand = ignoreStrandBox.isSelected();

		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
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
		gbc.weighty=0.1;
		gbc.fill = GridBagConstraints.BOTH;

		optionPanel.add(new JLabel("Stores to use"),gbc);

		gbc.gridy++;
		gbc.weighty = 0.9;

		DataStore [] stores = collection.getAllDataStores();

		storesList = new JList(stores);
		storesList.getSelectionModel().addListSelectionListener(this);
		storesList.setCellRenderer(new TypeColourRenderer());
		optionPanel.add(new JScrollPane(storesList),gbc);

		gbc.gridy++;

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridBagLayout());
		GridBagConstraints bgbc = new GridBagConstraints();
		bgbc.gridx=0;
		bgbc.gridy=0;
		bgbc.weightx=0.5;
		bgbc.weighty=0.5;
		bgbc.fill=GridBagConstraints.HORIZONTAL;

		bottomPanel.add(new JLabel("Use reads on strand "),bgbc);
		readStrandTypeBox = new JComboBox(ReadStrandType.getTypeOptions());
		bgbc.gridx=1;
		bottomPanel.add(readStrandTypeBox,bgbc);

		bgbc.gridx=0;
		bgbc.gridy++;
		bottomPanel.add(new JLabel("Minimum read count per position"),bgbc);

		bgbc.gridx=1;
		minCountField = new JTextField(""+minCount,4);
		minCountField.addKeyListener(this);
		bottomPanel.add(minCountField,bgbc);

		bgbc.gridx=0;
		bgbc.gridy++;
		bottomPanel.add(new JLabel("Valid positions per window"),bgbc);

		bgbc.gridx=1;
		readsPerWindowField = new JTextField(""+readsPerWindow,4);
		readsPerWindowField.addKeyListener(new NumberKeyListener(false, false));
		readsPerWindowField.addKeyListener(this);
		bottomPanel.add(readsPerWindowField,bgbc);

		bgbc.gridx=0;
		bgbc.gridy++;
		bottomPanel.add(new JLabel("Limit to region"),bgbc);

		bgbc.gridx=1;
		JPanel limitPanel = new JPanel();
		limitPanel.setLayout(new BorderLayout());

		designWithinExistingBox = new JCheckBox();
		limitPanel.add(designWithinExistingBox,BorderLayout.WEST);

		// They're not going to be given the option to select in the active probe
		// list if there isn't one.

		if (collection.probeSet() == null) {
			limitRegionBox = new JComboBox(new String [] {"Currently Visible Region"});			
		}
		else {
			limitRegionBox = new JComboBox(new String [] {"Active Probe List","Currently Visible Region"});			
		}

		limitRegionBox.setEnabled(false);
		designWithinExistingBox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				limitRegionBox.setEnabled(designWithinExistingBox.isSelected());
				isReady();
			}
		});

		limitPanel.add(limitRegionBox,BorderLayout.CENTER);

		bottomPanel.add(limitPanel,bgbc);

		optionPanel.add(bottomPanel,gbc);

		bgbc.gridx = 0;
		bgbc.gridy++;

		bottomPanel.add(new JLabel("Ignore Strand "),bgbc);

		bgbc.gridx=1;
		ignoreStrandBox = new JCheckBox();
		ignoreStrandBox.setSelected(true);
		bottomPanel.add(ignoreStrandBox,bgbc);

		optionPanel.add(bottomPanel,gbc);
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {

		try {
			minCount = Integer.parseInt(minCountField.getText());
			readsPerWindow = Integer.parseInt(readsPerWindowField.getText());
			if (readsPerWindow > 1) {
				ignoreStrand = true;
				ignoreStrandBox.setEnabled(false);
			}
			else {
				ignoreStrand = ignoreStrandBox.isSelected();
				ignoreStrandBox.setEnabled(true);
			}

			limitWithinRegion = designWithinExistingBox.isSelected();

			Object [] o = storesList.getSelectedValues();
			if (o == null || o.length == 0) {
				throw new SeqMonkException("No selected stores");
			}
			selectedStores = new DataStore [o.length];
			for (int i=0;i<o.length;i++) {
				selectedStores[i] = (DataStore)o[i];
			}

		}
		catch (Exception ex) {
			optionsNotReady();
			return false;
		}

		optionsReady();
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		if (limitWithinRegion && limitRegionBox.getSelectedItem().toString().equals("Currently Visible Region")) {
			chromosomes = new Chromosome [] {DisplayPreferences.getInstance().getCurrentChromosome()};
		}

		Vector<Probe> newProbes = new Vector<Probe>();

		for (int c=0;c<chromosomes.length;c++) {
			// Time for an update
			updateGenerationProgress("Processed "+c+" chromosomes", c, chromosomes.length);


			// We ultimately want to end up with an array of readsPlusCounts objects which 
			// cover all of the parts of this chromosome we want to design over.

			ReadsWithCounts [] rawReads = new ReadsWithCounts[0];



			
			// TODO: FIX THIS - NOT WORKING AT THE MOMENT
			if (limitWithinRegion) {

				// If we're limiting by region we need to get a set of probes which we can merge
				// to get the regions we want to analyse.

				Probe [] regionProbes = new Probe[0];

				if (limitRegionBox.getSelectedItem().toString().equals("Currently Visible Region")) {
					if (chromosomes[c] == DisplayPreferences.getInstance().getCurrentChromosome()) {
						regionProbes = new Probe[] {new Probe(chromosomes[c], DisplayPreferences.getInstance().getCurrentLocation())};
					}
				}
				else if (limitRegionBox.getSelectedItem().toString().equals("Active Probe List")) {
					regionProbes = collection.probeSet().getActiveList().getProbesForChromosome(chromosomes[c]);
				}
				else {
					throw new IllegalStateException("Don't know how to filter by "+limitRegionBox.getSelectedItem().toString());
				}

				// TODO: Merge the probes first?
				
				rawReads = new ReadsWithCounts [regionProbes.length];
				
				for (int r=0;r<regionProbes.length;r++) {
					if (selectedStores.length == 1) {
						rawReads[r] = selectedStores[0].getReadsWithCountsForProbe(regionProbes[r]);
					}
					else {
						ReadsWithCounts [] groupedCounts = new ReadsWithCounts[selectedStores.length];
						for (int i=0;i<selectedStores.length;i++) {
							groupedCounts[i] = selectedStores[i].getReadsWithCountsForProbe(regionProbes[r]);
						}
						rawReads[r] = new ReadsWithCounts(groupedCounts);
					}
				}
				
			}	
			else {
				// We assemble the counts for all of the stores together
				ReadsWithCounts [] groupedCounts = new ReadsWithCounts[selectedStores.length];
				for (int i=0;i<selectedStores.length;i++) {
					groupedCounts[i] = selectedStores[i].getReadsForChromosome(chromosomes[c]);
				}
				rawReads = new ReadsWithCounts[] {new ReadsWithCounts(groupedCounts)};
			}

			for (int p=0;p<rawReads.length;p++) {

				long [] reads = rawReads[p].reads;
				int [] counts = rawReads[p].counts;


				int currentStart = 0;
				int currentEnd = 0;
				int currentPositionCount = 0;
				int currentStrand = 0;

				for (int r=0;r<reads.length;r++) {

					// Are we ignoring this read
					if (!readStrandType.useRead(reads[r])) continue;

					// Do we have enough data
					if (counts[r] < minCount) continue;

					// Do we make a probe from the existing data
					if (currentPositionCount == 0 || currentPositionCount == readsPerWindow) {
						if (currentPositionCount == readsPerWindow) {
							newProbes.add(new Probe(chromosomes[c], currentStart,currentEnd,currentStrand));
						}
						// Now start a new probe with the data from this read
						currentStart = SequenceRead.start(reads[r]);
						currentEnd = SequenceRead.end(reads[r]);
						currentStrand = SequenceRead.strand(reads[r]);
						currentPositionCount = 1;

						if (ignoreStrand) {
							currentStrand = Location.UNKNOWN;
						}
					}

					else {
						// Extend the current read
						if (SequenceRead.end(reads[r]) > currentEnd) currentEnd = SequenceRead.end(reads[r]);

						if (SequenceRead.strand(reads[r]) != currentStrand) currentStrand = Location.UNKNOWN;
						
						++currentPositionCount;
					}

				}


			}
		}

		Probe [] finalList = newProbes.toArray(new Probe[0]);

		newProbes.clear();

		ProbeSet finalSet = new ProbeSet(getDescription(), finalList);

		generationComplete(finalSet);
	}

	/**
	 * Gets the description.
	 * 
	 * @return the description
	 */
	private String getDescription () {
		StringBuffer b = new StringBuffer();
		b.append("Read position generation using ");

		for (int i=0;i<selectedStores.length;i++) {
			b.append(selectedStores[i].name());
			b.append(" ");
		}

		b.append(" MinCount=");
		b.append(minCount);

		b.append(" Positions per window=");
		b.append(readsPerWindow);

		if (ignoreStrand) {
			b.append (" Ignoring strand");
		}

		if (limitWithinRegion) {
			b.append(" Designed within ");
			b.append(limitRegionBox.getSelectedItem());
		}

		return b.toString();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Read Position Probe Generator";
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent k) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent k) {
		isReady();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent k) {}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent arg0) {
		isReady();
	}
}
