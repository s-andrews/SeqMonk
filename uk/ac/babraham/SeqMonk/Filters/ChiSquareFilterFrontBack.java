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
package uk.ac.babraham.SeqMonk.Filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ChiSquareTest;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ProbeTTestValue;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Does a ChiSquare comparing the coverage at the front and back of a probe.  
 * Mostly used alongside the Transcription Termination pipeline.
 */

public class ChiSquareFilterFrontBack extends ProbeFilter {

	private final ChiSqareOptionsPanel options;
	private DataStore [] stores;
	private double stringency = 0.05;
	private boolean applyMultipleTestingCorrection = true;
	private int minObservations = 10;
	private int minPercentShift = 10;

	/**
	 * Instantiates a new box whisker filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection is not quantitated
	 */
	public ChiSquareFilterFrontBack (DataCollection collection) throws SeqMonkException {
		super(collection);
		stores = collection.getAllDataStores();

		options = new ChiSqareOptionsPanel();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters outliers based on a Chi Square of the front and back of each probe";
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
		if (stores.length > 1) {
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

		b.append("ChiSquare front back filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());

		b.append(" using data stores ");

		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
				b.append(" , ");
			}
		}
		
		b.append(" and ");
		b.append(options.strandLimitBox.getSelectedItem().toString());
		b.append(" reads ");
		
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
		
		QuantitationStrandType readFilter = (QuantitationStrandType)options.strandLimitBox.getSelectedItem();

		applyMultipleTestingCorrection = options.multiTestBox.isSelected();

		ProbeList newList;
		
		if (applyMultipleTestingCorrection) {
			newList = new ProbeList(startingList,"Filtered Probes","","Q-value");
		}
		else {
			newList = new ProbeList(startingList,"Filtered Probes","","P-value");			
		}

		Probe [] probes = startingList.getAllProbes();

		int [][] frontBackCounts = new int[stores.length][2];
		
		
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
			
			Probe frontProbe;
			Probe backProbe;
			
			if (probes[p].strand() == Location.REVERSE) {
				backProbe = new Probe(probes[p].chromosome(), probes[p].start(),probes[p].middle(),probes[p].strand());
				frontProbe = new Probe(probes[p].chromosome(), probes[p].middle(),probes[p].end(),probes[p].strand());
			}
			else {
				frontProbe = new Probe(probes[p].chromosome(), probes[p].start(),probes[p].middle(),probes[p].strand());
				backProbe = new Probe(probes[p].chromosome(), probes[p].middle(),probes[p].end(),probes[p].strand());
			}
			
//			System.err.println("Probe is "+probes[p].name()+" start="+probes[p].start()+" end="+probes[p].end());
//			System.err.println("Front Probe is start="+frontProbe.start()+" end="+frontProbe.end());
//			System.err.println("Back Probe is start="+backProbe.start()+" end="+backProbe.end());

			
			// For each dataset make up a list of forward and reverse probes under this probe
			for (int d=0;d<stores.length;d++) {
				
				long [] frontReads = stores[d].getReadsForProbe(frontProbe);
				long [] backReads = stores[d].getReadsForProbe(backProbe);
				
				frontBackCounts[d][0] = 0;
				frontBackCounts[d][1] = 0;
				
				for (int r=0;r<frontReads.length;r++) {
					if (readFilter.useRead(frontProbe, frontReads[r])) ++frontBackCounts[d][0];
				}
				for (int r=0;r<backReads.length;r++) {
					if (readFilter.useRead(backProbe, backReads[r])) ++frontBackCounts[d][1];
				}				
//				System.err.println("Datset = "+stores[d].name()+" Front counts="+frontBackCounts[d][0]+" Back counts="+frontBackCounts[d][1]);
			}
			
			
			// See if we have enough counts and difference to go on with this
			double minPercent=0;
			double maxPercent=0;
			for (int d=0;d<stores.length;d++) {
				if (frontBackCounts[d][0] < minObservations) {
//					System.err.println("Not enough counts to test");
					continue PROBE;
				}
				
				double percent = (((double)frontBackCounts[d][0])/(frontBackCounts[d][0]+frontBackCounts[d][1]))*100;
				
				if (d==0 || percent < minPercent) minPercent = percent;
				if (d==0 || percent > maxPercent) maxPercent = percent;
			}
			
			if (maxPercent-minPercent < minPercentShift) {
//				System.err.println("Not enough difference to test");
				continue PROBE;
			}
			
			
			// Now perform the Chi-Square test.
			
			double pValue = ChiSquareTest.chiSquarePvalue(frontBackCounts);
//			System.err.println("Raw p-value="+pValue);
			// Store this as a potential hit (after correcting p-values later)
			hits.add(new ProbeTTestValue(probes[p], pValue));

		}

		// Now we can correct the p-values if we need to
		
		ProbeTTestValue [] rawHits = hits.toArray(new ProbeTTestValue[0]);
		
		if (applyMultipleTestingCorrection) {
			BenjHochFDR.calculateQValues(rawHits);
		}
		
		for (int h=0;h<rawHits.length;h++) {
			if (applyMultipleTestingCorrection) {
				if (rawHits[h].q < stringency) {
					newList.addProbe(rawHits[h].probe,(float)rawHits[h].q);
				}
			}
			else {
				if (rawHits[h].p < stringency) {
					newList.addProbe(rawHits[h].probe,(float)rawHits[h].p);
				}
			}
		}

		filterFinished(newList);

	}


	/**
	 * The BoxWhiskerOptionsPanel.
	 */
	private class ChiSqareOptionsPanel extends JPanel implements ListSelectionListener {

		private JList dataList;
		private JComboBox strandLimitBox;
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

			DefaultListModel dataModel = new DefaultListModel();

			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					dataModel.addElement(stores[i]);
				}
			}

			dataList = new JList(dataModel);
			ListDefaultSelector.selectDefaultStores(dataList);
			dataList.setCellRenderer(new TypeColourRenderer());
			dataList.addListSelectionListener(this);
			

			// Fire a value change event so that the initial selected list is recorded
			valueChanged(new ListSelectionEvent(this, 0, 0, false));
			JScrollPane scrollPane = new JScrollPane(dataList);
			scrollPane.setPreferredSize(new Dimension(200,dataList.getPreferredSize().height));
			dataPanel.add(scrollPane,BorderLayout.CENTER);

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

			choicePanel.add(new JLabel("Use reads on strand"),gbc);

			gbc.gridx=2;

			strandLimitBox = new JComboBox(QuantitationStrandType.getTypeOptions());
			choicePanel.add(strandLimitBox,gbc);
			
			gbc.gridx=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Apply Multiple Testing Correction"),gbc);

			gbc.gridx=2;

			multiTestBox = new JCheckBox();
			multiTestBox.setSelected(applyMultipleTestingCorrection);
			choicePanel.add(multiTestBox,gbc);

			gbc.gridx=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Min Front Observations"),gbc);

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

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(600,300);
		}


		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			// Update the list of stores
			Object [] o = dataList.getSelectedValues();
			stores = new DataStore[o.length];
			for (int i=0;i<o.length;i++) {
				stores[i] = (DataStore)o[i];
			}

			optionsChanged();
		}
	}
}
