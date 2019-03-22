/**
 * Copyright Copyright 2010-19 Simon Andrews
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
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Network.DownloadableGenomes.DownloadableGenomeSet;
import uk.ac.babraham.SeqMonk.Network.DownloadableGenomes.DownloadableGenomeTreeModel;
import uk.ac.babraham.SeqMonk.Network.DownloadableGenomes.GenomeAssembly;

/**
 * The Class GenomeDownloadSelector provides a dialog which can be used to select an
 * assembly to download.
 */
public class GenomeDownloadSelector extends JDialog {
	
	/** The application. */
	private SeqMonkApplication application;
	
	/** The tree. */
	private JTree tree;
	
	/** The model behind the tree */
	private DownloadableGenomeTreeModel treeModel;
	
	/** The download button. */
	private JButton downloadButton;
	
	/**
	 * Instantiates a new genome download selector.
	 * 
	 * @param application the application
	 * @throws IOException if there was a problem getting or parsing the list of genomes
	 */
	public GenomeDownloadSelector (SeqMonkApplication application) throws IOException {
		super(application,"Select Genome to Download...");
		this.application = application;
		setSize(380,340);
		setLocationRelativeTo(application);
		setModal(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		getContentPane().setLayout(new BorderLayout());
		
		treeModel = new DownloadableGenomeTreeModel(new DownloadableGenomeSet());
		tree = new JTree(treeModel);
		tree.addTreeSelectionListener(new TreeListener());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		getContentPane().add(new JScrollPane(tree),BorderLayout.CENTER);
				
		// Create the buttons at the bottom.
		ButtonListener l = new ButtonListener();
		
		JPanel buttonPanel = new JPanel();
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(l);
		buttonPanel.add(cancelButton);

		downloadButton = new JButton("Download");
		downloadButton.setActionCommand("download");
		downloadButton.setEnabled(false);
		downloadButton.addActionListener(l);
		getRootPane().setDefaultButton(downloadButton);
		buttonPanel.add(downloadButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		
		setVisible(true);
	}

	/**
	 * The listener interface for receiving button events.
	 * The class that is interested in processing a button
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addButtonListener<code> method. When
	 * the button event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see ButtonEvent
	 */
	private class ButtonListener implements ActionListener {

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent ae) {
			if (ae.getActionCommand().equals("download")) {
				setVisible(false);
				GenomeAssembly selectedGenome = (GenomeAssembly)tree.getSelectionPath().getLastPathComponent();
				application.downloadGenome(selectedGenome.set().species().name(),selectedGenome.assembly(),selectedGenome.fileSize());
				dispose();
			}
			else if (ae.getActionCommand().equals("cancel")) {
				setVisible(false);
				dispose();
			}
			
		}
	}
	
	/**
	 * The listener interface for receiving tree events.
	 * The class that is interested in processing a tree
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addTreeListener<code> method. When
	 * the tree event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see TreeEvent
	 */
	private class TreeListener implements TreeSelectionListener {

		/* (non-Javadoc)
		 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
		 */
		public void valueChanged(TreeSelectionEvent tse) {
			if (tree.getSelectionPath() != null && tree.getSelectionPath().getLastPathComponent() instanceof GenomeAssembly) {
				downloadButton.setEnabled(true);
			}
			else {
				downloadButton.setEnabled(false);
			}
		}
		
	}
		
}
