/**
 * Copyright Copyright 2010-15 Simon Andrews
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.math3.distribution.NormalDistribution;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.IndexTTestValue;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Filters probes based on the the probability of their difference being
 * part of the local noise level for their average intensity.
 */
public class IntensityDifferenceFilter extends ProbeFilter {

	private Double pValueLimit = 0.05;
	private boolean applyMultipleTestingCorrection = true;

	private int probesPerSet;

	private DataStore [] fromStores = new DataStore[0];
	private DataStore [] toStores = new DataStore[0];

	private DifferencesOptionsPanel optionsPanel;

	/**
	 * Instantiates a new differences filter with default options.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public IntensityDifferenceFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		
		// Put out a warning if we see that we're not using all possible probes
		// for the test.
		if (!(startingList instanceof ProbeSet)) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "<html>This test requires a representative set of all probes to be valid.<br>Be careful running it on a biased subset of probes</html>", "Filtered list used", JOptionPane.WARNING_MESSAGE);
		}
		
		// We need to work out how many probes are going to be put into
		// each sub-distribution we calculate.  The rule is going to be that
		// we use 1% of the total, or 100 probes whichever is the higher

		Probe [] probes = startingList.getAllProbes();

		probesPerSet = probes.length/100;
		if (probesPerSet < 100) probesPerSet = 100;
		if (probesPerSet > probes.length) probesPerSet = probes.length;
		
		optionsPanel = new DifferencesOptionsPanel();

		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList() {

		applyMultipleTestingCorrection = optionsPanel.multipleTestingBox.isSelected();
		
		Probe [] probes = startingList.getAllProbes();

		// We'll pull the number of probes to sample from the preferences if they've changed it
		
		Integer updatedProbesPerSet = optionsPanel.probesPerSet();
		if (updatedProbesPerSet != null) probesPerSet = updatedProbesPerSet;

		ProbeList newList = new ProbeList(startingList,"Filtered Probes","","Diff p-value");

		// We'll build up a set of p-values as we go along
		float [] lowestPValues = new float[probes.length];
		for (int p=0;p<lowestPValues.length;p++) {
			lowestPValues[p] = 1;
		}

		// This is going to be the temporary array we populate with the set of
		// differences we are going to analyse.
		double [] currentDiffSet = new double[probesPerSet];

		// First work out the set of comparisons we're going to make

		Vector<SingleComparison> comparisons = new Vector<IntensityDifferenceFilter.SingleComparison>();
		for (int fromIndex=0;fromIndex<fromStores.length;fromIndex++) {
			for (int toIndex=0;toIndex<toStores.length;toIndex++) {
				if (fromStores[fromIndex]==toStores[toIndex]) continue;

				// If we can find the fromStore in the toStores we've already done and the
				// toStore anywhere in the fromStores then we can skip this.
				boolean canSkip = false;

				for (int i=0;i<fromIndex;i++) {
					if (fromStores[i] == toStores[toIndex]) {
						for (int j=0;j<toStores.length;j++) {
							if (toStores[j] == fromStores[fromIndex]) {
								canSkip = true;
								break;
							}
						}
						break;
					}
				}


				if (canSkip) continue;

				comparisons.add(new SingleComparison(fromIndex,toIndex));

			}
		}

		// Put something in the progress whilst we're ordering the probe values to make
		// the comparison.
		progressUpdated("Generating background model",0,1);

		for (int comparisonIndex=0;comparisonIndex<comparisons.size();comparisonIndex++) {
			
			int fromIndex = comparisons.elementAt(comparisonIndex).fromIndex;
			int toIndex = comparisons.elementAt(comparisonIndex).toIndex;
			
			// We need to generate a set of probe indices ordered by their average intensity
			Integer [] indices = new Integer[probes.length];
			for (int i=0;i<probes.length;i++) {
				indices[i] = i;
			}

			Comparator<Integer> comp = new AverageIntensityComparator(fromStores[fromIndex], toStores[toIndex], probes);

			Arrays.sort(indices,comp);

			progressUpdated("Made "+comparisonIndex+" out of "+comparisons.size()+" comparisons",comparisonIndex,comparisons.size());

			IndexTTestValue [] currentPValues = new IndexTTestValue[indices.length];
			
			for (int i=0;i<indices.length;i++) {

				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}
				
				if (i % 1000 == 0) {
					
					int progress = (i*100)/indices.length;
					
					progress += 100*comparisonIndex;
					
					progressUpdated("Made "+comparisonIndex+" out of "+comparisons.size()+" comparisons",progress,comparisons.size()*100);
				}

				// We need to make up the set of differences to represent this probe
				int startingIndex = i-(probesPerSet/2);
				if (startingIndex < 0) startingIndex = 0;
				if (startingIndex+(probesPerSet+1) >= probes.length) startingIndex = probes.length-(probesPerSet+1);

				try {
					for (int j=startingIndex;j<startingIndex+(probesPerSet+1);j++) {
						if (j == startingIndex) continue; // Don't include the point being tested in the background model
						else if (j < startingIndex) {
							currentDiffSet[j-startingIndex] = fromStores[fromIndex].getValueForProbe(probes[indices[j]]) - toStores[toIndex].getValueForProbe(probes[indices[j]]);
						}
						else {
							currentDiffSet[(j-startingIndex)-1] = fromStores[fromIndex].getValueForProbe(probes[indices[j]]) - toStores[toIndex].getValueForProbe(probes[indices[j]]);							
						}
					}

					// Should we fix the mean at 0?
					double mean = 0;
//					double mean = SimpleStats.mean(currentDiffSet);
					double stdev = SimpleStats.stdev(currentDiffSet,mean);

					if (stdev == 0) {
						currentPValues[indices[i]] = new IndexTTestValue(indices[i], 1);
						continue;
					}
					
					// Get the difference for this point
					double diff = fromStores[fromIndex].getValueForProbe(probes[indices[i]]) - toStores[toIndex].getValueForProbe(probes[indices[i]]);
							
					NormalDistribution nd = new NormalDistribution(mean, stdev);
					
					double significance;
					
					if (diff < mean) {
						significance = nd.cumulativeProbability(diff);
					}
					else {
						significance = 1-nd.cumulativeProbability(diff);
					}
					
					currentPValues[indices[i]] = new IndexTTestValue(indices[i], significance);

				}
				catch (SeqMonkException sme) {
					progressExceptionReceived(sme);
					return;
				}

			}
			
			// We now need to correct the set of pValues
			if (applyMultipleTestingCorrection) {
				BenjHochFDR.calculateQValues(currentPValues);
			}
			
			// Finally we compare these pValues to the lowest ones we have from
			// the combined set
			if (applyMultipleTestingCorrection) {
				for (int i=0;i<currentPValues.length;i++) {
					if (currentPValues[i].q < lowestPValues[currentPValues[i].index]) {
						lowestPValues[currentPValues[i].index] = (float)currentPValues[i].q;
					}
				}
			}
			else {
				for (int i=0;i<currentPValues.length;i++) {
					if (currentPValues[i].p < lowestPValues[currentPValues[i].index]) {
						lowestPValues[currentPValues[i].index] = (float)currentPValues[i].p;
					}
				}
			}
		}



		// Now we can go through the lowest P-value set and see if any of them
		// pass the filter.
		for (int i=0;i<lowestPValues.length;i++) {
			if (lowestPValues[i] < pValueLimit) {
				newList.addProbe(probes[i],lowestPValues[i]);
			}
		}

		filterFinished(newList);		
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the intensity corrected statistical differnce between stores";
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
		// Check for the special case of comparing just to ourselves
		if (fromStores.length==1 && toStores.length==1 && fromStores[0]==toStores[0]) {
			return false;
		}
		if (fromStores.length > 0 && toStores.length > 0 && pValueLimit != null) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Intensity Difference Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where minimum ");

		b.append("p-value when comparing ");

		for (int s=0;s<fromStores.length;s++) {
			b.append(fromStores[s].name());
			if (s < fromStores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" to ");

		for (int s=0;s<toStores.length;s++) {
			b.append(toStores[s].name());
			if (s < toStores.length-1) {
				b.append(" , ");
			}
		}


		b.append(" was below ");

		b.append(pValueLimit);
		
		if (applyMultipleTestingCorrection) {
			b.append(" multiple testing correction applied ");
		}
		
		b.append("with a sample size of ");
		b.append(probesPerSet);
		b.append(" when constructing the control distributions");

		b.append(". Quantitation was ");
		if (collection.probeSet().currentQuantitation() == null) {
			b.append("not known.");
		}
		else {
			b.append(collection.probeSet().currentQuantitation());
		}


		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		b.append("Intensity Difference p<");
		b.append(pValueLimit);
		return b.toString();
	}

	private class AverageIntensityComparator implements Comparator<Integer> {

		private DataStore d1;
		private DataStore d2;
		private Probe [] probes;

		public AverageIntensityComparator (DataStore d1, DataStore d2,Probe [] probes) {
			this.d1 = d1;
			this.d2 = d2;
			this.probes = probes;
		}

		public int compare(Integer i1, Integer i2) {
			try {
				return Double.compare(d1.getValueForProbe(probes[i2])+d2.getValueForProbe(probes[i2]),d1.getValueForProbe(probes[i1])+d2.getValueForProbe(probes[i1]));
			} 
			catch (SeqMonkException e) {
				return 0;
			}
		}
	}

	private class SingleComparison {

		public int fromIndex;
		public int toIndex;
		public SingleComparison (int fromIndex, int toIndex) {
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}
	}

	/**
	 * The DifferencesOptionsPanel.
	 */
	private class DifferencesOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ItemListener {

		private JList fromDataList;
		private JList toDataList;
		private JTextField pValueField;
		private JCheckBox multipleTestingBox;
		private JTextField pointsToSampleField;

		/**
		 * Instantiates a new differences options panel.
		 */
		public DifferencesOptionsPanel () {

			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			dataPanel.setLayout(new GridBagLayout());
			GridBagConstraints dpgbc = new GridBagConstraints();
			dpgbc.gridx=0;
			dpgbc.gridy=0;
			dpgbc.weightx=0.5;
			dpgbc.weighty=0.01;
			dpgbc.fill=GridBagConstraints.BOTH;

			dataPanel.add(new JLabel("From Data Store / Group",JLabel.CENTER),dpgbc);

			DefaultListModel fromDataModel = new DefaultListModel();
			DefaultListModel toDataModel = new DefaultListModel();

			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					fromDataModel.addElement(stores[i]);
					toDataModel.addElement(stores[i]);
				}
			}

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			fromDataList = new JList(fromDataModel);
			ListDefaultSelector.selectDefaultStores(fromDataList);
			fromStores=SeqMonkApplication.getInstance().drawnDataStores();
			fromDataList.setCellRenderer(new TypeColourRenderer());
			fromDataList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(fromDataList),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.01;

			dataPanel.add(new JLabel("To Data Store / Group",JLabel.CENTER),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			toDataList = new JList(fromDataModel);
			ListDefaultSelector.selectDefaultStores(toDataList);
			toStores=SeqMonkApplication.getInstance().drawnDataStores();
			toDataList.setCellRenderer(new TypeColourRenderer());
			toDataList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(toDataList),dpgbc);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			
			choicePanel.add(new JLabel("P-value must be below "),gbc);
			pValueField = new JTextField(""+pValueLimit,5);
			pValueField.addKeyListener(this);
			
			gbc.gridx = 2;
			choicePanel.add(pValueField,gbc);
			
			gbc.gridx = 1;
			gbc.gridy++;
			
			choicePanel.add(new JLabel("Apply Multiple Testing Correction"),gbc);
			
			gbc.gridx=2;
			
			multipleTestingBox = new JCheckBox();
			multipleTestingBox.setSelected(true);
			choicePanel.add(multipleTestingBox,gbc);
			
			gbc.gridx = 1;
			gbc.gridy++;
			
			choicePanel.add(new JLabel("Number of probes per sample"),gbc);
			
			gbc.gridx=2;
			
			pointsToSampleField = new JTextField(""+probesPerSet,5);
			pointsToSampleField.addKeyListener(new NumberKeyListener(false, false,startingList.getAllProbes().length/2));
			choicePanel.add(pointsToSampleField,gbc);
			
			add(new JScrollPane(choicePanel),BorderLayout.CENTER);

		}

		public Integer probesPerSet () {
			if (pointsToSampleField.getText().trim().length() == 0) {
				return null;
			}
			
			return Integer.parseInt(pointsToSampleField.getText());
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(600,300);
		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
		 */
		public void keyTyped(KeyEvent arg0) {
		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent ke) {

		}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
		 */
		public void keyReleased(KeyEvent ke) {
			JTextField f = (JTextField)ke.getSource();

			Double d = null;

			if (f.getText().length()>0) {

				if (f.getText().equals("-")) {
					d = 0d;
				}
				else {
					try {
						d = Double.parseDouble(f.getText());
					}
					catch (NumberFormatException e) {
						f.setText(f.getText().substring(0,f.getText().length()-1));
						return;
					}
				}
			}

			if (f == pValueField) {
				pValueLimit = d;
			}
			else {
				System.err.println("Unexpected text field "+f+" sending data to keylistener in differences filter");
			}
			optionsChanged();

		}

		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			// If we have 2 or less items selected then we can disable the
			// combobox which says whether we're looking at min, max or average
			// difference (since they're all the same with only 1 comparison)

			Object [] fromSelectedObjects = fromDataList.getSelectedValues();
			Object [] toSelectedObjects = toDataList.getSelectedValues();


			DataStore [] newFromStores = new DataStore[fromSelectedObjects.length];
			for (int i=0;i<fromSelectedObjects.length;i++){
				newFromStores[i] = (DataStore)fromSelectedObjects[i];
			}
			fromStores = newFromStores;

			DataStore [] newToStores = new DataStore[toSelectedObjects.length];
			for (int i=0;i<toSelectedObjects.length;i++){
				newToStores[i] = (DataStore)toSelectedObjects[i];
			}
			toStores = newToStores;

			optionsChanged();
		}
	}

}
