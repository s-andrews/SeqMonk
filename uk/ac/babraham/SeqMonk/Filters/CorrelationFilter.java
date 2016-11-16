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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * The ValuesFilter filters probes based on their associated values
 * from quantiation.  Each probe is filtered independently of all
 * other probes.
 */
public class CorrelationFilter extends ProbeFilter {

	private DataStore [] stores = new DataStore[0];
	private ProbeList referenceList;
	private Double correlationCutoff = 0.9;
	private ProbeList [] allProbeLists;
	
	private CorrelationFilterOptionPanel optionsPanel;
	

	/**
	 * Instantiates a new values filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public CorrelationFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		allProbeLists = collection.probeSet().getAllProbeLists();
		referenceList = allProbeLists[0];
		optionsPanel = new CorrelationFilterOptionPanel();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters based on the correlation to a quantitation profile of an exsisting probe list";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		
//		System.out.println("Data store size="+stores.length+" lower="+lowerLimit+" upper="+upperLimit+" type="+limitType+" chosen="+chosenNumber);
		
		Probe [] probes = startingList.getAllProbes();
		ProbeList newList = new ProbeList(startingList,"Filtered Probes","","R-value");
		
		double [] referenceProfile = null;
		
		try {
			referenceProfile = getReferneceProfile();
		}
		catch (SeqMonkException ex) {
			progressExceptionReceived(ex);
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
				
				float r = PearsonCorrelation.calculateCorrelation(referenceProfile, currentProfile);
				if (correlationCutoff >0) {
					if (r >= correlationCutoff) {
						newList.addProbe(probes[p], r);
					}
				}
				else {					
					if (r <= correlationCutoff) {
						newList.addProbe(probes[p], r);
					}
				}
				
			}
			catch (SeqMonkException ex) {
				continue;
			}
			
		}
		
		filterFinished(newList);
				
	}
	
	private double [] getReferneceProfile () throws SeqMonkException {
		
		double [] referenceProfile = new double[stores.length];
		
		Probe [] probes  = referenceList.getAllProbes();
		
		double [] currentProfile = new double[stores.length];
		double min = 0;
		double max = 0;
		
		for (int p=0;p<probes.length;p++) {
			// Get the raw probe values
			for (int s=0;s<stores.length;s++) {
				currentProfile[s] = stores[s].getValueForProbe(probes[p]);
				if (s == 0 || currentProfile[s] < min) min = currentProfile[s]; 
				if (s == 0 || currentProfile[s] > max) max = currentProfile[s]; 
			}
			
			// Scale between 0 and 1
			for (int s=0;s<stores.length;s++) {
				if (max == min) {
					currentProfile[s] = 0.5;
				}
				else {
					currentProfile[s] = (currentProfile[s] - min) / (max-min);
				}
			}
			
			// Add it to the total profile
			for (int s=0;s<stores.length;s++) {
				referenceProfile[s] += currentProfile[s];
			}
			
		}
		
		// Divide by the number of probes to get the reference profile
		// to scale between 0-1
		for (int s=0;s<stores.length;s++) {
			referenceProfile[s] /= probes.length;
		}
		
		return referenceProfile;
		
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
		return "Probe List Correlation Filter";
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
		
		b.append(" with probes in ");
		b.append(referenceList.name());
		
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
		b.append(" with ");
		b.append(referenceList.name());
		return b.toString();
	}

	/**
	 * The ValuesFilterOptionPanel.
	 */
	private class CorrelationFilterOptionPanel extends JPanel implements ListSelectionListener, KeyListener, ActionListener {
			
			private JList dataList;
			private JComboBox probeLists;
			private JTextField correlationField;
			
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
				choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


				JPanel choicePanel2 = new JPanel();
				choicePanel2.add(new JLabel("Correlation cutoff "));
				correlationField = new JTextField(""+correlationCutoff,3);
				correlationField.addKeyListener(this);
				choicePanel2.add(correlationField);
				choicePanel.add(choicePanel2);

				JPanel choicePanel3 = new JPanel();
				choicePanel3.add(new JLabel("Reference probe list"));
				probeLists = new JComboBox(allProbeLists);
				probeLists.addActionListener(this);
				choicePanel3.add(probeLists);

				choicePanel.add(choicePanel3);
				add(new JScrollPane(choicePanel),BorderLayout.CENTER);

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
					else if (f.getText().equals("-")) {
						correlationCutoff = 0d;
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
				
				optionsChanged();
			}

			/* (non-Javadoc)
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent ae) {
				// This comes from probe list selector
				referenceList = (ProbeList)probeLists.getSelectedItem();
				
				optionsChanged();
			}

	}
}
