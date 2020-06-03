/**
 * Copyright 2009-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Help;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;

/**
 * The Class HelpDialog shows the contents of the help system and
 * allows searching and navigation within it.
 */
public class HelpDialog extends JDialog implements TreeSelectionListener {
	
	/** The tree. */
	private JTree tree;
	
	/** The current page. */
	private HelpPageDisplay currentPage = null;
	
	/** The main split. */
	private JSplitPane mainSplit;
	
	
	/**
	 * Create a help dialog with the default starting location
	 */
	public HelpDialog() {
		super(SeqMonkApplication.getInstance(),"Help Contents");

		try {

			// Java has a bug in it which affects the creation of valid URIs from
			// URLs relating to an windows UNC path.  We therefore have to mung
			// URLs starting file file:// to add 5 forward slashes so that we
			// end up with a valid URI.

			URL url = ClassLoader.getSystemResource("Help");
			if (url.toString().startsWith("file://")) {
				try {
					url = new URL(url.toString().replace("file://", "file://///"));
				} catch (MalformedURLException e) {
					throw new IllegalStateException(e);
				}
			}
			buildTree(new File(url.toURI()));
		}
		catch (URISyntaxException ux) {
			System.err.println("Couldn't parse URL falling back to path");
			buildTree(new File(ClassLoader.getSystemResource("Help").getPath()));
		}

	}
	
	/**
	 * Instantiates a new help dialog.
	 * 
	 * @param parent the parent
	 * @param startingLocation the folder containing the html help documentation
	 */
	public HelpDialog (File startingLocation) {
		super(SeqMonkApplication.getInstance(),"Help Contents");

		buildTree(startingLocation);
	}
		
		
	private void buildTree(File startingLocation) {	

		HelpIndexRoot root = new HelpIndexRoot(startingLocation);

		mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		setContentPane(mainSplit);
		
		tree = new JTree(new DefaultTreeModel(root));
		
		JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		leftSplit.setTopComponent(new JScrollPane(tree));
		leftSplit.setBottomComponent(new HelpSearchPanel(root,this));
		
		mainSplit.setLeftComponent(leftSplit);
		currentPage = new HelpPageDisplay(null);
		mainSplit.setRightComponent(currentPage);

		tree.addTreeSelectionListener(this);
		
		
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
		leftSplit.setDividerLocation(0.7);
		mainSplit.setDividerLocation(0.3);
		findStartingPage();
	}

	/**
	 * Find starting page.
	 */
	private void findStartingPage () {
		DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)tree.getModel().getRoot();

		DisplayPage((HelpPage)currentNode.getFirstLeaf());
	}
	
	public void DisplayPage (String pageName) {
		Enumeration<TreeNode> en = ((DefaultMutableTreeNode)tree.getModel().getRoot()).depthFirstEnumeration();
		
		while (en.hasMoreElements()) {
			TreeNode node = en.nextElement();
			
			if (node instanceof HelpPage) {
				if (((HelpPage)node).name.equals(pageName)) {
					DisplayPage((HelpPage)node);
					return;
				}
			}
		}
		
		throw new IllegalStateException("Couldn't find help page called '"+pageName+"'");
		
		
	}
	
	/**
	 * Display page.
	 * 
	 * @param page the page
	 */
	public void DisplayPage(HelpPage page) {
		if (currentPage != null) {
			int d = mainSplit.getDividerLocation();
			mainSplit.remove(currentPage);
			currentPage = new HelpPageDisplay(page);
			mainSplit.setRightComponent(currentPage);
			mainSplit.setDividerLocation(d);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
	 */
	public void valueChanged(TreeSelectionEvent tse) {
		
		if (tse.getNewLeadSelectionPath() == null) return;
		
		Object o = tse.getNewLeadSelectionPath().getLastPathComponent();
		if (o instanceof HelpPage && ((HelpPage)o).isLeaf()) {
			DisplayPage((HelpPage)o);
		}
	}
	
}
