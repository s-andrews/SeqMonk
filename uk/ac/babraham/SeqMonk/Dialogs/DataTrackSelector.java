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
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

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
	private DefaultListModel availableGroupModel = new DefaultListModel();
	
	/** The available set model. */
	private DefaultListModel availableSetModel = new DefaultListModel();
	
	/** The available replicate model */
	private DefaultListModel availableReplicatesModel = new DefaultListModel();
	
	/** The available group list. */
	private JList availableGroupList;
	
	/** The available set list. */
	private JList availableSetList;
	
	/** The available replicate list */
	private JList availableReplicateList;
	
	/** The used model. */
	private DefaultListModel usedModel = new DefaultListModel();
	
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
		for (int i=0;i<availableSets.length;i++) {
			availableSetModel.addElement(availableSets[i]);
		}
		
		DataGroup [] availableGroups = application.dataCollection().getAllDataGroups();
		for (int i=0;i<availableGroups.length;i++) {
			availableGroupModel.addElement(availableGroups[i]);
		}
		
		ReplicateSet [] availableReplicates = application.dataCollection().getAllReplicateSets();
		for (int i=0;i<availableReplicates.length;i++) {
			availableReplicatesModel.addElement(availableReplicates[i]);
		}
		
		DataStore [] drawnStores = application.drawnDataStores();
		for (int i=0;i<drawnStores.length;i++) {
			if (drawnStores[i] instanceof DataSet) {
				availableSetModel.removeElement(drawnStores[i]);
			}
			if (drawnStores[i] instanceof ReplicateSet) {
				availableReplicatesModel.removeElement(drawnStores[i]);
			}
			else {
				availableGroupModel.removeElement(drawnStores[i]);				
			}
			usedModel.addElement(drawnStores[i]);
		}
		
		setVisible(true);
		
		// A bit of a hack to make sure all lists
		// resize as they should...
		availableSetModel.addElement("temp");
		availableGroupModel.addElement("temp");
		usedModel.addElement("temp");
		availableReplicatesModel.addElement("temp");
		validate();
		availableSetModel.removeElement("temp");
		availableGroupModel.removeElement("temp");
		usedModel.removeElement("temp");
		availableReplicatesModel.removeElement("temp");
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String c = ae.getActionCommand();

		if (c.equals("add")) {
			//TODO: Do this by index so it's not horrifically slow.
			Object [] adds = availableGroupList.getSelectedValues();
			for (int i=0;i<adds.length;i++) {
				usedModel.addElement(adds[i]);
				availableGroupModel.removeElement(adds[i]);
			}
			adds = availableSetList.getSelectedValues();
			for (int i=0;i<adds.length;i++) {
				usedModel.addElement(adds[i]);
				availableSetModel.removeElement(adds[i]);
			}
			adds = availableReplicateList.getSelectedValues();
			for (int i=0;i<adds.length;i++) {
				usedModel.addElement(adds[i]);
				availableReplicatesModel.removeElement(adds[i]);
			}
		}
		else if (c.equals("remove")) {
			
			int [] removeIndices = usedList.getSelectedIndices();
			Object [] removes = usedList.getSelectedValues();

			Arrays.sort(removeIndices);
			
			for (int i=removeIndices.length-1;i>=0;i--) {
				usedModel.removeElementAt(removeIndices[i]);
			}
			
			for (int i=0;i<removes.length;i++) {
				if (removes[i] instanceof DataSet) {
					availableSetModel.addElement(removes[i]);
				}
				else if (removes[i] instanceof ReplicateSet) {
					availableReplicatesModel.addElement(removes[i]);
				}
				else if (removes[i] instanceof DataGroup){
					availableGroupModel.addElement(removes[i]);
				}
				else {
					throw new IllegalStateException("Unknown type of removed store "+removes[i]);
				}
			}			
		}
		else if (c.equals("up")) {
			
			// Collect the list of selected indices
			int [] s = usedList.getSelectedIndices();
			Arrays.sort(s);
			
			// Get the set of objects associated with the selected indices
			Object [] current = new Object [s.length];
			
			for (int i=0;i<s.length;i++) {
				current[i] = usedModel.elementAt(s[i]);
			}
			
			// Get the object above, which is going to move below
			Object above = usedModel.elementAt(s[0]-1);
			
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
			Object [] current = new Object [s.length];
			
			for (int i=0;i<s.length;i++) {
				current[i] = usedModel.elementAt(s[i]);
			}
			
			// Get the object below, which is going to move below
			Object below = usedModel.elementAt(s[s.length-1]+1);
			
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
			Object [] o = usedModel.toArray();
			DataStore [] s = new DataStore [o.length];
			for (int i=0;i<s.length;i++) {
				s[i] = (DataStore)o[i];
			}
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
