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
public class ValuesFilter extends ProbeFilter {

	private DataStore [] stores = new DataStore[0];
	private Float lowerLimit = null;
	private Float upperLimit = null;
	private int limitType = EXACTLY;
	private int chosenNumber = -1;
	
	private ValuesFilterOptionPanel optionsPanel = new ValuesFilterOptionPanel();
	

	/**
	 * Instantiates a new values filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public ValuesFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the quantitated values associated with each probe";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		
//		System.out.println("Data store size="+stores.length+" lower="+lowerLimit+" upper="+upperLimit+" type="+limitType+" chosen="+chosenNumber);
		
		Probe [] probes = startingList.getAllProbes();
		ProbeList newList = new ProbeList(startingList,"Filtered Probes","",null);
		
		for (int p=0;p<probes.length;p++) {
			
			progressUpdated(p, probes.length);
			
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}

			
			int count = 0;
			for (int s=0;s<stores.length;s++) {
				float d = 0;
				if (! stores[s].hasValueForProbe(probes[p])) continue;
				
				try {
					d = stores[s].getValueForProbe(probes[p]);
				} 
				catch (SeqMonkException e) {
					e.printStackTrace();
					continue;
				}
				
				if (Float.isNaN(d)) continue; // NaN values always fail the filter.
				
				// Now we have the value we need to know if it passes the test
				if (upperLimit != null)
					if (d > upperLimit)
						continue;
				
				if (lowerLimit != null)
					if (d < lowerLimit)
						continue;
				
				// This one passes, we can add it to the count
				++count;
			}
			
			// We can now figure out if the count we've got lets us add this
			// probe to the probe set.
			switch (limitType) {
			case EXACTLY:
				if (count == chosenNumber)
					newList.addProbe(probes[p],null);
				break;
			
			case AT_LEAST:
				if (count >= chosenNumber)
					newList.addProbe(probes[p],null);
				break;

			case NO_MORE_THAN:
				if (count <= chosenNumber)
					newList.addProbe(probes[p],null);
				break;
			}
		}

		
		newList.setName("Value between "+lowerLimit+"-"+upperLimit);
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
		
		if (chosenNumber < 1 || chosenNumber > stores.length) return false;
		
		if (lowerLimit == null && upperLimit == null) return false;
		
		if (lowerLimit != null && upperLimit != null && lowerLimit > upperLimit) return false;
		
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Probe Values Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		
		b.append("Filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where ");
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
		
		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
				b.append(" , ");
			}
		}
		
		b.append(" had a value ");
		
		if (lowerLimit != null && upperLimit != null) {
			b.append("between ");
			b.append(lowerLimit);
			b.append(" and ");
			b.append(upperLimit);
		}
		
		else if (lowerLimit != null) {
			b.append("above ");
			b.append(lowerLimit);
		}
		else if (upperLimit != null) {
			b.append("below ");
			b.append(upperLimit);
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
			
		b.append("Value ");
		
		if (lowerLimit != null && upperLimit != null) {
			b.append("between ");
			b.append(lowerLimit);
			b.append(" and ");
			b.append(upperLimit);
		}
		
		else if (lowerLimit != null) {
			b.append("above ");
			b.append(lowerLimit);
		}
		else if (upperLimit != null) {
			b.append("below ");
			b.append(upperLimit);
		}
		
		return b.toString();
	}

	/**
	 * The ValuesFilterOptionPanel.
	 */
	private class ValuesFilterOptionPanel extends JPanel implements ListSelectionListener, KeyListener, ActionListener {
			
			private JList dataList;
			private JTextField lowerLimitField;
			private JTextField upperLimitField;
			private JComboBox limitTypeBox;
			private JTextField chosenNumberField;
			private JLabel dataAvailableNumber;
			
			/**
			 * Instantiates a new values filter option panel.
			 */
			public ValuesFilterOptionPanel () {
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

				JScrollPane scrollPane = new JScrollPane(dataList);
				scrollPane.setPreferredSize(new Dimension(200,dataList.getPreferredSize().height));
				dataPanel.add(scrollPane,BorderLayout.CENTER);

				add(dataPanel,BorderLayout.WEST);

				JPanel choicePanel = new JPanel();
				choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


				JPanel choicePanel2 = new JPanel();
				choicePanel2.add(new JLabel("Value must be between "));
				lowerLimitField = new JTextField(3);
				lowerLimitField.addKeyListener(this);
				choicePanel2.add(lowerLimitField);

				choicePanel2.add(new JLabel(" and "));

				upperLimitField = new JTextField(3);
				upperLimitField.addKeyListener(this);
				choicePanel2.add(upperLimitField);
				choicePanel.add(choicePanel2);

				JPanel choicePanel3 = new JPanel();
				choicePanel3.add(new JLabel(" for "));

				limitTypeBox = new JComboBox(new String [] {"Exactly","At least","No more than"});
				limitTypeBox.addActionListener(this);
				choicePanel3.add(limitTypeBox);

				chosenNumber = dataList.getSelectedIndices().length;
				chosenNumberField = new JTextField(""+chosenNumber,3);
				chosenNumberField.addKeyListener(this);
				choicePanel3.add(chosenNumberField);

				choicePanel3.add(new JLabel(" of the "));

				dataAvailableNumber = new JLabel("");
				valueChanged(null);
				choicePanel3.add(dataAvailableNumber);

				choicePanel3.add(new JLabel(" selected Data Stores "));

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
					if (f == lowerLimitField) {
						if (f.getText().length() == 0) {
							lowerLimit = null;
						}
						else if (f.getText().equals("-")) {
							lowerLimit = 0f;
						}
						else {
							lowerLimit = Float.parseFloat(f.getText());
						}
					}
					else if (f == upperLimitField) {
						if (f.getText().length() == 0) {
							upperLimit = null;
						}
						else if (f.getText().equals("-")) {
							upperLimit = 0f;
						}
						else {
							upperLimit = Float.parseFloat(f.getText());
						}
					}
					else if (f == chosenNumberField) {
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
				Object [] o = dataList.getSelectedValues();
				stores = new DataStore[o.length];
				for (int i=0;i<o.length;i++) {
					stores[i] = (DataStore)o[i];
				}
				dataAvailableNumber.setText(""+dataList.getSelectedIndices().length);
				
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
