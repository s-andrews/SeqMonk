/**
 * Copyright Copyright 2010-19 Simon Andrews and 2009 Ieuan Clay
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
package uk.ac.babraham.SeqMonk.Quantitation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;

/**
 * A quantitation which corrects all dataStores relative to a reference store.
 */
public class RelativeQuantitation extends Quantitation implements ItemListener, ListSelectionListener{

	private JPanel optionPanel = null;
	private JList referenceList;
	private JComboBox diffTypeBox;
	private boolean subtract;
	private boolean logTransform;
	private DataStore [] data;
	private ReferencePairingTableModel tableModel;

	/** The lowest value allowed when doing a log ratio (to avoid infinite values) */
	private static final float THRESHOLD = 0.001f;

	public RelativeQuantitation(SeqMonkApplication application) {
		super(application);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();

		DataStore [] targets = tableModel.getTargets();
		DataStore [] references = tableModel.getReferences();

		float [] refValues = new float[references.length];

		try {
			for (int p=0;p<probes.length;p++) {

				// See if we need to quit
				if (cancel) {
					progressCancelled();
					return;
				}

				// First get the reference values
				for (int i=0;i<references.length;i++) {
					refValues[i] = references[i].getValueForProbe(probes[p]);
				}


				progressUpdated(p, probes.length);

				for (int r=0;r<targets.length;r++) {

					float referenceValue = refValues[r];
					float probeValue;

					if ( (! subtract) && (referenceValue <= THRESHOLD) ) {
						// if reference value if less than the threshold and we are dividing, set reference value to threshold
						// this way avoiding compile time errors
						referenceValue = THRESHOLD;
					}

					probeValue = targets[r].getValueForProbe(probes[p]);


					float value;
					if (subtract) {
						value = probeValue - referenceValue;
					}
					else {
						value = probeValue/referenceValue;
					}

					if (value == Float.POSITIVE_INFINITY || value == Float.NEGATIVE_INFINITY) {
						throw new IllegalStateException("Infinite value when dividing by "+referenceValue+" limit is "+THRESHOLD);
					}

					if (logTransform) {
						if (value <= 0) {
							// We don't want an infinite value so make it
							// just very small.

							value = -10f; // 0.0001
						}
						else {
							value = (float)Math.log(value)/log2;
						}
					}
					targets[r].setValueForProbe(probes[p], value);

				}
			}
		}
		catch (SeqMonkException sme) {
			throw new IllegalStateException(sme);
		}

		quantitatonComplete();

	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {		
		if (optionPanel != null) {
			// We've done this already
			return optionPanel;
		}

		optionPanel = new JPanel();
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.01;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2, 2, 2, 2);

		optionPanel.add(new JLabel("Reference"),gbc);

		gbc.gridx = 2;

		optionPanel.add(new JLabel("Data Stores"),gbc);

		gbc.gridy++;
		gbc.gridx=1;
		gbc.weighty=0.99;
		gbc.fill = GridBagConstraints.BOTH;


		DefaultListModel dataModel = new DefaultListModel();

		DataStore [] stores = application.dataCollection().getAllDataStores();
		Vector<DataStore> quantitatedStores = new Vector<DataStore>();

		for (int i=0;i<stores.length;i++) {
			if (stores[i].isQuantitated()) {
				dataModel.addElement(stores[i]);
				if (! (stores[i] instanceof ReplicateSet)) {
					quantitatedStores.add(stores[i]);
				}
			}
		}

		data = quantitatedStores.toArray(new DataStore[0]);

		referenceList = new JList(dataModel);
		// only allow one to be selected
		referenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		referenceList.setCellRenderer(new TypeColourRenderer());
		referenceList.addListSelectionListener(this);

		optionPanel.add(new JScrollPane(referenceList),gbc);

		gbc.gridx=2;

		tableModel = new ReferencePairingTableModel();

		JTable refTable = new JTable(tableModel);

		optionPanel.add(new JScrollPane(refTable),gbc);


		gbc.gridx=1;
		gbc.gridy++;
		gbc.weighty = 0.01;
		gbc.fill = GridBagConstraints.NONE;


		JButton applyToSelectionButton = new JButton("Apply to Selected");
		applyToSelectionButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object value = referenceList.getSelectedValue();
				//				if (value == null) return;
				tableModel.setReference((DataStore)value);
			}
		});
		optionPanel.add(applyToSelectionButton,gbc);

		gbc.gridx=2;

		JButton selectAllButton = new JButton("Apply to All");
		selectAllButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				tableModel.selectAll();
				Object value = referenceList.getSelectedValue();
				tableModel.setReference((DataStore)value);
			}
		});

		//		applyToSelectionButton.addActionListener(this);
		optionPanel.add(selectAllButton,gbc);


		// define relative quantitation method
		gbc.gridx = 1;
		gbc.gridy++;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		optionPanel.add(new JLabel("Reference method"),gbc);

		gbc.gridx = 2;
		diffTypeBox = new JComboBox(new String [] {"Minus","Log Divide", "Divide",});
		diffTypeBox.setSelectedIndex(2); // default : divide
		optionPanel.add(diffTypeBox, gbc);

		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}	

	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		return existingDescription+" transformed by relative quantitation using "+diffTypeBox.getSelectedItem().toString()+" with pairs ";
		// TODO: add pairing information?
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {

		//		this.data = data;

		// Combine type
		if (diffTypeBox.getSelectedItem().equals("Minus")) {
			subtract = true;
			logTransform = false;
		}
		else if (diffTypeBox.getSelectedItem().equals("Log Divide")) {
			subtract = false;
			logTransform = true;
		}
		else if (diffTypeBox.getSelectedItem().equals("Divide")) {
			subtract = false;
			logTransform = false;
		}

		//		quantitateReference = quantitateReferenceBox.isSelected();

		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent ie) {
		optionsChanged();
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {


		optionsChanged();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Relative Quantitation";
	}


	private class ReferencePairingTableModel extends AbstractTableModel {

		private DataStore [] refs;
		private boolean [] selected;

		public ReferencePairingTableModel () {
			refs = new DataStore[data.length];
			selected = new boolean[data.length];
		}

		public boolean isCellEditable (int r, int c) {
			return c==0;
		}

		public void setValueAt (Object value, int r, int c) {
			if (c==0) {
				selected[r] = (Boolean)value;
			}
		}

		public int getColumnCount() {
			return 3;
		}

		public int getRowCount() {
			return data.length;
		}

		public String getColumnName (int c) {
			switch(c) {
			case 0: return "Select";
			case 1: return "Store";
			case 2: return "Reference";
			}

			return null;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Class getColumnClass (int c) {
			switch(c) {
			case 0: return Boolean.class;
			case 1: return DataStore.class;
			case 2: return DataStore.class;
			}

			return null;

		}



		public Object getValueAt(int r, int c) {
			switch(c) {
			case 0: return selected[r];
			case 1: return data[r];
			case 2: return refs[r];
			}

			return null;
		}

		public void selectAll () {
			for (int i=0;i<selected.length;i++) {
				selected[i] = true;
				fireTableCellUpdated(i, 0);
			}
		}

		public void setReference(DataStore d) {
			for (int i=0;i<selected.length;i++) {
				if (selected[i]) {
					refs[i] = d;
					selected[i] = false;
					fireTableCellUpdated(i, 2);
					fireTableCellUpdated(i, 0);
				}
			}
		}

		public DataStore [] getTargets () {
			Vector<DataStore> targets = new Vector<DataStore>();

			for (int i=0;i<data.length;i++) {
				if (refs[i] != null) {
					targets.add(data[i]);
				}
			}

			return targets.toArray(new DataStore[0]);
		}

		public DataStore [] getReferences () {
			Vector<DataStore> references = new Vector<DataStore>();

			for (int i=0;i<data.length;i++) {
				if (refs[i] != null) {
					references.add(refs[i]);
				}
			}

			return references.toArray(new DataStore[0]);

		}


	}


}