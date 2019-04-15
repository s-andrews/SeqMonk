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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
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
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

/**
 * The Class DataCollectionTreeModel provides a tree model which describes
 * the data sets, data groups and annotation sets in a data collection
 */
public class DataCollectionTreeModel implements TreeModel, DataChangeListener, AnnotationCollectionListener {

	/** The collection. */
	private DataCollection collection;
	
	/** The listeners. */
	private Vector<TreeModelListener>listeners = new Vector<TreeModelListener>();
	
	/** The root node. */
	private folderNode rootNode;
	
	/** The annotation node. */
	private folderNode annotationNode;
	
	/** The data set node. */
	private folderNode dataSetNode;
	
	/** The data group node. */
	private folderNode dataGroupNode;
	
	/** The replicate set node */
	private folderNode replicateSetNode;
	
	/**
	 * Instantiates a new data collection tree model.
	 * 
	 * @param collection the collection
	 */
	public DataCollectionTreeModel (DataCollection collection) {
		this.collection = collection;
		if (collection != null) {
			collection.addDataChangeListener(this);
			collection.genome().annotationCollection().addAnnotationCollectionListener(this);
		}
		rootNode = new folderNode(collection.genome().toString());
		annotationNode = new folderNode("Annotation Sets");
		dataSetNode = new folderNode("Data Sets");
		dataGroupNode = new folderNode("Data Groups");
		replicateSetNode = new folderNode("Replicate Sets");
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
			switch (index) {
			case 0: return annotationNode;
			case 1: return dataSetNode;
			case 2: return dataGroupNode;
			case 3: return replicateSetNode;
			}
		}
		else if (node.equals(annotationNode)) {
			return collection.genome().annotationCollection().anotationSets()[index];
		}
		else if (node.equals(dataSetNode)) {
			return collection.getAllDataSets()[index];
		}
		else if (node.equals(dataGroupNode)) {
			return collection.getAllDataGroups()[index];
		}
		else if (node.equals(replicateSetNode)) {
			return collection.getAllReplicateSets()[index];
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
		else if (node.equals(annotationNode)) {
			return collection.genome().annotationCollection().anotationSets().length;
		}
		else if (node.equals(rootNode)) {
			return 4; // Annotation sets, DataSets, DataGroups, Replicate sets
		}
		else if (node.equals(dataSetNode)) {
			return collection.getAllDataSets().length;
		}
		else if (node.equals(dataGroupNode)) {
			return collection.getAllDataGroups().length;
		}
		else if (node.equals(replicateSetNode)) {
			return collection.getAllReplicateSets().length;
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
		else if (node.equals(annotationNode)) {
			AnnotationSet [] sets = collection.genome().annotationCollection().anotationSets();
			for (int s=0;s<sets.length;s++) {
				if (sets[s] == child) return s;
			}
		}
		else if (node.equals(rootNode)) {
			if (child.equals(dataSetNode)) return 0;
			if (child.equals(dataGroupNode)) return 1;
		}
		else if (node.equals(dataSetNode)) {
			DataSet [] sets = collection.getAllDataSets();
			for (int i=0;i<sets.length;i++) {
				if (sets[i] == child) return i;
			}
		}
		else if (node.equals(dataGroupNode)) {
			DataGroup [] groups = collection.getAllDataGroups();
			for (int i=0;i<groups.length;i++) {
				if (groups[i] == child) return i;
			}
		}
		else if (node.equals(replicateSetNode)) {
			ReplicateSet [] sets = collection.getAllReplicateSets();
			for (int i=0;i<sets.length;i++) {
				if (sets[i] == child) return i;
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
		if (node instanceof folderNode) {
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
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupAdded(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupAdded(DataGroup g) {
		TreeModelEvent me = new TreeModelEvent(g, getPathToRoot(dataGroupNode),new int []{getIndexOfChild(dataGroupNode, g)},new DataGroup[]{g});
		
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesInserted(me);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupRemoved(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupsRemoved(DataGroup [] g) {
		
		// Find the indices of each of these datagroups and sort them low to high
		// before telling the listeners
		Hashtable<Integer, DataGroup> indices = new Hashtable<Integer, DataGroup>();
		
		for (int i=0;i<g.length;i++) {
			indices.put(getIndexOfChild(dataGroupNode, g[i]), g[i]);
		}
		
		// We have to make an Integer object array before we can convert this
		// to a primitive int array
		Integer [] deleteIndices = indices.keySet().toArray(new Integer[0]);
		Arrays.sort(deleteIndices);
		
		DataGroup [] deleteGroups = new DataGroup[deleteIndices.length];
		for (int i=0;i<deleteIndices.length;i++) {
			deleteGroups[i] = indices.get(deleteIndices[i]);
		}
		
		int [] delInd = new int [deleteIndices.length];
		for (int i=0;i<deleteIndices.length;i++) {
			delInd[i] = deleteIndices[i];
		}
		
		TreeModelEvent me = new TreeModelEvent(g, getPathToRoot(dataGroupNode),delInd,deleteGroups);
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesRemoved(me);
		}		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupRenamed(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupRenamed(DataGroup g, String oldName) {
		TreeModelEvent me = new TreeModelEvent(g, getPathToRoot(g));
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesChanged(me);
		}
		
		// We also need to let the tree know that the structure may have
		// changed since the new name may sort differently and therefore
		// appear in a different position.
		me = new TreeModelEvent(dataGroupNode, getPathToRoot(dataGroupNode));
		e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeStructureChanged(me);
		}

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupSamplesChanged(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupSamplesChanged(DataGroup g) {
		// This can affect the name we display if the group changes from being HiC
		// to non-hiC (or vice versa) so we treat this like a name change.
		dataGroupRenamed(g,null);
		
	}

	public void replicateSetAdded(ReplicateSet r) {
		TreeModelEvent me = new TreeModelEvent(r, getPathToRoot(replicateSetNode),new int []{getIndexOfChild(replicateSetNode, r)},new ReplicateSet[]{r});
		
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesInserted(me);
		}
	}

	public void replicateSetsRemoved(ReplicateSet [] r) {
		
		// Find the indices of each of these datasets and sort them low to high
		// before telling the listeners
		Hashtable<Integer, ReplicateSet> indices = new Hashtable<Integer, ReplicateSet>();
		
		for (int i=0;i<r.length;i++) {
			indices.put(getIndexOfChild(replicateSetNode, r[i]), r[i]);
		}
		
		// We have to make an Integer object array before we can convert this
		// to a primitive int array
		Integer [] deleteIndices = indices.keySet().toArray(new Integer[0]);
		Arrays.sort(deleteIndices);
		
		ReplicateSet [] deleteSets = new ReplicateSet[deleteIndices.length];
		for (int i=0;i<deleteIndices.length;i++) {
			deleteSets[i] = indices.get(deleteIndices[i]);
		}
		
		int [] delInd = new int [deleteIndices.length];
		for (int i=0;i<deleteIndices.length;i++) {
			delInd[i] = deleteIndices[i];
		}
		
		TreeModelEvent me = new TreeModelEvent(r, getPathToRoot(replicateSetNode),delInd,deleteSets);
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesRemoved(me);
		}			
	}

	public void replicateSetRenamed(ReplicateSet r, String oldName) {
		TreeModelEvent me = new TreeModelEvent(r, getPathToRoot(r));
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesChanged(me);
		}

		// We also need to let the tree know that the structure may have
		// changed since the new name may sort differently and therefore
		// appear in a different position.
		me = new TreeModelEvent(replicateSetNode, getPathToRoot(replicateSetNode));
		e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeStructureChanged(me);
		}

	}

	public void replicateSetStoresChanged(ReplicateSet r) {
		// This can affect the name we display if the group changes from being HiC
		// to non-hiC (or vice versa) so we treat this like a name change.
		replicateSetRenamed(r,null);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetAdded(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetAdded(DataSet d) {
		TreeModelEvent me = new TreeModelEvent(d, getPathToRoot(dataSetNode),new int []{getIndexOfChild(dataSetNode, d)},new DataSet[]{d});
		
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesInserted(me);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetRemoved(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetsRemoved(DataSet [] d) {
		
		// Find the indices of each of these datasets and sort them low to high
		// before telling the listeners
		Hashtable<Integer, DataSet> indices = new Hashtable<Integer, DataSet>();
		
		for (int i=0;i<d.length;i++) {
			indices.put(getIndexOfChild(dataSetNode, d[i]), d[i]);
		}
		
		// We have to make an Integer object array before we can convert this
		// to a primitive int array
		Integer [] deleteIndices = indices.keySet().toArray(new Integer[0]);
		Arrays.sort(deleteIndices);
		
		DataSet [] deleteSets = new DataSet[deleteIndices.length];
		for (int i=0;i<deleteIndices.length;i++) {
			deleteSets[i] = indices.get(deleteIndices[i]);
		}
		
		int [] delInd = new int [deleteIndices.length];
		for (int i=0;i<deleteIndices.length;i++) {
			delInd[i] = deleteIndices[i];
		}
		
		TreeModelEvent me = new TreeModelEvent(d, getPathToRoot(dataSetNode),delInd,deleteSets);
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesRemoved(me);
		}		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetRenamed(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetRenamed(DataSet d, String oldName) {
		TreeModelEvent me = new TreeModelEvent(d, getPathToRoot(d));
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesChanged(me);
		}
		
		// We also need to let the tree know that the structure may have
		// changed since the new name may sort differently and therefore
		// appear in a different position.
		me = new TreeModelEvent(dataSetNode, getPathToRoot(dataSetNode));
		e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeStructureChanged(me);
		}
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#probeSetReplaced(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet)
	 */
	public void probeSetReplaced(ProbeSet probes) {}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener#annotationSetAdded(uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet)
	 */
	public void annotationSetsAdded(AnnotationSet [] annotationSets) {
		
		int [] indices = new int [annotationSets.length];
		for (int i=0;i<annotationSets.length;i++) {
			indices [i] = getIndexOfChild(annotationNode, annotationSets[i]);
		}
			
			
		TreeModelEvent me = new TreeModelEvent(annotationSets, getPathToRoot(annotationNode),indices,annotationSets);
		
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesInserted(me);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener#annotationSetRemoved(uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet)
	 */
	public void annotationSetRemoved(AnnotationSet annotationSet) {
		TreeModelEvent me = new TreeModelEvent(annotationSet, getPathToRoot(annotationNode),new int []{getIndexOfChild(annotationNode, annotationSet)},new AnnotationSet[]{annotationSet});
		
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesRemoved(me);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener#annotationSetRenamed(uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet)
	 */
	public void annotationSetRenamed(AnnotationSet annotationSet) {
		TreeModelEvent me = new TreeModelEvent(annotationSet, getPathToRoot(annotationSet));
		Enumeration<TreeModelListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().treeNodesChanged(me);
		}				
	}

	public void annotationFeaturesRenamed(AnnotationSet annotationSet,String newName) {}

	public void activeDataStoreChanged(DataStore s) {}

	public void activeProbeListChanged(ProbeList l) {}

	/**
	 * Gets the path to root.
	 * 
	 * @param d the d
	 * @return the path to root
	 */
	private Object [] getPathToRoot (DataSet d) {
		return new Object [] {rootNode,dataSetNode,d};
	}

	/**
	 * Gets the path to root.
	 * 
	 * @param s the s
	 * @return the path to root
	 */
	private Object [] getPathToRoot (AnnotationSet s) {
		return new Object [] {rootNode,annotationNode,s};
	}
	
	/**
	 * Gets the path to root.
	 * 
	 * @param g the g
	 * @return the path to root
	 */
	private Object [] getPathToRoot (DataGroup g) {
		return new Object [] {rootNode,dataGroupNode,g};
	}
	
	/**
	 * Gets the path to root.
	 * 
	 * @param g the g
	 * @return the path to root
	 */
	private Object [] getPathToRoot (ReplicateSet s) {
		return new Object [] {rootNode,replicateSetNode,s};
	}
	
	/**
	 * Gets the path to root.
	 * 
	 * @param f the f
	 * @return the path to root
	 */
	private Object [] getPathToRoot (folderNode f) {
		if (f == rootNode) return new Object [] {f};
		else return new Object [] {rootNode,f};
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
