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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

/**
 * The Class EvenCoverageProbeGenerator makes probes of variable size to try to make the
 * number of reads in each probe the same
 */
public class EvenCoverageProbeGenerator extends ProbeGenerator implements Runnable, KeyListener, ListSelectionListener {

	/** The options panel. */
	private JPanel optionPanel = null;

	private JTextField targetReadCountField;
	private JTextField maxSizeField;
	private JList storesList;
	private DataStore [] selectedStores;
	private int targetReadCount;
	private int maxSize;
	private JComboBox readStrandTypeBox;
	private JCheckBox ignoreDuplicates;
	private ReadStrandType readStrandType = null;


	/**
	 * Instantiates a new contig probe generator.
	 * 
	 * @param collection The dataCollection to use for generation
	 */
	public EvenCoverageProbeGenerator(DataCollection collection) {
		super(collection);
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {

		// We can ignore the ignoreBlanks because we're only
		// designing around contiguous probes anyway.

		readStrandType = (ReadStrandType)readStrandTypeBox.getSelectedItem();
		readStrandType.setIgnoreDuplicates(ignoreDuplicates.isSelected());

		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	public boolean requiresExistingProbeSet () {
		return false;
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

		bgbc.gridy++;
		bgbc.gridx=0;

		bottomPanel.add(new JLabel("Ignore duplicate reads "),bgbc);
		bgbc.gridx=1;
		ignoreDuplicates = new JCheckBox();
		bottomPanel.add(ignoreDuplicates,bgbc);

		bgbc.gridx=0;
		bgbc.gridy++;
		bottomPanel.add(new JLabel("Target read count"),bgbc);

		bgbc.gridx=1;
		targetReadCountField = new JTextField("10000",5);
		targetReadCountField.addKeyListener(this);
		bottomPanel.add(targetReadCountField,bgbc);

		optionPanel.add(bottomPanel,gbc);

		bgbc.gridx = 0;
		bgbc.gridy++;

		bottomPanel.add(new JLabel("Max Probe Size (bp) "),bgbc);

		bgbc.gridx=1;
		maxSizeField = new JTextField("0",5);
		maxSizeField.addKeyListener(this);
		bottomPanel.add(maxSizeField,bgbc);

		optionPanel.add(bottomPanel,gbc);
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {

		try {
			targetReadCount = Integer.parseInt(targetReadCountField.getText());
			maxSize = Integer.parseInt(maxSizeField.getText());
			Object [] o = storesList.getSelectedValues();
			if (o == null || o.length == 0) {
				throw new SeqMonkException("No selected stores");
			}
			selectedStores = new DataStore [o.length];
			
			for (int i=0;i<selectedStores.length;i++) {
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

		Vector<Probe> newProbes = new Vector<Probe>();

		for (int c=0;c<chromosomes.length;c++) {
			// Time for an update
			updateGenerationProgress("Processed "+c+" chromosomes", c, chromosomes.length);


			// We'll merge together the reads for all of the selected DataStores and
			// compute a single set of probes which covers all of them.

			ReadsWithCounts [] v = new ReadsWithCounts[selectedStores.length];
			for (int s=0;s<selectedStores.length;s++) {
				v[s] = selectedStores[s].getReadsForChromosome(chromosomes[c]);
			}

			ReadsWithCounts reads = new ReadsWithCounts(v);

			v = null;

			if (reads.totalCount() == 0) {
				//					System.err.println("Skipping strand "+strandsToTry[strand]+" on chr "+chromosomes[c]);
				continue;
			}

			int strandForNewProbes = Location.UNKNOWN;


			int start = SequenceRead.start(reads.reads[0]);
			int end = SequenceRead.end(reads.reads[0]);
			int count = reads.counts[0];

			// We now start a process where we work out at what point we cross the
			// threshold of having enough reads to finish off the current probe

			for (int r=1;r<reads.reads.length;r++) {

				// See if we need to quit
				if (cancel) {
					generationCancelled();
					return;
				}
				
				// See if adding the next read would put us over the size threshold
				
				if (count > 0 && ((maxSize>0 &&(SequenceRead.end(reads.reads[r]) - start)+1 > maxSize)) || (count + reads.counts[r] > targetReadCount)) {
					
					// Make a probe out of what we have and start a new one with the 
					// read we're currently looking at
					Probe p = new Probe(chromosomes[c], start,end,strandForNewProbes);
					newProbes.add(p);
					
					start = end+1;
					end = Math.max(end+2, SequenceRead.end(reads.reads[r]));
					count = SequenceRead.end(reads.counts[r]);
					
				}
				else {
					count += reads.counts[r];
					if (SequenceRead.end(reads.reads[r]) > end) {
						end = SequenceRead.end(reads.reads[r]);
					}
				}
				
			}
			
			if (count > 0) {
				Probe p = new Probe(chromosomes[c], start,end,strandForNewProbes);
				newProbes.add(p);
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
		b.append("Even coverage generation using ");

		for (int i=0;i<selectedStores.length;i++) {
			b.append(selectedStores[i].name());
			b.append(" ");
		}

		b.append(" Target read count=");
		b.append(targetReadCount);
		b.append("reads");
		b.append(" Max probe size=");
		b.append(maxSize);
		
		//TODO: Add details of strands used.

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
		return "Even Coverage Probe Generator";
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
