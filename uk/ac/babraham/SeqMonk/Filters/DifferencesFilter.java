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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;

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
 * Filters probes based on the absolute differences between two
 * or more dataStores.
 */
public class DifferencesFilter extends ProbeFilter {

	public static final int MAXIMUM = 1;
	public static final int MINIMUM = 2;
	public static final int AVERAGE = 3;
	
	private int differenceType = AVERAGE;
	private Double lowerLimit = null;
	private Double upperLimit = null;
	
	private DataStore [] fromStores = new DataStore[0];
	private DataStore [] toStores = new DataStore[0];
	
	private final DifferencesOptionsPanel optionsPanel = new DifferencesOptionsPanel();
	
	/**
	 * Instantiates a new differences filter with default options.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public DifferencesFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList() {
		
						
		Probe [] probes = startingList.getAllProbes();
		ProbeList newList = new ProbeList(startingList,"Filtered Probes","","Difference");
		
		HashSet<DataStore> allStoresSet = new HashSet<DataStore>();
		for (int i=0;i<fromStores.length;i++) {
			allStoresSet.add(fromStores[i]);
		}
		for (int i=0;i<toStores.length;i++) {
			allStoresSet.add(toStores[i]);
		}
		
		DataStore [] allStores = allStoresSet.toArray(new DataStore[0]);
		
		
		PROBE: for (int p=0;p<probes.length;p++) {
			
			progressUpdated(p, probes.length);
			
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}

			// We do a quick check to test that this probe has a value in all of the datasets
			// and that that value isn't NaN.  If either of these fails then the probe can
			// never pass this filter.
			
			for (int s=0;s<allStores.length;s++) {
				if (!allStores[s].hasValueForProbe(probes[p])) continue PROBE;
				try {
					if (Float.isNaN(allStores[s].getValueForProbe(probes[p]))) continue PROBE;
				}
				catch (SeqMonkException sme){continue;}
			}
			
			
			int count = 0;
			float d = 0;
			for (int fromIndex=0;fromIndex<fromStores.length;fromIndex++) {
				for (int toIndex=0;toIndex<toStores.length;toIndex++) {
					if (fromStores[fromIndex]==toStores[toIndex]) continue;
					switch (differenceType) {
					case AVERAGE:
						d+= getDifferenceValue(toStores[toIndex],fromStores[fromIndex],probes[p]);
						count++;
						break;
					case MAXIMUM:
						float dt1 = getDifferenceValue(toStores[toIndex],fromStores[fromIndex],probes[p]);
						if (count == 0 || dt1 > d)
							d = dt1;
						count++;
						break;
					case MINIMUM:
						float dt2 = getDifferenceValue(toStores[toIndex],fromStores[fromIndex],probes[p]);
						if (count == 0 || dt2 < d)
							d = dt2;
						count++;
						break;
					default:
						progressExceptionReceived(new SeqMonkException("Unknown difference type "+differenceType));
					}
						
				}	
			}

			if (differenceType == AVERAGE) {
				if (count > 0) {
					d/=count;
				}
			}
			
			// Now we have the value we need to know if it passes the test
			if (upperLimit != null)
				if (d > upperLimit) {
					continue;
				}
			
			if (lowerLimit != null)
				if (d < lowerLimit) {
					continue;
				}			
			newList.addProbe(probes[p],new Float(d));
		}
		
		filterFinished(newList);		
	}
	
	/**
	 * Gets the absolute difference for a probe between two data stores.
	 * 
	 * @param s1 The first data store
	 * @param s2 The second data store
	 * @param p The probe to compare
	 * @return The absolute difference value
	 */
	private float getDifferenceValue (DataStore s1, DataStore s2, Probe p) {
		float d=0;
		
		try {
			d = s1.getValueForProbe(p)-s2.getValueForProbe(p);
		} 
		catch (SeqMonkException e) {
		}
				
		return d;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the differnce between stores";
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
		// Check for the special case of comparing just to ourselves
		if (fromStores.length==1 && toStores.length==1 && fromStores[0]==toStores[0]) {
			return false;
		}
		if (fromStores.length > 0 && toStores.length > 0 && (lowerLimit != null || upperLimit != null)) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Differences Filter";
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
		if (differenceType == AVERAGE) {
			b.append("average ");
		}
		else if (differenceType == MINIMUM) {
			b.append("minimum ");
		}
		else if (differenceType == MAXIMUM) {
			b.append("maximum ");
		}
		
		b.append("difference when comparing ");
				
		for (int s=0;s<fromStores.length;s++) {
			b.append(fromStores[s].name());
			if (s < fromStores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" to ");
		
		for (int s=0;s<toStores.length;s++) {
			b.append(toStores[s].name());
			if (s < toStores.length-1) {
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
			
		b.append("Difference ");
		
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
	 * The DifferencesOptionsPanel.
	 */
	private class DifferencesOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ItemListener {
	
		private JList fromDataList;
		private JList toDataList;
		private JComboBox combineTypeBox;
		private JTextField lowerLimitField;
		private JTextField upperLimitField;
	
		/**
		 * Instantiates a new differences options panel.
		 */
		public DifferencesOptionsPanel () {
		
			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			dataPanel.setLayout(new GridBagLayout());
			GridBagConstraints dpgbc = new GridBagConstraints();
			dpgbc.gridx=0;
			dpgbc.gridy=0;
			dpgbc.weightx=0.5;
			dpgbc.weighty=0.01;
			dpgbc.fill=GridBagConstraints.BOTH;
			
			dataPanel.add(new JLabel("From Data Store / Group",JLabel.CENTER),dpgbc);
	
			DefaultListModel fromDataModel = new DefaultListModel();
			DefaultListModel toDataModel = new DefaultListModel();
	
			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					fromDataModel.addElement(stores[i]);
					toDataModel.addElement(stores[i]);
				}
			}
	
			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			fromDataList = new JList(fromDataModel);
			ListDefaultSelector.selectDefaultStores(fromDataList);
			fromDataList.setCellRenderer(new TypeColourRenderer());
			fromDataList.addListSelectionListener(this);
			JScrollPane fromScrollPane = new JScrollPane(fromDataList);
			fromScrollPane.setPreferredSize(new Dimension(200,fromDataList.getPreferredSize().height));

			dataPanel.add(fromScrollPane,dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.01;

			dataPanel.add(new JLabel("To Data Store / Group",JLabel.CENTER),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			toDataList = new JList(fromDataModel);
			ListDefaultSelector.selectDefaultStores(toDataList);
			toDataList.setCellRenderer(new TypeColourRenderer());
			toDataList.addListSelectionListener(this);
			
			JScrollPane toScrollPane = new JScrollPane(toDataList);
			toScrollPane.setPreferredSize(new Dimension(200,toDataList.getPreferredSize().height));
			
			dataPanel.add(toScrollPane,dpgbc);
			
			
			add(dataPanel,BorderLayout.WEST);
	
			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));
	
			JPanel choicePanel1 = new JPanel();
	
			combineTypeBox = new JComboBox(new String [] {"Average","Maximum","Minimum"});
			combineTypeBox.setEnabled(false);
			combineTypeBox.addItemListener(this);
			this.valueChanged(null); // Trigger it to change this to the correct value
			choicePanel1.add(combineTypeBox);
	
			choicePanel1.add(new JLabel("difference in quantitated value"));
			choicePanel.add(choicePanel1);
	
			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel(" must be between "));
			lowerLimitField = new JTextField(3);
			lowerLimitField.addKeyListener(this);
			choicePanel2.add(lowerLimitField);
	
			choicePanel2.add(new JLabel(" and "));
	
			upperLimitField = new JTextField(3);
			upperLimitField.addKeyListener(this);
			choicePanel2.add(upperLimitField);
			choicePanel.add(choicePanel2);
	
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

			Double d = null;
			
			if (f.getText().length()>0) {
			
				if (f.getText().equals("-")) {
					d = 0d;
				}
				else {
					try {
						d = Double.parseDouble(f.getText());
					}
					catch (NumberFormatException e) {
						f.setText(f.getText().substring(0,f.getText().length()-1));
						return;
					}
				}
			}
			
			if (f == lowerLimitField) {
				lowerLimit = d;
			}
			else if (f == upperLimitField) {
				upperLimit = d;
			}
			else {
				System.err.println("Unexpected text field "+f+" sending data to keylistener in differences filter");
			}
			optionsChanged();
			
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {
			
			// Look at the limit type and convert to a char
			String l = (String)combineTypeBox.getSelectedItem();
			if (l.equals("Average"))
				differenceType = DifferencesFilter.AVERAGE;
			else if (l.equals("Maximum"))
				differenceType = DifferencesFilter.MAXIMUM;
			else if (l.equals("Minimum"))
				differenceType = DifferencesFilter.MINIMUM;
			else {
				System.err.println("Didn't recognise limit type "+l);
			}
			optionsChanged();
			
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			// If we have 2 or less items selected then we can disable the
			// combobox which says whether we're looking at min, max or average
			// difference (since they're all the same with only 1 comparison)
			
			Object [] fromSelectedObjects = fromDataList.getSelectedValues();
			Object [] toSelectedObjects = toDataList.getSelectedValues();
			
			if (fromSelectedObjects.length == 0 || toSelectedObjects.length == 0) {
				combineTypeBox.setEnabled(false);
			}
			else if (fromSelectedObjects.length <2 && toSelectedObjects.length < 2) {
				combineTypeBox.setEnabled(false);
			}
			else {
				combineTypeBox.setEnabled(true);
			}

			
			DataStore [] newFromStores = new DataStore[fromSelectedObjects.length];
			for (int i=0;i<fromSelectedObjects.length;i++){
				newFromStores[i] = (DataStore)fromSelectedObjects[i];
			}
			fromStores = newFromStores;

			DataStore [] newToStores = new DataStore[toSelectedObjects.length];
			for (int i=0;i<toSelectedObjects.length;i++){
				newToStores[i] = (DataStore)toSelectedObjects[i];
			}
			toStores = newToStores;

			optionsChanged();
		}
	}
	
}
