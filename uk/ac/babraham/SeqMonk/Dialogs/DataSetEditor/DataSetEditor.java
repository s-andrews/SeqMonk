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
package uk.ac.babraham.SeqMonk.Dialogs.DataSetEditor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.Utilities.IntVector;

/**
 * The Class DataSetEditor allows the editing and deleting data sets
 */
public class DataSetEditor extends JDialog implements ActionListener, ListSelectionListener {
	
	/** The data collection. */
	private DataCollection collection;
	
	/** The data set model. */
	private DefaultListModel dataSetModel = new DefaultListModel();
	
	/** The data set list. */
	private JList dataSetList;
	
	/** The delete button. */
	private JButton deleteButton;
	
	/** The rename button. */
	private JButton renameButton;

	/** The replace button. */
	private JButton replaceButton;
	
	/** The reset button */
	private JButton resetButton;

	
	/**
	 * Instantiates a new data set editor.
	 * 
	 * @param collection the application
	 */
	public DataSetEditor (DataCollection collection) {
		super(SeqMonkApplication.getInstance(),"Edit DataSets...");
		setSize(300,400);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		this.collection = collection;
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.weightx=0.9;
		c.weighty=0.9;
		c.fill = GridBagConstraints.BOTH;
		
		JPanel samplePanel = new JPanel();
		samplePanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx=0;
		gc.gridy=0;
		gc.weightx=0.5;
		gc.weighty=0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		samplePanel.add(new JLabel("DataSets",JLabel.CENTER),gc);
		
		gc.gridy++;
		gc.weighty=1;
		gc.fill = GridBagConstraints.BOTH;
		
		dataSetList = new JList(dataSetModel);
		dataSetList.addListSelectionListener(this);
		samplePanel.add(new JScrollPane(dataSetList),gc);

		gc.gridy++;
		gc.weighty=0;
		gc.fill = GridBagConstraints.HORIZONTAL;

		deleteButton = new JButton("Delete DataSet");
		deleteButton.setActionCommand("delete_dataset");
		deleteButton.addActionListener(this);
		deleteButton.setEnabled(false);
		samplePanel.add(deleteButton,gc);
		
		gc.gridy++;
		renameButton = new JButton("Rename DataSet");
		renameButton.setActionCommand("rename_dataset");
		renameButton.addActionListener(this);
		renameButton.setEnabled(false);
		samplePanel.add(renameButton,gc);

		gc.gridy++;
		replaceButton = new JButton("Replace in name");
		replaceButton.setActionCommand("replace");
		replaceButton.addActionListener(this);
		replaceButton.setEnabled(false);
		samplePanel.add(replaceButton,gc);

		gc.gridy++;
		JButton selectNamesButton = new JButton("Select Named DataSets");
		selectNamesButton.setActionCommand("select_names");
		selectNamesButton.addActionListener(this);
		selectNamesButton.setEnabled(true);
		samplePanel.add(selectNamesButton,gc);

		
		gc.gridy++;
		resetButton = new JButton("Reset names");
		resetButton.setActionCommand("reset");
		resetButton.addActionListener(this);
		resetButton.setEnabled(false);
		samplePanel.add(resetButton,gc);
		
		getContentPane().add(samplePanel,c);

		c.gridx++;
		
		c.gridx=0;
		c.gridy++;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.NONE;

		JPanel bottomPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		bottomPanel.add(closeButton);
		
		getContentPane().add(bottomPanel,c);
		
		// Fill the lists with the data we know about
		DataSet [] dataSets = collection.getAllDataSets();
		for (int i=0;i<dataSets.length;i++) {
			dataSetModel.addElement(dataSets[i]);
		}
				
		setVisible(true);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String c = ae.getActionCommand();

		if (c.equals("rename_dataset")) {
			
			DataSet s = (DataSet)dataSetList.getSelectedValue();
			String dataSetName=null;
			while (true) {
				dataSetName = (String)JOptionPane.showInputDialog(this,"Enter DataSet name","DataSet Name",JOptionPane.QUESTION_MESSAGE,null,null,s.name());
				if (dataSetName == null) return; // They cancelled
				
				if (dataSetName.length()>0) break;
			}
			s.setName(dataSetName);
			dataSetModel.setElementAt(s,dataSetList.getSelectedIndex());
		}
		
		else if (c.equals("replace")) {
			
			Object [] o = dataSetList.getSelectedValues();
			DataSet [] ds = new DataSet[o.length];
			for (int i=0;i<o.length;i++) {
				ds[i] = (DataSet)o[i];
			}
			
			String replaceWhat=null;
			while (true) {
				replaceWhat = (String)JOptionPane.showInputDialog(this,"Replace what","Replace text",JOptionPane.QUESTION_MESSAGE,null,null,"");
				if (replaceWhat == null) return; // They cancelled
				
				if (replaceWhat.length()>0) break;
			}

			String replaceWith = (String)JOptionPane.showInputDialog(this,"Replace with","Replace text",JOptionPane.QUESTION_MESSAGE,null,null,"");
			if (replaceWith == null) return; // They cancelled
			
			for (int s=0;s<ds.length;s++) {
				String oldName = ds[s].name();
				String newName = oldName.replaceAll(replaceWhat, replaceWith);
				ds[s].setName(newName);
			}
			
		}

		else if (c.equals("reset")) {
			Object [] o = dataSetList.getSelectedValues();
			
			if (JOptionPane.showConfirmDialog(this, "Reset names to original file names?", "Reset names?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
			
			for (int i=0;i<o.length;i++) {
				DataSet d = (DataSet)o[i];
				File f = new File(d.fileName());
				d.setName(f.getName());
			}
		}
		
		else if (c.equals("select_names")) {

			// We need to collect a list of names we want to keep
			CollectDataSetNamesDialog d = new CollectDataSetNamesDialog();
			
			String [] names = d.getNames();
			
			System.err.println("Got a list of "+names.length+" names");
			
			if (names.length == 0) return; // They cancelled or didn't enter anything
			
			IntVector iv = new IntVector();
			
			INDEX: for (int index = 0;index < dataSetModel.getSize();index++) {
				for (int n=0;n<names.length;n++) {
					
//					System.err.println("Checking '"+names[n]+" against '"+dataSetModel.elementAt(index)+"'");
					
					if (names[n].equals(dataSetModel.elementAt(index).toString())) {
//						System.err.println("It matches");
						iv.add(index);
						continue INDEX;
					}
				}
			}
			
			dataSetList.setSelectedIndices(iv.toArray());
			
		}
		
		else if (c.equals("close")) {
			setVisible(false);
			dispose();
		}			
		
		else if (c.equals("delete_dataset")) {
			Object [] o = dataSetList.getSelectedValues();
			
			if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete "+o.length+" data sets?", "Really delete?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
			
			DataSet [] ds = new DataSet[o.length];
			for (int i=0;i<o.length;i++) {
				ds[i] = (DataSet)o[i];
				dataSetModel.removeElement(o[i]);
			}
			collection.removeDataSets(ds);
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
		if (dataSetList.getSelectedValues().length > 0) {
			deleteButton.setEnabled(true);
			replaceButton.setEnabled(true);
			resetButton.setEnabled(true);
		}
		else {
			deleteButton.setEnabled(false);
			replaceButton.setEnabled(false);
			resetButton.setEnabled(false);
		}
		
		if (dataSetList.getSelectedValues().length == 1) {
			renameButton.setEnabled(true);
		}
		else {
			renameButton.setEnabled(false);
		}
	}
}
