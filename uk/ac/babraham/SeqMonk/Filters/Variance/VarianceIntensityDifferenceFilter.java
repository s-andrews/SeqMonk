/**
 * Copyright Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Filters.Variance;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.IndexTTestValue;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SmoothedVarianceDataset;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Displays.VariancePlot.VariancePlotPanel;
import uk.ac.babraham.SeqMonk.Filters.ProbeFilter;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Filters probes based on the the probability of their difference being
 * part of the local noise level for their average intensity.
 */
public class VarianceIntensityDifferenceFilter extends ProbeFilter {

	private Double pValueLimit = 0.05;
	private boolean applyMultipleTestingCorrection = true;

	private int probesPerSet;

	private ReplicateSet [] repSetsToUse = new ReplicateSet[0];

	private VarianceIntensityOptionsPanel optionsPanel;

	/**
	 * Instantiates a new variance intensity difference filter with default options.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public VarianceIntensityDifferenceFilter (DataCollection collection) throws SeqMonkException {
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
		
		optionsPanel = new VarianceIntensityOptionsPanel();

		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList() {

		applyMultipleTestingCorrection = optionsPanel.multipleTestingBox.isSelected();
		
		int varianceType = optionsPanel.getVarianceMeasure();
		
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

		// Put something in the progress whilst we're ordering the probe values to make
		// the comparison.
		progressUpdated("Generating background model",0,1);

		for (int r=0;r<repSetsToUse.length;r++) {
			
			SmoothedVarianceDataset var = new SmoothedVarianceDataset(repSetsToUse[r], probes, varianceType, probesPerSet);
			
			progressUpdated("Processing "+repSetsToUse[r].name(),r,repSetsToUse.length);
			
			IndexTTestValue [] currentPValues = new IndexTTestValue[probes.length];
			
			for (int p=0;p<probes.length;p++) {

				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}
				
				if (p % 1000 == 0) {
					
					int progress = (p*100)/probes.length;
					
					progress += 100*r;
					
					progressUpdated("Made "+r+" out of "+repSetsToUse.length+" comparisons",progress,repSetsToUse.length*100);
				}


				currentPValues[p] = new IndexTTestValue(p, var.getIntenstiyPValueForIndex(p,probesPerSet));

			}
			
			// We now need to correct the set of pValues
			if (applyMultipleTestingCorrection) {
				BenjHochFDR.calculateQValues(currentPValues);
			}
			
			// Finally we compare these pValues to the lowest ones we have from
			// the combined set
			for (int i=0;i<currentPValues.length;i++) {
				
				// Throw away anything which doesn't match the directionality
				// we need
				
				if (! optionsPanel.wantHighVariation()) {
					if (var.getDifferenceForIndex(currentPValues[i].index) >0) continue;
				}
				
				if (! optionsPanel.wantLowVariation()) {
					if (var.getDifferenceForIndex(currentPValues[i].index) <0) continue;
				}
				
				if (applyMultipleTestingCorrection) {
					if (currentPValues[i].q < lowestPValues[currentPValues[i].index]) {
						lowestPValues[currentPValues[i].index] = (float)currentPValues[i].q;
					}
				}
				else {
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
		if (repSetsToUse.length > 0 && pValueLimit != null) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Variance Intensity Difference Filter";
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

		for (int s=0;s<repSetsToUse.length;s++) {
			b.append(repSetsToUse[s].name());
			if (s < repSetsToUse.length-1) {
				b.append(" , ");
			}
		}

		b.append(" to ");


		b.append(" was below ");

		b.append(pValueLimit);
		
		if (applyMultipleTestingCorrection) {
			b.append(" multiple testing correction applied ");
		}
		
		b.append("with a sample size of ");
		b.append(probesPerSet);
		b.append(" when constructing the control distributions");

		if (! optionsPanel.wantHighVariation()) {
			b.append(". Selecting for decreased variation only");
		}

		if (! optionsPanel.wantLowVariation()) {
			b.append(". Selecting for increased variation only");
		}

		
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

		b.append("Variance Intensity Difference p<");
		b.append(pValueLimit);
		return b.toString();
	}



	/**
	 * The DifferencesOptionsPanel.
	 */
	private class VarianceIntensityOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ItemListener {

		private JList repSetList;
		private JComboBox varianceMeasureBox;
		private JTextField pValueField;
		private JCheckBox multipleTestingBox;
		private JTextField pointsToSampleField;
		private JComboBox directionBox;

		/**
		 * Instantiates a new differences options panel.
		 */
		public VarianceIntensityOptionsPanel () {

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

			dataPanel.add(new JLabel("Replicate Sets",JLabel.CENTER),dpgbc);

			DefaultListModel repSetModel = new DefaultListModel();

			ReplicateSet [] stores = collection.getAllReplicateSets();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated() && stores[i].dataStores().length > 1) {
					repSetModel.addElement(stores[i]);
				}
			}

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			repSetList = new JList(repSetModel);
			ListDefaultSelector.selectDefaultStores(repSetList);
			
			Vector<ReplicateSet> defaultRepSets = new Vector<ReplicateSet>();			
			DataStore [] drawnStores = SeqMonkApplication.getInstance().drawnDataStores();
			
			for (int d=0;d<drawnStores.length;d++) {
				if (drawnStores[d] instanceof ReplicateSet && drawnStores[d].isQuantitated() && ((ReplicateSet)drawnStores[d]).dataStores().length>1) {
					defaultRepSets.add((ReplicateSet)drawnStores[d]);
				}
			}
			
			repSetsToUse = defaultRepSets.toArray(new ReplicateSet[0]);
			
			repSetList.setCellRenderer(new TypeColourRenderer());
			repSetList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(repSetList),dpgbc);

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
			
			choicePanel.add(new JLabel("Variance measure "),gbc);
			varianceMeasureBox = new JComboBox(new String [] {"STDEV","SEM","QuartDisp","CoefVar","Unmeasured"});
			
			gbc.gridx = 2;
			choicePanel.add(varianceMeasureBox,gbc);

			
			gbc.gridx = 1;
			gbc.gridy++;

			
			choicePanel.add(new JLabel("Direction of change "),gbc);
			directionBox = new JComboBox(new String [] {"High or Low variation","High variation only","Low variation only"});

			gbc.gridx = 2;
			choicePanel.add(directionBox,gbc);
			
			gbc.gridx = 1;
			gbc.gridy++;

			
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
		
		public int getVarianceMeasure () {
			if (varianceMeasureBox.getSelectedItem().toString().equals("STDEV")) {
				return VariancePlotPanel.VARIANCE_STDEV;
			}
			else if (varianceMeasureBox.getSelectedItem().toString().equals("SEM")) {
				return VariancePlotPanel.VARIANCE_SEM;
			}
			else if (varianceMeasureBox.getSelectedItem().toString().equals("QuartDisp")) {
				return VariancePlotPanel.VARIANCE_QUARTILE_DISP;
			}
			else if (varianceMeasureBox.getSelectedItem().toString().equals("CoefVar")) {
				return VariancePlotPanel.VARIANCE_COEF;
			}
			else if (varianceMeasureBox.getSelectedItem().toString().equals("Unmeasured")) {
				return VariancePlotPanel.VARIANCE_NUMBER_UNMEASURED;
			}
			
			else {
				throw new IllegalArgumentException("Didn't recognise variance tag "+varianceMeasureBox.getSelectedItem().toString());
			}

		}

		public Integer probesPerSet () {
			if (pointsToSampleField.getText().trim().length() == 0) {
				return null;
			}
			
			return Integer.parseInt(pointsToSampleField.getText());
		}
		
		public boolean wantHighVariation () {
			if (directionBox.getSelectedItem().equals("High or Low variation") || directionBox.getSelectedItem().equals("High variation only")) {
				return true;
			}
			return false;
		}
		
		public boolean wantLowVariation () {
			if (directionBox.getSelectedItem().equals("High or Low variation") || directionBox.getSelectedItem().equals("Low variation only")) {
				return true;
			}
			return false;
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

			Object [] repSetSelectedObjects = repSetList.getSelectedValues();

			ReplicateSet [] newRepSets = new ReplicateSet[repSetSelectedObjects.length];
			for (int i=0;i<repSetSelectedObjects.length;i++){
				newRepSets[i] = (ReplicateSet)repSetSelectedObjects[i];
			}
			repSetsToUse = newRepSets;

			optionsChanged();
		}
	}

}
