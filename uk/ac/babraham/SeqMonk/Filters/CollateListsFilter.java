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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Iterator;

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
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * This filter collates probes from multiple existing probe lists.
 */
public class CollateListsFilter extends ProbeFilter {

	private ProbeList [] lists = new ProbeList[0];
	private int limitType = EXACTLY;
	private int chosenNumber = -1;
	
	private CollateListsOptionPanel optionsPanel = new CollateListsOptionPanel();
	

	/**
	 * Instantiates a new values filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public CollateListsFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Combines probes from multiple lists";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		
//		System.out.println("Data store size="+stores.length+" lower="+lowerLimit+" upper="+upperLimit+" type="+limitType+" chosen="+chosenNumber);
		
		HashMap<Probe, Integer> probeCounts = new HashMap<Probe, Integer>();
		
		ProbeList newList = new ProbeList(collection.probeSet(),"Filtered Probes","","Number of lists");
		
		for (int l=0;l<lists.length;l++) {
			progressUpdated(l, lists.length);
			
			Probe [] probes = lists[l].getAllProbes();
			
			for (int p=0;p<probes.length;p++) {
			
			
				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}

				if (probeCounts.containsKey(probes[p])) {
					probeCounts.put(probes[p], probeCounts.get(probes[p])+1);
				}
				else {
					probeCounts.put(probes[p],1);
				}
				
			}
		}
			
		// Now we can step through the set of probes we've seen and figure out if
		// we want to keep them.
		
		Iterator<Probe> pi = probeCounts.keySet().iterator();
		
		while (pi.hasNext()) {
			
			Probe p = pi.next();
		
			// We can now figure out if the count we've got lets us add this
			// probe to the probe set.
			switch (limitType) {
			case EXACTLY:
				if (probeCounts.get(p) == chosenNumber)
					newList.addProbe(p,(float)probeCounts.get(p));
				break;
			
			case AT_LEAST:
				if (probeCounts.get(p) >= chosenNumber)
					newList.addProbe(p,(float)probeCounts.get(p));
				break;

			case NO_MORE_THAN:
				if (probeCounts.get(p) <= chosenNumber)
					newList.addProbe(p,(float)probeCounts.get(p));
				break;
			}
		}

		
		newList.setName("Combined Lists");
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
		if (lists.length == 0) return false;
		
		if (chosenNumber < 1 || chosenNumber > lists.length) return false;
				
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Collate Probe Lists Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		
		b.append("Find probes which appeared in ");

		if (limitType == EXACTLY) {
			b.append("exactly ");
		}
		else if (limitType == AT_LEAST) {
			b.append("at least ");
		}
		else if (limitType == NO_MORE_THAN) {
			b.append("no more than ");
		}
		
		b.append(chosenNumber);

		b.append(" of ");
		
		for (int s=0;s<lists.length;s++) {
			b.append(lists[s].name());
			if (s < lists.length-1) {
				b.append(" , ");
			}
		}

		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();
			
		b.append("Probes in ");
		
		if (limitType == EXACTLY) {
			b.append("exactly ");
		}
		else if (limitType == AT_LEAST) {
			b.append("at least ");
		}
		else if (limitType == NO_MORE_THAN) {
			b.append("no more than ");
		}
		
		b.append(chosenNumber);

		b.append(" out of ");
		
		b.append(lists.length);
		
		b.append(" lists.");
		
		return b.toString();
	}

	/**
	 * The ValuesFilterOptionPanel.
	 */
	private class CollateListsOptionPanel extends JPanel implements ListSelectionListener, KeyListener, ActionListener {
			
			private JList probeListsList;
			private JComboBox limitTypeBox;
			private JTextField chosenNumberField;
			private JLabel dataAvailableNumber;
			
			/**
			 * Instantiates a new values filter option panel.
			 */
			public CollateListsOptionPanel () {
				setLayout(new BorderLayout());
				JPanel dataPanel = new JPanel();
				dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
				dataPanel.setLayout(new BorderLayout());
				dataPanel.add(new JLabel("Probe Lists",JLabel.CENTER),BorderLayout.NORTH);

				DefaultListModel dataModel = new DefaultListModel();

				ProbeList [] lists = collection.probeSet().getAllProbeLists();
				
				for (int i=0;i<lists.length;i++) {
					dataModel.addElement(lists[i]);
				}

				probeListsList = new JList(dataModel);
				ListDefaultSelector.selectDefaultStores(probeListsList);
				probeListsList.addListSelectionListener(this);

				JScrollPane scrollPane = new JScrollPane(probeListsList);
				scrollPane.setPreferredSize(new Dimension(200,probeListsList.getPreferredSize().height));
				dataPanel.add(scrollPane,BorderLayout.CENTER);

				add(dataPanel,BorderLayout.WEST);

				JPanel choicePanel = new JPanel();
				choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


				JPanel choicePanel2 = new JPanel();
				choicePanel2.add(new JLabel("Probe must be present in "));
				choicePanel.add(choicePanel2);

				JPanel choicePanel3 = new JPanel();
				limitTypeBox = new JComboBox(new String [] {"Exactly","At least","No more than"});
				limitTypeBox.addActionListener(this);
				choicePanel3.add(limitTypeBox);

				chosenNumber = probeListsList.getSelectedIndices().length;
				chosenNumberField = new JTextField(""+chosenNumber,3);
				chosenNumberField.addKeyListener(this);
				choicePanel3.add(chosenNumberField);

				choicePanel3.add(new JLabel(" of the "));

				dataAvailableNumber = new JLabel("");
				valueChanged(null);
				choicePanel3.add(dataAvailableNumber);

				choicePanel3.add(new JLabel(" selected Probe Lists "));

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
					if (f == chosenNumberField) {
						if (f.getText().length() == 0) {
							chosenNumber = -1; // Won't allow filter to register as ready
						}
						else {
							chosenNumber = Integer.parseInt(f.getText());
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
				Object [] o = probeListsList.getSelectedValues();
				lists = new ProbeList[o.length];
				for (int i=0;i<o.length;i++) {
					lists[i] = (ProbeList)o[i];
				}
				dataAvailableNumber.setText(""+probeListsList.getSelectedIndices().length);
				
				optionsChanged();
			}

			/* (non-Javadoc)
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			public void actionPerformed(ActionEvent ae) {
				// This comes from the limitTypeBox
				if (limitTypeBox.getSelectedItem().equals("Exactly"))
					limitType = EXACTLY;
				else if (limitTypeBox.getSelectedItem().equals("At least"))
					limitType = AT_LEAST;
				else if (limitTypeBox.getSelectedItem().equals("No more than"))
					limitType = NO_MORE_THAN;
				else
					throw new IllegalArgumentException("Unexpected value "+limitTypeBox.getSelectedItem()+" for limit type");
				
				optionsChanged();
			}

	}
}
