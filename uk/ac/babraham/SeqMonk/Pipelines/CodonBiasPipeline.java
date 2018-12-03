/**
 * Copyright 2011-18 Simon Andrews
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
import java.util.Vector;

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
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Put description here.
 * The codon bias pipeline aims to find whether there is a bias in the location of the starts of reads 
 * Bases 1, 2 or 3 can be selected and tested.
 * 
 * @author andrewss
 *
 */
public class CodonBiasPipeline extends Pipeline {

	private CodonBiasOptionsPanel optionsPanel;

	public CodonBiasPipeline (DataCollection collection) {
		super(collection);
		optionsPanel = new CodonBiasOptionsPanel(collection.genome().annotationCollection().listAvailableFeatureTypes());
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

	// TODO: this isn't used properly at the moment
	private boolean reverse = false;
	
	// storing probes here as there was an issue with the ordering not matching the features order.
	private Probe [] allProbes;
	
	protected void startPipeline() {

		// We first need to generate probes over all of the features listed in
		// the feature types.  The probes should cover the whole area of the
		// feature regardless of where it splices.

		Vector<Probe> probes = new Vector<Probe>();
		double pValue = optionsPanel.pValue();
		
		String libraryType = optionsPanel.libraryType();
				
		Chromosome [] chrs = collection().genome().getAllChromosomes();
		
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Making probes for chr"+chrs[c].name(), c, chrs.length*2);

			Feature [] features = collection().genome().annotationCollection().getFeaturesForType(chrs[c],optionsPanel.getSelectedFeatureType());
			
			
			for (int f=0;f<features.length;f++) {
				if (cancel) {
					progressCancelled();
					return;
				}
				Probe p = new Probe(chrs[c], features[f].location().start(), features[f].location().end(), features[f].location().strand(),features[f].name());
				probes.add(p);
				
			}
		}

		allProbes = probes.toArray(new Probe[0]);
	
		collection().setProbeSet(new ProbeSet("Features over "+optionsPanel.getSelectedFeatureType(), allProbes));
		
		// Now we can quantitate each individual feature and test for whether it is significantly 
		// showing codon bias		
		ArrayList<Vector<ProbeTTestValue>> significantProbes = new ArrayList<Vector<ProbeTTestValue>>();
		
		// data contains the data stores that this pipeline is going to use. We need to test each data store.
		for (int d=0;d<data.length;d++) {
			significantProbes.add(new Vector<ProbeTTestValue>());
		}

		int probeCounter = 0;
				
		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Quantitating features on chr"+chrs[c].name(), chrs.length+c, chrs.length*2);
						
			Feature [] features = collection().genome().annotationCollection().getFeaturesForType(chrs[c],optionsPanel.getSelectedFeatureType());
			
			for (int p=0;p<features.length;p++) {
				
				//Get the corresponding feature and work out the mapping between genomic position and codon sub position.	
				int [] mappingArray = createGenomeMappingArray(features[p]);			
																											
				DATASTORE_LOOP: for (int d=0;d<data.length;d++) {
									
					if (cancel) {
						progressCancelled();
						return;
					}

					long [] reads = data[d].getReadsForProbe(allProbes[probeCounter]);
					
					//System.err.println("Number of reads = " + reads.length);					
					
					//TODO: make this configurable
					if(reads.length < 5){
					
						data[d].setValueForProbe(allProbes[probeCounter], Float.NaN);
						continue DATASTORE_LOOP;
					}
										
					int pos1Count = 0;
					int pos2Count = 0;
					int pos3Count = 0;
					
					//System.out.println("quantitating " + data[d].name());
					//System.err.println("Number of reads = " + reads.length);	
					
					READ_LOOP: for (int r=0;r<reads.length;r++) {
			
						int genomicReadStart = SequenceRead.start(reads[r]); 
						int genomicReadEnd = SequenceRead.end(reads[r]); 
						int readStrand = SequenceRead.strand(reads[r]);
						int relativeReadStart = -1;
						
						// work out where the start of the read is relative to the feature, 
						// depending on strandedness of read, probe and library type  
						
						// forward reads
						if (readStrand == 1){
																				
							if (libraryType == "Same strand specific"){
								
								if (features[p].location().strand() == Location.FORWARD) {
																		
									// The start of the read needs to be within the feature
									if (genomicReadStart - features[p].location().start() < 0){
										continue READ_LOOP;
									}
									
									else{																			
										// look up the read start pos in the mapping array
										relativeReadStart = mappingArray[genomicReadStart - features[p].location().start()];
																																	
									}
								}	
								 
							}
							
							else if (libraryType == "Opposing strand specific"){
								
								if (features[p].location().strand() == Location.REVERSE) {
									
									// The start of the read needs to be within the feature
									// The "start" of a reverse read/probe is actually the end  
									if (features[p].location().end() - genomicReadEnd < 0 ){
										continue READ_LOOP;
									}
									
									else {																			
										relativeReadStart = mappingArray[features[p].location().end() - genomicReadEnd];											
									}									
								}																
							}								
						}
						
						// reverse reads
						if (readStrand == -1){
						
							if (libraryType == "Same strand specific"){
							
								if (features[p].location().strand() == Location.REVERSE) {																	
									
									if (features[p].location().end() - genomicReadEnd < 0 ){
										continue READ_LOOP;
									}
									
									else{	
										//System.out.println("features[p].location().end() is " + features[p].location().end() + ", genomicReadEnd is " + genomicReadEnd);
										//System.out.println("mapping array[0] is " + mappingArray[0]);
										relativeReadStart = mappingArray[features[p].location().end() - genomicReadEnd];	
									}	
								}	
							}
							
							else if (libraryType == "Opposing strand specific"){
								
								if (features[p].location().strand() == Location.FORWARD) {
									
									// The start of the read needs to be within the feature
									if (genomicReadStart - features[p].location().start() < 0){
										continue READ_LOOP;
									}
									
									else {
										// 	look up the read start position in the mapping array
										relativeReadStart = mappingArray[genomicReadStart - features[p].location().start()];
									}	
								}	
							}
																					
						}
						
						// find out which position the read is in
						if(relativeReadStart == -1){
							continue READ_LOOP;
						}						
						else if(relativeReadStart % 3 == 0){
							pos3Count++;
							continue READ_LOOP;
						}
						else if((relativeReadStart+1) % 3 == 0){
							pos2Count++;
							continue READ_LOOP;
						}
						else if((relativeReadStart+2) % 3 == 0){
							pos1Count++;
						}
						
					}// closing bracket for read loop
										
					//System.out.println("pos1Count for "+ features[p].name() + " is " + pos1Count);
					//System.out.println("pos2Count for "+ features[p].name() + " is " + pos2Count);
					//System.out.println("pos3Count for "+ features[p].name() + " is " + pos3Count);
				
					int interestingCodonCount = 0;
					int otherCodonCount = 0;
					
					if(optionsPanel.codonSubPosition() == 1){
						interestingCodonCount = pos1Count;
						otherCodonCount = pos2Count + pos3Count;
					}
					
					else if(optionsPanel.codonSubPosition() == 2){
						interestingCodonCount = pos2Count;
						otherCodonCount = pos1Count + pos3Count;
					}
					
					else if(optionsPanel.codonSubPosition() == 3){
						interestingCodonCount = pos3Count;
						otherCodonCount = pos1Count + pos2Count;
					}
					
					int totalCount = interestingCodonCount+otherCodonCount;
					
					//BinomialDistribution bd = new BinomialDistribution(interestingCodonCount+otherCodonCount, 1/3d);
					
					BinomialDistribution bd = new BinomialDistribution(totalCount, 1/3d);
															
					// Since the binomial distribution gives the probability of getting a value higher than
					// this we need to subtract one so we get the probability of this or higher.
					double thisPValue = 1-bd.cumulativeProbability(interestingCodonCount-1);							
					
					if (interestingCodonCount == 0) thisPValue = 1;
		
					// We have to add all results at this stage so we don't mess up the multiple 
					// testing correction later on.
					significantProbes.get(d).add(new ProbeTTestValue(allProbes[probeCounter], thisPValue,0));
					
					
					float percentageCount; 
					
					if (totalCount == 0){
						percentageCount = 0;
					}
					else{
						percentageCount = ((float)interestingCodonCount / (float)totalCount) *100;
					}
										
					data[d].setValueForProbe(allProbes[probeCounter], percentageCount);		
					
					//System.out.println("totalCount = " + totalCount);
					//System.out.println("interestingCodonCount " + interestingCodonCount);
					//System.out.println("pValue = " + thisPValue);
					//System.out.println("percentageCount = " + percentageCount);
					//System.out.println("");
		
				} 
				probeCounter ++;
			} 			
		} 
		
		// Now we can go through the set of significant probes, applying a correction and then
		// filtering those which pass our p-value cutoff
		for (int d=0;d<data.length;d++) {
			
			ProbeTTestValue [] ttestResults = significantProbes.get(d).toArray(new ProbeTTestValue[0]);
			
			BenjHochFDR.calculateQValues(ttestResults);
			
			ProbeList newList = new ProbeList(collection().probeSet(), "Codon bias < "+pValue+" in "+data[d].name(), "Probes showing significant codon bias for position "+optionsPanel.codonSubPosition()+" with a cutoff of "+pValue, "FDR");
			
			for (int i=0;i<ttestResults.length;i++) {
				
				//System.out.println("p value is " + ttestResults[i].p + ", q value is " + ttestResults[i].q);
				
				if (ttestResults[i].q < pValue) {					
					newList.addProbe(ttestResults[i].probe, (float)ttestResults[i].q);
				}
			}									
		}
		
		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Codon bias pipeline using codon position " + optionsPanel.codonSubPosition() + " for " + optionsPanel.libraryType() + " library.");

		collection().probeSet().setCurrentQuantitation(quantitationDescription.toString());

		quantitatonComplete();

	}
	

	public String name() {
		return "Codon Bias Pipeline";
	}
	
	
	// mostly copied from codon bias panel		
	// this should return an array of numbers for the feature where each base of an exon has a positive number
	private int [] createGenomeMappingArray(Feature feature){
				
		Location [] subLocations;
		
		if (feature.location() instanceof SplitLocation) {
			subLocations = ((SplitLocation)(feature.location())).subLocations();
		}
		else {
			subLocations = new Location [] {feature.location()};
		}
		
		//System.err.println("Working with "+feature.name());
		
		//System.err.println("There are "+subLocations.length+" sublocations");
		
		// First work out the total transcript length so we can make an appropriate data structure
		int totalLength = 0;
		for (int e=0;e<subLocations.length;e++) {
			totalLength += subLocations[e].length();
		}
		
		//System.err.println("Total exon length is "+totalLength);		

		// Now work out a mapping between relative genomic position and feature position.
		int [] genomeToFeatureMap = new int[1+feature.location().end()-feature.location().start()];
		
		for (int j=0;j<genomeToFeatureMap.length;j++) {
			genomeToFeatureMap[j] = -1;
		}
		
		//System.err.println("Genome to feature map length is "+genomeToFeatureMap.length);
		
		if (feature.location().strand() == Location.FORWARD) {
			
			// This starts at 1 so that the first position is 1, not 0
			int positionInFeature = 1;
			
			for (int i=0;i<subLocations.length;i++) {				
				//System.err.println("Looking at sublocation "+i+" from "+subLocations[i].start()+" to "+subLocations[i].end());
				
				for (int x=0;x<subLocations[i].length();x++) {	
					
					int genomePosition = subLocations[i].start()+x;
					int relativeGenomePosition = genomePosition - feature.location().start();
					
					//System.err.println("Sublocation Pos="+x+" Genome Pos="+genomePosition+" Rel Genome Pos="+relativeGenomePosition+" Feature pos="+positionInFeature);
					
					genomeToFeatureMap[relativeGenomePosition] = positionInFeature;
					positionInFeature++;
				}
				
			}
		}
		
		else if (feature.location().strand() == Location.REVERSE) {
			
			int positionInFeature = 1;
			
			//System.err.println("feature start is " + feature.location().start());
			
			
			for (int i=subLocations.length-1; i>=0; i--) {
				//System.err.println("Looking at sublocation "+i+" from "+subLocations[i].start()+" to "+subLocations[i].end());
				
				//over the length of this subLocation (exon) we're creating an array of numbers
				for (int x=0;x<subLocations[i].length();x++) {			
					
					//System.err.println("Sublocation Pos="+x+" Genome Pos="+genomePostion+" Rel Genome Pos="+relativeGenomePosition+" Feature pos="+positionInFeature);
			
					int genomePosition  = subLocations[i].end() - x; 
					
					int relativeGenomePosition  =  feature.location().end() - genomePosition;  									
					
					genomeToFeatureMap[relativeGenomePosition] = positionInFeature;
					
					positionInFeature++;
				}
			}
		}
		return genomeToFeatureMap;	
	}	
			
	
	private class CodonBiasOptionsPanel extends JPanel {

		JComboBox featureTypeBox;
		JComboBox libraryTypeBox;
		JComboBox codonPositionBox;
		JTextField pValueField;

		public CodonBiasOptionsPanel (String [] featureTypes) {

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
				if (featureTypes[i].toLowerCase().equals("cds")) {
					featureTypeBox.setSelectedIndex(i);
					break;
				}
			}

			add(featureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Codon position"),gbc);

			gbc.gridx=2;

			codonPositionBox = new JComboBox(new String [] {"1","2","3"});

			add(codonPositionBox,gbc);
			
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

		public double pValue () {
			if (pValueField.getText().trim().length() > 0) {
				return Double.parseDouble(pValueField.getText());
			}
			else {
				return 0.05;
			}
		}
		
		
		public int codonSubPosition () {
			return Integer.parseInt((String)codonPositionBox.getSelectedItem());
		}

		public String libraryType (){
			return (String)libraryTypeBox.getSelectedItem();
		}
		
		public String getSelectedFeatureType () {
			return (String)featureTypeBox.getSelectedItem();
		}
	
	}
	
}
