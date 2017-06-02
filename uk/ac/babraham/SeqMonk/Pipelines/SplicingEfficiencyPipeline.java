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
package uk.ac.babraham.SeqMonk.Pipelines;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * The splicing efficiency pipeline quantitates based on the relative number of reads
 * within exons and introns for a set of transcripts.
 * 
 * @author andrewss
 *
 */
public class SplicingEfficiencyPipeline extends Pipeline {

	private SplicingEfficiencyOptionsPanel optionsPanel;

	private static final int EXONS = 1;
	private static final int INTRONS = 2;
	private static final int RATIO = 3;


	public SplicingEfficiencyPipeline (DataCollection collection) {
		super(collection);
		optionsPanel = new SplicingEfficiencyOptionsPanel(collection.genome().annotationCollection().listAvailableFeatureTypes());
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


	protected void startPipeline() {

		// We first need to generate probes over all of the features listed in
		// the feature types.  The probes should cover the whole area of the
		// feature regardless of where it splices.

		Vector<Probe> probes = new Vector<Probe>();
		boolean correctReadLength = optionsPanel.correctForReadLength();
		boolean logTransform = optionsPanel.logTransform();
		boolean applyTranscriptLengthCorrection = optionsPanel.applyTranscriptLengthCorrection();

		// We need to make a lookup for chromosomes from their names for later on.
		Chromosome [] chrs = collection().genome().getAllChromosomes();
		
		Hashtable<String, Chromosome> chrLookup = new Hashtable<String, Chromosome>();
		for (int c=0;c<chrs.length;c++) {
			chrLookup.put(chrs[c].name(), chrs[c]);
		}

		// We're going to cache the gene to transcript mappings so we don't have to re-do them
		// later on.

		Hashtable<String, Vector<Feature>>transcriptLookup = new Hashtable<String, Vector<Feature>>();
		Vector<Feature> usedGeneFeatures = new Vector<Feature>();

		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Making features for chr"+chrs[c].name(), c, chrs.length*2);

			Feature [] geneFeatures = collection().genome().annotationCollection().getFeaturesForType(chrs[c], optionsPanel.getSelectedGeneFeatureType());
			Arrays.sort(geneFeatures);	

			Feature [] transcriptFeatures = collection().genome().annotationCollection().getFeaturesForType(chrs[c], optionsPanel.getSelectedTranscriptFeatureType());
			Arrays.sort(transcriptFeatures);

			// We need to figure out whether we can match based on name, or whether we need to do it based on location.

			boolean matchOnNames = true;

			for (int t=0;t<transcriptFeatures.length;t++) {
				if (! transcriptFeatures[t].name().matches("-\\d\\d\\d$")) {
					matchOnNames = false;
					break;
				}
			}

			if (matchOnNames) {
				for (int i=0;i<transcriptFeatures.length;i++) {

					String name = transcriptFeatures[i].name();
					name = name.replaceAll("-\\d\\d\\d$", "");
					if (! transcriptLookup.containsKey(name)) {
						transcriptLookup.put(name, new Vector<Feature>());
					}

					transcriptLookup.get(name).add(transcriptFeatures[i]);
				}
			}

			else {

				// We need to go through the genes and transcripts in parallel and match them up based on containment
				// and direction.

				int lastGeneIndex = 0;

				for (int t=0;t<transcriptFeatures.length;t++) {
					for (int g=lastGeneIndex;g<geneFeatures.length;g++) {
						// If this transcript is off the end of this gene then
						// we never need to look at this gene again

						if (transcriptFeatures[t].location().start() > geneFeatures[g].location().end()) {
							lastGeneIndex = g;
							continue;
						}

						// If the gene is beyond the end of the transcript we can stop looking
						if (geneFeatures[g].location().start() > transcriptFeatures[t].location().end()) {
							break;
						}

						// If we're on the same strand and contained within the gene then we have a match
						if (
								geneFeatures[g].location().strand() == transcriptFeatures[t].location().strand() &&
								transcriptFeatures[t].location().start() >= geneFeatures[g].location().start() &&
								transcriptFeatures[t].location().end() <= geneFeatures[g].location().end()
								) {
							String name = geneFeatures[g].name();
							if (! transcriptLookup.containsKey(name)) {
								transcriptLookup.put(name, new Vector<Feature>());
							}

							transcriptLookup.get(name).add(transcriptFeatures[t]);
						}
					}
				}

			}



			// We make probes over each gene, but use the combined set of transcripts to define the
			// exonic regions.
			// 
			// For us to keep a gene here we need to have at least one valid transcript (matches
			// and ovelaps.  At least one of the transcripts also needs to be multi-exon.

			for (int f=0;f<geneFeatures.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}

				if (! transcriptLookup.containsKey(geneFeatures[f].name())) continue; // No good making features for genes with no transcript.

				Feature [] localTranscripts = transcriptLookup.get(geneFeatures[f].name()).toArray(new Feature[0]);

				boolean validTranscript = false;
				for (int t=0;t<localTranscripts.length;t++) {
					if (!(localTranscripts[t].location() instanceof SplitLocation)) continue;
					
					if (((SplitLocation)localTranscripts[t].location()).subLocations().length < 2) continue;

					if (SequenceRead.overlaps(geneFeatures[f].location().packedPosition(), localTranscripts[t].location().packedPosition())) {
						validTranscript = true;
						break;
					}
				}


				if (validTranscript) {
					probes.add(new Probe(chrs[c], geneFeatures[f].location().start(), geneFeatures[f].location().end(), geneFeatures[f].location().strand(),geneFeatures[f].name()));
					usedGeneFeatures.add(geneFeatures[f]);
				}
			}
		}

		Probe [] allProbes = probes.toArray(new Probe[0]);

		collection().setProbeSet(new ProbeSet("Gene features over "+optionsPanel.getSelectedGeneFeatureType(), allProbes));

		// Having made probes we now need to quantitate them.  We'll get the full set of reads for the full gene
		// and then work out a combined set of exons.  We can then quantitate the bases which fall into introns
		// and exons separately and then work out a ratio from there.

		QuantitationStrandType readFilter = optionsPanel.readFilter();

		Feature [] geneFeatures = usedGeneFeatures.toArray(new Feature[0]);


		for (int g=0;g<geneFeatures.length;g++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			if (g % 100 == 0) {
				progressUpdated("Quantitating features", g, geneFeatures.length);
			}


			int [] readLengths = new int[data.length];

			if (correctReadLength) {
				for (int d=0;d<data.length;d++) {
					readLengths[d] = data[d].getMaxReadLength();
				}

			}


			// Find the set of transcripts which relate to this gene.  

			Feature [] transcripts = transcriptLookup.get(geneFeatures[g].name()).toArray(new Feature[0]);
			
			Vector<Feature> validTranscripts = new Vector<Feature>();
			
			for (int t=0;t<transcripts.length;t++) {
				if (SequenceRead.overlaps(geneFeatures[g].location().packedPosition(), transcripts[t].location().packedPosition())) {
					validTranscripts.add(transcripts[t]);
				}
			}
			
			transcripts = validTranscripts.toArray(new Feature[0]);
			
			// We should never get a gene with no transcripts at this point as they should have been removed
			// before so if we do then something has gone wrong.
			if (transcripts.length == 0) {
				throw new IllegalStateException("No transcripts for gene "+geneFeatures[g]+" on chr "+geneFeatures[g].chromosomeName());
			}
			

			Vector<Location> allExonsVector = new Vector<Location>();

			for (int t=0;t<transcripts.length;t++) {
				if (transcripts[t].location() instanceof SplitLocation) {
					Location [] sublocs = ((SplitLocation)transcripts[t].location()).subLocations();
					for (int i=0;i<sublocs.length;i++) allExonsVector.add(sublocs[i]);
				}
				else {
					allExonsVector.add(transcripts[t].location());
				}
			}

			//				if (geneFeatures[f].name().equals("Cpa6")) {
			//					System.err.println("Cpa6 had "+allExonsVector.size()+" total exons");
			//				}

			Collections.sort(allExonsVector);

			// Now go through and make a merged set of exons to remove any redundancy
			Vector<Location> mergedExonsVector = new Vector<Location>();

			Location lastLocation = null;

			Enumeration<Location> en = allExonsVector.elements();

			while (en.hasMoreElements()) {					

				Location l = en.nextElement();

				//					if (geneFeatures[f].name().equals("Cpa6")) {
				//						System.err.println("Looking at location "+l.toString());
				//					}


				if (lastLocation == null) {
					//						if (geneFeatures[f].name().equals("Cpa6")) {
					//							System.err.println("Setting as first location");
					//						}
					lastLocation = l;
					continue;
				}

				// Check if it's the same as the last, which is likely
				if (l.start() == lastLocation.start() && l.end() == lastLocation.end()) {
					//						if (geneFeatures[f].name().equals("Cpa6")) {
					//							System.err.println("Same as last location - skipping");
					//						}

					continue;
				}

				// Check if they overlap and can be merged
				if (l.start() <= lastLocation.end() && l.end() >= lastLocation.start()) {
					//						if (geneFeatures[f].name().equals("Cpa6")) {
					//							System.err.println("Overlaps with last location");
					//						}

					// It overlaps with the last location so merge them
					lastLocation = new Location(Math.min(l.start(),lastLocation.start()), Math.max(l.end(),lastLocation.end()), geneFeatures[g].location().strand());
					//						if (geneFeatures[f].name().equals("Cpa6")) {
					//							System.err.println("Made new location "+lastLocation.toString());
					//						}
					continue;
				}

				// Start a new location
				//					if (geneFeatures[f].name().equals("Cpa6")) {
				//						System.err.println("Doesn't overlap - adding last location and creating new one");
				//					}

				mergedExonsVector.add(lastLocation);
				lastLocation = l;
			}

			if (lastLocation != null) {
				mergedExonsVector.add(lastLocation);
			}

			//				if (geneFeatures[f].name().equals("Cpa6")) {
			//					System.err.println("Cpa6 had "+mergedExonsVector.size()+" merged exons");
			//				}


			// Now we can start the quantitation.

			int intronLength = geneFeatures[g].location().length();
			int exonLength = 0;

			Location [] subLocs = mergedExonsVector.toArray(new Location[0]);

			for (int l=0;l<subLocs.length;l++) {
				exonLength += subLocs[l].length();
			}

			//				if (geneFeatures[f].name().equals("Cpa6")) {
			//					System.err.println("Cpa6 total intron length="+intronLength+" exon length="+exonLength);
			//				}

			intronLength -= exonLength;

			//				if (geneFeatures[f].name().equals("Cpa6")) {
			//					System.err.println("Cpa6 corrected intron length="+intronLength);
			//				}


			if (intronLength <= 0) {
				progressWarningReceived(new IllegalStateException("Intron length of "+intronLength+" for gene "+geneFeatures[g]));
				continue;
			}
			if (exonLength <= 0) {
				throw new IllegalStateException("Exon length of "+exonLength+" for gene "+geneFeatures[g]);
			}

			for (int d=0;d<data.length;d++) {
				if (cancel) {
					progressCancelled();
					return;
				}

				int totalIntronCount = 0;
				int totalExonCount = 0;

				long [] reads = data[d].getReadsForProbe(new Probe(chrLookup.get(geneFeatures[g].chromosomeName()), geneFeatures[g].location().start(), geneFeatures[g].location().end()));
				for (int r=0;r<reads.length;r++) {	
					if (! readFilter.useRead(geneFeatures[g].location(), reads[r])) {
						continue;
					}

					int overlap = (Math.min(geneFeatures[g].location().end(), SequenceRead.end(reads[r]))-Math.max(geneFeatures[g].location().start(), SequenceRead.start(reads[r])))+1;
					totalIntronCount += overlap;

					// Now we see if we overlap with any of the exons
					for (int s=0;s<subLocs.length;s++) {
						if (subLocs[s].start()<=SequenceRead.end(reads[r]) && subLocs[s].end()>=SequenceRead.start(reads[r])) {
							int exonOverlap = (Math.min(subLocs[s].end(), SequenceRead.end(reads[r]))-Math.max(subLocs[s].start(), SequenceRead.start(reads[r])))+1;
							if (exonOverlap > 0) {
								totalExonCount += exonOverlap;
								totalIntronCount -= exonOverlap;
							}
						}
					}

				}

				//					if (geneFeatures[f].name().equals("Cpa6")) {
				//						System.err.println("Total exon count="+totalExonCount+" total intron count="+totalIntronCount);
				//					}


				if (totalIntronCount < 0) {
					progressWarningReceived(new SeqMonkException("Intron count of "+totalIntronCount+" for "+geneFeatures[g].name()));
					continue;						
				}

				// Now we correct the count by the total length of reads in the data and by
				// the length of the split parts of the probe, and assign this to the probe.

				// If we're correcting for read length then we work out the whole number of
				// reads which this count could comprise, rounding down to a whole number.
				if (correctReadLength) {
					totalIntronCount /= readLengths[d];
					totalExonCount /= readLengths[d];
				}

				float intronValue = totalIntronCount;
				float exonValue = totalExonCount;

				// If we're log transforming then we need to set zero values to 0.9
				if (logTransform && intronValue == 0) {
					intronValue = 0.9f;
				}
				if (logTransform && exonValue == 0) {
					exonValue = 0.9f;
				}

				// We now correct by the length of the exons in the probe if we've
				// been asked to.
				if (applyTranscriptLengthCorrection) {
					intronValue /= (intronLength/1000f);
					exonValue /=  (exonLength/1000f);
				}

				// We also correct by the total read count, or length
				if (correctReadLength) {
					intronValue /= (data[d].getTotalReadCount()/1000000f);
					exonValue /= (data[d].getTotalReadCount()/1000000f);
				}
				else {
					intronValue /= (data[d].getTotalReadLength()/1000000f);
					exonValue /= (data[d].getTotalReadLength()/1000000f);
				}

				//					if (geneFeatures[f].name().equals("Cpa6")) {
				//						System.err.println("Raw corrected counts are exon="+exonValue+" intron="+intronValue);
				//					}


				// Finally we do the log transform if we've been asked to
				if (logTransform) {
					if (intronValue == 0) {
						intronValue = 0.0001f;
					}
					if (exonValue == 0) {
						exonValue = 0.0001f;
					}
					intronValue = (float)Math.log(intronValue)/log2;
					exonValue = (float)Math.log(exonValue)/log2;
				}

				//					if (geneFeatures[f].name().equals("Cpa6")) {
				//						System.err.println("Logged corrected counts are exon="+exonValue+" intron="+intronValue);
				//					}


				// Now we check what value they actually wanted to record
				switch (optionsPanel.quantitationType()) {
				case (EXONS): {
					data[d].setValueForProbe(allProbes[g], exonValue);
					break;
				}
				case (INTRONS): {
					data[d].setValueForProbe(allProbes[g], intronValue);
					break;
				}
				case (RATIO): {
					if (logTransform) {
						data[d].setValueForProbe(allProbes[g], exonValue-intronValue);
					}
					else {
						if (intronValue == 0) {
							intronValue = 0.0001f;
						}
						data[d].setValueForProbe(allProbes[g], exonValue/intronValue);
					}
					break;
				}
				default: throw new IllegalStateException("Unknonwn quantitation type "+optionsPanel.quantitationType());
				}

				//					if (geneFeatures[f].name().equals("Cpa6")) {
				//						try {
				//							System.err.println("Stored value was "+data[d].getValueForProbe(allProbes[currentIndex]));
				//						} catch (SeqMonkException e) {
				//							e.printStackTrace();
				//						}
				//					}


			}

		}

		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Splicing efficiency quantitation");
		if (correctReadLength) {
			quantitationDescription.append(" counting reads");
		}
		else {
			quantitationDescription.append(" counting bases");
		}
		if (optionsPanel.logTransform()) {
			quantitationDescription.append(" log transformed");
		}

		if (applyTranscriptLengthCorrection) {
			quantitationDescription.append(" correcting for feature length");
		}

		//TODO: Add more description

		collection().probeSet().setCurrentQuantitation(quantitationDescription.toString());

		quantitatonComplete();

	}

	public String name() {
		return "Splicing efficiency quantitation";
	}


	private class SplicingEfficiencyOptionsPanel extends JPanel {

		JComboBox geneFeatureTypeBox;
		JComboBox transcriptFeatureTypeBox;
		JComboBox countTypeBox;
		JCheckBox readLengthCorrectionBox;
		JComboBox libraryTypeBox;
		JCheckBox logTransformBox;
		JCheckBox applyTranscriptLengthCorrectionBox;

		public SplicingEfficiencyOptionsPanel (String [] featureTypes) {

			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			add(new JLabel("Gene features "),gbc);

			gbc.gridx=2;

			geneFeatureTypeBox = new JComboBox(featureTypes);
			geneFeatureTypeBox.setPrototypeDisplayValue("No longer than this please");
			for (int i=0;i<featureTypes.length;i++) {
				if (featureTypes[i].toLowerCase().equals("gene")) {
					geneFeatureTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(geneFeatureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Transcript features "),gbc);

			gbc.gridx=2;

			transcriptFeatureTypeBox = new JComboBox(featureTypes);
			transcriptFeatureTypeBox.setPrototypeDisplayValue("No longer than this please");
			for (int i=0;i<featureTypes.length;i++) {
				if (featureTypes[i].toLowerCase().equals("mrna")) {
					transcriptFeatureTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(transcriptFeatureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Library type"),gbc);

			gbc.gridx=2;

			libraryTypeBox = new JComboBox(new String [] {"Non-strand specific", "Same strand specific", "Opposing strand specific"});

			add(libraryTypeBox,gbc);


			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Count what"),gbc);

			gbc.gridx=2;

			countTypeBox = new JComboBox(new String [] {"Ratio Exons:Introns", "Exons", "Introns" });

			add(countTypeBox,gbc);


			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Count reads rather than bases"),gbc);

			gbc.gridx=2;

			readLengthCorrectionBox = new JCheckBox("",true);
			readLengthCorrectionBox.addKeyListener(new NumberKeyListener(false, false));

			add(readLengthCorrectionBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Log transform"),gbc);

			gbc.gridx=2;

			logTransformBox = new JCheckBox();
			logTransformBox.setSelected(true);

			add(logTransformBox,gbc);	

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Apply transcript length correction"),gbc);

			gbc.gridx=2;

			applyTranscriptLengthCorrectionBox = new JCheckBox();
			applyTranscriptLengthCorrectionBox.setSelected(true);

			add(applyTranscriptLengthCorrectionBox,gbc);			

		}

		public boolean correctForReadLength () {
			return readLengthCorrectionBox.isSelected();
		}

		public boolean logTransform () {
			return logTransformBox.isSelected();
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

			for (int i=0;i<options.length;i++) {
				if (options[i].toString().equals(quantitationType)) {
					return options[i];
				}
			}

			throw new IllegalStateException("Couldn't find quantitationStrandType of type '"+quantitationType+"'");	
		}

		public boolean applyTranscriptLengthCorrection () {
			return applyTranscriptLengthCorrectionBox.isSelected();
		}

		public int quantitationType () {

			String quant = countTypeBox.getSelectedItem().toString();

			if (quant.equals("Exons")) return EXONS;
			if (quant.equals("Introns")) return INTRONS;
			if (quant.equals("Ratio Exons:Introns")) return RATIO;
			throw new IllegalStateException("Unknown quantitation type "+quant);

		}

		public String getSelectedGeneFeatureType () {
			return (String)geneFeatureTypeBox.getSelectedItem();
		}

		public String getSelectedTranscriptFeatureType () {
			return (String)transcriptFeatureTypeBox.getSelectedItem();
		}


	}

}
