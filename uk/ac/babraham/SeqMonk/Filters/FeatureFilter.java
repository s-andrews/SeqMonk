/**
 * Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Filters;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.FeaturePositionSelector.FeaturePositionSelectorPanel;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class FeatureFilter extends ProbeFilter {

	private FeatureFilterOptionsPanel options;
	
	private static final int ANY_STRAND = 1;
	private static final int FORWARD_ONLY = 2;
	private static final int REVERSE_ONLY = 3;
	private static final int SAME_STRAND = 4;
	private static final int OPPOSING_STRAND = 5;
	
	
	private static final int OVERLAPPING = 101;
	private static final int CLOSE_TO = 102;
	private static final int EXACTLY_MATCHING = 103;
	private static final int SURROUNDING = 104;
	private static final int CONTAINED_WITHIN = 105;
	
	private int strand = ANY_STRAND;
	private int relationship = OVERLAPPING;
	
	
	
	public FeatureFilter(DataCollection collection) throws SeqMonkException {
		super(collection);
		options = new FeatureFilterOptionsPanel();
	}

	
	protected String listName() {
		return options.getListNameSuggestion();
	}

	protected String listDescription() {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("Filter on regions based on ");
		sb.append(options.featurePositions.selectedFeatureType());
		sb.append(" ");
		if (options.featurePositions.useSubFeatures()) {
			if (options.featurePositions.useExonSubfeatures()) {
				sb.append("exons ");
			}
			else {
				sb.append("introns ");
			}
		}
		
		sb.append(options.featurePositions.positionType());
		
		sb.append(" ");
		int startOffset = options.featurePositions.startOffset();
		int endOffset = options.featurePositions.endOffset();
		if (startOffset != 0 || endOffset != 0) {
			sb.append("from ");
			sb.append(startOffset);
			sb.append(" to ");
			sb.append(endOffset);
		}
		
		if (strand != ANY_STRAND) {
			sb.append(" with strand filter ");
			sb.append(options.strandBox.getSelectedItem());
		}
		
		sb.append(" relationship is ");
		sb.append(options.relationshipTypeBox.getSelectedItem());
		
		
		if (relationship == CLOSE_TO) {
			sb.append(" with distance cutoff ");
			sb.append(options.closenessLimit());
		}
		
		
		return sb.toString();
	}

	protected void generateProbeList() {

		// We'll start by getting the complete set of probes from the position
		// filter.  We'll split these by chromosome at a later date but we
		// have to get them as a set to start with.
		Probe [] probesToMatch = options.featurePositions.getProbes();
		
		
		// This is the set of passing probes we're going to build up.
		ProbeList passedProbes = new ProbeList(startingList,"","",null);
		
		// We need to know how far beyond the feature we might need to look
		int annotationLimit = options.closenessLimit();
		
		// Since we're going to be making the annotations on the
		// basis of position we should go through all probes one
		// chromosome at a time.
		
		Chromosome [] chrs = collection.genome().getAllChromosomes();
		
		for (int c=0;c<chrs.length;c++) {
			
			progressUpdated("Processing features on Chr "+chrs[c].name(),c, chrs.length);
			
			Probe [] probes = startingList.getProbesForChromosome(chrs[c]);
			
			Vector<Probe> featuresForThisChromosome = new Vector<Probe>();
			for (int f=0;f<probesToMatch.length;f++) {
				if (probesToMatch[f].chromosome().equals(chrs[c])) {
					featuresForThisChromosome.add(probesToMatch[f]);
				}
			}
			
			Probe [] features = featuresForThisChromosome.toArray(new Probe[0]);
			
			Arrays.sort(probes);
			Arrays.sort(features);
			
			int lastFoundIndex = 0;
			
			// We can now step through the probes looking for the best feature match
			for (int p=0;p<probes.length;p++) {
				
				for (int f=lastFoundIndex;f<features.length;f++) {
					
					if (cancel) {
						cancel = false;
						progressCancelled();
						return;
					}
					
					if (features[f].end()+annotationLimit < probes[p].start()) {
						lastFoundIndex = f;
						continue;
					}
					
					// See if we're skipping this feature for this probe based on its strand
					if (strand != ANY_STRAND) {
						switch (strand) {
						
							case FORWARD_ONLY: {
								if (features[f].strand() != Location.FORWARD) continue;
								break;
							}
							case REVERSE_ONLY: {
								if (features[f].strand() != Location.REVERSE) continue;
								break;
							}
							case SAME_STRAND: {
								if (features[f].strand() != probes[p].strand()) continue;
								break;
							}
							case OPPOSING_STRAND: {
								if (!
										(features[f].strand() == Location.FORWARD  && probes[p].strand() == Location.REVERSE) ||
										(features[f].strand() == Location.REVERSE  && probes[p].strand() == Location.FORWARD)
										)
										continue;
								break;
							}
												
						}
					}

					if (relationship == EXACTLY_MATCHING) {
						
						// We can make a simple check to see if we're matching this exactly, either
						// overall or with one of our subfeatures.
						
						if (probes[p].start() == features[f].start() && probes[p].end() == features[f].end()) {
							passedProbes.addProbe(probes[p], null);
							break;
						}						
					}
					
					else if (relationship == OVERLAPPING) {
						// Quickest check is whether a probe overlaps a feature
					
						if (probes[p].start() < features[f].end() && probes[p].end() > features[f].start()) {
							passedProbes.addProbe(probes[p],null);
							break;
						}
					}

					
					else if (relationship == CONTAINED_WITHIN) {
						// The feature has to surround the probe
						
						if (probes[p].start() >= features[f].start() && probes[p].end() <= features[f].end()) {
							passedProbes.addProbe(probes[p],null);
							break;
						}
					}


					else if (relationship == SURROUNDING) {
						// The probe has to surround the feature
						
						if (probes[p].start() <= features[f].start() && probes[p].end() >= features[f].end()) {
							passedProbes.addProbe(probes[p],null);
							break;
						}
					}


					else if (relationship == CLOSE_TO) {
						// The probe has to be close to the feature
						
						if (probes[p].start() < features[f].end()+annotationLimit && probes[p].end() > features[f].start()-annotationLimit) {
							passedProbes.addProbe(probes[p],null);
							break;
						}
					}
				}
			}
		}
		
		filterFinished(passedProbes);
		
		
	}

	public boolean isReady() {
		return true;
	}

	public boolean hasOptionsPanel() {
		return true;
	}

	public JPanel getOptionsPanel() {
		return options;
	}

	public String name() {
		return "Feature Filter";
	}

	public String description() {
		return "A filter based on the relationship between probes and features";
	}

	private class FeatureFilterOptionsPanel extends JPanel implements ItemListener {
		
		private FeaturePositionSelectorPanel featurePositions;
		private JComboBox relationshipTypeBox;
		private JTextField distanceField;
		private JComboBox strandBox;
		
		public FeatureFilterOptionsPanel () {
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			
			gbc.gridx=0;
			gbc.gridy=0;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(5, 5, 5, 5);
			
			gbc.gridwidth=2;

			JLabel header1 = new JLabel("Define Feature Positions",JLabel.CENTER);
			header1.setFont(new Font(header1.getFont().getName(), Font.BOLD, (int)(header1.getFont().getSize()*1.5)));
			
			add(header1,gbc);
			
			gbc.gridy++;

			featurePositions = new FeaturePositionSelectorPanel(collection, false, false);
			gbc.fill = GridBagConstraints.BOTH;
			add(featurePositions,gbc);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridy++;
			
			JLabel header2 = new JLabel("Define Relationship with Probes",JLabel.CENTER);
			header2.setFont(new Font(header2.getFont().getName(), Font.BOLD, (int)(header2.getFont().getSize()*1.5)));
			
			add(header2,gbc);
			
			gbc.gridy++;
			gbc.gridwidth = 1;
			
			add(new JLabel("Select probes which are"),gbc);
			
			gbc.gridx=1;
						
			relationshipTypeBox = new JComboBox(new String [] {
					"Overlapping",
					"Close to",
					"Exactly matching",
					"Surrounding",
					"Contained within"
			});
			
			relationshipTypeBox.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					if (distanceField == null) return;
					
					if (relationshipTypeBox.getSelectedItem().equals("Close to")) {
						relationship = CLOSE_TO;
					}
					else if (relationshipTypeBox.getSelectedItem().equals("Overlapping")) {
						relationship = OVERLAPPING;
					}
					else if (relationshipTypeBox.getSelectedItem().equals("Exactly matching")) {
						relationship = EXACTLY_MATCHING;
					}
					else if (relationshipTypeBox.getSelectedItem().equals("Surrounding")) {
						relationship = SURROUNDING;
					}
					else if (relationshipTypeBox.getSelectedItem().equals("Contained within")) {
						relationship = CONTAINED_WITHIN;
					}
					else {
						throw new IllegalStateException("Unknown relationship type "+relationshipTypeBox.getSelectedItem());
					}
					
					
					if (relationshipTypeBox.getSelectedItem().equals("Close to")) {
						distanceField.setEnabled(true);
					}
					else {
						distanceField.setEnabled(false);
					}
					
					
					
				}
			});
			
			add(relationshipTypeBox,gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			
			add(new JLabel("Distance cutoff (bp)"),gbc);
			
			gbc.gridx=1;
			
			distanceField = new JTextField("2000",10);
			distanceField.addKeyListener(new NumberKeyListener(false, false));
			distanceField.setEnabled(false);
			
			add(distanceField,gbc);
			

			gbc.gridy++;
			gbc.gridx = 0;
			
			add(new JLabel("Use features on strand "),gbc);
			
			gbc.gridx=1;
			
			strandBox = new JComboBox(new String [] {"Any","Forward only","Reverse only","Same as probe","Opposite to probe"});
			strandBox.addItemListener(this);
			add(strandBox,gbc);
			
		}
		
		public int closenessLimit () {
			if (distanceField.getText().trim().length() == 0) {
				return 0;
			}
			else {
				return Integer.parseInt(distanceField.getText().trim());
			}
		}
		
		public Dimension getPreferredSize () {
			return new Dimension(600,400);
		}
		
		public String getListNameSuggestion () {
			return ""+relationshipTypeBox.getSelectedItem()+" "+featurePositions.selectedFeatureType();
		}

		public void itemStateChanged(ItemEvent e) {
			String strandString = strandBox.getSelectedItem().toString();
			
			if (strandString.equals("Any")) {
				strand = ANY_STRAND;
			}
			else if (strandString.equals("Forward only")) {
				strand = FORWARD_ONLY;
			}
			else if (strandString.equals("Reverse only")) {
				strand = REVERSE_ONLY;
			}
			else if (strandString.equals("Same as probe")) {
				strand = SAME_STRAND;
			}
			else if (strandString.equals("Opposite to probe")) {
				strand = OPPOSING_STRAND;
			}
			else {
				throw new IllegalStateException("Unknown strand '"+strandString+"'");
			}
		}
		
	}
	
	
}
