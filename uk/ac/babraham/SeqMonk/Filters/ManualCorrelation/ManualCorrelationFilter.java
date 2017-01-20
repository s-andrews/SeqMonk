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
package uk.ac.babraham.SeqMonk.Filters.ManualCorrelation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Correlation.PearsonCorrelation;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Filters.ProbeFilter;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * The ValuesFilter filters probes based on their associated values
 * from quantiation.  Each probe is filtered independently of all
 * other probes.
 */
public class ManualCorrelationFilter extends ProbeFilter {

	private DataStore [] stores = new DataStore[0];
	private Double correlationCutoff = 0.9;
	private double [][] profiles;

	private CorrelationFilterOptionPanel optionsPanel;


	/**
	 * Instantiates a new values filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public ManualCorrelationFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		optionsPanel = new CorrelationFilterOptionPanel();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters based on correlation to a user defined profile";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		//		System.out.println("Data store size="+stores.length+" lower="+lowerLimit+" upper="+upperLimit+" type="+limitType+" chosen="+chosenNumber);
		profiles  = new double [optionsPanel.graphs.length][];
			
		for (int g=0;g<optionsPanel.graphs.length;g++) {
			profiles[g] = optionsPanel.graphs[g].profile();
		}
		
		Probe [] probes = startingList.getAllProbes();

		ProbeList parentList = null;
		ProbeList [] newLists = new ProbeList [profiles.length];
		
		if (newLists.length > 1) {
			parentList = new ProbeList(startingList,"Filtered Probes","",null);
			for (int p=0;p<newLists.length;p++) {
				newLists[p] = new ProbeList(parentList,listName()+" "+(p+1),listDescription(),"R-value");
			}
		}
		else {
			// We won't have a parent list
			newLists[0] = new ProbeList(startingList,listName(),listDescription(),"R-value");
		}

		
		
		

		double [] currentProfile = new double [stores.length];

		for (int p=0;p<probes.length;p++) {

			progressUpdated(p, probes.length);

			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}
			try {
				for (int s=0;s<stores.length;s++) {
					currentProfile[s] = stores[s].getValueForProbe(probes[p]);
				}

				float bestR = 0;
				int bestRIndex = -1;
				
				for (int pr=0;pr<profiles.length;pr++) {
				
					float r = PearsonCorrelation.calculateCorrelation(profiles[pr], currentProfile);
					if (correlationCutoff >0) {
						if (r >= correlationCutoff && r>bestR) {
							bestR = r;
							bestRIndex = pr;
						}
					}
					else {					
						if (r <= correlationCutoff && r < bestR) {
							bestR = r;
							bestRIndex = pr;
						}
					}
				}
				
				if (bestRIndex >= 0) {
					newLists[bestRIndex].addProbe(probes[p], bestR);
					if (parentList != null) {
						parentList.addProbe(probes[p], null);
					}
				}
				
			}
			catch (SeqMonkException ex) {
				continue;
			}

		}

		
		// If we only have one cluster then we just return that list
		if (newLists.length == 1) {
			filterFinished(newLists[0]);
		}
		
		else {
			filterFinished(parentList);
		}

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
		if (stores.length < 3) return false;

		if (correlationCutoff == null || correlationCutoff > 1 || correlationCutoff < -1) return false;

		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Manual Correlation Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where quantitation profiles in ");
		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" were correlated R");
		if (correlationCutoff > 0) {
			b.append(">= ");
		}
		else {
			b.append("<= ");
		}
		b.append(correlationCutoff);

		b.append(" with a manually created profile");
		
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

		b.append("Correlation ");
		if (correlationCutoff > 0) {
			b.append("> ");
		}
		else {
			b.append("< ");
		}
		b.append(correlationCutoff);
		b.append(" with manual profile");
		return b.toString();
	}

	/**
	 * The ValuesFilterOptionPanel.
	 */
	private class CorrelationFilterOptionPanel extends JPanel implements ListSelectionListener, KeyListener, ActionListener {

		private JList dataList;
		private JTextField correlationField;
		private CorrelationProfileGraph [] graphs = new CorrelationProfileGraph[0];
		private JPanel graphsPanel;
		private JComboBox numberOfPanels;

		/**
		 * Instantiates a new values filter option panel.
		 */
		public CorrelationFilterOptionPanel () {
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
			dataPanel.add(new JScrollPane(dataList),BorderLayout.CENTER);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BorderLayout());


			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Correlation cutoff "));
			correlationField = new JTextField(""+correlationCutoff,3);
			correlationField.addKeyListener(this);
			choicePanel2.add(correlationField);
			
			choicePanel2.add(new JLabel("Number of profiles"));
			numberOfPanels = new JComboBox(new Integer [] {1,2,3,4,5,6,7,8,9,10,11,12});
			numberOfPanels.addActionListener(this);
			choicePanel2.add(numberOfPanels);
			
			
			choicePanel.add(choicePanel2,BorderLayout.NORTH);

			graphsPanel = new JPanel();

			choicePanel.add(graphsPanel,BorderLayout.CENTER);
			actionPerformed(null);
			add(choicePanel,BorderLayout.CENTER);

			valueChanged(null); // Populate the list of usable stores.

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

			try {
				if (f.getText().length() == 0) {
					correlationCutoff = null;
				}
				else {
					correlationCutoff = Double.parseDouble(f.getText());
				}
			}
			catch (NumberFormatException e) {
				f.setText(f.getText().substring(0,f.getText().length()-1));
			}

			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			Object [] o = dataList.getSelectedValues();
			stores = new DataStore[o.length];
			for (int i=0;i<o.length;i++) {
				stores[i] = (DataStore)o[i];
			}
			for (int g=0;g<graphs.length;g++) {
				graphs[g].setStores(stores);
			}

			optionsChanged();
		}

		public void actionPerformed(ActionEvent ae) {
			// The number of graphs to draw changed
			CorrelationProfileGraph [] newGraphs = new CorrelationProfileGraph [((Integer)numberOfPanels.getSelectedItem())];
			
			// Copy over any existing graphs we can
			for (int i=0;i<Math.min(graphs.length, newGraphs.length);i++) {
				newGraphs[i] = graphs[i];
			}
			
			// Make any new graphs we need to
			for (int i=graphs.length;i<newGraphs.length;i++) {
				newGraphs[i] = new CorrelationProfileGraph(stores);
			}
			
			// Replace the existing set of graphs
			graphs = newGraphs;
			
			graphsPanel.removeAll();
			
			if (graphs.length == 1) {
				graphsPanel.setLayout(new GridLayout(1,1));
			}
			if (graphs.length % 2 == 0) {
				graphsPanel.setLayout(new GridLayout(graphs.length/2, 2));
			}
			else {
				graphsPanel.setLayout(new GridLayout((graphs.length+1)/2, 2));				
			}
			
			for (int i=0;i<graphs.length;i++) {
				graphsPanel.add(graphs[i]);
			}
			
			if (graphs.length > 1 && graphs.length % 2 !=  0) {
				JPanel whitePanel = new JPanel();
				whitePanel.setOpaque(true);
				whitePanel.setBackground(Color.WHITE);
				graphsPanel.add(whitePanel);
			}
			
			graphsPanel.validate();
			graphsPanel.repaint();
			
		}
	}
}
