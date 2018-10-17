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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Displays.ManualGenomeBuilder.ManualGenomeBuilderDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * The Class GenomeSelector shows a tree of the currently available genomes
 */
public class GenomeSelector extends JDialog {

	/** The application. */
	private SeqMonkApplication application;

	/** The tree for the main genomes */
	private JTree genomesTree;

	/** The tree for control genomes we might want to add */
	private JTree controlsTree;

	/** The ok button. */
	private JButton okButton;
	
	private boolean theyCancelled = false;

	/**
	 * Instantiates a new genome selector.
	 * 
	 * @param application the application
	 */
	public GenomeSelector (SeqMonkApplication application) {
		super(application,"Select Genome...");
		this.application = application;
		setSize(750,350);
		setLocationRelativeTo(application);
		setModal(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		getContentPane().setLayout(new BorderLayout());

		// Create the tree of available genomes
		DefaultMutableTreeNode genomesRoot = new DefaultMutableTreeNode("Genomes");
		DefaultMutableTreeNode controlsRoot = new DefaultMutableTreeNode("Add-ons");

		File[] genomes;
		try {
			genomes = SeqMonkPreferences.getInstance().getGenomeBase().listFiles();
			if (genomes == null) {
				throw new FileNotFoundException();
			}
		} 
		catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(application, "Couldn't find the folder containing your genomes.  Please check your file preferences", "Error getting genomes", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Arrays.sort(genomes);

		for (int i=0;i<genomes.length;i++) {
			if (genomes[i].isDirectory()) {

				DefaultMutableTreeNode genomeNode = new DefaultMutableTreeNode(genomes[i].getName());
				File [] assemblies = genomes[i].listFiles();

				// On windows we get a stupid situation if the user has set their genomes folder to
				// be their documents folder.  This contains special pseudo-directories (called 
				// junctions) for My Music, My Pictures etc.  These appear to java to be normal 
				// directories but if you move into them then you get to what appears to be an 
				// invalid location and you can't list files, which results in a crash.
				
				// We can therefore check for whether the results of listFiles is null and just skip
				// this.  This isn't great as we might miss some real failures, but there's no 
				// native way to explicitly check for junctions.

				if (assemblies == null) continue;
				
				Hashtable<String, Vector<File>> assemblySets = new Hashtable<String, Vector<File>>();
				
				for (int j=0;j<assemblies.length;j++) {
					if (assemblies[j].isDirectory()) {
						String name = assemblies[j].getName();
						name = name.replaceAll("_v\\d+$", "");
					
						if (!assemblySets.containsKey(name)) {
							assemblySets.put(name,new Vector<File>());
						}
					
						assemblySets.get(name).add(assemblies[j]);
					}
				}
				
				String [] assemblySetNames = assemblySets.keySet().toArray(new String [0]);
				
				Arrays.sort(assemblySetNames);

				for (int j=0;j<assemblySetNames.length;j++) {

					File [] localAssemblies = assemblySets.get(assemblySetNames[j]).toArray(new File[0]);

					if (localAssemblies.length == 1) {
						genomeNode.add(new AssemblyNode(localAssemblies[0]));
					}
					else {
						AssemblySetNode setNode = new AssemblySetNode(assemblySetNames[j]);
						genomeNode.add(setNode);
					
					
						for (int k=0;k<localAssemblies.length;k++) {
							setNode.add(new AssemblyNode(localAssemblies[k]));
						}
					}
				}
				if (assemblySetNames.length > 0) {
					if (genomes[i].getName().equals("Controls")) {
						controlsRoot.add(genomeNode);
					}
					else {
						genomesRoot.add(genomeNode);
					}
				}
				else {
					System.err.println("Skipping genomes folder "+genomes[i].getAbsolutePath()+" which didn't contain any assemblies");
				}
			}
		}

		DefaultTreeModel genomesTreeModel = new DefaultTreeModel(genomesRoot);
		genomesTree = new JTree(genomesTreeModel);
		genomesTree.addTreeSelectionListener(new TreeListener());
		genomesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);


		// If there are controls we show two windows
		if (controlsRoot.getChildCount() > 0) {

			DefaultTreeModel controlsTreeModel = new DefaultTreeModel(controlsRoot);
			controlsTree = new JTree(controlsTreeModel);
			controlsTree.addTreeSelectionListener(new TreeListener());
			controlsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

			JSplitPane treePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			treePane.setBottomComponent(new JScrollPane(controlsTree));
			treePane.setTopComponent(new JScrollPane(genomesTree));
			treePane.setResizeWeight(0.8);

			getContentPane().add(treePane,BorderLayout.CENTER);

		}
		else {
			// There are no controls so just show the main window.
			getContentPane().add(new JScrollPane(genomesTree),BorderLayout.CENTER);
		}
		
		// Create the buttons at the bottom.
		ButtonListener l = new ButtonListener();

		JPanel buttonPanel = new JPanel();

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(l);
		buttonPanel.add(cancelButton);

		JButton importButton = new JButton("Import Genome From Server");
		importButton.setActionCommand("import");
		importButton.addActionListener(l);
		buttonPanel.add(importButton);

		JButton customButton = new JButton("Build Custom Genome");
		customButton.setActionCommand("custom");
		customButton.addActionListener(l);
		buttonPanel.add(customButton);


		okButton = new JButton("Start New Project");
		okButton.setActionCommand("ok");
		okButton.setEnabled(false);
		okButton.addActionListener(l);
		getRootPane().setDefaultButton(okButton);
		buttonPanel.add(okButton);

		getContentPane().add(buttonPanel,BorderLayout.SOUTH);


		setVisible(true);

	}
	
	public boolean theyCancelled () {
		return theyCancelled;
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
			if (ae.getActionCommand().equals("ok")) {
				setVisible(false);

				// Remove any currently loaded data
				application.wipeAllData();

				// Now load the new genome.

				Vector<File> baseLocations = new Vector<File>();

				// We must have at least one main assembly
				baseLocations.add(((AssemblyNode)genomesTree.getSelectionPath().getLastPathComponent()).file());

				// We may have one or more control genomes

				if (controlsTree != null) {
					TreePath [] controlPaths = controlsTree.getSelectionPaths();

					if (controlPaths != null) {
						for (int c=0;c<controlPaths.length;c++) {
							if (controlPaths[c].getLastPathComponent() instanceof AssemblyNode) {
								baseLocations.add(((AssemblyNode)controlPaths[c].getLastPathComponent()).file());
							}
						}
					}
				}

				application.loadGenome(baseLocations.toArray(new File[0]));
				dispose();
			}
			else if (ae.getActionCommand().equals("import")) {
				try {
					new GenomeDownloadSelector(application);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
				setVisible(false);
				dispose();
			}
			else if (ae.getActionCommand().equals("custom")) {
				new ManualGenomeBuilderDialog();
				setVisible(false);
				dispose();
			}
			else if (ae.getActionCommand().equals("cancel")) {
				theyCancelled = true;
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
			if (genomesTree.getSelectionPath() != null && genomesTree.getSelectionPath().getLastPathComponent() instanceof AssemblyNode) {
				okButton.setEnabled(true);
			}
			else {
				okButton.setEnabled(false);
			}
		}

	}

	
	/**
	 * The Class AssemblyNode.
	 */
	private class AssemblySetNode extends DefaultMutableTreeNode {


		/**
		 * Instantiates a new assembly node.
		 * 
		 * @param f the f
		 */
		public AssemblySetNode (String name) {
			super(name);
		}

	}
	
	
	/**
	 * The Class AssemblyNode.
	 */
	private class AssemblyNode extends DefaultMutableTreeNode {

		/** The f. */
		private File f;

		/**
		 * Instantiates a new assembly node.
		 * 
		 * @param f the f
		 */
		public AssemblyNode (File f) {
			super(f.getName());
			this.f = f;
		}

		/**
		 * File.
		 * 
		 * @return the file
		 */
		public File file () {
			return f;
		}

	}

}
