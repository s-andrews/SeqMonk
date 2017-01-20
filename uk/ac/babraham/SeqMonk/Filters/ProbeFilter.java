/**
 * Copyright 2009-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Filters;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

/**
 * A class representing a generic filter from which all 
 * of the actual filters derive
 */
public abstract class ProbeFilter implements Runnable,Cancellable {

	protected final DataCollection collection;
	protected final ProbeList startingList;
	private ArrayList<ProgressListener> listeners = new ArrayList<ProgressListener>();
	private ArrayList<OptionsListener> optionsListeners = new ArrayList<OptionsListener>();
	protected boolean cancel = false;
	public static final int EXACTLY = 1;
	public static final int AT_LEAST = 2;
	public static final int NO_MORE_THAN = 3;
	
	/**
	 * Instantiates a new probe filter.
	 * 
	 * @param collection The dataCollection
	 * @throws SeqMonkException if the dataCollection isn't quantitated
	 */
	public ProbeFilter (DataCollection collection) throws SeqMonkException {
		if (! collection.isQuantitated()) {
			throw new SeqMonkException("You must quantitate your data before running filters.");
		}
		this.collection = collection;
		startingList = collection.probeSet().getActiveList();
	}
	
	public ProbeFilter (DataCollection collection,ProbeList startingList) throws SeqMonkException {
		if (! collection.isQuantitated()) {
			throw new SeqMonkException("You must quantitate your data before running filters.");
		}
		this.collection = collection;
		this.startingList = startingList;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
	}
	
	/**
	 * Starts the filter running.  This will start a new thread implemented
	 * by the filter and return immediately.  Further progress will only be 
	 * reported via the listeners.
	 * 
	 * @throws SeqMonkException if the filter is not ready to run.
	 */
	public void runFilter () throws SeqMonkException {
		if (! isReady()) {
			throw new SeqMonkException("Filter is not ready to run");
		}
		
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * Adds a progress listener.
	 * 
	 * @param l The progress listener to add
	 */
	public void addProgressListener (ProgressListener l) {
		if (l == null) {
			throw new NullPointerException("ProgressListener can't be null");
		}
		
		if (! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	/**
	 * Removes a progress listener.
	 * 
	 * @param l The progress listener to remove
	 */
	public void removeProgressListener (ProgressListener l) {		
		if (l !=null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	/**
	 * Adds an options listener.
	 * 
	 * @param l The options listener to add
	 */
	public void addOptionsListener (OptionsListener l) {
		if (l == null) {
			throw new NullPointerException("OptionsListener can't be null");
		}
		
		if (! optionsListeners.contains(l)) {
			optionsListeners.add(l);
		}
	}

	/**
	 * Removes an options listener.
	 * 
	 * @param l The options listener to remove
	 */
	public void removeOptionsListener (OptionsListener l) {		
		if (l !=null && optionsListeners.contains(l)) {
			optionsListeners.remove(l);
		}
	}

	
	/**
	 * A shortcut method if you're processing one probe at a time.  This allows
	 * you to call this method with every probe and it will put up progress at
	 * suitable points and add a suitable message
	 * 
	 * @param current The current number of probes processed
	 * @param total The progress value at completion
	 */
	protected void progressUpdated(int current, int total) {
		if (current % ((total/100)+1) == 0) {
			progressUpdated("Processed "+current+" out of "+total+" probes", current, total);
		}
	}
	
	/**
	 * Passes on Progress updated messages to all listeners
	 * 
	 * @param message The message to display
	 * @param current The current progress value
	 * @param total The progress value at completion
	 */
	protected void progressUpdated(String message, int current, int total) {
		
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressUpdated(message, current, total);
		}
	}
	
	
	/**
	 * Passes on Options changed message to all listeners
	 */
	protected void optionsChanged () {
		Iterator<OptionsListener> i = optionsListeners.iterator();
		while (i.hasNext()) {
			i.next().optionsChanged();
		}
	}
	
	/**
	 * Passes on Progress cancelled message to all listeners
	 */
	protected void progressCancelled () {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressCancelled();
		}
	}
	
	/**
	 * Passes on Progress exception received message to all listeners
	 * 
	 * @param e The exception
	 */
	protected void progressExceptionReceived (Exception e) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressExceptionReceived(e);
		}
	}
		
	/**
	 * Passes on Filter finished message to all listeners
	 * 
	 * @param newList The newly created probe list
	 */
	protected void filterFinished(ProbeList newList) {
		newList.setName(listName());
		newList.setDescription(listDescription());
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressComplete("new_probe_list", newList);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run () {
		generateProbeList();
	}

	/**
	 * List name. This just needs to be a short reasonable name
	 * for the newly created list.
	 * 
	 * @return A suitable name for the newly generated probe list.
	 */
	abstract protected String listName ();
	
	/**
	 * List description.  This should provide a complete but concise summary
	 * of all of the options selected when the filter was run.  This description
	 * doesn't have to be computer parsable but it should be able to be interpreted
	 * by a human.
	 * 
	 * @return A suitable description for the newly generated probe list
	 */
	abstract protected String listDescription ();
	
	/**
	 * Start the generation of the probe list.  This will be called from within
	 * a new thread so you don't need to implemet threading within the filter.
	 */
	abstract protected void generateProbeList();
	
	/**
	 * Checks if the currently set options allow the filter to be run
	 * 
	 * @return true, if the filter is ready to run.
	 */
	abstract public boolean isReady ();

	/**
	 * Checks if this filter has an options panel.
	 * 
	 * @return true, if the filter has an options panel
	 */
	abstract public boolean hasOptionsPanel ();
	
	/**
	 * Gets the options panel.
	 * 
	 * @return The options panel
	 */
	abstract public JPanel getOptionsPanel();
	
	/**
	 * Name.
	 * 
	 * @return The name of this filter
	 */
	abstract public String name ();
	
	/**
	 * Description.
	 * 
	 * @return A longer description describing what this filter does.
	 */
	abstract public String description ();
	
	
	
}
