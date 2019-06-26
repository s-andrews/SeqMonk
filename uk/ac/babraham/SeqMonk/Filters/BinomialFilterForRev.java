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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.apache.commons.math3.stat.inference.BinomialTest;
import org.apache.commons.math3.stat.interval.ConfidenceInterval;
import org.apache.commons.math3.stat.interval.WilsonScoreInterval;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ProbeTTestValue;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Filters probes based on a binomial test on the ratio of for/rev compared to
 * the behaviour of other similar probes.
 */
public class BinomialFilterForRev extends ProbeFilter {

	private final BinomialOptionsPanel options;
	private DataStore fromStore;
	private DataStore toStore;
	private double stringency = 0.05;
	private boolean applyMultipleTestingCorrection = true;
	private int minObservations = 10;
	private int minPercentShift = 0;
	private boolean useCurrentQuant;

	/**
	 * Instantiates a new box whisker filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection is not quantitated
	 */
	public BinomialFilterForRev (DataCollection collection) throws SeqMonkException {
		super(collection);
	
		options = new BinomialOptionsPanel();
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
		if (fromStore != null && toStore != null) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Binomial For/Rev Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Binomial For/Rev filter on probes in ");
		b.append(startingList.name());

		b.append(" with reference ");

		b.append(fromStore.name());

		b.append(" and testing ");

		b.append(toStore.name());

		
		b.append(" with significance < ");
		b.append(stringency);
		if (applyMultipleTestingCorrection) {
			b.append(" after multiple testing correction");
		}
		
		b.append(" Min observations was ");
		b.append(minObservations);
		
		b.append(" Min percentage difference was ");
		b.append(minPercentShift);
		
		if (useCurrentQuant) {
			b.append(" Difference also taken from existing quantitation ");
		}


		return b.toString();

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();
		b.append("Binomial For/Rev p<");
		b.append(stringency);

		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList () {

		boolean aboveOnly = false;
		boolean belowOnly = false;
		
		if (options.directionBox.getSelectedItem().equals("Above")) aboveOnly = true;
		else if (options.directionBox.getSelectedItem().equals("Below")) belowOnly = true;

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
		
		useCurrentQuant = options.useCurrentQuantBox.isSelected();

		applyMultipleTestingCorrection = options.multiTestBox.isSelected();

		ProbeList newList = new ProbeList(startingList,"Filtered Probes","",new String [] {"P-value","FDR","Difference"});

		Probe [] probes = startingList.getAllProbes();
		
		// We need to create a set of mean end methylation values for all starting values
		// We found to the nearest percent so we'll end up with a set of 101 values (0-100)
		// which are the expected end points
		double [] expectedEnds = calculateEnds(probes);

		if (expectedEnds == null) return; // They cancelled whilst calculating.

		for (int i=0;i<expectedEnds.length;i++) {
			System.err.println(""+i+"\t"+expectedEnds[i]);
		}
		
		
		// This is where we'll store any hits
		Vector<ProbeTTestValue> hits = new Vector<ProbeTTestValue>();
		BinomialTest bt = new BinomialTest();
		AlternativeHypothesis hypothesis = AlternativeHypothesis.TWO_SIDED;
		
		if (aboveOnly) hypothesis = AlternativeHypothesis.GREATER_THAN;
		if (belowOnly) hypothesis = AlternativeHypothesis.LESS_THAN;
		
		for (int p=0;p<probes.length;p++) {

			if (p % 100 == 0) {
				progressUpdated("Processed "+p+" probes", p, probes.length);
			}

			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}
			
			long [] reads = fromStore.getReadsForProbe(probes[p]);
			
			int forCount = 0;
			int revCount = 0;
			
			for (int r=0;r<reads.length;r++) {
				if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
					++forCount;
				}
				else if (SequenceRead.strand(reads[r]) == Location.REVERSE) {
					++revCount;
				}
			}
			
			if (forCount+revCount < minObservations) continue;
			
			int fromPercent = Math.round((forCount*100f)/(forCount+revCount));
			
			// We need to calculate the confidence range for the from reads and work
			// out the most pessimistic value we could take as a starting value
			WilsonScoreInterval wi = new WilsonScoreInterval();
			ConfidenceInterval ci = wi.createInterval(forCount+revCount, forCount, 1-stringency);
//			System.err.println("From percent="+fromPercent+" meth="+forCount+" unmeth="+revCount+" sig="+stringency+" ci="+ci.getLowerBound()*100+" - "+ci.getUpperBound()*100);			

			reads = toStore.getReadsForProbe(probes[p]);

			forCount = 0;
			revCount = 0;
			
			for (int r=0;r<reads.length;r++) {
				if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
					++forCount;
				}
				else if (SequenceRead.strand(reads[r]) == Location.REVERSE) {
					++revCount;
				}
			}
			
			if (forCount+revCount < minObservations) continue;

			float toPercent = (forCount*100f)/(forCount+revCount);

//			System.err.println("Observed toPercent is "+toPercent+ "from meth="+forCount+" unmeth="+revCount+" and true predicted is "+expectedEnds[Math.round(toPercent)]);
			
			// Find the most pessimistic fromPercent such that the expected toPercent is as close
			// to the observed value based on the confidence interval we calculated before.
			
			double worseCaseExpectedPercent = 0;
			double smallestTheoreticalToActualDiff = 100;
			
			// Just taking the abs diff can still leave us with a closest value which is still
			// quite far from where we are.  We therefore also check if our confidence interval
			// gives us a potential value range which spans the actual value, and if it does we
			// fail it without even running the test.
			boolean seenLower = false;
			boolean seenHigher = false;
						
			for (int m=Math.max((int)Math.floor(ci.getLowerBound()*100),0);m<=Math.min((int)Math.ceil(ci.getUpperBound()*100),100);m++) {
				double expectedPercent = expectedEnds[m];
				double diff = expectedPercent-toPercent;
				if (diff <= 0) seenLower = true;
				if (diff >=0) seenHigher = true;
				
				if (Math.abs(diff) < smallestTheoreticalToActualDiff) {
					worseCaseExpectedPercent = expectedPercent;
					smallestTheoreticalToActualDiff = Math.abs(diff);
				}
			}
			
//			System.err.println("Worst case percent is "+worseCaseExpectedPercent+" with diff of "+smallestTheoreticalToActualDiff+" to "+toPercent);	
		
			// Sanity check
			if (smallestTheoreticalToActualDiff > Math.abs((toPercent-expectedEnds[Math.round(fromPercent)]))) {
				throw new IllegalStateException("Can't have a worst case which is better than the actual");
			}
			
			if (Math.abs(toPercent-worseCaseExpectedPercent) < minPercentShift) continue;
			
			// If they want to use the current quantitation as well then we can do that calculation now.
			if (useCurrentQuant) {
				try {
					if (Math.abs(toStore.getValueForProbe(probes[p])-expectedEnds[Math.round(fromStore.getValueForProbe(probes[p]))]) < minPercentShift) continue;
				}
				catch(SeqMonkException sme){
					throw new IllegalStateException(sme);
				}
			}
			

			// Check the directionality
			if (aboveOnly && worseCaseExpectedPercent-toPercent > 0) continue;
			if (belowOnly && worseCaseExpectedPercent-toPercent < 0) continue;

			
			// Now perform the Binomial test.

			double pValue = bt.binomialTest(forCount+revCount, forCount, worseCaseExpectedPercent/100d, hypothesis);
			
			double diff = ((forCount / (double)(forCount+revCount))*100)-worseCaseExpectedPercent;

			if (seenLower && seenHigher) pValue = 0.5; // Our confidence range spanned the actual value we had so we can't be significant
			
//			System.err.println("P value is "+pValue);
			
			// Store this as a potential hit (after correcting p-values later)
			hits.add(new ProbeTTestValue(probes[p], pValue,diff));

		}

		// Now we can correct the p-values if we need to
		
		ProbeTTestValue [] rawHits = hits.toArray(new ProbeTTestValue[0]);
		
		BenjHochFDR.calculateQValues(rawHits);
		
		for (int h=0;h<rawHits.length;h++) {
			if (applyMultipleTestingCorrection) {
				if (rawHits[h].q < stringency) {
					newList.addProbe(rawHits[h].probe,new float[]{(float)rawHits[h].p,(float)rawHits[h].q,(float)rawHits[h].diff});
				}
			}
			else {
				if (rawHits[h].p < stringency) {
					newList.addProbe(rawHits[h].probe,new float[]{(float)rawHits[h].p,(float)rawHits[h].q,(float)rawHits[h].diff});
				}
			}
		}

		filterFinished(newList);

	}

	private double [] calculateEnds (Probe [] probes) {
		int [] counts = new int [101];
		float [] sums = new float [101];
		
		for (int p=0;p<probes.length;p++) {
			
			if (p % 100 == 0) {
				progressUpdated("Calculating background model", p, probes.length);
			}
			
			if (cancel) {
				cancel = false;
				progressCancelled();
				return null;
			}


			long [] reads = fromStore.getReadsForProbe(probes[p]);
			
			int forCount = 0;
			int revCount = 0;
			
			for (int r=0;r<reads.length;r++) {
				if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
					++forCount;
				}
				else if (SequenceRead.strand(reads[r]) == Location.REVERSE) {
					++revCount;
				}
			}
			
			if (forCount+revCount < minObservations) continue;
			
			int fromPercent = Math.round((forCount*100f)/(forCount+revCount));
			

			reads = toStore.getReadsForProbe(probes[p]);

			forCount = 0;
			revCount = 0;
			
			for (int r=0;r<reads.length;r++) {
				if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
					++forCount;
				}
				else if (SequenceRead.strand(reads[r]) == Location.REVERSE) {
					++revCount;
				}
			}
			
			if (forCount+revCount < minObservations) continue;

			float toPercent = ((forCount*100f)/(forCount+revCount));
			
			counts[fromPercent]++;
			sums[fromPercent] += toPercent;
			
		}
		
		double lastValidPercent = 0;
		double [] finalEnds = new double[101];
		for (int i=0;i<finalEnds.length;i++) {
						
			if (counts[i] == 0) {
				finalEnds[i] = lastValidPercent;
			}
			else {
				finalEnds[i] = sums[i]/counts[i];
				lastValidPercent = finalEnds[i];
			}
		}
		
		return finalEnds;
	}
	

	/**
	 * The BoxWhiskerOptionsPanel.
	 */
	private class BinomialOptionsPanel extends JPanel implements ListSelectionListener {

		private JList startDataList;
		private JList endDataList;
		private JComboBox directionBox = new JComboBox(new String [] {"Above or Below","Above","Below"});
		private JTextField stringencyField;
		private JCheckBox multiTestBox;
		private JTextField minObservationsField;
		private JTextField minDifferenceField;
		private JCheckBox useCurrentQuantBox;

		/**
		 * Instantiates a new box whisker options panel.
		 */
		public BinomialOptionsPanel () {

			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));
			dataPanel.add(new JLabel("Starting Store",JLabel.CENTER));

			DefaultListModel dataModel = new DefaultListModel();

			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				dataModel.addElement(stores[i]);
			}

			startDataList = new JList(dataModel);
			startDataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			startDataList.setCellRenderer(new TypeColourRenderer());
			startDataList.addListSelectionListener(this);

			dataPanel.add(new JScrollPane(startDataList));

			dataPanel.add(new JLabel("Finishing Store",JLabel.CENTER));

			endDataList = new JList(dataModel);
			endDataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			endDataList.setCellRenderer(new TypeColourRenderer());
			endDataList.addListSelectionListener(this);

			dataPanel.add(new JScrollPane(endDataList));

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
			
			
			gbc.gridx=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Use current quant for difference"),gbc);

			gbc.gridx=2;

			useCurrentQuantBox = new JCheckBox();
			choicePanel.add(useCurrentQuantBox,gbc);
			
			gbc.gridx=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Direction"),gbc);

			gbc.gridx=2;

			choicePanel.add(directionBox,gbc);
			
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
			if (lse.getSource().equals(startDataList)) {
				fromStore = (DataStore)startDataList.getSelectedValue();
			}

			if (lse.getSource().equals(endDataList)) {
				toStore = (DataStore)endDataList.getSelectedValue();
			}
			optionsChanged();
		}
	}
}
