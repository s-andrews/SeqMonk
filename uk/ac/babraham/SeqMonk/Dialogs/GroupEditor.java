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
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.Dialogs.DataTrackSelector.DataStoreListModel;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.IntVector;

/**
 * The Class GroupEditor sets the list and contents of Data Groups
 */
public class GroupEditor extends JDialog implements ActionListener, ListSelectionListener {
	
	/** The application. */
	private SeqMonkApplication application;
	
	/** The available model. */
	private DataStoreListModel availableModel = new DataStoreListModel();
	
	/** The available list. */
	private JList availableList;
	
	/** The used model. */
	private DataStoreListModel usedModel = new DataStoreListModel();
	
	/** The used list. */
	private JList usedList;
	
	/** The group model. */
	private DataStoreListModel groupModel = new DataStoreListModel();
	
	/** The group list. */
	private JList groupList;
	
	/** The add button. */
	private JButton addButton;

	/** The add button. */
	private JButton selectNamedButton;	
	
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
		super(application,"Edit Data Groups...");
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

		newButton = new JButton("Add New Data Group");
		newButton.setActionCommand("new_set");
		newButton.addActionListener(this);
		groupPanel.add(newButton,gc);
		
		gc.gridy++;
		deleteButton = new JButton("Delete Data Group");
		deleteButton.setActionCommand("delete_set");
		deleteButton.addActionListener(this);
		deleteButton.setEnabled(false);
		groupPanel.add(deleteButton,gc);
		
		gc.gridy++;
		renameButton = new JButton("Rename Data Group");
		renameButton.setActionCommand("rename_set");
		renameButton.addActionListener(this);
		renameButton.setEnabled(false);
		groupPanel.add(renameButton,gc);
		
		getContentPane().add(groupPanel,c);

		c.gridx++;
		
		JPanel usedPanel = new JPanel();
		usedPanel.setLayout(new BorderLayout());
		JLabel l = new JLabel("Used DataStores",JLabel.CENTER);
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

		selectNamedButton = new JButton("Select Named");
		selectNamedButton.setActionCommand("select_named");
		selectNamedButton.addActionListener(this);
		middlePanel.add(selectNamedButton);
		
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
		JLabel l2 = new JLabel("Unused DataStores",JLabel.CENTER);
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
		DataStore [] dataStores = application.dataCollection().getAllDataSets();
		availableModel.addElements(dataStores);
		
		DataGroup [] dataGroups = application.dataCollection().getAllDataGroups();
		groupModel.addElements(dataGroups);

		
		setVisible(true);
		
		// A bif of a hack to make sure all lists
		// resize as they should...
//		availableModel.addElement("temp");
//		usedModel.addElement("temp");
//		groupModel.addElement("temp");
//		validate();
//		availableModel.removeElement("temp");
//		usedModel.removeElement("temp");
//		groupModel.removeElement("temp");
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String c = ae.getActionCommand();

		if (c.equals("add")) {
			Object [] addObj = availableList.getSelectedValues();
			DataStore [] adds = new DataStore[addObj.length];
			for (int i=0;i<adds.length;i++) {
				adds[i] = (DataStore)addObj[i];
			}
			usedModel.addElements(adds);
			availableModel.removeElements(adds);
			availableList.setSelectedIndices(new int[0]);
			usedList.setSelectedIndices(new int[0]);
			
			Object [] o = usedModel.getStores();
			DataSet [] s = new DataSet [o.length];
			
			for (int i=0;i<s.length;i++) {
				s[i] = (DataSet)o[i];
			}
			
			DataGroup r = (DataGroup)groupList.getSelectedValue();
			r.setDataSets(s);
			
		}
		else if (c.equals("select_named")) {
			new NameGatherer();			
		}
		else if (c.equals("remove")) {
			Object [] addObj = usedList.getSelectedValues();
			DataStore [] adds = new DataStore[addObj.length];
			for (int i=0;i<adds.length;i++) {
				adds[i] = (DataStore)addObj[i];
			}
			usedModel.removeElements(adds);
			availableModel.addElements(adds);
			
			availableList.setSelectedIndices(new int[0]);
			usedList.setSelectedIndices(new int[0]);

			
			Object [] o = usedModel.getStores();
			DataSet [] s = new DataSet [o.length];
			
			for (int i=0;i<s.length;i++) {
				s[i] = (DataSet)o[i];
			}
			
			DataGroup r = (DataGroup)groupList.getSelectedValue();
			r.setDataSets(s);

		}

		else if (c.equals("new_set")) {
			
			String setName=null;
			while (true) {
				setName = (String)JOptionPane.showInputDialog(this,"Enter group name","Group Name",JOptionPane.QUESTION_MESSAGE,null,null,"New Data Group");
				if (setName == null)
					return;  // They cancelled
					
					
				if (setName.length() == 0)
					continue; // Try again
				
				break;
			}

			DataGroup s = new DataGroup(setName,new DataSet[0]);
			application.dataCollection().addDataGroup(s);
			groupModel.addElements(new DataStore []{s});
		}
		
		else if (c.equals("rename_set")) {
			
			DataGroup s = (DataGroup)groupList.getSelectedValue();
			String setName=null;
			while (true) {
				setName = (String)JOptionPane.showInputDialog(this,"Enter group name","Set Name",JOptionPane.QUESTION_MESSAGE,null,null,s.name());
				if (setName == null) return; // They cancelled
				if (setName.length()>0) break;
			}
			s.setName(setName);
			groupModel.setElementAt(s,groupList.getSelectedIndex());
		}

		else if (c.equals("delete_set")) {
			Object [] o = groupList.getSelectedValues();
			DataGroup [] dataGroups = new DataGroup [o.length];
			for (int i=0;i<o.length;i++) {
				dataGroups[i]=(DataGroup)o[i];
			}
			
			groupModel.removeElements(dataGroups);

			application.dataCollection().removeDataGroups(dataGroups);

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
		// Check for a new set being selected
		if (ae.getSource() == groupList) {

			// Move all samples back to the available list
			DataStore [] o = usedModel.getStores();
			availableModel.addElements(o);
			usedModel.removeAllElements();

			if(groupList.getSelectedValues().length == 1) {
				renameButton.setEnabled(true);
				deleteButton.setEnabled(true);
				DataGroup s = (DataGroup)groupList.getSelectedValue();
				DataSet [] st = s.dataSets();
				usedModel.addElements(st);
				availableModel.removeElements(st);
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
	
	private class NameGatherer extends JDialog implements ActionListener {

		JTextArea queriesArea;
		JCheckBox allowPartialBox;
		JCheckBox caseInsensitiveBox;

		public NameGatherer () {
			
			super(GroupEditor.this,"Select named stores");

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=0;
			gbc.gridy=0;
			gbc.weightx=0.5;
			gbc.weighty=0.999;
			gbc.fill = GridBagConstraints.BOTH;
			
			getContentPane().setLayout(new GridBagLayout());

			JPanel choicePanel1 = new JPanel();
			choicePanel1.setLayout(new BorderLayout());
			choicePanel1.add(new JLabel("Query terms",JLabel.CENTER),BorderLayout.NORTH);
			queriesArea = new JTextArea();
			choicePanel1.add(new JScrollPane(queriesArea),BorderLayout.CENTER);
			getContentPane().add(choicePanel1,gbc);

			gbc.gridy++;
			gbc.weighty=0.001;
			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Allow Partial Matches "));
			allowPartialBox = new JCheckBox();
			allowPartialBox.setSelected(false);
			choicePanel2.add(allowPartialBox);
			getContentPane().add(choicePanel2,gbc);

			gbc.gridy++;
			JPanel choicePanel3 = new JPanel();
			choicePanel3.add(new JLabel("Case insensitive "));
			caseInsensitiveBox = new JCheckBox();
			caseInsensitiveBox.setSelected(false);
			choicePanel3.add(caseInsensitiveBox);
			getContentPane().add(choicePanel3,gbc);
			
			gbc.gridy++;
			JPanel buttonPanel = new JPanel();
			JButton findButton = new JButton("Find Stores");
			findButton.addActionListener(this);
			buttonPanel.add(findButton);
			getContentPane().add(buttonPanel,gbc);
			
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			setSize(400,500);
			setLocationRelativeTo(GroupEditor.this);
			setModal(true);
			setVisible(true);
			
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(400,500);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// Go through the queries to see what we have
			String [] queries = queriesArea.getText().split("\n");
			
			HashSet<String>queryStrings = new HashSet<String>();

			for (int q=0;q<queries.length;q++) {
				String query = queries[q].trim();
				if (caseInsensitiveBox.isSelected()) {
					query = query.toLowerCase();
				}				
				queryStrings.add(query);
			}
			
			queries = queryStrings.toArray(new String[0]);
			
			// Now work our way through the available list and select those
			// which match.
			
			IntVector indicesToSelect = new IntVector();
			
			for (int i=0;i<availableList.getModel().getSize();i++) {
				String name = availableList.getModel().getElementAt(i).toString();
				if (caseInsensitiveBox.isSelected()) {
					name = name.toLowerCase();
				}
				
				for (String test : queries) {
					if (allowPartialBox.isSelected()) {
						if (name.contains(test)) {
							indicesToSelect.add(i);
							break;
						}
					}
					else {
						if (name.equals(test)) {
							indicesToSelect.add(i);
							break;
						}
					}
				}
			}
			
			availableList.setSelectedIndices(indicesToSelect.toArray());
			
			setVisible(false);
			dispose();
			
		}

			
			
	}
	

}
