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
package uk.ac.babraham.SeqMonk.Filters.CorrelationCluster;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Filters.ProbeFilter;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * The Correlation Cluster Filter finds groups of genes whose quantitation
 * is correlated across a set of DataStores.
 */
public class CorrelationClusterFilter extends ProbeFilter {

	private DataStore [] stores = new DataStore[0];
	private Integer minSize = null;
	private Double minCorrelation = 0.9;
	
	private CorrelationFilterOptionPanel optionsPanel = new CorrelationFilterOptionPanel();
	

	/**
	 * Instantiates a new correlation filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public CorrelationClusterFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters groups of probes which are correlated with each other";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		
//		System.out.println("Data store size="+stores.length+" lower="+lowerLimit+" upper="+upperLimit+" type="+limitType+" chosen="+chosenNumber);
		
		Probe [] probes = startingList.getAllProbes();
		ProbeList newList = new ProbeList(startingList,"Correlation Cluster","",new String[0]);
		
		Vector<CorrelationCluster>clusters = new Vector<CorrelationCluster>();
		
		for (int p=0;p<probes.length;p++) {
			progressUpdated(p, probes.length);
			
			if (cancel) {
				progressCancelled();
				return;
			}
			
			Enumeration<CorrelationCluster>cc = clusters.elements();
			
			double maxR = 0;
			CorrelationCluster bestCluster = null;
			
			while (cc.hasMoreElements()) {
				CorrelationCluster cluster = cc.nextElement();
				
				double r = cluster.minRValue(probes[p], minCorrelation);
				
				if (r > minCorrelation && r > maxR) {
					maxR = r;
					bestCluster = cluster;
				}
								
			}
			
			if (bestCluster != null) {
				bestCluster.addProbe(probes[p]);
			}
			else {
				// If we get here we need to make a new cluster
				clusters.add(new CorrelationCluster(stores, probes[p]));
			}
			
		}
		
		// Now we need to go through the clusters making a list for each one
		// and putting the full set of passed probes in the main list.
		
		CorrelationCluster [] allClusters = clusters.toArray(new CorrelationCluster[0]);
		Arrays.sort(allClusters);
		
		
		for (int c=0;c<allClusters.length;c++) {
						
			if (allClusters[c].size() >= minSize) {
				Probe [] clusterProbes = allClusters[c].getProbes();
				
				ProbeList clusterList = new ProbeList(newList, "Group"+(c+1), "Correlated Probes", new String[0]);
				
				for (int p=0;p<clusterProbes.length;p++) {
					newList.addProbe(clusterProbes[p], null);
					clusterList.addProbe(clusterProbes[p],null);
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
		if (stores.length < 3) return false;
				
		if (minSize == null) return false;
		
		if (minCorrelation == null) return false;
		
		if (minCorrelation < 0.1 || minCorrelation > 1) return false;
		
		if (minSize < 2) return false;
		
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Correlation Cluster Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		
		b.append("Correlate probes in ");
		b.append(startingList.name());
		b.append(" for stores ");

		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" with minimum cluster size ");
		b.append(minSize);
		b.append(" and R > ");
		b.append(minCorrelation);
		
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
		
		b.append("Correlation R > ");
		b.append(minCorrelation);
		
		
		return b.toString();
	}

	/**
	 * The CorrelationFilterOptionPanel.
	 */
	private class CorrelationFilterOptionPanel extends JPanel implements ListSelectionListener, KeyListener {
			
			private JList dataList;
			private JTextField minSizeField;
			private JTextField minCorrelationField;
			
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
				valueChanged(null);
				dataList.setCellRenderer(new TypeColourRenderer());
				dataList.addListSelectionListener(this);
				dataPanel.add(new JScrollPane(dataList),BorderLayout.CENTER);

				add(dataPanel,BorderLayout.WEST);

				JPanel choicePanel = new JPanel();
				choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


				JPanel choicePanel2 = new JPanel();
				choicePanel2.add(new JLabel("Minimum group size "));
				minSizeField = new JTextField(3);
				minSizeField.addKeyListener(this);
				choicePanel2.add(minSizeField);

				choicePanel.add(choicePanel2);

				JPanel choicePanel3 = new JPanel();
				choicePanel3.add(new JLabel("Minimum correlation "));

				minCorrelationField = new JTextField(3);
				minCorrelationField.setText(""+minCorrelation);
				minCorrelationField.addKeyListener(this);
				choicePanel3.add(minCorrelationField);

				choicePanel.add(choicePanel3);
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

				try {
					if (f == minSizeField) {
						if (f.getText().length() == 0) {
							minSize = null;
						}
						else {
							minSize = Integer.parseInt(f.getText());
						}
					}
					else if (f == minCorrelationField) {
						if (f.getText().length() == 0) {
							minCorrelation = null;
						}
						else {
							minCorrelation = Double.parseDouble(f.getText());
						}
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
				
				optionsChanged();
			}

	}
}
