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

import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;

/**
 * The Class ProbeSet is a special instance of probe list which 
 * represents a full set of probes as created by a probe generator.
 * All probe lists are therefore subsets of the containing probeset.
 */
public class ProbeSet extends ProbeList{

	/** The active list. */
	private ProbeList activeList = null;

	/** The listeners. */
	private Vector<ProbeSetChangeListener> listeners = new Vector<ProbeSetChangeListener>();

	/** The index count. */
	private int indexCount = 0;

	/** The expected total count. */
	private int expectedTotalCount = 0;

	/** The containing data collection */
	private DataCollection collection = null;

	private String currentQuantitation = null;

	/**
	 * Instantiates a new probe set.
	 * 
	 * @param description the description
	 * @param probes the probes
	 */
	public ProbeSet (String description, Probe [] probes) {
		super(null,"All Probes",description,new String[0]);
		setProbes(probes);
	}

	/**
	 * Instantiates a new probe set.
	 * 
	 * @param description the description
	 * @param expectedSize the expected size
	 */
	public ProbeSet (String description, int expectedSize) {
		/**
		 * This constructor should only be called by the SeqMonkParser since it
		 * relies on the correct number of probes eventually being added.  Ideally
		 * we'd go back to sort out this requirement by changing the SeqMonk
		 * file format, but for now we're stuck with this work round
		 */
		super(null,"All Probes",description,new String[0]);
		expectedTotalCount = expectedSize;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList#addProbe(uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe, java.lang.Double)
	 */
	public void addProbe (Probe p, float [] values) {

		/**
		 * This method is only used by the SeqMonk parser.  All other probe
		 * generators add their probes in bulk using the setProbes method
		 * which is more efficient.
		 */

		// Update the index
		p.setIndex(indexCount);
		indexCount++;

		// Call the super method so we can still be treated like a
		// normal probe list
		super.addProbe(p, values);
	}

	public void setCollection (DataCollection collection) {
		this.collection = collection;
	}

	/**
	 * Sets the probes.
	 * 
	 * @param probes the new probes
	 */
	private void setProbes (Probe [] probes) {

		// Reset the probe index
		indexCount = 0;
		expectedTotalCount = probes.length;

		for (int p=0;p<probes.length;p++) {
			addProbe(probes[p],null);
		}

	}

	public String justDescription () {
		return super.description();
	}

	public String description () {
		if (currentQuantitation != null) {
			return super.description()+". Quantitated with "+currentQuantitation;
		}
		else {
			return super.description();
		}
	}

	public String currentQuantitation () {
		return currentQuantitation;
	}

	public void setCurrentQuantitation (String currentQuantitation) {
		this.currentQuantitation = currentQuantitation;
	}

	/**
	 * Size.
	 * 
	 * @return the int
	 */
	public int size () {
		return expectedTotalCount;
	}


	/**
	 * Sets the active list.
	 * 
	 * @param list the new active list
	 * @throws SeqMonkException the seq monk exception
	 */
	public void setActiveList (ProbeList list) throws SeqMonkException {

		if (list == null) {
			activeList = null;
			return;
		}
		activeList = list;

		if (collection != null) {
			collection.activeProbeListChanged(list);
		}
	}

	/**
	 * Gets the active list.
	 * 
	 * @return the active list
	 */
	public ProbeList getActiveList () {
		if (activeList != null) {
			return activeList;
		}
		else {
			return this;
		}
	}


	/**
	 * Adds the probe set change listener.
	 * 
	 * @param l the l
	 */
	public void addProbeSetChangeListener (ProbeSetChangeListener l) {
		if (l != null && !listeners.contains(l)) {
			listeners.add(l);
		}
	}

	/**
	 * Removes the probe set change listener.
	 * 
	 * @param l the l
	 */
	public void removeProbeSetChangeListener (ProbeSetChangeListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList#delete()
	 */
	synchronized public void delete (){
		// This is overridden from ProbeList and is called as the
		// list is removed.
		super.delete();
		// Now we can get rid of our list of listeners
		listeners.removeAllElements();
		// Drop the link to the collection
		collection = null;
	}

	// These methods propogate up through the tree of probe lists
	// to here where we override them to pass the messages on to
	// any listeners we have
	//
	// Because these methods are often called from other threads we
	// can have problems when the things listening to these events
	// are swing components because they really don't like messages
	// coming from sources other than the Event Dispatch Thread.  We
	// therefore wrap these calls in a SwingUtilites.invokeLater call
	// so that the actual notifications happen on the EDT.

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList#probeListAdded(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	protected void probeListAdded (final ProbeList l) {
		if (listeners == null) return;
		Enumeration<ProbeSetChangeListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			ProbeSetChangeListener pl = e.nextElement();
			pl.probeListAdded(l);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList#probeListRemoved(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	protected void probeListRemoved (final ProbeList l) {
		Enumeration<ProbeSetChangeListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().probeListRemoved(l);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList#probeListRenamed(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	protected void probeListRenamed (final ProbeList l) {

		Enumeration<ProbeSetChangeListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().probeListRenamed(l);
		}
	}


}
