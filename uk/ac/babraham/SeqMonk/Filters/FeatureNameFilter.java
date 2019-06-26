/**
 * Copyright Copyright 2010-19 Simon Andrews
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
import java.util.HashSet;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The FeatureNameFilter filter matches the name of probes to the name of
 * features
 */
public class FeatureNameFilter extends ProbeFilter {

	private String annotationType = null;
	private boolean stripSuffixes;
	private boolean stripTranscriptSuffixes;

	private final FeatureFilterOptionsPanel optionsPanel;
	
	
	/**
	 * Instantiates a new feature filter with default options
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public FeatureNameFilter (DataCollection collection)  throws SeqMonkException {
		this(collection,collection.genome().annotationCollection().listAvailableFeatureTypes()[0],true);
	}
	
	/**
	 * Instantiates a new feature filter with all options set so the filter
	 * is immediately ready to run.
	 * 
	 * @param collection The dataCollection to filter
	 * @param annotationType The type of annotation to use for the filter
	 * @param allowOvelapping Whether to add probes which overlap the feature
	 * @param allowUpstream Whether to add probes which are upstream of the filter
	 * @param allowDownstream Whether to add probes which are downstream of the filter
	 * @param cutoffDistance How far from the feature counts as upstream / downstream
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public FeatureNameFilter (DataCollection collection,String annotationType, boolean stripSuffixes) throws SeqMonkException {
		super(collection);
		this.annotationType = annotationType;
		this.stripSuffixes = stripSuffixes;
		
		optionsPanel = new FeatureFilterOptionsPanel();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters probes based on a name match to a set of features";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		
		annotationType = optionsPanel.annotationTypeBox.getSelectedItem().toString();
		stripSuffixes = optionsPanel.stripSuffixesBox.isSelected();
		stripTranscriptSuffixes = optionsPanel.stripTranscriptSuffixesBox.isSelected();
		
		ProbeList passedProbes = new ProbeList(startingList,"","",startingList.getValueNames());
				
		// Since we're going to be making the annotations on the
		// basis of position we should go through all probes one
		// chromosome at a time.  We therefore make a stipulation that
		// not only do the feature names have to match, so do the
		// chromosomes.
		
		Chromosome [] chrs = collection.genome().getAllChromosomes();
		
		for (int c=0;c<chrs.length;c++) {
		
			
			// We start by building a list of the feature names we're going to
			// check against.
			
			HashSet<String>featureNames = new HashSet<String>();

			
			progressUpdated("Processing features on Chr "+chrs[c].name(),c, chrs.length);
			
			Probe [] probes = startingList.getProbesForChromosome(chrs[c]);
			Feature [] features = collection.genome().annotationCollection().getFeaturesForType(chrs[c],annotationType);

			for (int f=0;f<features.length;f++) {
				String name = features[f].name();
				if (stripSuffixes) {
					name = name.replaceFirst("_upstream$", "").replaceAll("_downstream$", "").replaceAll("_gene$", "");
				}
				if (stripTranscriptSuffixes) {
					name = name.replaceAll("-\\d\\d\\d$", "");
				}
				
				featureNames.add(name);
			}
			
			
			// We can now step through the probes looking for a match to the stored feature names
			for (int p=0;p<probes.length;p++) {
					
				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}

				String name = probes[p].name();
					
				if (stripSuffixes) {
					name = name.replaceFirst("_upstream$", "").replaceAll("_downstream$", "").replaceAll("_gene$", "");
				}
				if (stripTranscriptSuffixes) {
					name = name.replaceAll("-\\d\\d\\d$", "");
				}
					
				if (featureNames.contains(name)) {
					passedProbes.addProbe(probes[p], startingList.getValuesForProbe(probes[p]));						
				}
			}
		}
		
		filterFinished(passedProbes);
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		return optionsPanel;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#hasOptionsPanel()
	 */
	@Override
	public boolean hasOptionsPanel() {
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#isReady()
	 */
	@Override
	public boolean isReady() {
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Feature Name Filter";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		b.append("Probes from ");
		b.append(startingList.name());
		b.append(" whose name matches features of type ");

		b.append(annotationType);
		
		if (stripSuffixes) {
			b.append(" after stripping probe generator suffixes");
		}
		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		b.append("Name match to ");
		
		b.append(annotationType);

		return b.toString();
	}


	/**
	 * The FeatureFilterOptionsPanel.
	 */
	private class FeatureFilterOptionsPanel extends JPanel {

		private JComboBox annotationTypeBox;
		private JCheckBox stripSuffixesBox;
		private JCheckBox stripTranscriptSuffixesBox;
		
		/**
		 * Instantiates a new feature filter options panel.
		 */
		public FeatureFilterOptionsPanel () {

			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			
			JPanel choicePanel1 = new JPanel();

			choicePanel1.add(new JLabel("Match against "));
			annotationTypeBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
			annotationTypeBox.setPrototypeDisplayValue("No longer than this please");
			choicePanel1.add(annotationTypeBox);
			add(choicePanel1);


			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Remove probe generator suffixes "));
			stripSuffixesBox = new JCheckBox();
			stripSuffixesBox.setSelected(true);
			choicePanel2.add(stripSuffixesBox);
			add(choicePanel2);

			JPanel choicePanel3 = new JPanel();
			choicePanel3.add(new JLabel("Remove transcript suffixes "));
			stripTranscriptSuffixesBox = new JCheckBox();
			stripTranscriptSuffixesBox.setSelected(false);
			choicePanel3.add(stripTranscriptSuffixesBox);
			add(choicePanel3);

		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(600,200);
		}
		
	}
}
