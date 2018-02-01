/**
 * Copyright 2011-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Pipelines.Transcription;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Pipelines.Pipeline;

/**
 * The RPKM pipeline is used for the quantitation of RNA-Seq data.  It takes in a 
 * list of features (optionally spliced features) and will calculate a single 
 * normalised value for each feature based on the total number of hits to all exons
 * of that feature.
 * 
 * @author andrewss
 *
 */
public class RNASeqPipeline extends Pipeline {

	// Keep some values around for future use
	private static String lastFeatureType = "mRNA";
	private static boolean lastRawCounts = false;
	private static boolean lastMergeTranscripts = true;
	private static boolean lastPairedEnd = false;
	private static String lastLibraryType = "Non-strand specific";
	private static boolean lastLogTransform = true;
	private static boolean lastTranscriptLengthCorrection = false;
	private static boolean lastNoValueForZero = false;
	private static boolean lastCorrectDNAContamination = false;
	private static boolean lastCorrectDNADuplication = false;


	protected RNASeqOptionsPanel optionsPanel;

	public RNASeqPipeline (DataCollection collection) {
		super(collection);
		optionsPanel = new RNASeqOptionsPanel(collection.genome().annotationCollection().listAvailableFeatureTypes());
	}

	public JPanel getOptionsPanel(SeqMonkApplication application) {
		return optionsPanel;
	}

	public boolean isReady() {
		return true;
	}

	public boolean createsNewProbes () {
		return true;
	}

	protected FeatureGroup [] mergeTranscripts (Feature [] features, boolean merge) {

		if (merge) {

			// Test to see if these features have names which suggest that we can merge
			// based on their names along.  We can also test to see if there is a gene_name
			// or gene_id tag which we can use
			boolean goodNames = true;
			boolean geneNames = true;
			boolean geneIDs = true;
			for (int i=0;i<features.length;i++) {
				if (goodNames && !features[i].name().matches(".*-\\d{3}")){
					goodNames = false;
				}
				if (geneIDs && !features[i].hasTag("gene_id")) {
					geneIDs = false;
				}
				if (geneNames && !features[i].hasTag("gene_name")) {
					geneNames = false;
				}
			}
			
			if (goodNames) {
				System.err.println("Names are good");
			}
			if (geneNames) {
				System.err.println("Gene names present");
			}
			if (geneIDs) {
				System.err.println("Gene IDs present");
			}

			if (goodNames || geneNames || geneIDs) {
				Hashtable<String, FeatureGroup> groupedNames = new Hashtable<String, FeatureGroup>();

				for (int i=0;i<features.length;i++) {
					String name;
					
					if (goodNames) {
						name = features[i].name().replaceFirst("-\\d{3}$", "");
					}
					else if (geneNames) {
						name = features[i].getValueForTag("gene_name");
					}
					else if (geneIDs) {
						name = features[i].getValueForTag("gene_id");
					}
					else {
						throw new IllegalStateException("One of the previous name mappings should have worked.");
					}
					
					
					
					if (!groupedNames.containsKey(name)) {
						groupedNames.put(name,new FeatureGroup(name));
					}

					groupedNames.get(name).addFeature(features[i]);
				}


				FeatureGroup [] mergedTranscripts = new FeatureGroup [groupedNames.size()];

				int i=0;
				Enumeration<String> en = groupedNames.keys();
				while (en.hasMoreElements()) {
					mergedTranscripts[i] = groupedNames.get(en.nextElement());
					i++;
				}

				//			System.err.println("Before splitting we had "+mergedTranscripts.length+" groups");

				// Add an extra constraint that the features within the group must overlap
				// to avoid problems where the same gene name is used multiple times on different
				// parts of the chromosome.
				mergedTranscripts = splitNonOverlappingTranscripts(mergedTranscripts);

				//			System.err.println("After splitting we had "+mergedTranscripts.length+" groups");

				// We now need to sort these
				Arrays.sort(mergedTranscripts);

				return mergedTranscripts;
			}
			else {
				// We're going to have to merge based on overlap on the same strand
				ArrayList<FeatureGroup> tempGroups = new ArrayList<FeatureGroup>();

				FEATURE: for (int i=0;i<features.length;i++) {
					for (FeatureGroup group : tempGroups) {
						if (!group.chromosomeName().equals(features[i].chromosomeName())) continue;
						if (group.strand() != features[i].location().strand()) continue;

						if (group.overlaps(features[i].location())) {
							group.addFeature(features[i]);
							continue FEATURE;
						}
					}

					// If we get here then we need to make a new feature group
					FeatureGroup fg = new FeatureGroup(features[i].name());
					fg.addFeature(features[i]);
					tempGroups.add(fg);
				}
				
				// TODO: We should do more passes through the groups to see if any
				// groups overlap and join them together.
				
				return (tempGroups.toArray(new FeatureGroup[0]));


			}

		}
		else {
			FeatureGroup [] mergedTranscripts = new FeatureGroup [features.length];
			for (int i=0;i<features.length;i++) {
				FeatureGroup fg = new FeatureGroup(features[i].name());
				fg.addFeature(features[i]);
				mergedTranscripts[i] = fg;
			}

			return mergedTranscripts;
		}
	}

	protected FeatureGroup [] splitNonOverlappingTranscripts (FeatureGroup [] startingGroup) {

		Vector<FeatureGroup> splitGroup = new Vector<FeatureGroup>();

		for (int i=0;i<startingGroup.length;i++) {

			FeatureGroup [] splitGroups = splitNonOverlappingFeatureGroup(startingGroup[i]);
			for (int j=0;j<splitGroups.length;j++) {
				splitGroup.add(splitGroups[j]);
			}
		}

		return splitGroup.toArray(new FeatureGroup[0]);
	}

	protected FeatureGroup [] splitNonOverlappingFeatureGroup (FeatureGroup group) {

		Feature [] features = group.features();
		//		System.err.println("Group has "+features.length+" features");

		// Make a set of non-overlapping feature groups.

		Vector<FeatureGroup> splitGroups = new Vector<FeatureGroup>();

		FEATURE: for (int f=0;f<features.length;f++) {
			Enumeration<FeatureGroup> en = splitGroups.elements();
			while (en.hasMoreElements()) {
				FeatureGroup fg = en.nextElement();
				if (fg.overlaps(features[f].location())) {
					//					System.err.println("Feature from "+features[f].location().start()+" to "+features[f].location().end()+" overlaps with "+fg.start()+" to "+fg.end());
					fg.addFeature(features[f]);
					continue FEATURE;
				}
			}

			// If we get here then we need to make a new group.
			//			System.err.println("Feature from "+features[f].location().start()+" to "+features[f].location().end()+" needs a new group");
			FeatureGroup newFeatureGroup = new FeatureGroup(group.name());
			newFeatureGroup.addFeature(features[f]);
			splitGroups.add(newFeatureGroup);
		}

		//		System.err.println("Initial split groups is "+splitGroups.size());
		// Now we need to go round trying to merge groups together.

		boolean joinedSomething = true;

		JOIN: while (joinedSomething) {
			joinedSomething = false;

			Enumeration<FeatureGroup> en = splitGroups.elements();

			while (en.hasMoreElements()) {
				FeatureGroup primaryGroup = en.nextElement();

				Enumeration<FeatureGroup> en2 = splitGroups.elements();
				while (en2.hasMoreElements()) {
					FeatureGroup compareGroup = en2.nextElement();

					if (compareGroup == primaryGroup) continue; // Can't merge with itself.

					if (compareGroup.start()<=primaryGroup.end() && compareGroup.end() >= primaryGroup.start()) {
						//						System.err.println("Group from "+compareGroup.start()+" to "+compareGroup.end()+" overlaps with "+primaryGroup.start()+" to "+primaryGroup.end());
						// They match and can be merged.
						Feature [] featuresToMerge = compareGroup.features();
						for (int f=0;f<featuresToMerge.length;f++) {
							primaryGroup.addFeature(featuresToMerge[f]);
						}

						splitGroups.remove(compareGroup);
						joinedSomething = true;
						continue JOIN;
					}
				}
			}


		}



		return splitGroups.toArray(new FeatureGroup[0]);
	}

	protected void startPipeline() {

		// We first need to generate probes over all of the features listed in
		// the feature types.  The probes should cover the whole area of the
		// feature regardless of where it splices.

		Vector<Probe> probes = new Vector<Probe>();
		boolean mergeTranscripts = optionsPanel.mergeTranscripts();
		boolean pairedEnd = optionsPanel.pairedEnd();
		boolean logTransform = optionsPanel.logTransform();
		boolean applyTranscriptLengthCorrection = optionsPanel.applyTranscriptLengthCorrection();
		boolean rawCounts = optionsPanel.rawCounts();
		boolean noValueForZeroCounts = optionsPanel.noValueForZeroCounts();
		boolean correctDNAContamination = optionsPanel.correctForDNAContamination();
		boolean correctDuplication = optionsPanel.correctForDNADuplication();

		if (rawCounts) {
			logTransform = false;
			applyTranscriptLengthCorrection = false;
			noValueForZeroCounts = false;
		}

		Chromosome [] chrs = collection().genome().getAllChromosomes();

		for (int c=0;c<chrs.length;c++) {
			//			System.err.println("Processing chr "+chrs[c].name());
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Making features for chr"+chrs[c].name(), c, chrs.length*2);

			Feature [] features = collection().genome().annotationCollection().getFeaturesForType(chrs[c], optionsPanel.getSelectedFeatureType());
			Arrays.sort(features);

			FeatureGroup [] mergedTranscripts = mergeTranscripts(features,mergeTranscripts);

			//			if (mergedTranscripts.length == 0) {
			//				System.err.println("No groups for chr "+chrs[c].name());
			//			}
			//			else if (mergedTranscripts[0] instanceof IntronFeatureGroup) {
			//				System.err.println("These are intron groups");
			//			}
			//			else {
			//				System.err.println("These are NOT intron groups");
			//			}

			for (int f=0;f<mergedTranscripts.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}
				probes.add(new Probe(chrs[c], mergedTranscripts[f].start(), mergedTranscripts[f].end(), mergedTranscripts[f].strand(),mergedTranscripts[f].name()));

			}
		}

		Probe [] allProbes = probes.toArray(new Probe[0]);
		Arrays.sort(allProbes);

		// Check these probes against the existing probe set from the genome.  If they're the same
		// then don't replace the existing set but just requantitate.

		if (collection().probeSet() == null) {
			collection().setProbeSet(new ProbeSet("Transcript features over "+optionsPanel.getSelectedFeatureType(), allProbes));
		}
		else {
			Probe [] existingProbes = collection().probeSet().getAllProbes();

			Arrays.sort(existingProbes);

			if (allProbes.length != existingProbes.length) {
				collection().setProbeSet(new ProbeSet("Transcript features over "+optionsPanel.getSelectedFeatureType(), allProbes));				
			}
			else {

				// Check the positions against the new ones
				boolean areTheyTheSame = true;
				for (int p=0;p<allProbes.length;p++) {										
					if (allProbes[p].packedPosition() != existingProbes[p].packedPosition()) {
						areTheyTheSame = false;
						break;
					}
				}

				if (areTheyTheSame) {
					allProbes = existingProbes;
				}
				else {
					collection().setProbeSet(new ProbeSet("Transcript features over "+optionsPanel.getSelectedFeatureType(), allProbes));
				}
			}


		}




		// If we're correcting for DNA contamination we need to work out the average density of
		// reads in intergenic regions
		float [] dnaDensityPerKb = new float[data.length];
		int [] correctedTotalCounts = new int[data.length];

		if (correctDNAContamination) {
			// We need to make interstitial probes to the set we already have, ignoring those at the end of chromosomes

			Vector<Probe> intergenicProbes = new Vector<Probe>();

			Chromosome lastChr = allProbes[0].chromosome();
			for (int p=1;p<allProbes.length;p++) {
				if (allProbes[p].chromosome() != lastChr) {
					lastChr = allProbes[p].chromosome();
					continue;
				}

				// See if there's a gap back to the last probe
				if (allProbes[p].start() > allProbes[p-1].end()) {
					if (allProbes[p].start() - allProbes[p-1].end() < 1000) {
						continue; // Don't bother with really short probes
					}

					intergenicProbes.add(new Probe(lastChr, allProbes[p-1].end()+1,allProbes[p].start()-1));

				}	
			}

			Probe [] allIntergenicProbes = intergenicProbes.toArray(new Probe[0]);

			for (int d=0;d<data.length;d++) {

				progressUpdated("Quantitating DNA contamination", 1, 2);

				float [] densities = new float[allIntergenicProbes.length];

				for (int p=0;p<allIntergenicProbes.length;p++) {
					densities[p] = data[d].getReadsForProbe(allIntergenicProbes[p]).length / (allIntergenicProbes[p].length()/1000f);
				}

				dnaDensityPerKb[d] = SimpleStats.median(densities);

			}


			// Work out adjusted total counts having subtracted the DNA contamination
			for (int d=0;d<data.length;d++) {
				int predictedContamination = (int)(dnaDensityPerKb[d]*(SeqMonkApplication.getInstance().dataCollection().genome().getTotalGenomeLength()/1000));
				int correctedTotalReadCount = data[d].getTotalReadCount() - predictedContamination;

				correctedTotalCounts[d] = correctedTotalReadCount;
			}


			// Halve the density if they're doing a directional quantitation
			if (optionsPanel.isDirectional()) {
				for (int i=0;i<dnaDensityPerKb.length;i++) {
					dnaDensityPerKb[i] /= 2;
				}
			}

			// Halve the density if the libraries are paired end
			if (pairedEnd) {
				for (int i=0;i<dnaDensityPerKb.length;i++) {
					dnaDensityPerKb[i] /= 2;
				}				
			}

		}
		
		for (int i=0;i<dnaDensityPerKb.length;i++) {
			System.err.println("For "+data[i].name()+" dna/kb was "+dnaDensityPerKb[i]);
		}
		
		
		// If we're correcting for duplication we need to work out the modal count depth in 
		// intergenic regions
		int [] modalDuplicationLevels = new int[data.length];

		if (correctDuplication) {
			// We're counting within the probes we already have

			for (int d=0;d<data.length;d++) {

				progressUpdated("Quantitating DNA duplication", 1, 2);

				// We're not going to look at depths which are > 200.  If it's that duplicated
				// then there's no point continuing anyway.
				int [] depthCount = new int[200];

				for (int p=0;p<allProbes.length;p++) {
					long [] reads = data[d].getReadsForProbe(allProbes[p]); 

					int currentCount = 0;
					for (int r=1;r<reads.length;r++) {
						if (reads[r] == reads[r-1]) {
							++currentCount;
						}
						else {
							if (currentCount > 0 && currentCount<200) {
								++depthCount[currentCount];
							}
							currentCount = 1;
						}
					}
				
				}

				// Find the modal depth in intergenic regions. This is the best estimate
				// of duplication
				
				// Since unique reads turn up all over the place even in duplicated 
				// data we say that if unique reads are higher than the sum of 2-10 there
				// is no duplication
				int twoTenSum = 0;
				for (int i=2;i<=10;i++) {
					twoTenSum += depthCount[i];
				}
				
				if (depthCount[1] > twoTenSum) {
					modalDuplicationLevels[d] = 1;
				}
				
				else {
				
					int highestDepth = 0;
					int	bestDupGuess = 1;
					for (int i=2;i<depthCount.length;i++) {
//						System.err.println("For depth "+i+" count was "+depthCount[i]);
						if (depthCount[i] > highestDepth) {
							bestDupGuess = i;
							highestDepth = depthCount[i];
						}
					}

				
					modalDuplicationLevels[d] = bestDupGuess;
				}

			}

		}
		
	
		for (int i=0;i<modalDuplicationLevels.length;i++) {
			System.err.println("For "+data[i].name()+" duplication was "+modalDuplicationLevels[i]);
		}
		
	


		// Having made probes we now need to quantitate them.  We'll fetch the
		// probes overlapping each sub-feature and then aggregate these together
		// to get the final quantitation.

		QuantitationStrandType readFilter = optionsPanel.readFilter();
		int currentIndex = 0;
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Quantitating features on chr"+chrs[c].name(), chrs.length+c, chrs.length*2);

			Feature [] features = collection().genome().annotationCollection().getFeaturesForType(chrs[c], optionsPanel.getSelectedFeatureType());
			Arrays.sort(features);

			FeatureGroup [] mergedTranscripts = mergeTranscripts(features,mergeTranscripts);

			int [] readLengths = new int[data.length];

			for (int d=0;d<data.length;d++) {
				readLengths[d] = data[d].getMaxReadLength();
				// If this library is paired end then the effective read length is twice the
				// actual length.
				if (pairedEnd) {
					readLengths[d] *= 2;
				}
			}

			for (int f=0;f<mergedTranscripts.length;f++) {

				Location [] subLocations = mergedTranscripts[f].getSubLocations();

				int totalLength = 0;

				// Find the total length of all of the exons
				for (int s=0;s<subLocations.length;s++) {
					totalLength += subLocations[s].length();
				}

				//				System.err.println("Transcript "+mergedTranscripts[f].name+" had length "+totalLength);


				for (int d=0;d<data.length;d++) {
					if (cancel) {
						progressCancelled();
						return;
					}

					long totalCount = 0;

					for (int s=0;s<subLocations.length;s++) {
						long [] reads = data[d].getReadsForProbe(new Probe(chrs[c], subLocations[s].start(), subLocations[s].end()));
						for (int r=0;r<reads.length;r++) {

							if (! readFilter.useRead(subLocations[s], reads[r])) {
								continue;
							}

							int overlap = (Math.min(subLocations[s].end(), SequenceRead.end(reads[r]))-Math.max(subLocations[s].start(), SequenceRead.start(reads[r])))+1;
							totalCount += overlap;
						}
					}

					// Now we correct the count by the total length of reads in the data and by
					// the length of the split parts of the probe, and assign this to the probe.

					// As we're correcting for read length then we work out the whole number of
					// reads which this count could comprise, rounding down to a whole number.
					totalCount /= readLengths[d];

					// We can now subtract the DNA contamination prediction.
					if (correctDNAContamination) {
						int predictedContamination = (int)((totalLength/1000f)*dnaDensityPerKb[d]);

						totalCount -= predictedContamination;

						if (totalCount < 0) totalCount = 0; // Makes no sense to have negative counts

					}

					// ..and we can divide by the duplication level if we know it.
					if (correctDuplication) {
						totalCount /= modalDuplicationLevels[d];
					}


					//					System.err.println("Total read count for "+mergedTranscripts[f].name+" is "+totalCount);

					float value = totalCount;
					if (value == 0 && noValueForZeroCounts) {
						value = Float.NaN;
					}

					// If we're log transforming then we need to set zero values to 0.9
					if (logTransform && value == 0 && !noValueForZeroCounts) {
						value = 0.9f;
					}

					// We now correct by the length of the exons in the probe if we've
					// been asked to.
					if (applyTranscriptLengthCorrection) {
						value /= (totalLength/1000f);
					}

					//					System.err.println("Length corrected read count for "+mergedTranscripts[f].name+" is "+totalCount);


					// We also correct by the total read count
					if (!rawCounts) {

						//	System.err.println("True total is "+data[d].getTotalReadCount()+" corrected total is "+correctedTotalCounts[d]);

						// If these libraries are paired end then the total number of
						// reads is also effectively halved.
						
						float totalReadCount;
						
						// We start by getting the original total.  For DNA contamination correction we'll have
						// calculated this already, but otherwise we'll take the total count (total length/read length)
						if (correctDNAContamination) {
							totalReadCount = correctedTotalCounts[d];
						}
						else {
							totalReadCount = data[d].getTotalReadLength()/readLengths[d];
						}
						
						
						// If we're correcting for duplication we divide by the duplication level.
						if (correctDuplication) {
							totalReadCount /= modalDuplicationLevels[d];
						}
						
						
						// Finally we work out millions of reads (single end) or fragments (paired end)
						if (pairedEnd) {
							totalReadCount /= 2000000f;
						}
						else {
							totalReadCount /= 1000000f;
						}
						
						
						// Lastly we divide the value by the total millions of reads to get the globally corrected count.
						value /= totalReadCount;
												
					}

					//	System.err.println("Total corrected read count for "+mergedTranscripts[f].name+" is "+totalCount);


					// Finally we do the log transform if we've been asked to
					if (logTransform) {
						value = (float)Math.log(value)/log2;
					}

					data[d].setValueForProbe(allProbes[currentIndex], value);

				}

				currentIndex++;
			}
		}

		collection().probeSet().setCurrentQuantitation(getQuantitationDescription(mergeTranscripts,applyTranscriptLengthCorrection,correctDNAContamination,logTransform,rawCounts));

		quantitatonComplete();

	}

	protected String getQuantitationDescription (boolean mergeTranscripts, boolean applyTranscriptLengthCorrection, boolean correctDNAContamination, boolean logTransform, boolean rawCounts) {

		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("RNA-Seq pipeline quantitation");
		if (mergeTranscripts) {
			quantitationDescription.append(" on merged transcripts");
		}

		quantitationDescription.append(" counting reads over exons");
		
		if (rawCounts) {
			quantitationDescription.append(" as raw counts");
		}

		else {
			if (applyTranscriptLengthCorrection) {
				quantitationDescription.append(" correcting for feature length");
			}

			if (correctDNAContamination) {
				quantitationDescription.append(" correcting for DNA contamination");
			}

			if (logTransform) {
				quantitationDescription.append(". Log transformed");
			}
		}

		quantitationDescription.append(". Assuming a ");
		quantitationDescription.append(optionsPanel.libraryTypeBox.getSelectedItem());
		quantitationDescription.append(" library");

		return (quantitationDescription.toString());
	}

	public String name() {
		return "RNA-Seq quantitation pipeline";
	}


	protected class RNASeqOptionsPanel extends JPanel {

		JComboBox featureTypeBox;
		JCheckBox rawCountsBox;
		JCheckBox mergeTranscriptsBox;
		JComboBox libraryTypeBox;
		JCheckBox pairedEndBox;
		JCheckBox logTransformBox;
		JCheckBox applyTranscriptLengthCorrectionBox;
		JCheckBox noValueForZeroBox;
		JCheckBox correctForDNAContaminationBox;
		JCheckBox correctForDNADuplicationBox;

		public RNASeqOptionsPanel (String [] featureTypes) {

			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			// Check to see if we need to issue a warning...
			DataSet [] allSets = collection().getAllDataSets();
			boolean warnAboutLength = false;
			for (int s=0;s<allSets.length;s++) {
				// If we have data which jumps over splice sites then this
				// isn't an appropriate thing to do and we need to warn the
				// user
				if (allSets[s].getMaxReadLength()>5000) {
					// We're probably safe for a bit with a 5kb real length limit
					warnAboutLength = true;
				}
			}

			if (warnAboutLength) {
				gbc.gridx=1;
				gbc.gridwidth = 2;
				gbc.gridy=0;
				JLabel warningLabel = new JLabel("<html><center>This quantitation requires data to have been imported with <br>the 'Treat as RNA-Seq data' option turned on<br>and your data doesn't look like it had that.</center></html>",JLabel.CENTER);
				warningLabel.setForeground(Color.RED);
				add(warningLabel,gbc);
				gbc.gridwidth = 1;
			}


			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			add(new JLabel("Transcript features "),gbc);

			gbc.gridx=2;

			featureTypeBox = new JComboBox(featureTypes);
			featureTypeBox.setPrototypeDisplayValue("No longer than this please");
			for (int i=0;i<featureTypes.length;i++) {
				if (featureTypes[i].toLowerCase().equals(lastFeatureType.toLowerCase())) {
					featureTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(featureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Library type"),gbc);

			gbc.gridx=2;

			libraryTypeBox = new JComboBox(new String [] {"Non-strand specific", "Same strand specific", "Opposing strand specific"});
			for (int i=0;i<libraryTypeBox.getModel().getSize();i++) {
				if (libraryTypeBox.getItemAt(i).equals(lastLibraryType)) {
					libraryTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(libraryTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Libraries are paired end"),gbc);

			gbc.gridx=2;

			pairedEndBox = new JCheckBox("",lastPairedEnd);

			add(pairedEndBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Merge transcript isoforms"),gbc);

			gbc.gridx=2;

			mergeTranscriptsBox = new JCheckBox("",lastMergeTranscripts);

			add(mergeTranscriptsBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Generate Raw Counts"),gbc);

			gbc.gridx=2;

			rawCountsBox = new JCheckBox();
			rawCountsBox.setSelected(lastRawCounts);
			rawCountsBox.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					logTransformBox.setEnabled(!rawCountsBox.isSelected());
					applyTranscriptLengthCorrectionBox.setEnabled(!rawCountsBox.isSelected());
					noValueForZeroBox.setEnabled(!rawCountsBox.isSelected());
				}
			});

			add(rawCountsBox,gbc);	

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Log transform"),gbc);

			gbc.gridx=2;

			logTransformBox = new JCheckBox();
			logTransformBox.setSelected(lastLogTransform);
			logTransformBox.setEnabled(!rawCountsBox.isSelected());

			add(logTransformBox,gbc);	

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Apply transcript length correction"),gbc);

			gbc.gridx=2;

			applyTranscriptLengthCorrectionBox = new JCheckBox();
			applyTranscriptLengthCorrectionBox.setSelected(lastTranscriptLengthCorrection);

			add(applyTranscriptLengthCorrectionBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Don't quantitate probes with no counts"),gbc);

			gbc.gridx=2;

			noValueForZeroBox = new JCheckBox();
			noValueForZeroBox.setSelected(lastNoValueForZero);

			add(noValueForZeroBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Correct for DNA Contamination"),gbc);

			gbc.gridx=2;

			correctForDNAContaminationBox = new JCheckBox();
			correctForDNAContaminationBox.setSelected(lastCorrectDNAContamination);

			add(correctForDNAContaminationBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Correct for DNA Duplication"),gbc);

			gbc.gridx=2;

			correctForDNADuplicationBox = new JCheckBox();
			correctForDNADuplicationBox.setSelected(lastCorrectDNADuplication);

			add(correctForDNADuplicationBox,gbc);

			
		}

		public boolean mergeTranscripts () {
			lastMergeTranscripts = mergeTranscriptsBox.isSelected();
			return mergeTranscriptsBox.isSelected();
		}

		public boolean pairedEnd () {
			lastPairedEnd = pairedEndBox.isSelected();
			return lastPairedEnd;
		}

		public boolean logTransform () {
			lastLogTransform = logTransformBox.isSelected();
			return logTransformBox.isSelected();
		}

		public boolean rawCounts () {
			lastRawCounts = rawCountsBox.isSelected();
			return rawCountsBox.isSelected();
		}

		public boolean isDirectional () {
			return (! libraryTypeBox.getSelectedItem().toString().equals("Non-strand specific"));
		}

		public QuantitationStrandType readFilter () {

			QuantitationStrandType [] options = QuantitationStrandType.getTypeOptions();

			String libraryType = libraryTypeBox.getSelectedItem().toString();
			String quantitationType = "";

			if (libraryType.equals("Non-strand specific")) {
				quantitationType = "All Reads";
			}
			else if (libraryType.equals("Same strand specific")) {
				quantitationType = "Same Strand as Probe";
			}
			else if (libraryType.equals("Opposing strand specific")) {
				quantitationType = "Opposite Strand to Probe";
			}
			else {
				throw new IllegalStateException("Unknown library type '"+libraryType+"'");
			}

			lastLibraryType = libraryType;

			for (int i=0;i<options.length;i++) {
				if (options[i].toString().equals(quantitationType)) {
					return options[i];
				}
			}

			throw new IllegalStateException("Couldn't find quantitationStrandType of type '"+quantitationType+"'");


		}

		public boolean applyTranscriptLengthCorrection () {
			lastTranscriptLengthCorrection = applyTranscriptLengthCorrectionBox.isSelected();
			return applyTranscriptLengthCorrectionBox.isSelected();
		}

		public boolean noValueForZeroCounts () {
			lastNoValueForZero = noValueForZeroBox.isSelected();
			return noValueForZeroBox.isSelected();
		}

		public boolean correctForDNAContamination () {
			lastCorrectDNAContamination = correctForDNAContaminationBox.isSelected();
			return lastCorrectDNAContamination;
		}

		
		public boolean correctForDNADuplication () {
			lastCorrectDNADuplication = correctForDNADuplicationBox.isSelected();
			return lastCorrectDNADuplication;
		}


		public String getSelectedFeatureType () {
			lastFeatureType = (String)featureTypeBox.getSelectedItem();
			return (String)featureTypeBox.getSelectedItem();
		}

	}


}
