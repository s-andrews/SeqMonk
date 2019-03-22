/**
 * Copyright 2010-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.DataViewer;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener;

/**
 * The Class ProbeSetTreeModel provides a tree model which describes the
 * relationships between probe sets.
 */
public class ProbeSetTreeModel implements TreeModel, ProbeSetChangeListener, DataChangeListener {

	/** The probes. */
	private ProbeSet probes;
	
	/** The listeners. */
	private Vector<TreeModelListener>listeners = new Vector<TreeModelListener>();
	
	/** The root node. */
	private folderNode rootNode;
	
	/**
	 * Instantiates a new probe set tree model.
	 * 
	 * @param collection the collection
	 */
	public ProbeSetTreeModel (DataCollection collection) {
		if (collection != null) {
			collection.addDataChangeListener(this);
			probes = collection.probeSet();
		}
		if (probes != null) {
			collection.probeSet().addProbeSetChangeListener(this);
		}
		rootNode = new folderNode("Probe Lists");
	}
		
	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	public void addTreeModelListener(TreeModelListener tl) {
		if (tl != null && !listeners.contains(tl)) {
			listeners.add(tl);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	public void removeTreeModelListener(TreeModelListener tl) {
		if (tl != null && listeners.contains(tl)) {
			listeners.remove(tl);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	public Object getChild(Object node, int index) {
		if (node instanceof ProbeList) {
			return ((ProbeList)node).children()[index];
		}
		else if (node.equals(rootNode)) {
			if (index == 0) return probes;
		}

		throw new NullPointerException("Null child from "+node+" at index "+index);
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	public int getChildCount(Object node) {
		if (node instanceof ProbeList) {
			return ((ProbeList)node).children().length;
		}
		else if (node.equals(rootNode)) {
			if (probes == null) return 0;
			return 1;
		}
		else {
			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
	 */
	public int getIndexOfChild(Object node, Object child) {
		if (node instanceof ProbeList) {
			ProbeList [] children =  ((ProbeList)node).children();
			for (int i=0;i<children.length;i++) {
				if (children[i].equals(child)) {
					return i;
				}
			}
		}
		else if (node.equals(rootNode)) {
			if (child == probes) {
				return 0;
			}
		}
		System.err.println("Couldn't find valid index for "+node+" and "+child);
		return 0;
				
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#getRoot()
	 */
	public Object getRoot() {
		return rootNode;
	}


	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
	 */
	public boolean isLeaf(Object node) {
		if (node instanceof ProbeList) {
			return ((ProbeList)node).children().length == 0;
		}
		else if (node instanceof folderNode) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
	 */
	public void valueForPathChanged(TreePath tp, Object node) {
		// This only applies to editable trees - which this isn't.
		System.out.println("Value for path changed called on node "+node);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener#probeListAdded(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	public void probeListAdded(ProbeList l) {
		Object [] pathToRoot = getPathToRoot(l.parent());
		TreeModelEvent me = new TreeModelEvent(l, pathToRoot,new int []{getIndexOfChild(l.parent(), l)},new ProbeList[]{l});
				
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesInserted(me);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener#probeListRemoved(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	public void probeListRemoved(ProbeList l) {
		TreeModelEvent me;
		if (l instanceof ProbeSet) {
			me = new TreeModelEvent(l, getPathToRoot(l.parent()),new int []{0},new ProbeList[]{l});
		}
		else {
			me = new TreeModelEvent(l, getPathToRoot(l.parent()),new int []{getIndexOfChild(l.parent(), l)},new ProbeList[]{l});
		}
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesRemoved(me);
		}		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener#probeListRenamed(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	public void probeListRenamed(ProbeList l) {
		TreeModelEvent me = new TreeModelEvent(l, getPathToRoot(l));
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesChanged(me);
		}		
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupAdded(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupAdded(DataGroup g) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupRemoved(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupsRemoved(DataGroup [] g) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupRenamed(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupRenamed(DataGroup g) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupSamplesChanged(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupSamplesChanged(DataGroup g) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetAdded(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetAdded(DataSet d) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetRemoved(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetsRemoved(DataSet [] d) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetRenamed(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetRenamed(DataSet d) {}

	public void replicateSetAdded(ReplicateSet r) {}

	public void replicateSetsRemoved(ReplicateSet [] r) {}

	public void replicateSetRenamed(ReplicateSet r) {}

	public void replicateSetStoresChanged(ReplicateSet r) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#probeSetReplaced(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet)
	 */
	public void probeSetReplaced(ProbeSet probes) {
		if (this.probes != null) {
			this.probes.removeProbeSetChangeListener(this);
		}
		this.probes = probes;
		
		if (probes != null) {
			probes.addProbeSetChangeListener(this);
		}
		
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeStructureChanged(new TreeModelEvent(rootNode, new ProbeList[] {probes}));
		}
	}
	
	public void activeDataStoreChanged(DataStore s) {}

	public void activeProbeListChanged(ProbeList l) {}

	/**
	 * Gets the path to root.
	 * 
	 * @param l the l
	 * @return the path to root
	 */
	private Object [] getPathToRoot (ProbeList l) {

		LinkedList<Object> nodes = new LinkedList<Object>();

		if (l != null) {
			nodes.add(l);
		
			while (l.parent() != null) {
				l = l.parent();
				nodes.addFirst(l);
			}
		}
		
		nodes.addFirst(rootNode);
		
		// Now make this into an array
		Object [] pathToNode = nodes.toArray(new Object[0]);
						
		return pathToNode;
	}

	/**
	 * The Class folderNode.
	 */
	private class folderNode {
		
		/** The name. */
		private String name;
		
		/**
		 * Instantiates a new folder node.
		 * 
		 * @param name the name
		 */
		public folderNode (String name) {
			this.name = name;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString () {
			return name;
		}
	}
	
}
