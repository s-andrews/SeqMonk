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
package uk.ac.babraham.SeqMonk.DataTypes.Probes;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;

/**
 * The Class ProbeList stores as set of probes and associated quantiation values
 */
public class ProbeList implements Comparable<ProbeList> {
	
	// This hash only stores probe values for probes which have a value.  This 
	// gets out of hand when we have too many probes as the hash memory usage
	// goes through the roof.
	/** The probe list. */
	private Hashtable<Probe, float []> probeList = new Hashtable<Probe, float []>();

	// This vector stores all of the probes currently in the list and keeps
	// them sorted for convenience.
	/** The sorted probes. */
	private Vector<Probe> sortedProbes = new Vector<Probe>();
	
	/** This flag says whether the list of probes is actually sorted at the moment **/
	private boolean isSorted = false;
	
	/** The name. */
	private String name;
	
	/** The description. */
	private String description;
	
	/** The comments */
	private String comments = "";
	
	/** The value name. */
	private String [] valueNames;
	
	/** The parent. */
	private ProbeList parent;
	
	/** The children. */
	private Vector<ProbeList> children = new Vector<ProbeList>();
	
	/**
	 * Instantiates a new probe list.
	 * 
	 * @param parent the parent
	 * @param name the name
	 * @param description the description
	 * @param valueName the value name
	 */

	public ProbeList (ProbeList parent, String name, String description, String valueName) {
		this(parent,name,description,new String[]{valueName});
	}
	
	public ProbeList (ProbeList parent, String name, String description, String [] valueNames) {
		this.parent = parent;
		
		if (parent != null) {
			parent.addChild(this);
		}
		
		this.name = name;
		
		// Because of the way we save value names we can't allow a double percent sign (%%) to
		// be in them, so we need to make sure this is removed.
		
		for (int i=0;i<valueNames.length;i++) {
			while (valueNames[i].contains("%%")){
				valueNames[i] = valueNames[i].replace("%%", "%");
			}
		}
		
		this.valueNames = valueNames;
		this.description = description;
		probeListAdded(this);
	}
	
	/**
	 * Gets the probes for chromosome.
	 * 
	 * @param c the c
	 * @return the probes for chromosome
	 */
	public Probe [] getProbesForChromosome (Chromosome c) {
		if (!isSorted) {
			sortProbes();
		}
		Enumeration<Probe> en = sortedProbes.elements();
		Vector<Probe> tempChr = new Vector<Probe>();
			
		while (en.hasMoreElements()) {
			Probe p = en.nextElement();
			if (p.chromosome() == c) {
				tempChr.add(p);
			}
		}
		Probe [] chrProbes = tempChr.toArray(new Probe [0]);			
		return chrProbes;
	}

	
	/**
	 * Parent.
	 * 
	 * @return the probe list
	 */
	public ProbeList parent () {
		return parent;
	}
	
	/**
	 * Delete.
	 */
	synchronized public void delete () {
		/**
		 * This method should be called when this list is to be
		 * removed from the tree of lists.  It disconnects itself
		 * from its parent leaving it free to be garbage collected
		 */
		
		// We need to fire this event before actually doing the delete
		// or our Data view can't use the tree connections to remove
		// the node from the existing tree cleanly
		probeListRemoved(this);
		
		// This actually breaks the link between this node and the rest
		// of the tree.
		if (parent != null) {
			parent.removeChild(this);
		}
		parent = null;
		probeList.clear();
		sortedProbes.clear();
	}
	
	/**
	 * Children.
	 * 
	 * @return the probe list[]
	 */
	public ProbeList [] children () {
		return children.toArray(new ProbeList[0]);
	}
	
	/**
	 * Removes the child.
	 * 
	 * @param child the child
	 */
	private void removeChild (ProbeList child) {
		/**
		 * Should only be called from within the ProbeList class as part of the
		 * public delete() method.  Breaks a node away from the rest of the tree
		 */
		children.remove(child);
	}
	
	/**
	 * Adds the child.
	 * 
	 * @param child the child
	 */
	private void addChild (ProbeList child) {
		/**
		 * Should only be called from within the ProbeList class as part of the
		 * constructor. Creates a two way link between nodes and their parents
		 */
		children.add(child);
	}
	
	/**
	 * Gets the all probe lists.
	 * 
	 * @return the all probe lists
	 */
	public ProbeList [] getAllProbeLists () {
		/**
		 * Returns this probe list and all lists below this point in the tree
		 */
		
		Vector<ProbeList> v = new Vector<ProbeList>();
		v.add(this);
		getAllProbeLists(v);
		return v.toArray(new ProbeList[0]);
	}
	
	/**
	 * Gets the all probe lists.
	 * 
	 * @param v the v
	 * @return the all probe lists
	 */
	synchronized protected void getAllProbeLists (Vector<ProbeList> v) {
		// This recursive function iterates through the tree
		// of lists building up a complete flattened list
		// of ProbeLists.  If called from a particular node
		// it will return all lists at or below that node
		
		// For the SeqMonkDataWriter to work it is essential that the
		// lists in this vector are never reordered otherwise we can
		// lose the linkage when we save and reopen the data.
		
		Enumeration<ProbeList> e = children.elements();
		while (e.hasMoreElements()) {
			ProbeList l = e.nextElement();
			v.add(l);
			l.getAllProbeLists(v);
		}
	}
	
	/**
	 * Adds the probe.
	 * 
	 * @param p the p
	 * @param value the value
	 */

	public synchronized void addProbe (Probe p, float value) {
		addProbe(p, new float [] {value});
	}
	public synchronized void addProbe (Probe p, float [] values) {
		
		if (values!=null) {
			if (values.length != valueNames.length) {
				throw new IllegalStateException("Added probe "+p.name()+" to list "+name()+" with "+values.length+" values, but "+valueNames.length+" names were declared");
			}
			probeList.put(p,values);
		}
		sortedProbes.add(p);
		
		isSorted = false;

	}
		
	/**
	 * Sets the name.
	 * 
	 * @param s the new name
	 */
	public void setName (String s) {
		this.name = s;
		probeListRenamed(this);
	}
	
	/**
	 * Sets the description.
	 * 
	 * @param d the new description
	 */
	public void setDescription (String d) {
		this.description = d;
		this.description = this.description.replaceAll("[\\t\\n\\r]", " ");
	}
	
	
	public void setComments (String comments) {
		this.comments = comments;
		this.comments = this.comments.replaceAll("[\\t]", " ");
		this.comments = this.comments.replaceAll("`", "'");
	}
	
	/**
	 * Description.
	 * 
	 * @return the string
	 */
	public String description () {
		return description;
	}
	
	public String comments () {
		return comments;
	}
	
	
	private synchronized void sortProbes () {
		if (!isSorted) {
			Collections.sort(sortedProbes);
			isSorted = true;
		}
		
		try {
		// Do a sanity check to ensure we don't have any duplication here
		for (int i=1;i<sortedProbes.size();i++) {
			if (sortedProbes.elementAt(i) == sortedProbes.elementAt(i-1)) {
				throw new Exception("Duplicate probe "+sortedProbes.elementAt(i)+" and "+sortedProbes.elementAt(i-1)+" in "+name());
			}
			if (sortedProbes.elementAt(i).compareTo(sortedProbes.elementAt(i-1)) == 0) {
				throw new Exception("Unsortable probe "+sortedProbes.elementAt(i)+" and "+sortedProbes.elementAt(i-1)+" in "+name());
			}
		}
		}
		catch (Exception ex) {
			// There are duplicate probes and we need to remove them.
			Vector<Probe> dedup = new Vector<Probe>();
			Probe lastProbe = null;
			Enumeration<Probe>en = sortedProbes.elements();
			while (en.hasMoreElements()) {
				Probe p = en.nextElement();
				if (p == lastProbe) continue;
				dedup.add(p);
				lastProbe = p;
			}
			
			sortedProbes = dedup;
		}
		
		sortedProbes.trimToSize();
	}
	
	/**
	 * Gets the all probes.
	 * 
	 * @return the all probes
	 */
	public Probe [] getAllProbes () {
		if (!isSorted) {
			sortProbes();
		}
		
		/*
		 * We had all kinds of problems with this.  Because the sorted
		 * list has to stay sorted we ended up with a method which
		 * took the list from this method and resorted it a different
		 * way.  That affected the ordering of the list in here, and
		 * breakage ensued.
		 * 
		 * The only way we can ensure that this doesn't happen is to return
		 * a copy of this array rather than the original.
		 */
		
		Probe [] returnArray = new Probe [sortedProbes.size()];
		Enumeration<Probe> en = sortedProbes.elements();
	
		int i=0;
		while (en.hasMoreElements()) {
			returnArray[i] = en.nextElement();
			i++;
		}
		
		return 	returnArray;
	}
		
	/**
	 * Gets the list name.
	 * 
	 * @return the list name
	 */
	public String name () {
		return name;
	}
	
	/**
	 * Gets the value name.
	 * 
	 * @return the value name
	 */
	public String [] getValueNames () {
		if (valueNames == null) {
			return new String [] {};
		}
		return valueNames;
	}
	
	public String getConcatenatedValueNames() {
		if (valueNames == null || valueNames.length == 0) return "";
		
		StringBuffer sb = new StringBuffer();
		
		sb.append(valueNames[0]);
		for (int i=1;i<valueNames.length;i++) {
			sb.append("%%");
			sb.append(valueNames[i]);
		}
		
		return (sb.toString());
	}
	
	/**
	 * Gets the value for probe.
	 * 
	 * @param p the p
	 * @return the value for probe
	 */
	public float [] getValueForProbe (Probe p) {
		if (probeList.containsKey(p)) {
			return probeList.get(p);
		}
			
		return null;
	}
	
	public String getConcatenatedValuesForProbe(Probe p) {

		if (!probeList.containsKey(p)) {
			return "";
		}
		
		float [] values = getValueForProbe(p);
		if (values == null || values.length == 0) return "null";
		
		StringBuffer sb = new StringBuffer();
		
		sb.append(values[0]);
		for (int i=1;i<values.length;i++) {
			sb.append(",");
			sb.append(values[i]);
		}
		
		return (sb.toString());
	}

	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return name+" ("+sortedProbes.size()+")";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ProbeList p) {
		return name.toLowerCase().compareTo(p.name.toLowerCase());
	}
	
	
	// We use the following methods to notify up the tree about
	// changes which have occurred somewhere in the tree.  They
	// are private versions of the methods in the ProbeSetChangeListener
	
	/**
	 * Probe list added.
	 * 
	 * @param l the l
	 */
	protected void probeListAdded (ProbeList l) {
		parent.probeListAdded(l);
	}
	
	/**
	 * Probe list removed.
	 * 
	 * @param l the l
	 */
	protected void probeListRemoved (ProbeList l) {
		parent.probeListRemoved(l);
	}
	
	/**
	 * Probe list renamed.
	 * 
	 * @param l the l
	 */
	protected void probeListRenamed (ProbeList l) {
		parent.probeListRenamed(l);
	}
	
}
