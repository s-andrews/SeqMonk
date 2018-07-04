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
package uk.ac.babraham.SeqMonk.Dialogs.DataTrackSelector;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;

/**
 * The Class DataTrackSelector provides a means to select the data
 * tracks which are shown in the chromosome view.
 */
public class DataTrackSelector extends JDialog implements ActionListener, ListSelectionListener {
	
	/** The application. */
	private SeqMonkApplication application;
	
	/** The available group model. */
	private DataStoreListModel availableGroupModel = new DataStoreListModel();
	
	/** The available set model. */
	private DataStoreListModel availableSetModel = new DataStoreListModel();
	
	/** The available replicate model */
	private DataStoreListModel availableReplicatesModel = new DataStoreListModel();
	
	/** The available group list. */
	private JList availableGroupList;
	
	/** The available set list. */
	private JList availableSetList;
	
	/** The available replicate list */
	private JList availableReplicateList;
	
	/** The used model. */
	private DataStoreListModel usedModel = new DataStoreListModel();
	
	/** The used list. */
	private JList usedList;
	
	/** The add button. */
	private JButton addButton;
	
	/** The remove button. */
	private JButton removeButton;
	
	/** The up button. */
	private JButton upButton;
	
	/** The down button. */
	private JButton downButton;
	
	/** The renderer. */
	private TypeColourRenderer renderer = new TypeColourRenderer();
	
	/**
	 * Instantiates a new data track selector.
	 * 
	 * @param application the application
	 */
	public DataTrackSelector (SeqMonkApplication application) {
		super(application,"Select Data Tracks");
		setSize(600,400);
		setLocationRelativeTo(application);
		
		this.application = application;
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2,2,2,2);
		c.gridx=0;
		c.gridy=0;
		c.weightx=0.9;
		c.weighty=0.9;
		c.fill = GridBagConstraints.BOTH;
		
		JPanel availablePanel = new JPanel();
		availablePanel.setLayout(new GridBagLayout());
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx=0;
		c2.gridy=0;
		c2.weightx=1;
		c2.weighty=0.01;
		c2.fill = GridBagConstraints.HORIZONTAL;
		availablePanel.add(new JLabel("Available Tracks",JLabel.CENTER),c2);
		
		c2.gridy++;
		availablePanel.add(new JLabel("Data Sets",JLabel.LEFT),c2);

		c2.gridy++;
		c2.weighty=1;
		c2.fill=GridBagConstraints.BOTH;

		availableSetList = new JList(availableSetModel);
		availableSetList.addListSelectionListener(this);
		availableSetList.setCellRenderer(renderer);
		availablePanel.add(new JScrollPane(availableSetList),c2);

		c2.gridy++;
		c2.weighty=0.01;
		c2.fill=GridBagConstraints.HORIZONTAL;
		availablePanel.add(new JLabel("Data Groups",JLabel.LEFT),c2);
		
		c2.gridy++;
		c2.weighty=1;
		c2.fill=GridBagConstraints.BOTH;
		
		availableGroupList = new JList(availableGroupModel);
		availableGroupList.addListSelectionListener(this);
		availableGroupList.setCellRenderer(renderer);
		availablePanel.add(new JScrollPane(availableGroupList),c2);
		
		c2.gridy++;
		c2.weighty=0.01;
		c2.fill=GridBagConstraints.HORIZONTAL;
		availablePanel.add(new JLabel("Replicate Sets",JLabel.LEFT),c2);

		c2.gridy++;
		c2.weighty=1;
		c2.fill=GridBagConstraints.BOTH;

		availableReplicateList = new JList(availableReplicatesModel);
		availableReplicateList.addListSelectionListener(this);
		availableReplicateList.setCellRenderer(renderer);
		availablePanel.add(new JScrollPane(availableReplicateList),c2);

		
		getContentPane().add(availablePanel,c);

		c.gridx++;
		c.weightx = 0.2;
		c.fill = GridBagConstraints.NONE;
		
		JPanel middlePanel = new JPanel();
		middlePanel.setLayout(new GridLayout(4,1));
		addButton = new JButton("Add");
		addButton.setActionCommand("add");
		addButton.addActionListener(this);
		addButton.setEnabled(false);
		middlePanel.add(addButton);
		
		removeButton = new JButton("Remove");
		removeButton.setActionCommand("remove");
		removeButton.addActionListener(this);
		removeButton.setEnabled(false);
		middlePanel.add(removeButton);
		
		upButton = new JButton("Move up");
		upButton.setActionCommand("up");
		upButton.addActionListener(this);
		upButton.setEnabled(false);
		middlePanel.add(upButton);
		
		downButton = new JButton("Move down");
		downButton.setActionCommand("down");
		downButton.addActionListener(this);
		downButton.setEnabled(false);
		middlePanel.add(downButton);
		
		getContentPane().add(middlePanel,c);
		
		c.gridx++;
		c.weightx = 0.9;
		c.fill = GridBagConstraints.BOTH;

		JPanel usedPanel = new JPanel();
		usedPanel.setLayout(new BorderLayout());
		usedPanel.add(new JLabel("Displayed Tracks",JLabel.CENTER),BorderLayout.NORTH);
		usedList = new JList(usedModel);
		usedList.addListSelectionListener(this);
		usedList.setCellRenderer(renderer);
		usedPanel.add(new JScrollPane(usedList),BorderLayout.CENTER);
		getContentPane().add(usedPanel,c);
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth=3;
		c.weighty = 0.01;
		c.fill = GridBagConstraints.NONE;

		JPanel bottomPanel = new JPanel();
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		bottomPanel.add(cancelButton);
		
		JButton okButton = new JButton("OK");
		okButton.setActionCommand("ok");
		okButton.addActionListener(this);
		getRootPane().setDefaultButton(okButton);
		bottomPanel.add(okButton);
		
		getContentPane().add(bottomPanel,c);
		
		DataSet [] availableSets = application.dataCollection().getAllDataSets();
		availableSetModel.addElements(availableSets);
		
		DataGroup [] availableGroups = application.dataCollection().getAllDataGroups();
		availableGroupModel.addElements(availableGroups);
		
		ReplicateSet [] availableReplicates = application.dataCollection().getAllReplicateSets();
		availableReplicatesModel.addElements(availableReplicates);
		
		DataStore [] drawnStores = application.drawnDataStores();
		Vector<DataStore> drawnSets = new Vector<DataStore>();
		Vector<DataStore> drawnGroups = new Vector<DataStore>();
		Vector<DataStore> drawnReplicates = new Vector<DataStore>();
		
		
		for (int i=0;i<drawnStores.length;i++) {
			if (drawnStores[i] instanceof DataSet) {
				drawnSets.add(drawnStores[i]);
			}
			if (drawnStores[i] instanceof ReplicateSet) {
				drawnReplicates.add(drawnStores[i]);
			}
			else {
				drawnGroups.add(drawnStores[i]);				
			}
		}
		
		availableGroupModel.removeElements(drawnGroups.toArray(new DataStore[0]));
		availableSetModel.removeElements(drawnSets.toArray(new DataStore[0]));
		availableReplicatesModel.removeElements(drawnReplicates.toArray(new DataStore[0]));
		
		usedModel.addElements(drawnStores);
		
		setVisible(true);
		
		// A bit of a hack to make sure all lists
		// resize as they should...
//		availableSetModel.addElement("temp");
//		availableGroupModel.addElement("temp");
//		usedModel.addElement("temp");
//		availableReplicatesModel.addElement("temp");
//		validate();
//		availableSetModel.removeElement("temp");
//		availableGroupModel.removeElement("temp");
//		usedModel.removeElement("temp");
//		availableReplicatesModel.removeElement("temp");
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String c = ae.getActionCommand();

		if (c.equals("add")) {
			Object [] addObj = availableGroupList.getSelectedValues();
			DataStore [] adds = new DataStore[addObj.length];
			
			for (int i=0;i<adds.length;i++) {
				adds[i] = (DataStore)addObj[i];
			}
			
			usedModel.addElements(adds);
			availableGroupModel.removeElements(adds);
			
			usedList.setSelectedIndices(new int[0]);
			availableGroupList.setSelectedIndices(new int[0]);

			
			addObj = availableSetList.getSelectedValues();
			adds = new DataStore[addObj.length];

			for (int i=0;i<adds.length;i++) {
				adds[i] = (DataStore)addObj[i];
			}

			usedModel.addElements(adds);
			availableSetModel.removeElements(adds);
			availableSetList.setSelectedIndices(new int[0]);

			addObj = availableReplicateList.getSelectedValues();
			adds = new DataStore[addObj.length];

			for (int i=0;i<adds.length;i++) {
				adds[i] = (DataStore)addObj[i];
			}
			usedModel.addElements(adds);
			availableReplicatesModel.removeElements(adds);
			availableReplicateList.setSelectedIndices(new int[0]);

		}
		else if (c.equals("remove")) {
			
			Object [] removeObj = usedList.getSelectedValues();
			DataStore [] removes = new DataStore[removeObj.length];

			for (int i=0;i<removes.length;i++) {
				removes[i] = (DataStore)removeObj[i];
			}
			
			usedModel.removeElements(removes);
			usedList.setSelectedIndices(new int[0]);


			Vector<DataStore> removeSets = new Vector<DataStore>();
			Vector<DataStore> removeGroups = new Vector<DataStore>();
			Vector<DataStore> removeReps = new Vector<DataStore>();
			
			
			for (int i=0;i<removes.length;i++) {
				if (removes[i] instanceof DataSet) {
					removeSets.add(removes[i]);
				}
				else if (removes[i] instanceof ReplicateSet) {
					removeReps.add(removes[i]);
				}
				else if (removes[i] instanceof DataGroup){
					removeGroups.add(removes[i]);
				}
				else {
					throw new IllegalStateException("Unknown type of removed store "+removes[i]);
				}
			}
			availableSetModel.addElements(removeSets.toArray(new DataStore[0]));
			availableGroupModel.addElements(removeGroups.toArray(new DataStore[0]));
			availableReplicatesModel.addElements(removeReps.toArray(new DataStore[0]));
			availableSetList.setSelectedIndices(new int[0]);
			availableGroupList.setSelectedIndices(new int[0]);
			availableReplicateList.setSelectedIndices(new int[0]);

		}
		else if (c.equals("up")) {
			
			// Collect the list of selected indices
			int [] s = usedList.getSelectedIndices();
			Arrays.sort(s);
			
			// Get the set of objects associated with the selected indices
			DataStore [] current = new DataStore [s.length];
			
			for (int i=0;i<s.length;i++) {
				current[i] = usedModel.elementAt(s[i]);
			}
			
			// Get the object above, which is going to move below
			DataStore above = usedModel.elementAt(s[0]-1);
			
			// Move all the selected indices up one
			for (int i=0;i<s.length;i++) {
				usedModel.setElementAt(current[i],s[i]-1);
			}
			
			// Move the above object below the selected ones
			usedModel.setElementAt(above,s[s.length-1]);
			
			// Decrease the set of selected indices and select them again
			for (int i=0;i<s.length;i++) {
				s[i]--;
			}
			usedList.setSelectedIndices(s);
		}
		else if (c.equals("down")) {
			// Collect the list of selected indices
			int [] s = usedList.getSelectedIndices();
			Arrays.sort(s);
			
			// Get the set of objects associated with the selected indices
			DataStore [] current = new DataStore [s.length];
			
			for (int i=0;i<s.length;i++) {
				current[i] = usedModel.elementAt(s[i]);
			}
			
			// Get the object below, which is going to move below
			DataStore below = usedModel.elementAt(s[s.length-1]+1);
			
			// Move all the selected indices down one
			for (int i=0;i<s.length;i++) {
				usedModel.setElementAt(current[i],s[i]+1);
			}
			
			// Move the below object above the selected ones
			usedModel.setElementAt(below,s[0]);
			
			// Increase the set of selected indices and select them again
			for (int i=0;i<s.length;i++) {
				s[i]++;
			}
			usedList.setSelectedIndices(s);
		}
		else if (c.equals("cancel")) {
			setVisible(false);
			dispose();
		}
		else if (c.equals("ok")) {
			DataStore [] s = usedModel.getStores();
			application.setDrawnDataStores(s);
			setVisible(false);
			dispose();
		}
		
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent ae) {
		// Check to see if we can add anything...
		if (availableSetList.getSelectedIndices().length>0 || availableGroupList.getSelectedIndices().length >0 || availableReplicateList.getSelectedIndices().length>0) {
			addButton.setEnabled(true);
		}
		else {
			addButton.setEnabled(false);
		}
		
		//Or remove anything
		if (usedList.getSelectedIndices().length>0) {
			removeButton.setEnabled(true);
		}
		else {
			removeButton.setEnabled(false);
		}
		
		// If we can move up or down
		int [] indexList = usedList.getSelectedIndices();
		if (indexList.length != 0 && isListContiguous(indexList)) {
			Arrays.sort(indexList);
			if (indexList[0] == 0) {
				upButton.setEnabled(false);
			}
			else {
				upButton.setEnabled(true);
			}
			
			if (indexList[indexList.length-1] == usedList.getLastVisibleIndex()) {
				downButton.setEnabled(false);
			}
			else {
				downButton.setEnabled(true);
			}
			
		}
		else {
			upButton.setEnabled(false);
			downButton.setEnabled(false);
		}
	}
	
	private boolean isListContiguous (int [] list) {
		Arrays.sort(list);
		for (int i=1;i<list.length;i++) {
			if (list[i] != list[i-1]+1) {
				return false;
			}
		}
		return true;
	}
}
