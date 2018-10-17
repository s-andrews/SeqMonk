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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.ListIterator;
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
import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

/**
 * The Class ContigProbeGenerator makes probes based on clusters of sequence reads
 */
public class ContigProbeGenerator extends ProbeGenerator implements Runnable, KeyListener, ListSelectionListener {

	/** The options panel. */
	private JPanel optionPanel = null;

	private JTextField distField;
	private JTextField minSizeField;
	private JTextField minDepthField;
	private JLabel depthIntLabel;
	private JList storesList;
	private DataStore [] selectedStores;
	private int distance;
	private int minSize;
	private int depthCutoff = 1;
	private JComboBox readStrandTypeBox;
	private JCheckBox ignoreDuplicates;
	private JCheckBox separateStrands;
	private ReadStrandType readStrandType = null;


	/**
	 * Instantiates a new contig probe generator.
	 * 
	 * @param collection The dataCollection to use for generation
	 */
	public ContigProbeGenerator(DataCollection collection) {
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

		bottomPanel.add(new JLabel("Build peaks for strands seprately "),bgbc);
		bgbc.gridx=1;
		separateStrands = new JCheckBox();
		bottomPanel.add(separateStrands,bgbc);

		bgbc.gridy++;
		bgbc.gridx=0;

		bottomPanel.add(new JLabel("Ignore duplicate reads "),bgbc);
		bgbc.gridx=1;
		ignoreDuplicates = new JCheckBox();
		bottomPanel.add(ignoreDuplicates,bgbc);

		bgbc.gridx=0;
		bgbc.gridy++;
		bottomPanel.add(new JLabel("Merge contigs closer than (bp)"),bgbc);

		bgbc.gridx=1;
		distField = new JTextField("0",4);
		distField.addKeyListener(this);
		bottomPanel.add(distField,bgbc);

		optionPanel.add(bottomPanel,gbc);

		bgbc.gridx = 0;
		bgbc.gridy++;

		bottomPanel.add(new JLabel("Min Contig Size (bp) "),bgbc);

		bgbc.gridx=1;
		minSizeField = new JTextField("0",4);
		minSizeField.addKeyListener(this);
		bottomPanel.add(minSizeField,bgbc);

		bgbc.gridx = 0;
		bgbc.gridy++;
		bottomPanel.add(new JLabel("Depth Cutoff "),bgbc);

		bgbc.gridx=1;
		// TODO: Work out the effective fold change for the current cutoff
		JPanel depthPanel = new JPanel();
		minDepthField = new JTextField("0",4);
		minDepthField.addKeyListener(this);
		depthPanel.add(minDepthField);
		depthPanel.add(new JLabel("fold "));
		depthIntLabel = new JLabel("("+depthCutoff);
		depthPanel.add(depthIntLabel);
		depthPanel.add(new JLabel(" reads)"));

		bottomPanel.add(depthPanel,bgbc);

		optionPanel.add(bottomPanel,gbc);
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {

		try {
			distance = Integer.parseInt(distField.getText());
			minSize = Integer.parseInt(minSizeField.getText());
			Object [] o = storesList.getSelectedValues();
			if (o == null || o.length == 0) {
				throw new SeqMonkException("No selected stores");
			}
			selectedStores = new DataStore [o.length];
			long totalReadLength = 0;
			for (int i=0;i<o.length;i++) {
				selectedStores[i] = (DataStore)o[i];
				totalReadLength += selectedStores[i].getTotalReadLength();
			}

			// Here we're converting the enrichment they asked for into an integer
			// number for the minimim number of overalapping reads required.
			double enrichment = Double.parseDouble(minDepthField.getText());

			double baseFoldCoverage = ((double)totalReadLength/collection.genome().getTotalGenomeLength());

			double requiredFoldCoverage = baseFoldCoverage * enrichment;

			depthCutoff = (int)requiredFoldCoverage;

			if (depthCutoff < 1) depthCutoff = 1;

			depthIntLabel.setText("("+depthCutoff);
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

			ReadsWithCounts rawReads = new ReadsWithCounts(v);
						
			v = null;

			// We now want to convert this list into a non-redundant set of
			// read positions with counts.  If we don't do this then we get
			// appalling performance where we have many reads mapped at the
			// same position

			// Our default is to do all strands at once
			int [] strandsToTry = new int[] {100};

			if (separateStrands.isSelected()) {
				strandsToTry = new int []{Location.FORWARD,Location.REVERSE,Location.UNKNOWN};
			}

			for (int strand=0;strand<strandsToTry.length;strand++) {
				ReadsWithCounts reads = getNonRedundantReads(rawReads,strandsToTry[strand]);
				
//				System.err.println("Found "+reads.length+" reads on chr "+chromosomes[c]+" with strand "+strandsToTry[strand]);
				
				if (reads.totalCount() == 0) {
//					System.err.println("Skipping strand "+strandsToTry[strand]+" on chr "+chromosomes[c]);
					continue;
				}
				
				int strandForNewProbes = Location.UNKNOWN;
				
				if (strandsToTry.length > 1) {
					strandForNewProbes = strandsToTry[strand];
				}
				

				int start = -1;

				// We now start a process where we work out at what point we cross the
				// threshold of having more than depthCutoff reads overlapping at any
				// point

				LinkedList<SequenceReadWithCount> currentSet = new LinkedList<SequenceReadWithCount>();
				int currentSetSize = 0;

				for (int r=0;r<reads.reads.length;r++) {

					// See if we need to quit
					if (cancel) {
						generationCancelled();
						return;
					}

					// Firstly we need to remove any reads from the set which
					// end before this read starts

					while (currentSetSize>0 && SequenceRead.end(currentSet.getFirst().read)<SequenceRead.start(reads.reads[r])) {
						SequenceReadWithCount lastRead = currentSet.removeFirst();
						currentSetSize -= lastRead.count;

						if (start>0 && currentSetSize < depthCutoff) {

							// We just got to the end of a probe
							Probe p = new Probe(chromosomes[c],start,SequenceRead.end(lastRead.read),strandForNewProbes);

							// Check to see if we have a previous probe against which we can check
							Probe lastProbe = null;
							if (!newProbes.isEmpty())lastProbe = newProbes.lastElement();

							// Can we merge?
							if (lastProbe != null && p.chromosome() == lastProbe.chromosome() && p.strand()==lastProbe.strand() && p.start() - lastProbe.end() <= distance) {
								// Remove the last probe from the stored set
								newProbes.remove(newProbes.size()-1);

								// Expand this probe to cover the last one and add it to the stored set
								newProbes.add(new Probe(p.chromosome(),lastProbe.start(),p.end(),strandForNewProbes));
							}

							else if (lastProbe != null) {
								// We might still remove this if it's too small
								if (lastProbe.length() < minSize) {
									newProbes.remove(newProbes.size()-1);
								}
								// We still need to add the new probe

								newProbes.add(p);
							}
							else {
								newProbes.add(p);
							}
							start = -1;
						}
					}


					// Now we need to add the current read to the set.

					// If there's nothing there already then just add it
					if (currentSetSize == 0) {
						currentSet.add(new SequenceReadWithCount(reads.reads[r],reads.counts[r]));
						currentSetSize += reads.counts[r];
					}

					// If there are reads in the current set then we need to add this read
					// so that the current set is ordered by the end positions of the
					// reads, with the earliest end first.  We therefore start from the back
					// and work our way to the front, as soon as we see an entry whose end
					// is lower than ours we add ourselves after that

					else {
						// Now we add this read at a position based on its end position
						ListIterator<SequenceReadWithCount>it = currentSet.listIterator(currentSet.size());
						while (true) {

							// If we reach the front of the set then we add ourselves to the front
							if (! it.hasPrevious()){
								currentSet.addFirst(new SequenceReadWithCount(reads.reads[r],reads.counts[r]));
								currentSetSize += reads.counts[r];
								break;
							}
							else {
								SequenceReadWithCount previousRead = it.previous();
								if (SequenceRead.end(previousRead.read)<SequenceRead.end(reads.reads[r])) {

									// We want to add ourselves after this element so backtrack
									// by one position (which must exist because we just went
									// past it
									it.next();
									it.add(new SequenceReadWithCount(reads.reads[r],reads.counts[r]));
									currentSetSize += reads.counts[r];
									break;
								}
							}
						}
					}


					// See if we crossed the threshold for starting a new probe
					if (start < 0 && currentSetSize >= depthCutoff) {
						start = SequenceRead.start(reads.reads[r]);
					}

				}

				// We now need to see if we need to make a contig
				// out of the so far unprocessed reads on this chromosome
				if (start > 0) {
					Probe p = new Probe(chromosomes[c],start,SequenceRead.end(currentSet.getFirst().read),strandForNewProbes);

					// Check to see if we can merge with the last probe made
					Probe lastProbe = null;
					if (!newProbes.isEmpty())lastProbe = newProbes.lastElement();

					// Can we merge?
					if (lastProbe != null && p.chromosome() == lastProbe.chromosome() && p.start() - lastProbe.end() <= distance) {
						newProbes.remove(newProbes.size()-1);
						newProbes.add(new Probe(p.chromosome(),lastProbe.start(),p.end(),strandForNewProbes));
					}
					else if (lastProbe != null) {
						// We might still remove this if it's too small
						if (lastProbe.length() < minSize) {
							newProbes.remove(newProbes.size()-1);									
						}
						// We still need to add the new probe.  We need to check it's
						// size this time since we can't possibly extend it so this is our
						// final chance
						if (p.length() > minSize) {
							newProbes.add(p);
						}
					}
					else {
						// Add the remaining probe if it's big enough.
						if (p.length() > minSize) {
							newProbes.add(p);
						}
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
		b.append("Contig generation using ");

		for (int i=0;i<selectedStores.length;i++) {
			b.append(selectedStores[i].name());
			b.append(" ");
		}

		b.append(" Depth=");
		b.append(depthCutoff);
		b.append("reads (");
		b.append(minDepthField.getText());
		b.append("fold)");
		b.append(" Min size=");
		b.append(minSize);
		b.append(" Distance to merge=");
		b.append(distance);
		if (separateStrands.isSelected()) {
			b.append(". Strands clustered separately");
		}

		return b.toString();
	}

	/**
	 * Gets the non redundant reads.
	 * 
	 * @param reads the reads
	 * @return the non redundant reads
	 */
	private ReadsWithCounts getNonRedundantReads (ReadsWithCounts reads, int limitToStrand) {
		
		boolean limitStrand = false;
		if (limitToStrand == Location.FORWARD || limitToStrand == Location.REVERSE || limitToStrand == Location.UNKNOWN) {
			limitStrand = true;
		}
		
		if (!limitStrand) {
			return reads; // It's already done.
		}

		
		LongVector keptReads = new LongVector();
		IntVector keptCounts = new IntVector();
		
		for (int r=0;r<reads.reads.length;r++) {

			if (! readStrandType.useRead(reads.reads[r])) {
				continue;
			}

			if (limitStrand && (SequenceRead.strand(reads.reads[r]) != limitToStrand)) continue;

			keptReads.add(reads.reads[r]);
			keptCounts.add(reads.counts[r]);
		}
		return new ReadsWithCounts(keptReads.toArray(),keptCounts.toArray());
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
		return "Contig Probe Generator";
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

	private class SequenceReadWithCount {

		/** The read. */
		public long read;

		/** The count. */
		public int count;

		/**
		 * Instantiates a new sequence read with count.
		 * 
		 * @param read the read
		 * @param count the count
		 */
		public SequenceReadWithCount (long read, int count) {
			this.read = read;
			this.count = count;
		}
	}
}
