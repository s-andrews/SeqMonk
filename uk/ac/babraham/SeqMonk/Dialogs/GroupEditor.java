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

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;

/**
 * The Class GroupEditor sets the list and contents of DataGroups
 */
public class GroupEditor extends JDialog implements ActionListener, ListSelectionListener {
	
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
	
	/** The group model. */
	private DefaultListModel groupModel = new DefaultListModel();
	
	/** The group list. */
	private JList groupList;
	
	/** The add button. */
	private JButton addButton;
	
	/** The remove button. */
	private JButton removeButton;
	
	/** The new button. */
	private JButton newButton;
	
	/** The delete button. */
	private JButton deleteButton;
	
	/** The rename button. */
	private JButton renameButton;
	
	/**
	 * Instantiates a new group editor.
	 * 
	 * @param application the application
	 */
	public GroupEditor (SeqMonkApplication application) {
		super(application,"Edit Groups...");
		setSize(600,300);
		setLocationRelativeTo(application);
		
		this.application = application;
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.weightx=0.9;
		c.weighty=0.9;
		c.fill = GridBagConstraints.BOTH;
		
		JPanel groupPanel = new JPanel();
		groupPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx=0;
		gc.gridy=0;
		gc.weightx=0.5;
		gc.weighty=0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(2,2,2,2);
		groupPanel.add(new JLabel("Groups",JLabel.CENTER),gc);
		
		gc.gridy++;
		gc.weighty=1;
		gc.fill = GridBagConstraints.BOTH;
		
		groupList = new JList(groupModel);
		groupList.setCellRenderer(new TypeColourRenderer());
		groupList.addListSelectionListener(this);
		groupPanel.add(new JScrollPane(groupList),gc);

		gc.gridy++;
		gc.weighty=0;
		gc.fill = GridBagConstraints.HORIZONTAL;

		newButton = new JButton("Add New Group");
		newButton.setActionCommand("new_group");
		newButton.addActionListener(this);
		groupPanel.add(newButton,gc);
		
		gc.gridy++;
		deleteButton = new JButton("Delete Group");
		deleteButton.setActionCommand("delete_group");
		deleteButton.addActionListener(this);
		deleteButton.setEnabled(false);
		groupPanel.add(deleteButton,gc);
		
		gc.gridy++;
		renameButton = new JButton("Rename Group");
		renameButton.setActionCommand("rename_group");
		renameButton.addActionListener(this);
		renameButton.setEnabled(false);
		groupPanel.add(renameButton,gc);
		
		getContentPane().add(groupPanel,c);

		c.gridx++;
		
		JPanel usedPanel = new JPanel();
		usedPanel.setLayout(new BorderLayout());
		JLabel l = new JLabel("Used DataSets",JLabel.CENTER);
		l.setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
		usedPanel.add(l,BorderLayout.NORTH);
		usedList = new JList(usedModel);
		usedList.setCellRenderer(new TypeColourRenderer());
		usedList.addListSelectionListener(this);
		usedPanel.add(new JScrollPane(usedList),BorderLayout.CENTER);
		getContentPane().add(usedPanel,c);

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
				
		getContentPane().add(middlePanel,c);

		c.gridx++;
		c.weightx = 0.9;
		c.fill = GridBagConstraints.BOTH;
		
		
		JPanel availablePanel = new JPanel();
		availablePanel.setLayout(new BorderLayout());
		JLabel l2 = new JLabel("Unused DataSets",JLabel.CENTER);
		l2.setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
		availablePanel.add(l2,BorderLayout.NORTH);
		availableList = new JList(availableModel);
		availableList.setCellRenderer(new TypeColourRenderer());
		availableList.addListSelectionListener(this);
		availablePanel.add(new JScrollPane(availableList),BorderLayout.CENTER);
		getContentPane().add(availablePanel,c);

		
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth=4;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.NONE;

		JPanel bottomPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		bottomPanel.add(closeButton);
		
		getContentPane().add(bottomPanel,c);
		
		// Fill the lists with the data we know about
		DataSet [] dataSets = application.dataCollection().getAllDataSets();
		for (int i=0;i<dataSets.length;i++) {
			availableModel.addElement(dataSets[i]);
		}
		
		DataGroup [] dataGroups = application.dataCollection().getAllDataGroups();
		for (int i=0;i<dataGroups.length;i++) {
			groupModel.addElement(dataGroups[i]);
		}

		
		setVisible(true);
		
		// A bif of a hack to make sure all lists
		// resize as they should...
		availableModel.addElement("temp");
		usedModel.addElement("temp");
		groupModel.addElement("temp");
		validate();
		availableModel.removeElement("temp");
		usedModel.removeElement("temp");
		groupModel.removeElement("temp");
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
			
			Object [] o = usedModel.toArray();
			DataSet [] s = new DataSet [o.length];
			
			for (int i=0;i<s.length;i++) {
				s[i] = (DataSet)o[i];
			}
			
			DataGroup g = (DataGroup)groupList.getSelectedValue();
			g.setDataSets(s);
			
		}
		else if (c.equals("remove")) {
			Object [] adds = usedList.getSelectedValues();
			for (int i=0;i<adds.length;i++) {
				usedModel.removeElement(adds[i]);
				availableModel.addElement(adds[i]);
			}
			
			Object [] o = usedModel.toArray();
			DataSet [] s = new DataSet [o.length];
			
			for (int i=0;i<s.length;i++) {
				s[i] = (DataSet)o[i];
			}
			
			DataGroup g = (DataGroup)groupList.getSelectedValue();
			g.setDataSets(s);

		}

		else if (c.equals("new_group")) {
			
			String groupName=null;
			while (true) {
				groupName = (String)JOptionPane.showInputDialog(this,"Enter group name","Group Name",JOptionPane.QUESTION_MESSAGE,null,null,"New group");
				if (groupName == null)
					return;  // They cancelled
					
					
				if (groupName.length() == 0)
					continue; // Try again
				
				break;
			}

			DataGroup g = new DataGroup(groupName,new DataSet[0]);
			application.dataCollection().addDataGroup(g);
			groupModel.addElement(g);
		}
		
		else if (c.equals("rename_group")) {
			
			DataGroup g = (DataGroup)groupList.getSelectedValue();
			String groupName=null;
			while (true) {
				groupName = (String)JOptionPane.showInputDialog(this,"Enter group name","Group Name",JOptionPane.QUESTION_MESSAGE,null,null,g.name());
				if (groupName == null) return; // They cancelled
				if (groupName.length()>0) break;
			}
			g.setName(groupName);
			groupModel.setElementAt(g,groupList.getSelectedIndex());
		}

		else if (c.equals("delete_group")) {
			Object [] o = groupList.getSelectedValues();
			DataGroup [] dg = new DataGroup[o.length];
			for (int i=0;i<o.length;i++) {
				dg[i] = (DataGroup)o[i];
				groupModel.removeElement(o[i]);
			}
			application.dataCollection().removeDataGroups(dg);
		}
		
		else if (c.equals("close")) {
			setVisible(false);
			dispose();
		}		
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent ae) {
		// Check for a new group being selected
		if (ae.getSource() == groupList) {

			// Move all samples back to the available list
			Object [] o = usedModel.toArray();
			for (int i=0;i<o.length;i++) {
				availableModel.addElement(o[i]);
			}
			usedModel.removeAllElements();

			if(groupList.getSelectedValues().length == 1) {
				renameButton.setEnabled(true);
				deleteButton.setEnabled(true);
				DataGroup g = (DataGroup)groupList.getSelectedValue();
				DataSet [] s = g.dataSets();
				for (int i=0;i<s.length;i++) {
					usedModel.addElement(s[i]);
					availableModel.removeElement(s[i]);
				}
			}
			else if (groupList.getSelectedValues().length > 1) {
				deleteButton.setEnabled(true);
			}
			else {
				deleteButton.setEnabled(false);
			}
			
			
		}
		 
		
		// Check to see if we can add anything...
		if (availableList.getSelectedIndices().length>0 && groupList.getSelectedIndices().length == 1) {
			addButton.setEnabled(true);
		}
		else {
			addButton.setEnabled(false);
		}
		
		//Or remove anything
		if (usedList.getSelectedIndices().length>0 && groupList.getSelectedIndices().length == 1) {
			removeButton.setEnabled(true);
		}
		else {
			removeButton.setEnabled(false);
		}		
	}

}
