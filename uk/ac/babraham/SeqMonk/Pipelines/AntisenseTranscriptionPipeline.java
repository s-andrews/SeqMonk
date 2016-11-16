/**
 * Copyright 2011-15 Simon Andrews
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.math3.distribution.BinomialDistribution;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ProbeTTestValue;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * The antisense transcription pipeline aims to find sets of features where
 * significant amounts of antisense transcription is occurring.  It works out
 * a global level of antisense observation and then uses this to statistically
 * test the observed level of antisense in each one of a set of features.
 * 
 * @author andrewss
 *
 */
public class AntisenseTranscriptionPipeline extends Pipeline {

	private AntisenseOptionsPanel optionsPanel;

	public AntisenseTranscriptionPipeline (DataCollection collection) {
		super(collection);
		optionsPanel = new AntisenseOptionsPanel(collection.genome().annotationCollection().listAvailableFeatureTypes());
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
		double pValue = optionsPanel.pValue();
		QuantitationStrandType readFilter = optionsPanel.readFilter();

		
		long [] senseCounts = new long [data.length];
		long [] antisenseCounts = new long [data.length];
		
		
		Chromosome [] chrs = collection().genome().getAllChromosomes();

		// First find the overall rate of antisense reads
		
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Getting total antisense rate for chr"+chrs[c].name(), c, chrs.length*2);

			Feature [] features = getValidFeatures(chrs[c]);
			
			for (int f=0;f<features.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}
				Probe p = new Probe(chrs[c], features[f].location().start(), features[f].location().end(), features[f].location().strand(),features[f].name());
				probes.add(p);

				for (int d=0;d<data.length;d++) {
					long [] reads = data[d].getReadsForProbe(p);
					for (int r=0;r<reads.length;r++) {
						if (readFilter.useRead(p, reads[r])) {
							senseCounts[d] += SequenceRead.length(reads[r]);
						}
						else {
							antisenseCounts[d] += SequenceRead.length(reads[r]);							
						}
					}
				}
				
			}
		}

		Probe [] allProbes = probes.toArray(new Probe[0]);

		collection().setProbeSet(new ProbeSet("Features over "+optionsPanel.getSelectedFeatureType(), allProbes));

		// Now we can work out the overall antisense rate
		double [] antisenseProbability = new double [data.length];
		for (int d=0;d<data.length;d++) {
			
			System.err.println("Antisense counts are "+antisenseCounts[d]+" sense counts are "+senseCounts[d]);
			
			antisenseProbability[d] = antisenseCounts[d] / (double)(antisenseCounts[d]+senseCounts[d]);
			
			System.err.println("Antisense probability for "+data[d].name()+" is "+antisenseProbability[d]);
		}
		
		// Now we can quantitate each individual feature and test for whether it is significantly 
		// showing antisense expression
		
		ArrayList<Vector<ProbeTTestValue>> significantProbes = new ArrayList<Vector<ProbeTTestValue>>();
		
		for (int d=0;d<data.length;d++) {
			significantProbes.add(new Vector<ProbeTTestValue>());
		}
		

		int [] readLengths = new int[data.length];

		for (int d=0;d<readLengths.length;d++) {
			readLengths[d] = data[d].getMaxReadLength();
			System.err.println("For "+data[d].name()+" max read len is "+readLengths[d]);
		}
				
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Quantitating features on chr"+chrs[c].name(), chrs.length+c, chrs.length*2);

			Probe [] thisChrProbes = collection().probeSet().getProbesForChromosome(chrs[c]);
			

			for (int p=0;p<thisChrProbes.length;p++) {

				for (int d=0;d<data.length;d++) {
					if (cancel) {
						progressCancelled();
						return;
					}

					long senseCount = 0;
					long antisenseCount = 0;

					long [] reads = data[d].getReadsForProbe(thisChrProbes[p]);
					for (int r=0;r<reads.length;r++) {

						if (readFilter.useRead(thisChrProbes[p], reads[r])) {
							// TODO: Just count overlap?
							senseCount += SequenceRead.length(reads[r]);
						}
						else {
							antisenseCount += SequenceRead.length(reads[r]);
						}

					}
					
//					if (thisChrProbes[p].name().equals("RP4-798A10.2")) {
//						System.err.println("Raw base counts are sense="+senseCount+" anti="+antisenseCount+" from "+reads.length+" reads");
//					}
					
					int senseReads = (int)(senseCount/readLengths[d]);
					int antisenseReads = (int)(antisenseCount/readLengths[d]);

//					if (thisChrProbes[p].name().equals("RP4-798A10.2")) {
//						System.err.println("Raw read counts are sense="+senseReads+" anti="+antisenseReads+" from "+reads.length+" reads");
//					}
					
					BinomialDistribution bd = new BinomialDistribution(senseReads+antisenseReads, antisenseProbability[d]);
					
					// Since the binomial distribution gives the probability of getting a value higher than
					// this we need to subtract one so we get the probability of this or higher.
					double thisPValue = 1-bd.cumulativeProbability(antisenseReads-1);
																				
					if (antisenseReads == 0) thisPValue = 1;

					// We have to add all results at this stage so we don't mess up the multiple 
					// testing correction later on.
					significantProbes.get(d).add(new ProbeTTestValue(thisChrProbes[p], thisPValue));
					
					double expected = ((senseReads+antisenseReads)*antisenseProbability[d]);

//					if (thisChrProbes[p].name().equals("RP4-798A10.2")) {
//						System.err.println("Probe="+thisChrProbes[p]+" sense="+senseReads+" anti="+antisenseReads+" anti-prob="+antisenseProbability[d]+" expected="+expected+" raw-p="+thisPValue);
//					}
					
					if (expected < 1) expected = 1;
					
					float obsExp = antisenseReads/(float)expected;

					data[d].setValueForProbe(thisChrProbes[p], obsExp);

				}

			}
		}
		
		// Now we can go through the set of significant probes, applying a correction and then
		// filtering those which pass our p-value cutoff
		for (int d=0;d<data.length;d++) {
			
			ProbeTTestValue [] ttestResults = significantProbes.get(d).toArray(new ProbeTTestValue[0]);
			
			BenjHochFDR.calculateQValues(ttestResults);
			
			ProbeList newList = new ProbeList(collection().probeSet(), "Antisense < "+pValue+" in "+data[d].name(), "Probes showing significant antisense transcription from a basal level of "+antisenseProbability[d]+" with a cutoff of "+pValue, "FDR");
			
			for (int i=0;i<ttestResults.length;i++) {
				if (ttestResults[i].probe.name().equals("RP4-798A10.2")) {
					System.err.println("Raw p="+ttestResults[i].p+" q="+ttestResults[i].q);
				}

				
				if (ttestResults[i].q < pValue) {					
					newList.addProbe(ttestResults[i].probe, (float)ttestResults[i].q);
				}
			}
			
			
			
		}
		

		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Antisense transcription pipeline quantitation ");
		quantitationDescription.append(". Directionality was ");
		quantitationDescription.append(optionsPanel.libraryTypeBox.getSelectedItem());
		
		
		if (optionsPanel.ignoreOverlaps()) {
			quantitationDescription.append(". Ignoring existing overlaps");
		}
		
		quantitationDescription.append(". P-value cutoff was ");
		quantitationDescription.append(optionsPanel.pValue());
		

		collection().probeSet().setCurrentQuantitation(quantitationDescription.toString());

		quantitatonComplete();

	}
	
	private Feature [] getValidFeatures (Chromosome c) {
		
		Feature [] features = collection().genome().annotationCollection().getFeaturesForType(c, optionsPanel.getSelectedFeatureType());

		Arrays.sort(features);
		
		if (optionsPanel.ignoreOverlaps()) {
			Vector<Feature> validFeatures = new Vector<Feature>();
			
			FEATURE: for (int f=0;f<features.length;f++) {
				for (int g=f-1;g>=0;g--) {
					
					if (!features[g].chromosomeName().equals(features[f].chromosomeName())) {
						break;
					}
					
					if (SequenceRead.overlaps(features[f].location().packedPosition(), features[g].location().packedPosition())) {
						if (features[f].location().strand() != features[g].location().strand()) {

							continue FEATURE;
						}
					}
					else {
						
						if (features[g].location().end() < features[f].location().start()-1000000) {

							break;
						}
					}
				}
				for (int g=f+1;g<features.length;g++) {
					
					if (!features[g].chromosomeName().equals(features[f].chromosomeName())) {
						break;
					}

					
					if (SequenceRead.overlaps(features[f].location().packedPosition(), features[g].location().packedPosition())) {
						if (features[f].location().strand() != features[g].location().strand()) {
							continue FEATURE;
						}
					}
					else {
						break;
					}
				}
				
				validFeatures.add(features[f]);
			}
				
			features = validFeatures.toArray(new Feature[0]);
				
		}
		
		return features;
	}

	public String name() {
		return "Antisense transcription pipeline";
	}


	private class AntisenseOptionsPanel extends JPanel {

		JComboBox featureTypeBox;
		JComboBox libraryTypeBox;
		JCheckBox ignoreOverlapsBox;
		JTextField pValueField;

		public AntisenseOptionsPanel (String [] featureTypes) {

			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
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
				if (featureTypes[i].toLowerCase().equals("gene")) {
					featureTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(featureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Ignore features with existing antisense overlaps"),gbc);

			gbc.gridx=2;

			ignoreOverlapsBox = new JCheckBox();

			add(ignoreOverlapsBox,gbc);
			
			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Library type"),gbc);

			gbc.gridx=2;

			libraryTypeBox = new JComboBox(new String [] {"Same strand specific", "Opposing strand specific"});

			add(libraryTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("P-value"),gbc);

			gbc.gridx=2;

			pValueField = new JTextField("0.05");
			pValueField.addKeyListener(new NumberKeyListener(true, false, 1));
			add(pValueField,gbc);	


		}

		public QuantitationStrandType readFilter () {

			QuantitationStrandType [] options = QuantitationStrandType.getTypeOptions();

			String libraryType = libraryTypeBox.getSelectedItem().toString();
			String quantitationType = "";
			
			if (libraryType.equals("Same strand specific")) {
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

		public double pValue () {
			if (pValueField.getText().trim().length() > 0) {
				return Double.parseDouble(pValueField.getText());
			}
			else {
				return 0.05;
			}
		}
		
		public boolean ignoreOverlaps () {
			return ignoreOverlapsBox.isSelected();
		}

		public String getSelectedFeatureType () {
			return (String)featureTypeBox.getSelectedItem();
		}

	}

}
