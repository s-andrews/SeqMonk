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


import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * This pipeline tries to measure the degree to which transcription terminates
 * at the end of an annotated mRNA.  It quantitates the data upstream and 
 * downstream of the termination point and expresses the quantitation as 
 * the percentage loss of transcribed reads after the terminator.
 * 
 * @author andrewss
 *
 */
public class TranscriptionTerminationPipeline extends Pipeline {

	private TranscriptionTerminationOptionsPanel optionsPanel;

	public TranscriptionTerminationPipeline (DataCollection collection) {
		super(collection);
		optionsPanel = new TranscriptionTerminationOptionsPanel(collection.genome().annotationCollection().listAvailableFeatureTypes());
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
		int minCount = optionsPanel.minCount();
		int measurementLength = optionsPanel.measurementLength();
		QuantitationStrandType readFilter = optionsPanel.readFilter();
		
		
		Chromosome [] chrs = collection().genome().getAllChromosomes();

		// First find the overall rate of antisense reads
		
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Creating Probes"+chrs[c].name(), c, chrs.length*2);

			Feature [] features = getValidFeatures(chrs[c],measurementLength);
			
			for (int f=0;f<features.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}
				
				if (features[f].location().strand() == Location.REVERSE) {
					Probe p = new Probe(chrs[c], features[f].location().start()-measurementLength, features[f].location().start()+measurementLength, features[f].location().strand(),features[f].name());
					probes.add(p);										
				}
				else {
					Probe p = new Probe(chrs[c], features[f].location().end()-measurementLength, features[f].location().end()+measurementLength, features[f].location().strand(),features[f].name());
					probes.add(p);					
				}
				
			}
		}

		Probe [] allProbes = probes.toArray(new Probe[0]);

		collection().setProbeSet(new ProbeSet("Features "+measurementLength+"bp around the end of "+optionsPanel.getSelectedFeatureType(), allProbes));

		int probeIndex = 0;
		
		
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Quantitating features on chr"+chrs[c].name(), chrs.length+c, chrs.length*2);
			

			Feature [] features = getValidFeatures(chrs[c],measurementLength);
			
			for (int f=0;f<features.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}
				
				
				for (int d=0;d<data.length;d++) {
					
					if (allProbes[probeIndex].strand() == Location.REVERSE) {
						
						Probe downstreamProbe = new Probe(chrs[c], features[f].location().start()-measurementLength, features[f].location().start(), features[f].location().strand(),features[f].name());
						Probe upstreamProbe = new Probe(chrs[c], features[f].location().start(), features[f].location().start()+measurementLength, features[f].location().strand(),features[f].name());

						long [] upstreamReads = data[d].getReadsForProbe(upstreamProbe);
						long [] downstreamReads = data[d].getReadsForProbe(downstreamProbe);
						
						int upstreamCount = 0;
						for (int i=0;i<upstreamReads.length;i++) {
							if (readFilter.useRead(allProbes[probeIndex], upstreamReads[i])) ++upstreamCount;
						}
						int downstreamCount = 0;
						for (int i=0;i<downstreamReads.length;i++) {
							if (readFilter.useRead(allProbes[probeIndex], downstreamReads[i])) ++downstreamCount;
						}
						
						float percentage = ((upstreamCount - downstreamCount)/(float)upstreamCount)*100f;
						
//						System.err.println("Feature "+features[f].name()+" start="+features[f].location().start()+" end="+features[f].location().end());
//						System.err.println("Upstream pos start="+upstreamProbe.start()+" end="+upstreamProbe.end());
//						System.err.println("Downstream pos start="+downstreamProbe.start()+" end="+downstreamProbe.end());
//						System.err.println("Reverse "+chrs[c].name()+" name="+features[f].name()+" upcount="+upstreamCount+" downcount="+downstreamCount+" percentage="+percentage);
//						System.err.println("");
						
						if (upstreamCount >= minCount) {
							data[d].setValueForProbe(allProbes[probeIndex], percentage);
						}
						else {
							data[d].setValueForProbe(allProbes[probeIndex], Float.NaN);
						}
					}
					else {
						Probe upstreamProbe = new Probe(chrs[c], features[f].location().end()-measurementLength, features[f].location().end(), features[f].location().strand(),features[f].name());
						Probe downstreamProbe = new Probe(chrs[c], features[f].location().end(), features[f].location().end()+measurementLength, features[f].location().strand(),features[f].name());

						long [] upstreamReads = data[d].getReadsForProbe(upstreamProbe);
						long [] downstreamReads = data[d].getReadsForProbe(downstreamProbe);
						
						int upstreamCount = 0;
						for (int i=0;i<upstreamReads.length;i++) {
							if (readFilter.useRead(allProbes[probeIndex], upstreamReads[i])) ++upstreamCount;
						}
						int downstreamCount = 0;
						for (int i=0;i<downstreamReads.length;i++) {
							if (readFilter.useRead(allProbes[probeIndex], downstreamReads[i])) ++downstreamCount;
						}

						
						float percentage = ((upstreamCount-downstreamCount)/(float)upstreamCount)*100f;
						
//						System.err.println("Forward c="+chrs[c].name()+" name="+features[f].name()+" upcount="+upstreamCount+" downcount="+downstreamCount+" percentage="+percentage);
						
						if (upstreamCount >= minCount) {
							data[d].setValueForProbe(allProbes[probeIndex], percentage);
						}
						else {
							data[d].setValueForProbe(allProbes[probeIndex], Float.NaN);
						}
					}
				}					
				
				++probeIndex;
			}
		}
		
		

		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Transcription termination pipeline quantitation ");
		quantitationDescription.append(". Directionality was ");
		quantitationDescription.append(optionsPanel.libraryTypeBox.getSelectedItem());
		
	
		quantitationDescription.append(". Measurement length was ");
		quantitationDescription.append(optionsPanel.measurementLength());
		
		quantitationDescription.append(". Min count was ");
		quantitationDescription.append(optionsPanel.minCount());

		collection().probeSet().setCurrentQuantitation(quantitationDescription.toString());

		quantitatonComplete();

	}
	
	private Feature [] getValidFeatures (Chromosome c, int measurementLength) {
		
		Feature [] features = collection().genome().annotationCollection().getFeaturesForType(c, optionsPanel.getSelectedFeatureType());
		
		Arrays.sort(features);
		
		// Features are only valid if they're not too close to the end of a chromosome
		// (closer than the measurement length)
		
		Vector<Feature> validFeatures = new Vector<Feature>();
		
		for (int f=0;f<features.length;f++) {
			if (features[f].location().strand() == Location.REVERSE) {
				if(features[f].location().start()-measurementLength < 1 || features[f].location().start()+measurementLength > SeqMonkApplication.getInstance().dataCollection().genome().getChromosome(features[f].chromosomeName()).chromosome().length()) {
					continue;
				}
			}
			else {
				if(features[f].location().end()-measurementLength < 1 || features[f].location().end()+measurementLength > SeqMonkApplication.getInstance().dataCollection().genome().getChromosome(features[f].chromosomeName()).chromosome().length()) {
					continue;	
				}
			}
			
			validFeatures.add(features[f]);
		}
		
		
		return validFeatures.toArray(new Feature[0]);
	}

	public String name() {
		return "Transcription termination pipeline";
	}


	private class TranscriptionTerminationOptionsPanel extends JPanel {

		JComboBox featureTypeBox;
		JComboBox libraryTypeBox;
		JTextField minCountField;
		JTextField measurementLengthField;

		public TranscriptionTerminationOptionsPanel (String [] featureTypes) {

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

			libraryTypeBox = new JComboBox(new String [] {"Non strand specific","Same strand specific", "Opposing strand specific"});

			add(libraryTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Min count in feature"),gbc);

			gbc.gridx=2;

			minCountField = new JTextField("100");
			minCountField.addKeyListener(new NumberKeyListener(false, false));
			add(minCountField,gbc);	
			
			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Measurement length"),gbc);

			gbc.gridx=2;

			measurementLengthField = new JTextField("500");
			measurementLengthField.addKeyListener(new NumberKeyListener(false, false));
			add(measurementLengthField,gbc);	

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
			else if (libraryType.equals("Non strand specific")) {
				quantitationType = "All Reads";
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

		public int minCount () {
			if (minCountField.getText().trim().length() > 0) {
				int minCount = Integer.parseInt(minCountField.getText());
				if (minCount <= 0) minCount = 1;
				return minCount;
			}
			else {
				return 1;
			}
		}
		
		public int measurementLength () {
			if (measurementLengthField.getText().trim().length() > 0) {
				int measurementLength = Integer.parseInt(measurementLengthField.getText());
				if (measurementLength <= 0) measurementLength = 500;
				return measurementLength;
			}
			else {
				return 500;
			}
		}
		

		public String getSelectedFeatureType () {
			return (String)featureTypeBox.getSelectedItem();
		}

	}

}
