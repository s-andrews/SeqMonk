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
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;
import uk.ac.babraham.SeqMonk.Utilities.LongSorter.LongSetSorter;

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


			// We'll merge together the reads for all of the selected DataStores and
			// compute a single set of probes which covers all of them.

			Probe [] regions = new Probe [0];
			if (limitWithinRegion) {
				if (limitRegionBox.getSelectedItem().toString().equals("Currently Visible Region")) {
					if (chromosomes[c] == DisplayPreferences.getInstance().getCurrentChromosome()) {
						regions = new Probe[] {new Probe(chromosomes[c], DisplayPreferences.getInstance().getCurrentLocation())};
					}
				}
				else if (limitRegionBox.getSelectedItem().toString().equals("Active Probe List")) {
					regions = collection.probeSet().getActiveList().getProbesForChromosome(chromosomes[c]);
				}
				else {
					throw new IllegalStateException("Don't know how to filter by "+limitRegionBox.getSelectedItem().toString());
				}
			}	
			else {
				regions = new Probe[] {new Probe(chromosomes[c], 0, chromosomes[c].length())};
			}

			for (int p=0;p<regions.length;p++) {

				long [][] v = new long[selectedStores.length][];
				
				for (int s=0;s<selectedStores.length;s++) {
					v[s] = selectedStores[s].getReadsForProbe(regions[p]);
				}

				long [] rawReads = getUsableRedundantReads(LongSetSorter.sortLongSets(v));
				v = null;

				int currentCount = 1;

				int currentStart = 0;
				int currentEnd = 0;
				int currentPositionCount = 0;

				for (int r=1;r<rawReads.length;r++) {
					// See if this read is different to the last one

					if (SequenceRead.start(rawReads[r]) == SequenceRead.start(rawReads[r-1]) && SequenceRead.end(rawReads[r]) == SequenceRead.end(rawReads[r-1]) && (ignoreStrand || SequenceRead.strand(rawReads[r]) == SequenceRead.strand(rawReads[r-1]))) {
						// It's the same
						++currentCount;
					}
					else {
						// Check if we need to make a new probe
						if (currentCount >= minCount) {

							// Add this probe to the current set
							if (currentPositionCount == 0) {
								// Start a new position
								currentStart = SequenceRead.start(rawReads[r-1]);
								currentEnd = SequenceRead.end(rawReads[r-1]);
							}
							else {
								// Extend the existing position

								if (SequenceRead.end(rawReads[r-1]) > currentEnd) {
									currentEnd = SequenceRead.end(rawReads[r-1]);
								}

							}

							currentPositionCount++;

							// Check if we have enough data to create a new probe

							if (currentPositionCount == readsPerWindow) {
								int strand = Probe.UNKNOWN;
								if (! ignoreStrand) {
									strand = SequenceRead.strand(rawReads[r-1]);
								}
								newProbes.add(new Probe(chromosomes[c], currentStart,currentEnd,strand));
								currentPositionCount = 0;
							}

						}
						currentCount = 1;
					}
				}

				// See if we need to add the last read
				if (currentCount >= minCount  && rawReads.length >= 1) {

					// Add this probe to the current set
					if (currentPositionCount == 0) {
						// Start a new position
						currentStart = SequenceRead.start(rawReads[rawReads.length-1]);
						currentEnd = SequenceRead.end(rawReads[rawReads.length-1]);
					}
					else {
						// Extend the existing position

						if (SequenceRead.end(rawReads[rawReads.length-1]) > currentEnd) {
							currentEnd = SequenceRead.end(rawReads[rawReads.length-1]);
						}

					}

					currentPositionCount++;

					// Make a probe with whatever we have left

					int strand = Probe.UNKNOWN;
					if (! ignoreStrand) {
						strand = SequenceRead.strand(rawReads[rawReads.length-1]);
					}
					newProbes.add(new Probe(chromosomes[c], currentStart,currentEnd,strand));

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

	/**
	 * Removes reads the user chose to ignore
	 * 
	 * @param reads the reads
	 * @return the non redundant reads
	 */
	private long [] getUsableRedundantReads (long [] reads) {
		LongVector keepers = new LongVector();

		for (int r=0;r<reads.length;r++) {

			if (! readStrandType.useRead(reads[r])) {
				continue;
			}
			keepers.add(reads[r]);
		}
		return keepers.toArray();
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
