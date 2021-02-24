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

/**
 * The Class AnnotationTrackSelector allows the user to specify which 
 * annotation tracks should be displayed in the chromosome view.
 */
public class AnnotationTrackSelector extends JDialog implements ActionListener, ListSelectionListener {
	
	/** The application. */
	private SeqMonkApplication application;
	
	/** The available model. */
	private DefaultListModel availableModel = new DefaultListModel();
	
	/** The available list. */
	private JList availableList;
	
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
	 * Instantiates a new annotation track selector.
	 * 
	 * @param application the application
	 */
	public AnnotationTrackSelector (SeqMonkApplication application) {
		super(application,"Select Annotation Tracks");
		setSize(600,400);
		setLocationRelativeTo(application);
		
		this.application = application;
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.weightx=0.9;
		c.weighty=0.9;
		c.fill = GridBagConstraints.BOTH;
		
		JPanel availablePanel = new JPanel();
		availablePanel.setLayout(new BorderLayout());
		availablePanel.add(new JLabel("Available Tracks",JLabel.CENTER),BorderLayout.NORTH);
		availableList = new JList(availableModel);
		availableList.addListSelectionListener(this);
		availablePanel.add(new JScrollPane(availableList),BorderLayout.CENTER);
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
		usedPanel.add(new JScrollPane(usedList),BorderLayout.CENTER);
		getContentPane().add(usedPanel,c);
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth=3;
		c.weighty = 0.1;
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
		
		String [] availableTypes = application.dataCollection().genome().annotationCollection().listAvailableFeatureTypes();
		for (int i=0;i<availableTypes.length;i++) {
			availableModel.addElement(availableTypes[i]);
		}
		
		String [] drawnTypes = application.drawnFeatureTypes();
		for (int i=0;i<drawnTypes.length;i++) {
			availableModel.removeElement(drawnTypes[i]);
			usedModel.addElement(drawnTypes[i]);
		}
		
		setVisible(true);
		
		// A bif of a hack to make sure both lists
		// resize as they should...
		availableModel.addElement("temp");
		availableModel.removeElement("temp");
		usedModel.addElement("temp");
		usedModel.removeElement("temp");
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String c = ae.getActionCommand();

		if (c.equals("add")) {
			Object [] adds = availableList.getSelectedValues();
			for (int i=0;i<adds.length;i++) {
				usedModel.addElement(adds[i]);
				availableModel.removeElement(adds[i]);
			}
		}
		else if (c.equals("remove")) {
			Object [] adds = usedList.getSelectedValues();
			for (int i=0;i<adds.length;i++) {
				usedModel.removeElement(adds[i]);
				availableModel.addElement(adds[i]);
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
			String [] s = new String [o.length];
			for (int i=0;i<s.length;i++) {
				s[i] = (String)o[i];
			}
			application.setDrawnFeatureTypes(s);
			setVisible(false);
			dispose();
		}
		
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent ae) {
		// Check to see if we can add anything...
		if (availableList.getSelectedIndices().length>0) {
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
