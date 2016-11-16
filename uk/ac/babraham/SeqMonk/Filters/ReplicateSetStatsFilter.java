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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
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
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ProbeTTestValue;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.TTest;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.CrashReporter;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * Performs a t-test based filter across one or more replicate sets.
 * Each set must have 3 or more data stores within it.
 */
public class ReplicateSetStatsFilter extends ProbeFilter {

	private ReplicateSet [] replicateSets = new ReplicateSet[0];
	private Double cutoff = null;

	private boolean multiTest = true;
	
	private final ReplicateSetOptionsPanel optionsPanel;

	/**
	 * Instantiates a new replicate set stats filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public ReplicateSetStatsFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		// Set some defaults
		cutoff = 0.01;
		multiTest = true;
		optionsPanel = new ReplicateSetOptionsPanel();
		
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filter to look for significant changes between sets of replicates";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
			
		Chromosome [] chromosomes = collection.genome().getAllChromosomes();
		
		// Make up the list of DataStores in each replicate set
		DataStore [][] stores = new DataStore[replicateSets.length][];
		
		for (int i=0;i<replicateSets.length;i++) {
			stores[i] = replicateSets[i].dataStores();
		}
		
		Vector<ProbeTTestValue> newListProbesVector = new Vector<ProbeTTestValue>();
	
		for (int c=0;c<chromosomes.length;c++) {
	
			progressUpdated("Processing probes on Chr"+chromosomes[c].name(), c, chromosomes.length);
	
			Probe [] probes = startingList.getProbesForChromosome(chromosomes[c]);
	
			for (int p=0;p<probes.length;p++) {
				
				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}

		
				double [][] values = new double[replicateSets.length][];
				for (int i=0;i<replicateSets.length;i++) {
					values[i] = new double[stores[i].length];
					for (int j=0;j<stores[i].length;j++) {
						try {
							values[i][j] = stores[i][j].getValueForProbe(probes[p]);
						} 
						catch (SeqMonkException e) {
						}
					}
				}
	
				double pValue = 0;
				try {
					if (replicateSets.length == 1) {
						pValue = TTest.calculatePValue(values[0],0);
					}
					else if (replicateSets.length == 2) {
						pValue = TTest.calculatePValue(values[0],values[1]);
					}
					else {
						pValue = AnovaTest.calculatePValue(values);
					}
				} 
				catch (SeqMonkException e) {
					new CrashReporter(e);
				}
		
				newListProbesVector.add(new ProbeTTestValue(probes[p],pValue));
			}
		}
	
		ProbeTTestValue [] newListProbes = newListProbesVector.toArray(new ProbeTTestValue[0]);
	
		// Do the multi-testing correction if necessary
		if (multiTest) {
			BenjHochFDR.calculateQValues(newListProbes);
		}
	
		ProbeList newList;
	
		if (multiTest) {
			newList = new ProbeList(startingList,"","","Q-value");
			for (int i=0;i<newListProbes.length;i++) {
				if (newListProbes[i].q <= cutoff) {
					newList.addProbe(newListProbes[i].probe,new Float(newListProbes[i].q));
				}
			}
		}
		else {
			newList = new ProbeList(startingList,"","","P-value");
			for (int i=0;i<newListProbes.length;i++) {
				if (newListProbes[i].p <= cutoff) {
					newList.addProbe(newListProbes[i].probe,new Float(newListProbes[i].p));
				}
			}
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
		if (replicateSets.length == 0) return false;

		if (cutoff == null || cutoff > 1 || cutoff < 0) return false;
						
		return true;	
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Replicate Set Stats Filter";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		
		b.append("Replicate set stats filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where ");

		for (int s=0;s<replicateSets.length;s++) {
			b.append(replicateSets[s].name());
			if (s < replicateSets.length-1) {
				b.append(" , ");
			}
		}
		
		b.append(" had a significance below ");
		
		b.append(cutoff);
		
		if (multiTest) {
			b.append(" after Benjamimi and Hochberg correction");
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
		
		b.append("Replicate set stats p<");
		
		b.append(cutoff);
		
		if (multiTest) {
			b.append(" after correction");
		}
		
		return b.toString();	}

	/**
	 * The WindowedReplicateOptionsPanel.
	 */
	private class ReplicateSetOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ChangeListener {
	
		private JList dataList;
		private JTextField cutoffField;
		private JCheckBox multiTestBox;
	
		/**
		 * Instantiates a new windowed replicate options panel.
		 */
		public ReplicateSetOptionsPanel () {
	
			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setLayout(new BorderLayout());
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
	
			dataPanel.add(new JLabel("Replicate Sets to Test",JLabel.CENTER),BorderLayout.NORTH);
	
			DefaultListModel dataModel = new DefaultListModel();
	
			ReplicateSet [] sets = collection.getAllReplicateSets();
			for (int i=0;i<sets.length;i++) {
				if (sets[i].isQuantitated() && sets[i].dataStores().length >=3) {
					dataModel.addElement(sets[i]);
				}
			}
	
	
			dataList = new JList(dataModel);
			ListDefaultSelector.selectDefaultStores(dataList);
			valueChanged(null); // Set the initial lists
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
	
	
			gbc.gridwidth=1;
			gbc.gridy++;
	
			choicePanel.add(new JLabel("P-value cutoff",JLabel.RIGHT),gbc);
	
			gbc.gridx++;
			gbc.weightx=0.6;
	
			cutoffField = new JTextField(cutoff.toString(),5);
			cutoffField.addKeyListener(this);
			choicePanel.add(cutoffField,gbc);
		
			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;
	
			choicePanel.add(new JLabel("Apply multiple testing correction",JLabel.RIGHT),gbc);
	
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
			
			else {
				System.err.println("Unknown text field "+f);
			}	
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			Object [] o = dataList.getSelectedValues();
			ReplicateSet [] newSets = new ReplicateSet[o.length];
			for (int i=0;i<o.length;i++) {
				newSets[i] = (ReplicateSet)o[i];
			}
			replicateSets = newSets;
			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		public void stateChanged(ChangeEvent ce) {
			multiTest = multiTestBox.isSelected();
			optionsChanged();
		}
	
	}


}
