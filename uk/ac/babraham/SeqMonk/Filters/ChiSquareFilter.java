/**
 * Copyright Copyright 2010-18 Simon Andrews
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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ChiSquareTest;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ProbeTTestValue;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Filters probes which are outliers from a BoxWhisker plot
 * This version of the filter works with multiple datasets 
 * and counts the splits between them.
 */
public class ChiSquareFilter extends ProbeFilter {

	private final ChiSqareOptionsPanel options;
	private DataStore [] stores;
	private DataStorePair [] pairs = new DataStorePair[0];
	private double stringency = 0.05;
	private boolean applyMultipleTestingCorrection = true;
	private int minObservations = 10;
	private int minPercentShift = 10;

	/**
	 * Instantiates a new box whisker filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection is not quantiated
	 */
	public ChiSquareFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		stores = collection.getAllDataStores();

		options = new ChiSqareOptionsPanel();
	}

	/**
	 * Instantiates a new box whisker filter with all options set allowing
	 * it to be run immediately.
	 * 
	 * @param collection The dataCollection to filter
	 * @param stores The list of stores to use to create BoxWhisker plots
	 * @param outlierType Use constants ABOVE_ONLY, BELOW_ONLY or EITHER_ABOVE_OR_BELOW
	 * @param filterType Use constants from ProbeFilter EXACTLY, AT_LEAST, NO_MORE_THAN
	 * @param stringency The BoxWhisker stringency (default is 2)
	 * @param storeCutoff How many stores need to pass the filter
	 * @throws SeqMonkException If the collection is not quantitated.
	 */
	public ChiSquareFilter (DataCollection collection, DataStore [] stores, double stringency, boolean applyMutipleTestingCorrection, int minObservations, int minPercentShift) throws SeqMonkException {
		super(collection);

		if (stores == null) {
			throw new IllegalArgumentException("List of stores cannot be null");
		}

		this.stores = stores;
		this.stringency = stringency;
		this.applyMultipleTestingCorrection = applyMutipleTestingCorrection;
		this.minObservations = minObservations;
		this.minPercentShift = minPercentShift;

		options = new ChiSqareOptionsPanel();

	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters outliers based on a BoxWhisker Plot";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		return options;
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
		if (options.getSelectedPairs().length > 1) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Chi Square Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("ChiSquare filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());

		b.append(" using data stores ");

		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" with significance < ");
		b.append(stringency);
		if (applyMultipleTestingCorrection) {
			b.append(" after multiple testing correction");
		}
		
		b.append(" Min observations was ");
		b.append(minObservations);
		
		b.append(" Min percentage difference was ");
		b.append(minPercentShift);


		return b.toString();

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();
		b.append("ChiSquare p<");
		b.append(stringency);

		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList () {


		if (options.stringencyField.getText().length() == 0) {
			stringency = 0.05;
		}
		else {
			stringency = Double.parseDouble(options.stringencyField.getText());
		}
		if (options.minObservationsField.getText().length() == 0) {
			minObservations = 10;
		}
		else {
			minObservations = Integer.parseInt(options.minObservationsField.getText());
		}
		if (options.minDifferenceField.getText().length() == 0) {
			minPercentShift = 10;
		}
		else {
			minPercentShift = Integer.parseInt(options.minDifferenceField.getText());
		}

		applyMultipleTestingCorrection = options.multiTestBox.isSelected();
		
		pairs = options.getSelectedPairs();

		ProbeList newList = new ProbeList(startingList,"Filtered Probes","",new String[] {"P-value","FDR","Difference"});

		Probe [] probes = startingList.getAllProbes();

		int [][] pairCounts = new int[pairs.length][2];
		
		
		// This is where we'll store any hits
		Vector<ProbeTTestValue> hits = new Vector<ProbeTTestValue>();
		
		PROBE: for (int p=0;p<probes.length;p++) {

			if (p % 100 == 0) {
				progressUpdated("Processed "+p+" probes", p, probes.length);
			}

			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}
			
			
			// For each dataset make up a list of forward and reverse probes under this probe
			for (int d=0;d<pairs.length;d++) {				
				pairCounts[d][0] = pairs[d].store1.getReadsForProbe(probes[p]).length;
				pairCounts[d][1] = pairs[d].store2.getReadsForProbe(probes[p]).length;
			}
			
			// See if we have enough counts and difference to go on with this
			double minPercent=0;
			double maxPercent=0;
			for (int d=0;d<pairs.length;d++) {
				if (pairCounts[d][0]+pairCounts[d][1] < minObservations) {
					continue PROBE;
				}
				
				double percent = (((double)pairCounts[d][0])/(pairCounts[d][0]+pairCounts[d][1]))*100;
				
				if (d==0 || percent < minPercent) minPercent = percent;
				if (d==0 || percent > maxPercent) maxPercent = percent;
			}
			
			if (maxPercent-minPercent < minPercentShift) continue PROBE;
			
			
			// Now perform the Chi-Square test.
			
			double pValue = ChiSquareTest.chiSquarePvalue(pairCounts);
			double diff = getDiff(pairCounts);
			// Store this as a potential hit (after correcting p-values later)
			hits.add(new ProbeTTestValue(probes[p], pValue,diff));

		}

		// Now we can correct the p-values if we need to
		
		ProbeTTestValue [] rawHits = hits.toArray(new ProbeTTestValue[0]);
		
		if (applyMultipleTestingCorrection) {
			BenjHochFDR.calculateQValues(rawHits);
		}
		
		for (int h=0;h<rawHits.length;h++) {
			if (applyMultipleTestingCorrection) {
				if (rawHits[h].q < stringency) {
					newList.addProbe(rawHits[h].probe,new float[] {(float)rawHits[h].p,(float)rawHits[h].q,(float)rawHits[h].diff});
				}
			}
			else {
				if (rawHits[h].p < stringency) {
					newList.addProbe(rawHits[h].probe,new float[] {(float)rawHits[h].p,(float)rawHits[h].q,(float)rawHits[h].diff});
				}
			}
		}

		filterFinished(newList);

	}
	
	private double getDiff (int [][] counts) {
		double minRatio = 0.5;
		double maxRatio = 0.5;
		boolean ratioSet = false;
		
		for (int i=0;i<counts.length;i++) {
			int sum = counts[i][0]+counts[i][1];
			if (sum==0) continue;
			double ratio = counts[i][0]  / (double)sum;
			
			if (!ratioSet) {
				minRatio = ratio;
				maxRatio = ratio;
				ratioSet = true;
			}
			else {
				if (ratio < minRatio) minRatio = ratio;
				if (ratio > maxRatio) maxRatio = ratio;
			}
		}
		
		return (maxRatio - minRatio)*100; // Actually send them back a percentage diff
	}

	private class GroupMakerPanel extends JPanel implements ActionListener {
		
		private DefaultListModel availableModel = new DefaultListModel();
		private DefaultListModel chosenModel = new DefaultListModel();
		private JList availableList1 = new JList(availableModel);
		private JList availableList2 = new JList(availableModel);
		private JList chosenList = new JList(chosenModel);
		private JButton addButton;
		private JButton removeButton;
		
		public GroupMakerPanel (DataStore [] stores) {

			for (int i=0;i<stores.length;i++) {
				availableModel.addElement(stores[i]);
			}

			addButton = new JButton("Add");
			addButton.setEnabled(false);
			addButton.setActionCommand("add");
			addButton.addActionListener(this);

			removeButton = new JButton("Remove");
			removeButton.setEnabled(false);
			removeButton.setActionCommand("remove");
			removeButton.addActionListener(this);

			availableList1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			availableList1.addListSelectionListener(new ListSelectionListener() {
				
				public void valueChanged(ListSelectionEvent e) {
					if (availableList1.getSelectedIndex()>=0  && availableList2.getSelectedIndex() >=0 && availableList1.getSelectedIndex() != availableList2.getSelectedIndex() ) {
						addButton.setEnabled(true);
					}
					else {
						addButton.setEnabled(false);
					}
					optionsChanged();
				}
			});

			availableList2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			availableList2.addListSelectionListener(new ListSelectionListener() {
				
				public void valueChanged(ListSelectionEvent e) {
					if (availableList1.getSelectedIndex()>=0  && availableList2.getSelectedIndex() >=0 && availableList1.getSelectedIndex() != availableList2.getSelectedIndex() ) {
						addButton.setEnabled(true);
					}
					else {
						addButton.setEnabled(false);
					}
					optionsChanged();
				}
			});

			chosenList.addListSelectionListener(new ListSelectionListener() {
				
				public void valueChanged(ListSelectionEvent e) {
					if (chosenList.getSelectedIndices().length > 0) {
						removeButton.setEnabled(true);
					}
					else {
						removeButton.setEnabled(false);
					}
					optionsChanged();
				}
			});

			
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx=0.5;
			gbc.weighty=0.1;
			gbc.fill = GridBagConstraints.BOTH;
			
			add(new JLabel("Store 1",JLabel.CENTER),gbc);
			gbc.gridx=3;
			add(new JLabel("Pairs",JLabel.CENTER),gbc);
			
			gbc.gridx=1;
			gbc.gridy=2;
			gbc.weighty=0.9;

			add(new JScrollPane(availableList1),gbc);
			gbc.gridy=3;
			gbc.weighty=0.1;
			
			add(new JLabel("Store 1",JLabel.CENTER),gbc);
			
			gbc.gridy=4;
			gbc.weighty=0.9;

			add(new JScrollPane(availableList2),gbc);

			gbc.gridx=2;
			gbc.gridy=2;
			gbc.gridheight=3;
			gbc.weightx=0.1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new GridLayout(2, 1));
			buttonPanel.add(addButton);
			buttonPanel.add(removeButton);
			
			add(buttonPanel,gbc);
			
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx=0.5;
			gbc.gridx=3;
			add(new JScrollPane(chosenList),gbc);
			
		}
		
		public Dimension getPreferredSize () {
			return new Dimension(400,400);
		}

		public void actionPerformed(ActionEvent ae) {

			if (ae.getActionCommand().equals("add")) {
				DataStorePair pair = new DataStorePair((DataStore)availableList1.getSelectedValue(), (DataStore)availableList2.getSelectedValue());
				availableModel.removeElement(pair.store1);
				availableModel.removeElement(pair.store2);
				chosenModel.addElement(pair);
				optionsChanged();
			}
			else if (ae.getActionCommand().equals("remove")) {
				Object [] removables = chosenList.getSelectedValues();
				
				for (int i=0;i<removables.length;i++) {
					DataStorePair onePair = (DataStorePair)removables[i];
					availableModel.addElement(onePair.store1);
					availableModel.addElement(onePair.store2);
					chosenModel.removeElement(onePair);
				}
				optionsChanged();
				
			}
		}
		
		public DataStorePair [] selectedPairs () {
			Object [] objs = chosenModel.toArray();
			DataStorePair [] stores = new DataStorePair[objs.length];
			for (int i=0;i<stores.length;i++) {
				stores[i] = (DataStorePair)objs[i];
			}
			
			return stores;
		}
	}
	
	private class DataStorePair {
		
		public DataStore store1;
		public DataStore store2;
		
		public DataStorePair (DataStore store1,DataStore store2) {
			this.store1 = store1;
			this.store2 = store2;
		}
		
		public String toString () {
			return store1.name()+" & "+store2.name();
		}
	}

	/**
	 * The BoxWhiskerOptionsPanel.
	 */
	private class ChiSqareOptionsPanel extends JPanel {

		private GroupMakerPanel makerPanel;
		private JTextField stringencyField;
		private JCheckBox multiTestBox;
		private JTextField minObservationsField;
		private JTextField minDifferenceField;
		

		/**
		 * Instantiates a new box whisker options panel.
		 */
		public ChiSqareOptionsPanel () {

			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			dataPanel.setLayout(new BorderLayout());
			dataPanel.add(new JLabel("Data Sets/Groups",JLabel.CENTER),BorderLayout.NORTH);

			makerPanel = new GroupMakerPanel(collection.getAllDataStores());
			dataPanel.add(makerPanel,BorderLayout.CENTER);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx=0.5;
			gbc.weighty=0.5;
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.NONE;

			choicePanel.add(new JLabel("P-value cutoff"),gbc);

			gbc.gridx=2;

			stringencyField = new JTextField(""+stringency,5);
			stringencyField.addKeyListener(new NumberKeyListener(true, false, 1));
			choicePanel.add(stringencyField,gbc);

			gbc.gridx=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Apply Multiple Testing Correction"),gbc);

			gbc.gridx=2;

			multiTestBox = new JCheckBox();
			multiTestBox.setSelected(applyMultipleTestingCorrection);
			choicePanel.add(multiTestBox,gbc);

			gbc.gridx=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Min Observations"),gbc);

			gbc.gridx=2;

			minObservationsField = new JTextField(""+minObservations,5);
			minObservationsField.addKeyListener(new NumberKeyListener(false, false));
			choicePanel.add(minObservationsField,gbc);

			gbc.gridx=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Min Percentage Diff"),gbc);

			gbc.gridx=2;

			minDifferenceField = new JTextField(""+minPercentShift,5);
			minDifferenceField.addKeyListener(new NumberKeyListener(false, false,100));
			choicePanel.add(minDifferenceField,gbc);
			add(new JScrollPane(choicePanel),BorderLayout.CENTER);

		}
		
		public DataStorePair [] getSelectedPairs () {
			return makerPanel.selectedPairs();
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(800,400);
		}

	}
}
