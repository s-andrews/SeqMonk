/**
 * Copyright 2012-18 Simon Andrews
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * The Bisulphite feature pipeline quantitates a set of features based
 * on the ratio of forward to reverse reads.  What it actually quantitates
 * is the number of forward and reverse reads at each individual position.
 * It then filters out positions with fewer than the specified number of 
 * reads in them, and then works out a percentage methylation for each
 * position.  The feature is then given the mean methylation percentage
 * from each of the individual positions.
 * 
 * @author Simon Andrews
 *
 */
public class BisulphiteFeaturePipeline extends Pipeline {

	private BisulphiteOptionsPanel optionsPanel;

	private static final int MEAN = 1;
	private static final int MEDIAN = 2;
	private static final int MAX = 3;
	private static final int MIN = 4;


	public BisulphiteFeaturePipeline (DataCollection collection) {
		super(collection);
		String [] availableFeatures = collection.genome().annotationCollection().listAvailableFeatureTypes();
		if (collection.probeSet() != null) {
			String [] temp = new String [availableFeatures.length +1 ];
			temp[0] = "[Existing Probes]";
			for (int i=0;i<availableFeatures.length;i++) {
				temp[i+1] = availableFeatures[i];
			}

			availableFeatures = temp;
		}
		optionsPanel = new BisulphiteOptionsPanel(availableFeatures);
	}

	public boolean createsNewProbes () {
		return ! optionsPanel.featureTypeBox.getSelectedItem().equals("[Existing Probes]");
	}


	public JPanel getOptionsPanel(SeqMonkApplication application) {
		return optionsPanel;
	}

	public boolean isReady() {
		return true;
	}

	protected void startPipeline() {

		// We first need to generate probes over all of the features listed in
		// the feature types.  The probes should cover the whole area of the
		// feature regardless of where it splices.

		Vector<Probe> probes = new Vector<Probe>();
		int minCount = optionsPanel.getMinCount();
		int minFeatureCountValue = optionsPanel.getMinFeatureCount();

		String combineString = optionsPanel.getCombineType();

		int combineType = 0;

		if (combineString.equals("Mean")) {
			combineType = MEAN;
		}			
		else if (combineString.equals("Median")) {
			combineType = MEDIAN;
		}
		else if (combineString.equals("Max")) {
			combineType = MAX;
		}
		else if (combineString.equals("Min")) {
			combineType = MIN;
		}
		else {
			throw new IllegalStateException("Unknown combine type '"+combineString+"' found in methylation pipeline options");
		}

		Chromosome [] chrs = collection().genome().getAllChromosomes();

		if (optionsPanel.getSelectedFeatureType() != "[Existing Probes]") {
			for (int c=0;c<chrs.length;c++) {
				if (cancel) {
					progressCancelled();
					return;
				}

				progressUpdated("Making features for chr"+chrs[c].name(), c, chrs.length*2);

				Feature [] features = collection().genome().annotationCollection().getFeaturesForType(chrs[c], optionsPanel.getSelectedFeatureType());
				Arrays.sort(features);

				for (int f=0;f<features.length;f++) {
					if (cancel) {
						progressCancelled();
						return;
					}
					probes.add(new Probe(chrs[c], features[f].location().start(), features[f].location().end(), features[f].location().strand(),features[f].name()));
				}
			}

			Probe [] allProbes = probes.toArray(new Probe[0]);

			collection().setProbeSet(new ProbeSet("Features over "+optionsPanel.getSelectedFeatureType(), allProbes));
		}

		// Having made probes we now need to quantitate them.  We quanatitate each position
		// separately and then aggregate these into a final measure.

		for (int c=0;c<chrs.length;c++) {
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated("Quantitating features on chr"+chrs[c].name(), chrs.length+c, chrs.length*2);

			Probe [] thisChrProbes = collection().probeSet().getProbesForChromosome(chrs[c]);

			for (int p=0;p<thisChrProbes.length;p++) {				

				// This data structure is going to hold the counts.  The int array will hold
				// pairs of values for the different data stores (meth and unmeth) so the length
				// of this array will be data store length * 2.

				Hashtable<Integer, int []> counts = new Hashtable<Integer, int[]>();

				for (int d=0;d<data.length;d++) {
					if (cancel) {
						progressCancelled();
						return;
					}


					long [] reads = data[d].getReadsForProbe(thisChrProbes[p]);

					for (int r=0;r<reads.length;r++) {
						for (int pos=SequenceRead.start(reads[r]);pos<=SequenceRead.end(reads[r]);pos++) {

							if (pos < thisChrProbes[p].start()) continue;
							if (pos > thisChrProbes[p].end()) break;

							if (! counts.containsKey(pos)) {
								counts.put(pos, new int[data.length*2]);
							}
							if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
								counts.get(pos)[2*d]++;
							}
							else if (SequenceRead.strand(reads[r]) == Location.REVERSE) {
								counts.get(pos)[(2*d)+1]++;
							}
						}
					}

				}

				// If we're requiring a consistent set of measures across all stores then
				// we can filter this list now to get rid of all traces of positions without
				// enough data.

				if (optionsPanel.applyMinCountAcrossAllStores()) {
					Enumeration<Integer> en = counts.keys();

					OUTER: while (en.hasMoreElements()) {
						Integer i = en.nextElement();

						int [] values = counts.get(i);

						for (int d=0;d<data.length;d++) {
							if (values[2*d]+values[(2*d)+1] < minCount) {
								counts.remove(i);
								continue OUTER;
							}
						}
					}
				}

				// Now we go through the counts working out which ones we can keep and
				// building up a list of methylation percentages.

				for (int d=0;d<data.length;d++) {
					Enumeration<Integer> en = counts.keys();
					Vector<Float>percentages = new Vector<Float>();

					while (en.hasMoreElements()) {
						Integer pos = en.nextElement();
						int total = counts.get(pos)[2*d] + counts.get(pos)[(2*d)+1];

						if (total < minCount) continue;

						float percent = 100f * (counts.get(pos)[2*d]/(float)total);

						percentages.add(percent);

					}

					// Now combine the methylation percentages we have in the way they
					// wanted.

					float [] validMeasures = new float[percentages.size()];
					float finalMeasure = Float.NaN;
					for (int i=0;i<validMeasures.length;i++) {
						validMeasures[i] = percentages.elementAt(i).floatValue();
					}

					if (validMeasures.length < minFeatureCountValue) {
						finalMeasure = Float.NaN;
					}
					else if (validMeasures.length == 0) {
						finalMeasure = Float.NaN;
					}
					else if (validMeasures.length == 1) {
						finalMeasure = validMeasures[0];
					}
					else {

						Arrays.sort(validMeasures);

						switch (combineType) {
						case MEAN :
							finalMeasure = SimpleStats.mean(validMeasures);
							break;
						case MEDIAN:
							finalMeasure = SimpleStats.median(validMeasures);
							break;
						case MIN:
							finalMeasure = validMeasures[0];
							break;
						case MAX:
							finalMeasure = validMeasures[validMeasures.length-1];
							break;
						}

					}


					data[d].setValueForProbe(thisChrProbes[p], finalMeasure);
				}

			}

		}

		if (optionsPanel.getSelectedFeatureType() != "[Existing Probes]") {
			collection().probeSet().setDescription("Probes over "+optionsPanel.getSelectedFeatureType()+" features");
		}

		StringBuffer quantitationDescription = new StringBuffer();
		quantitationDescription.append("Methylation feature pipeline quantitation using ");
		quantitationDescription.append(optionsPanel.getSelectedFeatureType());
		quantitationDescription.append(" features with min count ");
		if (optionsPanel.applyMinCountAcrossAllStores()) {
			quantitationDescription.append(" in all stores ");
		}
		quantitationDescription.append("of ");
		quantitationDescription.append(optionsPanel.getMinCount());
		quantitationDescription.append(" per position, and at least ");
		quantitationDescription.append(optionsPanel.getMinFeatureCount());
		quantitationDescription.append(" observations per feature, then combining using the ");
		quantitationDescription.append(optionsPanel.getCombineType());

		collection().probeSet().setCurrentQuantitation(quantitationDescription.toString());

		quantitatonComplete();

	}

	public String name() {
		return "Bisulphite methylation over features";
	}


	private class BisulphiteOptionsPanel extends JPanel {

		JComboBox featureTypeBox;
		JTextField minCountField;
		JCheckBox applyAcrossAllStoresBox;
		JTextField minFeatureCountField;
		JComboBox combineTypeBox;

		public BisulphiteOptionsPanel (String [] featureTypes) {

			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			add(new JLabel("Features to quantitate "),gbc);

			gbc.gridx=2;

			featureTypeBox = new JComboBox(featureTypes);
			featureTypeBox.setPrototypeDisplayValue("No longer than this please");

			add(featureTypeBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Minimum count to include position"),gbc);

			gbc.gridx=2;

			minCountField = new JTextField("1");
			minCountField.addKeyListener(new NumberKeyListener(false, false));

			add(minCountField,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Apply min count over all stores"),gbc);

			gbc.gridx=2;

			applyAcrossAllStoresBox = new JCheckBox();

			add(applyAcrossAllStoresBox,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Minimum observations to include feature"),gbc);

			gbc.gridx=2;

			minFeatureCountField = new JTextField("1");
			minFeatureCountField.addKeyListener(new NumberKeyListener(false, false));

			add(minFeatureCountField,gbc);

			gbc.gridy++;
			gbc.gridx=1;

			add(new JLabel("Combined value to report"),gbc);

			gbc.gridx=2;

			combineTypeBox = new JComboBox (new String [] {"Mean","Median","Max","Min"});

			add(combineTypeBox,gbc);			


		}

		public String getSelectedFeatureType () {
			return featureTypeBox.getSelectedItem().toString();
		}

		public int getMinCount () {
			if (minCountField.getText().length() == 0) return 10;

			return Integer.parseInt(minCountField.getText());
		}

		public int getMinFeatureCount () {
			if (minFeatureCountField.getText().length() == 0) return 1;

			return Integer.parseInt(minFeatureCountField.getText());

		}

		public String getCombineType () {
			return combineTypeBox.getSelectedItem().toString();
		}

		public boolean applyMinCountAcrossAllStores () {
			return applyAcrossAllStoresBox.isSelected();
		}

	}

}
