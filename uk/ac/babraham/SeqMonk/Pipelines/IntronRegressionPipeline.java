/**
 * Copyright 2011- 21 Simon Andrews
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
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
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
 * The antisense transcription pipeline aims to find sets of features where
 * significant amounts of antisense transcription is occurring.  It works out
 * a global level of antisense observation and then uses this to statistically
 * test the observed level of antisense in each one of a set of features.
 * 
 * @author andrewss
 *
 */
public class IntronRegressionPipeline extends Pipeline {

	private IntronRegressionOptionsPanel optionsPanel;

	public IntronRegressionPipeline (DataCollection collection) {
		super(collection);
		optionsPanel = new IntronRegressionOptionsPanel(collection.genome().annotationCollection().listAvailableFeatureTypes());
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
		int minDensity = optionsPanel.minDensity();
		int minLength = optionsPanel.minLength();
		double maxPValue = optionsPanel.maxPValue();
		int binSize=optionsPanel.measurementBinSize();
		
		
		QuantitationStrandType readFilter = optionsPanel.readFilter();

		Chromosome [] chrs = collection().genome().getAllChromosomes();

		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			Vector<Probe> probesForThisChromosome = new Vector<Probe>();

			progressUpdated("Making probes", c, chrs.length);

			Feature [] features = getValidFeatures(chrs[c]);

			for (int f=0;f<features.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}

				// Now we can iterate through the introns in this feature
				if (! (features[f].location() instanceof SplitLocation)) continue; // There are no introns here

				Location [] subLocations = ((SplitLocation)features[f].location()).subLocations();

				// TODO: Reverse the subLocations if its a reverse feature				
				for (int intron=1;intron<subLocations.length;intron++) {

					int start = subLocations[intron-1].end();
					int end = subLocations[intron].start();

					if ((end-start)+1 < minLength) {
						continue; // This intron is too short.
					}

					// TODO: We could throw away any probes which didn't have enough reads in any feature

					Probe p = new Probe(chrs[c], start, end, features[f].location().strand(),features[f].name()+"_"+intron);
					probesForThisChromosome.add(p);

				}
			}

			// Now we can deduplicate the probes for this chromosome and add them to the main collection
			Probe [] dupProbes = probesForThisChromosome.toArray(new Probe[0]);
			Arrays.sort(dupProbes);

			for (int p=0;p<dupProbes.length;p++) {
				if (p>0 && dupProbes[p].packedPosition() == dupProbes[p-1].packedPosition()) continue;
				probes.add(dupProbes[p]);
			}

		}

		Probe [] allProbes = probes.toArray(new Probe[0]);

		collection().setProbeSet(new ProbeSet("Features over "+optionsPanel.getSelectedFeatureType(), allProbes));

		// Now we go back through the probes and quantitate them
		for (int p=0;p<allProbes.length;p++){

			if (cancel) {
				progressCancelled();
				return;
			}

			if (p%1000 == 0) {
				progressUpdated("Quantitated "+p+" out of "+allProbes.length+" probes",p,allProbes.length);
			}

			for (int d=0;d<data.length;d++) {
				long [] reads = data[d].getReadsForProbe(allProbes[p]);

				int [] countsPerSite = new int [allProbes[p].length()];

				int usableCounts = 0;

				for (int r=0;r<reads.length;r++) {
					if (readFilter.useRead(allProbes[p], reads[r])) {
						++usableCounts;
						for (int pos=Math.max(0, SequenceRead.start(reads[r])-allProbes[p].start()); pos<= Math.min(countsPerSite.length-1, SequenceRead.end(reads[r])-allProbes[p].start());pos++) {
							++countsPerSite[pos];
						}
					}
				}

				if (usableCounts/(allProbes[p].length()/1000d) >= minDensity) {
					// We're going to do a linear regression rather than a correlation

					// We're analysing in bins so we'll work out the bin counts and
					// add them dynamically to the regression.
					
					SimpleRegression regression = new SimpleRegression();
					
					int binCount = 0;
					for (int i=0;i<countsPerSite.length;i++) {
						if (i>0 && i % binSize == 0) {
							regression.addData(i,binCount);
							binCount = 0;
						}
						
						binCount += countsPerSite[i];
					}					
										
					float slope = (float)(regression.getSlope()*1000000);
					double pValue = regression.getSignificance();

					if (allProbes[p].strand() == Location.REVERSE) {
						slope = 0-slope;
					}
					
					if (pValue <= maxPValue) {
						data[d].setValueForProbe(allProbes[p], slope);
					}
					else {
						data[d].setValueForProbe(allProbes[p], Float.NaN);						
					}
				}
				else {
					data[d].setValueForProbe(allProbes[p], Float.NaN);
				}

			}
		}

		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Intron regression pipeline quantitation ");
		quantitationDescription.append(". Directionality was ");
		quantitationDescription.append(optionsPanel.libraryTypeBox.getSelectedItem());
		quantitationDescription.append(". Min intron length was ");
		quantitationDescription.append(minLength);
		quantitationDescription.append(". Min read density was ");		
		quantitationDescription.append(minDensity);
		quantitationDescription.append(". Max slope p-value was ");		
		quantitationDescription.append(maxPValue);

		collection().probeSet().setCurrentQuantitation(quantitationDescription.toString());

		quantitatonComplete();

	}

	private Feature [] getValidFeatures (Chromosome c) {

		Feature [] features = collection().genome().annotationCollection().getFeaturesForType(c, optionsPanel.getSelectedFeatureType());

		Arrays.sort(features);

		return features;
	}

	public String name() {
		return "Intron regression pipeline";
	}


	private class IntronRegressionOptionsPanel extends JPanel {

		JComboBox featureTypeBox;
		JComboBox libraryTypeBox;
		JTextField minCountField;
		JTextField maxPValueField;
		JTextField minLengthField;
		JTextField measurementBinField;

		public IntronRegressionOptionsPanel (String [] featureTypes) {

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
				if (featureTypes[i].toLowerCase().equals("mrna")) {
					featureTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(featureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Library type"),gbc);

			gbc.gridx=2;

			libraryTypeBox = new JComboBox(new String [] {"Same strand specific", "Opposing strand specific"});

			add(libraryTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Min read density (counts/kb)"),gbc);

			gbc.gridx=2;

			minCountField = new JTextField("20");
			minCountField.addKeyListener(new NumberKeyListener(false, false));
			add(minCountField,gbc);	

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Max slope p-value"),gbc);

			gbc.gridx=2;

			maxPValueField = new JTextField("0.05");
			maxPValueField.addKeyListener(new NumberKeyListener(true, false, 1));
			add(maxPValueField,gbc);	

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Min intron length (bp)"),gbc);

			gbc.gridx=2;

			minLengthField = new JTextField("1000");
			minLengthField.addKeyListener(new NumberKeyListener(false, false));
			add(minLengthField,gbc);	

			
			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Measurement bin size (bp)"),gbc);

			gbc.gridx=2;

			measurementBinField = new JTextField("100");
			measurementBinField.addKeyListener(new NumberKeyListener(false, false));
			add(measurementBinField,gbc);	

			
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

		public int minDensity () {
			if (minCountField.getText().trim().length() > 0) {
				int minCount = Integer.parseInt(minCountField.getText());
				if (minCount < 3) return 3;
				return minCount;
			}
			else {
				return 3;
			}
		}


		public int minLength () {
			if (minLengthField.getText().trim().length() > 0) {
				return(Integer.parseInt(minLengthField.getText()));
			}
			else {
				return 0;
			}
		}

		public double maxPValue () {
			if (maxPValueField.getText().trim().length() > 0) {
				return(Double.parseDouble(maxPValueField.getText()));
			}
			else {
				return 1;
			}
		}
		
		public int measurementBinSize () {
			if (measurementBinField.getText().trim().length() > 0) {
				return(Integer.parseInt(measurementBinField.getText()));
			}
			else {
				return 200;
			}
		}


		public String getSelectedFeatureType () {
			return (String)featureTypeBox.getSelectedItem();
		}

	}

}
