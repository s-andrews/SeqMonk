/**
 * Copyright 2010- 21 Simon Andrews
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.DataViewer.DataTreeRenderer;
import uk.ac.babraham.SeqMonk.Displays.DataViewer.ProbeSetTreeModel;

public class ProbeListSelectorDialog extends JDialog {
	
	private JTree tree;
	private ProbeList [] selectedProbeLists = new ProbeList[0];
	
	public static ProbeList [] selectProbeLists () {
		
		ProbeListSelectorDialog dialog = new ProbeListSelectorDialog();
		
		return dialog.selectedProbeLists();
	}
	
	private ProbeList [] selectedProbeLists () {
		return selectedProbeLists;
	}
	
	private ProbeListSelectorDialog () {
		
		super(SeqMonkApplication.getInstance(),"Select Probe Lists to use");
		
		setModal(true);
		getContentPane().setLayout(new BorderLayout());
		
		tree = new JTree(new ProbeSetTreeModel(SeqMonkApplication.getInstance().dataCollection()));
		tree.setCellRenderer(new DataTreeRenderer());
		
		getContentPane().add(new JScrollPane(tree),BorderLayout.CENTER);
		
		// Fully expand the tree
		for (int i=0;i<tree.getRowCount();i++) {
			tree.expandRow(i);
		}
		
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		
		buttonPanel.add(closeButton);
		
		JButton selectButton = new JButton("Select");
		selectButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				
				TreePath [] paths = tree.getSelectionPaths();
				
				if (paths == null) {
					selectedProbeLists = new ProbeList[0];
				}
				else {
					Vector<ProbeList> newList = new Vector<ProbeList>();
					
					for (int i=0;i<paths.length;i++) {
						if (paths[i].getLastPathComponent() instanceof ProbeList) {
							newList.add((ProbeList)paths[i].getLastPathComponent());
						}
					}
					
					selectedProbeLists = newList.toArray(new ProbeList[0]);
				}
				
				setVisible(false);
				dispose();
				
			}
		});
		
		buttonPanel.add(selectButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(400,300);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}
	

}
