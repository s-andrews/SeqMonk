/**
 * Copyright 2012-15 Simon Andrews
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class MacsPeakCaller extends ProbeGenerator implements Runnable,ListSelectionListener {

	public boolean cancel = false;

	/** The options panel. */
	private JPanel optionPanel = null;

	private JList chipStoresList;
	private JList inputStoresList;
	private DataStore [] selectedChIPStores;
	private DataStore [] selectedInputStores;
	private JTextField pValueField;
	private double pValue = 0.00001d;
	private JCheckBox skipDeduplicationBox;

	private JTextField fragmentSizeField;
	private int fragmentSize = 600;  // Average selected fragment size

	private double minFoldEnrichment = 20;
	private double maxFoldEnrichment = 100;

	public MacsPeakCaller(DataCollection collection) {
		super(collection);
	}

	public void cancel() {
		cancel = true;
	}

	public boolean requiresExistingProbeSet() {
		return false;
	}

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

		optionPanel.add(new JLabel("ChIP Stores to use"),gbc);

		gbc.gridy++;
		gbc.weighty = 0.5;

		DataStore [] stores = collection.getAllDataStores();

		chipStoresList = new JList(stores);
		chipStoresList.getSelectionModel().addListSelectionListener(this);
		chipStoresList.setCellRenderer(new TypeColourRenderer());
		optionPanel.add(new JScrollPane(chipStoresList),gbc);

		gbc.gridy++;
		gbc.weighty=0.1;

		optionPanel.add(new JLabel("Input Stores to use"),gbc);

		gbc.gridy++;
		gbc.weighty = 0.5;

		inputStoresList = new JList(stores);
		inputStoresList.getSelectionModel().addListSelectionListener(this);
		inputStoresList.setCellRenderer(new TypeColourRenderer());
		optionPanel.add(new JScrollPane(inputStoresList),gbc);

		gbc.gridy++;

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridBagLayout());
		GridBagConstraints bgbc = new GridBagConstraints();
		bgbc.gridx=0;
		bgbc.gridy=0;
		bgbc.weightx=0.5;
		bgbc.weighty=0.5;
		bgbc.fill=GridBagConstraints.HORIZONTAL;

		bottomPanel.add(new JLabel("P-value cutoff "),bgbc);
		pValueField = new JTextField(""+pValue);
		pValueField.addKeyListener(new NumberKeyListener(true, false,1));
		bgbc.gridx=1;
		bottomPanel.add(pValueField,bgbc);

		bgbc.gridx=0;
		bgbc.gridy++;

		bottomPanel.add(new JLabel("Sonicated fragment size "),bgbc);
		fragmentSizeField = new JTextField("300");
		fragmentSizeField.addKeyListener(new NumberKeyListener(false, false));
		bgbc.gridx=1;
		bottomPanel.add(fragmentSizeField,bgbc);

		bgbc.gridx=0;
		bgbc.gridy++;

		bottomPanel.add(new JLabel("Skip deduplication step "),bgbc);
		skipDeduplicationBox = new JCheckBox();
		bgbc.gridx=1;
		bottomPanel.add(skipDeduplicationBox,bgbc);


		optionPanel.add(bottomPanel,gbc);
		return optionPanel;	
	}

	
	public boolean isReady() {
		try {
			pValue = Double.parseDouble(pValueField.getText());
			fragmentSize = Integer.parseInt(fragmentSizeField.getText());

			Object [] o = chipStoresList.getSelectedValues();
			if (o == null || o.length == 0) {
				throw new SeqMonkException("No selected ChIP stores");
			}
			selectedChIPStores = new DataStore [o.length];
			for (int i=0;i<o.length;i++) {
				selectedChIPStores[i] = (DataStore)o[i];
			}

			o = inputStoresList.getSelectedValues();
			// It's OK to have no input stores
			if (o == null) {
				throw new SeqMonkException("No selected input stores");
			}
			selectedInputStores = new DataStore [o.length];
			for (int i=0;i<o.length;i++) {
				selectedInputStores[i] = (DataStore)o[i];
			}


		}
		catch (Exception ex) {
			optionsNotReady();
			return false;
		}

		optionsReady();
		return true;
	}

	public String toString () {
		return "MACS peak caller";
	}

	public void generateProbes() {
		if (!isReady()) {
			generationCancelled();
		}
		cancel = false;
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * Gets a text description of the current set of options.
	 * 
	 * @return Text describing the current options
	 */
	private String getDescription () {
		StringBuffer b = new StringBuffer();
		b.append("MACS peaks using chip samples ");

		for (int i=0;i<selectedChIPStores.length;i++) {
			b.append(selectedChIPStores[i].name());
			if (i<selectedChIPStores.length-1) {
				b.append(",");
			}
		}
		
		if (selectedInputStores.length > 0) {
			b.append(" and input samples ");
			for (int i=0;i<selectedInputStores.length;i++) {
				b.append(selectedInputStores[i].name());
				if (i<selectedInputStores.length-1) {
					b.append(",");
				}
			}
			
		}
		else {
			b.append(" and no input samples");
		}
		
		b.append(". Fragment size was ");
		b.append(fragmentSize);
		b.append(". Significance threshold was ");
		b.append(pValue);
		//b.append(".");
		if (skipDeduplicationBox.isSelected()){
			b.append(". Deduplication step skipped");
		}
				
		return b.toString();
	}
	

	public void run() {

//		for (int i=0;i<selectedChIPStores.length;i++) {
//			System.err.println("Selcted ChIP="+selectedChIPStores[i]);
//		}
//		for (int i=0;i<selectedInputStores.length;i++) {
//			System.err.println("Selcted Input="+selectedInputStores[i]);
//		}
		
		
		// First find the tag offsets between the watson and crick strands

		// Work out the total average coverage for all of the combined ChIP samples
		long totalChIPCoverage = 0;

		for (int i=0;i<selectedChIPStores.length;i++) {
			totalChIPCoverage += selectedChIPStores[i].getTotalReadLength();
		}
		
		if (cancel) {
			generationCancelled();
			return;
		}

		double averageChIPCoveragePerBase = totalChIPCoverage / (double)collection.genome().getTotalGenomeLength();
		
		
		double lowerCoverage = averageChIPCoveragePerBase*minFoldEnrichment;
		double upperCoverage = averageChIPCoveragePerBase*maxFoldEnrichment;

		System.err.println("Coverage range for high confidence peaks is "+lowerCoverage+" - "+upperCoverage);		

		// Now we go through the data to find locations for our high confidence peaks so we can
		// randomly select 1000 of these to use to find the offset between the two strands

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		Vector<Probe> potentialHighConfidencePeaks = new Vector<Probe>();

		for (int c=0;c<chromosomes.length;c++) {
			
			if (cancel) {
				generationCancelled();
				return;
			}

			
			// Time for an update
			updateGenerationProgress("Finding high confidence peaks on chromosome "+chromosomes[c].name(), c, chromosomes.length);

			Probe lastValidProbe = null;

			for (int startPosition = 1;startPosition<chromosomes[c].length()-fragmentSize;startPosition+=fragmentSize/2) {

				// See if we need to quit
				if (cancel) {
					generationCancelled();
					return;
				}
				
				long totalLength = 0;
				Probe probe = new Probe(chromosomes[c], startPosition, startPosition+fragmentSize);

				for (int s=0;s<selectedChIPStores.length;s++) {
					long [] reads = selectedChIPStores[s].getReadsForProbe(probe);
					for (int j=0;j<reads.length;j++) {
						totalLength += SequenceRead.length(reads[j]);
					}
				}

				if (totalLength >= (lowerCoverage*probe.length()) && totalLength <= upperCoverage*probe.length()) {

					// We have a potential high quality peak.

					// See if we can merge it with the last valid probe

					if (lastValidProbe != null && SequenceRead.overlaps(lastValidProbe.packedPosition(), probe.packedPosition())) {

						lastValidProbe = new Probe(chromosomes[c], lastValidProbe.start(),probe.end());

					}
					else if (lastValidProbe != null) {

						// Check that the overall density over the region falls within our limits
						totalLength = 0;
						for (int s=0;s<selectedChIPStores.length;s++) {
							long [] reads = selectedChIPStores[s].getReadsForProbe(lastValidProbe);
							for (int j=0;j<reads.length;j++) {
								totalLength += SequenceRead.length(reads[j]);
							}
						}

						if (totalLength >= (lowerCoverage*lastValidProbe.length()) && totalLength <= upperCoverage*lastValidProbe.length()) {
							potentialHighConfidencePeaks.add(lastValidProbe);
						}						

						lastValidProbe = probe;
					}
					else {
						lastValidProbe = probe;
					}
				}


			}

			if (lastValidProbe != null) {
				long totalLength = 0;
				for (int s=0;s<selectedChIPStores.length;s++) {
					long [] reads = selectedChIPStores[s].getReadsForProbe(lastValidProbe);
					for (int j=0;j<reads.length;j++) {
						totalLength += SequenceRead.length(reads[j]);
					}
				}

				if (totalLength >= (lowerCoverage*lastValidProbe.length()) && totalLength <= upperCoverage*lastValidProbe.length()) {
					potentialHighConfidencePeaks.add(lastValidProbe);
				}						
			}
		}

		if (potentialHighConfidencePeaks.size() == 0) {

			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "No high confidence peaks found","Quitting generator",JOptionPane.INFORMATION_MESSAGE);
			generationCancelled();
			return;
		}
		
//		System.err.println("Found "+potentialHighConfidencePeaks.size()+" high confidence peaks");

		// Now we select 1000 random probes from this set
		Probe [] highConfidencePeaks = potentialHighConfidencePeaks.toArray(new Probe[0]);

		Collections.shuffle(Arrays.asList(highConfidencePeaks));

		Probe [] randomHighConfidenceProbes = new Probe[Math.min(highConfidencePeaks.length,1000)];

		for (int i=0;i<randomHighConfidenceProbes.length;i++) {
			randomHighConfidenceProbes[i] = highConfidencePeaks[i];
		}
		
		// Now find the average distance between forward / reverse reads in the candidate peaks

		int [] distances = new int [highConfidencePeaks.length];

		// Sort the candidates so we don't do stupid stuff with the cache
		Arrays.sort(randomHighConfidenceProbes);

		for (int p=0;p<randomHighConfidenceProbes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				generationCancelled();
				return;
			}
			
			distances[p] = getInterStrandDistance(randomHighConfidenceProbes[p], selectedChIPStores);
		}

		int medianInterStrandDistance = (int)SimpleStats.median(distances);

		if (medianInterStrandDistance < 0) medianInterStrandDistance = 0;

//		System.err.println("Median inter strand difference = "+medianInterStrandDistance);


		// Now we find the depth cutoff for overrepresented single tags using a binomial distribution
		int totalReadCount = 0;
		for (int i=0;i<selectedChIPStores.length;i++) {
			totalReadCount += selectedChIPStores[i].getTotalReadCount();
		}

		BinomialDistribution bin = new BinomialDistribution(totalReadCount, 1d/collection.genome().getTotalGenomeLength());

		// We want to know what depth has a chance of less than 1^-5
		int redundantThreshold = bin.inverseCumulativeProbability(1-0.00001d);

		if (redundantThreshold < 1) redundantThreshold = 1;
		
//		System.err.println("Redundancy threshold is "+redundantThreshold);
	

		// Now we construct a poisson distribution to work out the threshold to use for
		// constructing a full candidate peak set.

		updateGenerationProgress("Counting non-redundant reads", 0, 1);

		// To do this we need to get the full non-redundant length from the whole set
		int totalNonRedCount = getNonRedundantReadCount(selectedChIPStores, redundantThreshold);

//		System.err.println("Total non-redundant sequences is "+totalNonRedCount);
		
		// We need to know the median read length for the data
		int readLength = 0;
		for (int i=0;i<selectedChIPStores.length;i++) {
			readLength += selectedChIPStores[i].getTotalReadLength()/selectedChIPStores[i].getTotalReadCount();
		}
		readLength /= selectedChIPStores.length;
		
		double expectedCountsPerWindow = getExpectedCountPerWindow(totalNonRedCount, collection.genome().getTotalGenomeLength(), fragmentSize, readLength);
		
		PoissonDistribution poisson = new PoissonDistribution(expectedCountsPerWindow);

		int readCountCutoff = poisson.inverseCumulativeProbability(1-pValue);

//		System.err.println("Threshold for enrichment in a window is "+readCountCutoff+" reads using a p-value of "+pValue+" and a mean of "+(totalNonRedCount/(collection.genome().getTotalGenomeLength()/(double)fragmentSize)));

		// Now we go back through the whole dataset to do a search for all possible candidate probes 

		// We re-use the peak vector we came up with before.
		potentialHighConfidencePeaks.clear();

		for (int c=0;c<chromosomes.length;c++) {
			// Time for an update
			updateGenerationProgress("Finding candidate peaks on chromosome "+chromosomes[c].name(), c, chromosomes.length);

			Probe lastValidProbe = null;

			for (int startPosition = 1;startPosition<chromosomes[c].length()-fragmentSize;startPosition+=fragmentSize/2) {
				
				// See if we need to quit
				if (cancel) {
					generationCancelled();
					return;
				}

				// We expand the region we're looking at by the inter-strand distance as we're going to
				// be adjusting the read positions
				Probe probe = new Probe(chromosomes[c], startPosition, (startPosition+fragmentSize-1));

				long [] mergedProbeReads = getReadsFromDataStoreCollection(probe, selectedChIPStores, medianInterStrandDistance);

				mergedProbeReads = deduplicateReads(mergedProbeReads,redundantThreshold);

				SequenceRead.sort(mergedProbeReads);

				int thisProbeOverlapCount = 0;
				for (int i=0;i<mergedProbeReads.length;i++) {
					if (SequenceRead.overlaps(mergedProbeReads[i], probe.packedPosition())) {
						++thisProbeOverlapCount;
					}
				}


				if (thisProbeOverlapCount > readCountCutoff) {

					// We have a potential high quality peak.

					// See if we can merge it with the last valid probe

					if (lastValidProbe != null && SequenceRead.overlaps(lastValidProbe.packedPosition(), probe.packedPosition())) {

						lastValidProbe = new Probe(chromosomes[c], lastValidProbe.start(),probe.end());

					}
					else if (lastValidProbe != null) {
						potentialHighConfidencePeaks.add(lastValidProbe);
						lastValidProbe = probe;
					}
					else {
						lastValidProbe = probe;
					}
				}	

			}

			if (lastValidProbe != null) {
				potentialHighConfidencePeaks.add(lastValidProbe);
			}
		}


		// Finally we re-filter the peaks we have using local poisson distributions with densities taken
		// from either the input samples (if there are any), or the local region.  The densities are 
		// estimated over 1,5 and 10kb around the peak and genome wide and the max of these is taken.
		// If there is no input then the 1kb region is not used.

		Probe [] allCandidateProbes = potentialHighConfidencePeaks.toArray(new Probe[0]);

		// Work out which stores we're using to validate against.
		DataStore [] validationStores;
		boolean useInput = false;
		double inputCorrection = 1;
		
		int validationNonRedCount;

		if (selectedInputStores != null && selectedInputStores.length > 0) {
			
			// See if we need to quit
			if (cancel) {
				generationCancelled();
				return;
			}
			
			validationStores = selectedInputStores;
			useInput = true;

			// We also need to work out the total number of nonredundant seqences
			// in the input so we can work out a scaling factor so that the densities
			// for input and chip are comparable.
			validationNonRedCount = getNonRedundantReadCount(validationStores, redundantThreshold);
			
			inputCorrection = totalNonRedCount/(double)validationNonRedCount;

			System.err.println("From chip="+totalNonRedCount+" input="+validationNonRedCount+" correction is "+inputCorrection);
			


		}
		else {
			validationStores = selectedChIPStores;
			validationNonRedCount = totalNonRedCount;
		}


		Vector<Probe> finalValidatedProbes = new Vector<Probe>();

		for (int p=0;p<allCandidateProbes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				generationCancelled();
				return;
			}

			if (p%100 == 0) {
				updateGenerationProgress("Validated "+p+" out of "+allCandidateProbes.length+" raw peaks", p, allCandidateProbes.length);
			}

//			System.err.println("Validating "+allCandidateProbes[p].chromosome()+":"+allCandidateProbes[p].start()+"-"+allCandidateProbes[p].end());
			
			// We now need to find the maximum read density per 2*bandwidth against which
			// we're going to validate this peak

			// We're going to get all reads within 10kb of the peak, and then we can subselect from there

			int midPoint = allCandidateProbes[p].middle();

			Probe region10kb = new Probe(allCandidateProbes[p].chromosome(), Math.max(midPoint-5000,1),Math.min(midPoint+4999,allCandidateProbes[p].chromosome().length()),allCandidateProbes[p].strand());
			Probe region5kb = new Probe(allCandidateProbes[p].chromosome(), Math.max(midPoint-2500,1),Math.min(midPoint+2499,allCandidateProbes[p].chromosome().length()),allCandidateProbes[p].strand());
			Probe region1kb = new Probe(allCandidateProbes[p].chromosome(), Math.max(midPoint-500,1),Math.min(midPoint+499,allCandidateProbes[p].chromosome().length()),allCandidateProbes[p].strand());

			// Get the probes for the largest region
			long [] thisRegionReads = getReadsFromDataStoreCollection(region10kb, validationStores, 0);

			// Deduplicate so it's a fair comparison
			thisRegionReads = deduplicateReads(thisRegionReads, redundantThreshold); // Should we recalculate the redundant threshold based on the input coverage?

			int region10kbcount = thisRegionReads.length;
			int region5kbcount = 0;
			int region1kbcount = 0;

			// Go through the reads seeing if they fit into the 5 or 1kb regions
			for (int r=0;r<thisRegionReads.length;r++) {
				if (SequenceRead.overlaps(region5kb.packedPosition(), thisRegionReads[r])) ++region5kbcount;
				if (SequenceRead.overlaps(region1kb.packedPosition(), thisRegionReads[r])) ++region1kbcount;
			}

//			System.err.println("Input counts 10kb="+region10kbcount+" 5kb="+region5kbcount+" 1kb="+region1kbcount);

			// Convert to densities per window and ajdust for global coverage

			double globalDensity = getExpectedCountPerWindow(validationNonRedCount, collection.genome().getTotalGenomeLength(), allCandidateProbes[p].length(), readLength) * inputCorrection;
			double density10kb = getExpectedCountPerWindow(region10kbcount, region10kb.length(), allCandidateProbes[p].length(), readLength) * inputCorrection;
			double density5kb = getExpectedCountPerWindow(region5kbcount, region5kb.length(), allCandidateProbes[p].length(), readLength) * inputCorrection;
			double density1kb = getExpectedCountPerWindow(region1kbcount, region1kb.length(), allCandidateProbes[p].length(), readLength) * inputCorrection;
			
			// Find the highest density to use for the validation
			double highestDensity = globalDensity;
			if (density10kb > highestDensity) highestDensity = density10kb;
			if (density5kb > highestDensity) highestDensity = density5kb;
			if (useInput && density1kb > highestDensity) highestDensity = density1kb;

//			System.err.println("Global="+globalDensity+" 10kb="+density10kb+" 5kb="+density5kb+" 1kb="+density1kb+" using="+highestDensity);
			
			// Construct a poisson distribution with this density
			PoissonDistribution localPoisson = new PoissonDistribution(highestDensity);

			
//			System.err.println("Cutoff from global="+(new PoissonDistribution(globalDensity)).inverseCumulativeProbability(1-pValue)+" 10kb="+(new PoissonDistribution(density10kb)).inverseCumulativeProbability(1-pValue)+" 5kb="+(new PoissonDistribution(density5kb)).inverseCumulativeProbability(1-pValue)+" 1kb="+(new PoissonDistribution(density1kb)).inverseCumulativeProbability(1-pValue));
			// Now check to see if the actual count from this peak is enough to still pass
			long [] mergedProbeReads = getReadsFromDataStoreCollection(allCandidateProbes[p], selectedChIPStores, medianInterStrandDistance);
			mergedProbeReads = deduplicateReads(mergedProbeReads,redundantThreshold);

			SequenceRead.sort(mergedProbeReads);

			int thisProbeOverlapCount = 0;
			for (int i=0;i<mergedProbeReads.length;i++) {
				if (SequenceRead.overlaps(mergedProbeReads[i], allCandidateProbes[p].packedPosition())) {
					++thisProbeOverlapCount;
				}
			}

//			System.err.println("Read count in ChIP is "+thisProbeOverlapCount);
			
			if (thisProbeOverlapCount > localPoisson.inverseCumulativeProbability(1-pValue)) {
				finalValidatedProbes.add(allCandidateProbes[p]);
//				System.err.println("Adding probe to final set");
			}

		}
		
//		System.err.println("From "+allCandidateProbes.length+" candidates "+finalValidatedProbes.size()+" peaks were validated");

		ProbeSet finalSet = new ProbeSet(getDescription(), finalValidatedProbes.toArray(new Probe[0]));

		generationComplete(finalSet);

	}
	
	private double getExpectedCountPerWindow (int totalCount, long totalLength, int windowSize, int readLength) {
		
		// Basic count is simply the count divided by the number of
		// windows
		
		double expectedCount = totalCount/(totalLength/(double)windowSize);
		
		// We now need to account for how many reads are going to span more than
		// one window
		double readOverlap = (readLength-1d)/windowSize;
		
		// We can now multiply this by the expected count and add that to the expected
		// value to get the true expected value
		
		expectedCount += (expectedCount*readOverlap);
		
		return expectedCount;
	}

	private long [] deduplicateReads (long [] reads, int threshold) {

		if (skipDeduplicationBox.isSelected()) return reads;
		
//		System.err.println("Threshold is "+threshold);
		
		LongVector passed = new LongVector();

		int lastReadCount = 0;

		if (reads.length > 0) {
			lastReadCount = 1;
		}

		for (int r=1;r<reads.length;r++) {
			if (reads[r] == reads[r-1]) {
				++lastReadCount;
			}
			else {
				for (int i=0;i<Math.min(lastReadCount,threshold);i++) {
					passed.add(reads[r-1]);
				}
				lastReadCount = 1;
			}
		}

		for (int i=0;i<Math.min(lastReadCount,threshold);i++) {
			passed.add(reads[reads.length-1]);
		}

		return passed.toArray();
	}

	private int getNonRedundantReadCount (DataStore [] stores, int threshold) {

		int totalCount = 0;

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		for (int c=0;c<chromosomes.length;c++) {

			long [] mergedChrReads = getReadsFromDataStoreCollection(chromosomes[c], stores, 0);

//			System.err.println("Found "+mergedChrReads.length+" raw reads on "+chromosomes[c]);
			SequenceRead.sort(mergedChrReads);

			mergedChrReads = deduplicateReads(mergedChrReads, threshold);
//			System.err.println("Found "+mergedChrReads.length+" deduplicated reads on "+chromosomes[c]);

			totalCount += mergedChrReads.length;
		}

		return totalCount;	

	}

	private long [] getReadsFromDataStoreCollection (Probe probe, DataStore [] stores, int strandOffset) {

		Probe p = probe;
		if (strandOffset > 0) {
			p = new Probe(probe.chromosome(), Math.max(probe.start()-strandOffset,1), Math.min(probe.end()+strandOffset,probe.chromosome().length()));
		}
		long [][] allProbeReads = new long[stores.length][];
		for (int d=0;d<stores.length;d++) {
			allProbeReads[d] = stores[d].getReadsForProbe(p);
		}

		int totalProbeReadCount = 0;
		for (int i=0;i<allProbeReads.length;i++) {
			totalProbeReadCount += allProbeReads[i].length;
		}
		long [] mergedChrReads = new long[totalProbeReadCount];				

		int index = 0;
		for (int i=0;i<allProbeReads.length;i++) {
			for (int j=0;j<allProbeReads[i].length;j++) {
				if (strandOffset == 0 || SequenceRead.strand(allProbeReads[i][j]) == Location.UNKNOWN) {
					mergedChrReads[index] = allProbeReads[i][j];
				}
				else if (SequenceRead.strand(allProbeReads[i][j]) == Location.FORWARD) {
					mergedChrReads[index] = SequenceRead.packPosition(Math.min(SequenceRead.start(allProbeReads[i][j])+strandOffset,p.chromosome().length()), Math.min(SequenceRead.end(allProbeReads[i][j])+strandOffset,p.chromosome().length()), SequenceRead.strand(allProbeReads[i][j]));
				}
				else if (SequenceRead.strand(allProbeReads[i][j]) == Location.REVERSE) {
					mergedChrReads[index] = SequenceRead.packPosition(Math.max(SequenceRead.start(allProbeReads[i][j])-strandOffset,1), Math.max(SequenceRead.end(allProbeReads[i][j])-strandOffset,1), SequenceRead.strand(allProbeReads[i][j]));
				}
				index++;
			}
			allProbeReads[i] = null;
		}

		return mergedChrReads;

	}

	private long [] getReadsFromDataStoreCollection (Chromosome c, DataStore [] stores, int strandOffset) {

		long [][] allChrReads = new long[stores.length][];
		for (int d=0;d<stores.length;d++) {
			allChrReads[d] = stores[d].getReadsForChromosome(c);
		}

		int totalProbeReadCount = 0;
		for (int i=0;i<allChrReads.length;i++) {
			totalProbeReadCount += allChrReads[i].length;
		}
		long [] mergedChrReads = new long[totalProbeReadCount];

		int index = 0;
		for (int i=0;i<allChrReads.length;i++) {
			for (int j=0;j<allChrReads[i].length;j++) {
				if (strandOffset == 0 || SequenceRead.strand(allChrReads[i][j]) == Location.UNKNOWN) {
					mergedChrReads[index] = allChrReads[i][j];
				}
				else if (SequenceRead.strand(allChrReads[i][j]) == Location.FORWARD) {
					mergedChrReads[index] = SequenceRead.packPosition(Math.min(SequenceRead.start(allChrReads[i][j])+strandOffset,c.length()), Math.min(SequenceRead.end(allChrReads[i][j])+strandOffset,c.length()), SequenceRead.strand(allChrReads[i][j]));
				}
				else if (SequenceRead.strand(allChrReads[i][j]) == Location.REVERSE) {
					mergedChrReads[index] = SequenceRead.packPosition(Math.max(SequenceRead.start(allChrReads[i][j])-strandOffset,1), Math.max(SequenceRead.end(allChrReads[i][j])-strandOffset,1), SequenceRead.strand(allChrReads[i][j]));
				}
				index++;
			}
			allChrReads[i] = null;
		}

		return mergedChrReads;

	}

	private int getInterStrandDistance (Probe p, DataStore [] stores) {

		int [] forwardCounts = new int [p.length()];
		int [] reverseCounts = new int [p.length()];

		//		System.err.println("Getting reads for region "+p.chromosome()+":"+p.start()+"-"+p.end());

		for (int d=0;d<stores.length;d++) {
			long [] reads = stores[d].getReadsForProbe(p);
			for (int r=0;r<reads.length;r++) {

				//				System.err.println("Looking at read from "+SequenceRead.start(reads[r])+"-"+SequenceRead.end(reads[r]));

				if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
					for (int pos=SequenceRead.start(reads[r]);pos<=SequenceRead.end(reads[r]);pos++) {

						int index = pos-p.start();

						//						System.err.println("For pos="+pos+" index="+index+" from start="+p.start());

						if (index >=0 && index < forwardCounts.length) {
							forwardCounts[index]++;
						}
						//						else {
						//							System.err.println("Skipping pos "+index+" from "+pos+" with start "+p.start());
						//						}
					}
				}
				else if (SequenceRead.strand(reads[r]) == Location.REVERSE) {
					for (int pos=SequenceRead.start(reads[r]);pos<=SequenceRead.end(reads[r]);pos++) {
						int index = pos-p.start();

						//						System.err.println("Rev pos="+pos+" index="+index+" from start="+SequenceRead.start(reads[r]));

						if (index >=0 && index < reverseCounts.length) {
							reverseCounts[index]++;
						}
						//						else {
						//							System.err.println("Skipping pos "+index+" from "+pos+" with start "+SequenceRead.start(reads[r]));
						//						}
					}
				}
			}
		}

		// Now find the max depth for each of the forward and reverse sets
		int maxForIndex = 0;
		int maxForCount = 0;
		int maxRevIndex = 0;
		int maxRevCount = 0;

		for (int i=0;i<forwardCounts.length;i++) {

			//			System.err.println("Position "+i+" for="+forwardCounts[i]+" rev="+reverseCounts[i]);

			if (forwardCounts[i] > maxForCount) {
				maxForCount = forwardCounts[i];
				maxForIndex = i;
			}
			if (reverseCounts[i] > maxRevCount) {
				maxRevCount = reverseCounts[i];
				maxRevIndex = i;
			}
		}

		//		System.err.println("For= "+maxForCount+" at "+maxForIndex+" Rev="+maxRevCount+" at "+maxRevIndex);

		return maxRevIndex-maxForIndex;
	}


	public void valueChanged(ListSelectionEvent arg0) {
		isReady();
	}



}
