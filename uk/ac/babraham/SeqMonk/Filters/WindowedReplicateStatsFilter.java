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
import java.util.Enumeration;
import java.util.Hashtable;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.AnovaTest;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ProbeGroupTTestValue;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.TTest;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.ConsecutiveProbeGenerator;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.FeatureProbeGroupGenerator;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.ProbeGroupGenerator;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.ProbeWindowGenerator;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * Performs a t-test based filter where a set of probes falling into
 * a window of a defined size are assumed to be technical replicates
 * of a single effect. The set is compared either to zero or one or
 * more other sets to determine a significance level which is then
 * used as the basis for the filter.
 */
public class WindowedReplicateStatsFilter extends ProbeFilter {


	private static final int DISTANCE_WINDOW = 111;
	private static final int CONSECUTIVE_WINDOW = 112;
	private static final int FEATURE_WINDOW = 113;

	private DataStore [] stores = new DataStore[0];
	private Double cutoff = null;
	private Integer windowSize = null;
	private int windowType = DISTANCE_WINDOW;

	private boolean multiTest = true;
	private boolean splitReplicateSets = false;

	private final WindowedReplicateOptionsPanel optionsPanel;

	/**
	 * Instantiates a new windowed replicate stats filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantiated.
	 */
	public WindowedReplicateStatsFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		// Set some defaults
		cutoff = 0.01;
		windowSize = 1000;
		multiTest = true;
		optionsPanel = new WindowedReplicateOptionsPanel();

	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filter to remove regions where probes have high variability in their values";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		Vector<ProbeGroupTTestValue> newListProbesVector = new Vector<ProbeGroupTTestValue>();

		for (int c=0;c<chromosomes.length;c++) {

			progressUpdated("Processing windows on Chr"+chromosomes[c].name(), c, chromosomes.length);

			Probe [] probes = startingList.getProbesForChromosome(chromosomes[c]);
			ProbeGroupGenerator gen = null;
			if (windowType == DISTANCE_WINDOW) {
				gen = new ProbeWindowGenerator(probes,windowSize);
			}
			else if (windowType == CONSECUTIVE_WINDOW) {
				gen = new ConsecutiveProbeGenerator(probes, windowSize);
			}
			else if (windowType == FEATURE_WINDOW) {
				gen = new FeatureProbeGroupGenerator(probes, collection.genome().annotationCollection().getFeaturesForType(optionsPanel.featureTypeBox.getSelectedItem().toString()));
			}

			while (true) {

				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}


				Probe [] theseProbes = gen.nextSet();

				if (theseProbes == null) {
					//					System.err.println("List of probes was null");
					break;
				}


				// We need at least 3 probes in a set to calculate a p-value
				if (theseProbes.length < 3) {
					//					System.err.println("Only "+theseProbes.length+" probes in the set");
					continue;
				}
				
				double [][] values = new double[stores.length][];
				
				for (int j=0;j<stores.length;j++) {
					if (splitReplicateSets & stores[j] instanceof ReplicateSet) {
						values[j] = new double[theseProbes.length * ((ReplicateSet)stores[j]).dataStores().length];
					}
					else {
						values[j] = new double[theseProbes.length];
					}
				}
				
				for (int j=0;j<stores.length;j++) {
					
					int index = 0;
										
					for (int i=0;i<theseProbes.length;i++) {
						try {
							
							if (splitReplicateSets & stores[j] instanceof ReplicateSet) {
								DataStore [] localStores = ((ReplicateSet)stores[j]).dataStores();
								for (int l=0;l<localStores.length;l++) {
									values[j][index] = localStores[l].getValueForProbe(theseProbes[i]);
									index++;
								}
							}
							else {
								values[j][index] = stores[j].getValueForProbe(theseProbes[i]);
								index++;
							}
						} 
						catch (SeqMonkException e) {
						}
					}
					
					if (index != values[j].length) {
						throw new IllegalStateException("Didn't fill all values total="+values[j].length+" index="+index);
					}
				}

				double pValue = 0;
				try {
					if (stores.length == 1) {
						pValue = TTest.calculatePValue(values[0],0);
					}
					else if (stores.length == 2) {
						pValue = TTest.calculatePValue(values[0],values[1]);
					}
					else {
						pValue = AnovaTest.calculatePValue(values);
					}
				} 
				catch (SeqMonkException e) {
					throw new IllegalStateException(e);
				}

				newListProbesVector.add(new ProbeGroupTTestValue(theseProbes,pValue));
			}
		}

		ProbeGroupTTestValue [] newListProbes = newListProbesVector.toArray(new ProbeGroupTTestValue[0]);

		// Do the multi-testing correction if necessary
		if (multiTest) {
			BenjHochFDR.calculateQValues(newListProbes);
		}

		ProbeList newList;

		// We need to handle duplicate hits internally since probe lists can't do
		// this themselves any more.

		Hashtable<Probe, Float>newListTemp = new Hashtable<Probe, Float>();

		if (multiTest) {
			newList = new ProbeList(startingList,"","","Q-value");
			for (int i=0;i<newListProbes.length;i++) {
				if (newListProbes[i].q <= cutoff) {
					Probe [] passedProbes = newListProbes[i].probes;
					for (int p=0;p<passedProbes.length;p++) {
						if (newListTemp.containsKey(passedProbes[p])) {
							// We always give a probe the lowest possible q-value
							if (newListTemp.get(passedProbes[p])<=newListProbes[i].q) {
								continue;
							}
						}
						newListTemp.put(passedProbes[p],(float)newListProbes[i].q);								
					}
				}
			}
		}
		else {
			newList = new ProbeList(startingList,"","","P-value");
			for (int i=0;i<newListProbes.length;i++) {
				if (newListProbes[i].p <= cutoff) {
					Probe [] passedProbes = newListProbes[i].probes;
					for (int p=0;p<passedProbes.length;p++) {
						if (newListTemp.containsKey(passedProbes[p])) {
							// We always give a probe the lowest possible p-value
							if (newListTemp.get(passedProbes[p])<=newListProbes[i].p) {
								continue;
							}
						}
						newListTemp.put(passedProbes[p],(float)newListProbes[i].p);								
					}
				}
			}
		}

		// Add the cached hits to the new list
		Enumeration<Probe>en = newListTemp.keys();

		while (en.hasMoreElements()) {
			Probe p = en.nextElement();
			newList.addProbe(p, newListTemp.get(p));
		}

		filterFinished(newList);

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
		if (stores.length == 0) return false;

		if (cutoff == null || cutoff > 1 || cutoff < 0) return false;

		if (windowSize == null || windowSize < 0) return false;

		return true;	
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Windowed Replicate Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Windowed replicate filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where ");

		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" had a significance below ");

		b.append(cutoff);

		if (multiTest) {
			b.append(" after Benjamimi and Hochberg correction");
		}

		if (splitReplicateSets) {
			b.append(" splitting replicate sets into separate observations");
		}

		b.append(" over a window of ");

		if (windowType == FEATURE_WINDOW) {
			b.append(optionsPanel.featureTypeBox.getSelectedItem().toString());
			b.append(" features");
		}
		else {
			b.append(windowSize);
			if (windowType == DISTANCE_WINDOW) {
				b.append("bp");
			}
			else {
				b.append(" probes");
			}
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

		b.append("Windowed replicate p<");

		b.append(cutoff);

		if (multiTest) {
			b.append(" after correction");
		}

		b.append(" over ");

		if (windowType == FEATURE_WINDOW) {
			b.append(optionsPanel.featureTypeBox.getSelectedItem().toString());
			b.append(" features");
		}
		else {
			b.append(windowSize);
			if (windowType == DISTANCE_WINDOW) {
				b.append("bp");
			}
			else {
				b.append(" probes");
			}
		}

		return b.toString();
	}

	/**
	 * The WindowedReplicateOptionsPanel.
	 */
	private class WindowedReplicateOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ChangeListener, ItemListener {

		private JList dataList;
		private JTextField cutoffField;
		private JTextField windowSizeField;
		private JCheckBox splitReplicateSetsBox;
		private JCheckBox multiTestBox;
		private JComboBox windowTypeBox;
		private JComboBox featureTypeBox;


		/**
		 * Instantiates a new windowed replicate options panel.
		 */
		public WindowedReplicateOptionsPanel () {

			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setLayout(new BorderLayout());
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));

			dataPanel.add(new JLabel("Data Store(s) to Test",JLabel.CENTER),BorderLayout.NORTH);

			DefaultListModel dataModel = new DefaultListModel();

			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					dataModel.addElement(stores[i]);
				}
			}	

			dataList = new JList(dataModel);
			ListDefaultSelector.selectDefaultStores(dataList);
			valueChanged(null);
			dataList.setCellRenderer(new TypeColourRenderer());
			dataList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(dataList),BorderLayout.CENTER);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();

			choicePanel.setLayout(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=0;
			gbc.gridy=0;
			gbc.weightx=0.2;
			gbc.weighty=0.5;
			gbc.fill=GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(5, 5, 5, 5);


			gbc.gridwidth=1;
			gbc.gridy++;
			gbc.fill = GridBagConstraints.NONE;

			choicePanel.add(new JLabel("P-value cutoff",JLabel.CENTER),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;
			gbc.fill = GridBagConstraints.HORIZONTAL;


			cutoffField = new JTextField(cutoff.toString(),5);
			cutoffField.addKeyListener(this);
			choicePanel.add(cutoffField,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;
			gbc.fill = GridBagConstraints.NONE;

			windowTypeBox = new JComboBox(new String [] {"Window size (bp)","Number of probes","Probes overlapping features"});
			windowTypeBox.addItemListener(this);
			choicePanel.add(windowTypeBox,gbc);

			gbc.gridx++;
			gbc.weightx=0.6;
			gbc.fill = GridBagConstraints.HORIZONTAL;

			windowSizeField = new JTextField(windowSize.toString(),5);
			windowSizeField.addKeyListener(this);
			choicePanel.add(windowSizeField,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			choicePanel.add(new JLabel("Select Feature Type",JLabel.CENTER),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			featureTypeBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
			featureTypeBox.setEnabled(false);

			choicePanel.add(featureTypeBox,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			choicePanel.add(new JLabel("Split Replicate Sets into separate observations",JLabel.CENTER),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;


			splitReplicateSetsBox = new JCheckBox();
			if (splitReplicateSets) {
				splitReplicateSetsBox.setSelected(true);
			}
			else {
				splitReplicateSetsBox.setSelected(false);
			}
			splitReplicateSetsBox.addChangeListener(this);
			choicePanel.add(splitReplicateSetsBox,gbc);
			
			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			choicePanel.add(new JLabel("Apply multiple testing correction",JLabel.CENTER),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;


			multiTestBox = new JCheckBox();
			if (multiTest) {
				multiTestBox.setSelected(true);
			}
			else {
				multiTestBox.setSelected(false);
			}
			multiTestBox.addChangeListener(this);
			choicePanel.add(multiTestBox,gbc);

			add(new JScrollPane(choicePanel),BorderLayout.CENTER);

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

			if (f.equals(cutoffField)) {
				if (f.getText().length() == 0) cutoff = null;
				else {
					try {
						cutoff = Double.parseDouble(cutoffField.getText());
					}
					catch (NumberFormatException e) {
						cutoffField.setText(cutoffField.getText().substring(0,cutoffField.getText().length()-1));
					}
				}
			}

			else if (f.equals(windowSizeField)) {
				if (f.getText().length() == 0) windowSize = null;
				else {
					try {
						windowSize = Integer.parseInt(windowSizeField.getText());
					}
					catch (NumberFormatException e) {
						windowSizeField.setText(windowSizeField.getText().substring(0,windowSizeField.getText().length()-1));
					}
				}
			}

			else {
				System.err.println("Unknown text field "+f);
			}	
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			Object [] o = dataList.getSelectedValues();
			DataStore [] newStores = new DataStore[o.length];
			for (int i=0;i<o.length;i++) {
				newStores[i] = (DataStore)o[i];
			}
			stores = newStores;
			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		public void stateChanged(ChangeEvent ce) {
//			System.err.println("Changing option");
			multiTest = multiTestBox.isSelected();
			splitReplicateSets = splitReplicateSetsBox.isSelected();
			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {

			if (ie.getSource() == windowTypeBox) {
				String type = (String)windowTypeBox.getSelectedItem();
				if (type.equals("Window size (bp)")) {
					windowType = DISTANCE_WINDOW;
					featureTypeBox.setEnabled(false);
					windowSizeField.setEnabled(true);
				}
				else if (type.equals("Number of probes")) {
					windowType = CONSECUTIVE_WINDOW;
					featureTypeBox.setEnabled(false);
					windowSizeField.setEnabled(true);
				}
				else if (type.equals("Probes overlapping features")) {
					windowType = FEATURE_WINDOW;
					featureTypeBox.setEnabled(true);
					windowSizeField.setEnabled(false);
				}
				else {
					System.err.println("Unknown window type '"+type+"'");
					windowType = -1;
				}

			}
			optionsChanged();

		}

	}


}
