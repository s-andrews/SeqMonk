/**
 * Copyright Copyright 2010- 21 Simon Andrews
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

import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The Class OrderedListSelector provides a means to select a set of
 * probe lists in a specific order, initially for use in the scatterplot
 * dialog but we can probably use it elsewhere as well.
 */
public class OrderedListSelector extends JDialog implements ActionListener, ListSelectionListener {
		
	/** The available set model. */
	private DefaultListModel availableListsModel = new DefaultListModel();

	/** The available set list. */
	private JList availableListList;
	
	/** The final list of selected lists */
	private ProbeList [] orderedLists = null;

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
	
	/**
	 * Instantiates a new ordered list selector.
	 * 
	 * @param application the application
	 */
	public OrderedListSelector (JDialog parent, ProbeList parentList, ProbeList [] initiallySelectedLists) {
		super(parent,"Select Probe Lists");
		setSize(600,400);
		setLocationRelativeTo(parent);
		setModal(true);
		
		if (initiallySelectedLists == null) initiallySelectedLists = new ProbeList[0];
		
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
		availablePanel.add(new JLabel("Available Lists",JLabel.CENTER),c2);
		
		c2.gridy++;
		c2.weighty=1;
		c2.fill=GridBagConstraints.BOTH;

		availableListList = new JList(availableListsModel);
		availableListList.addListSelectionListener(this);
		availablePanel.add(new JScrollPane(availableListList),c2);
		
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
		usedPanel.add(new JLabel("Selected Lists",JLabel.CENTER),BorderLayout.NORTH);
		usedList = new JList(usedModel);
		usedList.addListSelectionListener(this);
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
		
		// Populate the initially selected lists
		for (int i=0;i<initiallySelectedLists.length;i++) {
			usedModel.addElement(initiallySelectedLists[i]);
		}
		
		// Populate the available lists, but missing out the initially
		// selected lists
		ProbeList [] availableLists = parentList.getAllProbeLists();
		AVAILABLE: for (int i=0;i<availableLists.length;i++) {
			for (int j=0;j<initiallySelectedLists.length;j++) {
				if (availableLists[i] == initiallySelectedLists[j]) {
					continue AVAILABLE;
				}
			}
			availableListsModel.addElement(availableLists[i]);
		}
		
		setVisible(true);
		
		// A bit of a hack to make sure all lists
		// resize as they should...
		availableListsModel.addElement("temp");
		usedModel.addElement("temp");
		validate();
		availableListsModel.removeElement("temp");
		usedModel.removeElement("temp");
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String c = ae.getActionCommand();

		if (c.equals("add")) {
			Object [] adds = availableListList.getSelectedValues();
			for (int i=0;i<adds.length;i++) {
				usedModel.addElement(adds[i]);
				availableListsModel.removeElement(adds[i]);
			}
		}
		else if (c.equals("remove")) {
			Object [] removes = usedList.getSelectedValues();
			for (int i=0;i<removes.length;i++) {
				usedModel.removeElement(removes[i]);
				availableListsModel.addElement(removes[i]);
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
			orderedLists = new ProbeList [o.length];
			for (int i=0;i<o.length;i++) {
				orderedLists[i] = (ProbeList)o[i];
			}
			setVisible(false);
		}
		
	}
	
	public ProbeList [] getOrderedLists () {
		return orderedLists;
	}
	

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent ae) {
		// Check to see if we can add anything...
		if (availableListList.getSelectedIndices().length>0) {
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
