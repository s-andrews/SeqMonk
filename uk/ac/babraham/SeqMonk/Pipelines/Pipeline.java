/**
 * Copyright Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Pipelines;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Filters.OptionsListener;

/**
 * An abstract representation of an analysis pipeline.
 * 
 * Pipelines generally implement a combined probe generation and
 * quantitation.  They can optionally implement a default set of
 * filters.  There is no real standardisation to how a pipline 
 * should run since it can use all of the facilities available within
 * the SeqMonk data model.  It doesn't have to stick to aggregating
 * other existing tools, it can do things itself.
 */
public abstract class Pipeline implements Cancellable, Runnable {

	/** The data collection on which the pipeline will be working */
	private DataCollection collection;
	
	/** The set of listeners listening to the progress of the quantitation. */
	private ArrayList<ProgressListener> listeners = new ArrayList<ProgressListener>();
	
	/** The set of listeners listening to the change of options for the quantitation */
	private ArrayList<OptionsListener> optionsListeners = new ArrayList<OptionsListener>();
	
	/** A flag to say whether we need to cancel */
	protected boolean cancel = false;

	/** A fixed log2 value to make log2 calculation quicker */
	protected final float log2 = (float)Math.log(2);

	/** The list of stores which are actually going to be processed by the pipeline */
	protected DataStore [] data;
	
	public Pipeline (DataCollection collection) {
		this.collection = collection;
	}
	
	public abstract String name();
	
	public String toString () {
		return name();
	}
	
	public DataCollection collection () {
		return collection;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
	}
	
	/**
	 * Gets an options panel which can display and set the options for the pipeline
	 * 
	 * @param application The enclosing SeqMonkApplication
	 * @return A panel to display and set the options for the quantitation.
	 */
	public abstract JPanel getOptionsPanel(SeqMonkApplication application);
	
	/**
	 * Says if this pipeline will wipe out any existing probes we may have
	 * 
	 * @return
	 */
	public abstract boolean createsNewProbes ();
	
	/**
	 * Says whether the current options allow the quantitation to be run.
	 * 
	 * @return true, if it is ready to run.
	 */
	public abstract boolean isReady();
		
	/**
	 * Start quantitating the data
	 * 
	 * @param data The sets of data to quantitate
	 */
	public void runPipeline (DataStore [] data) {
		this.data = data;
		Thread t = new Thread(this);
		t.start();
	}
	
	public void run () {
		startPipeline();
	}
	
	/** This method will be called by the superclass in a separate thread
	 * and should contain the code to actually do the pipeline processing. 
	 */
	protected abstract void startPipeline ();

	/**
	 * Adds a progress listener.
	 * 
	 * @param l The listener to add.
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
	 * @param l The listener to remove
	 */
	public void removeProgressListener (ProgressListener l) {		
		if (l !=null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	/**
	 * Adds an options listener.
	 * 
	 * @param l The listener to add
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
	 * Removes a options listener.
	 * 
	 * @param l The listener to remove
	 */
	public void removeOptionsListener (OptionsListener l) {		
		if (l !=null && optionsListeners.contains(l)) {
			optionsListeners.remove(l);
		}
	}
	
	/**
	 * Informs all progress listeners of an update to the progress
	 * 
	 * @param message The message to display
	 * @param current The current level of completion
	 * @param total The final level of completion
	 */
	protected void progressUpdated(String message, int current, int total) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressUpdated(message, current, total);
		}
		try {
			Thread.sleep(10);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Informs all options listeners that the quantitation options have changed
	 */
	protected void optionsChanged () {
		Iterator<OptionsListener> i = optionsListeners.iterator();
		while (i.hasNext()) {
			i.next().optionsChanged();
		}
	}
	
	/**
	 * Informs all progress listeners that an exception was received
	 * 
	 * @param e The exception received
	 */
	protected void progressExceptionReceived (Exception e) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressExceptionReceived(e);
		}
	}

	/**
	 * Informs all progress listeners that a warning was recieved
	 * 
	 * @param e The exception received
	 */
	protected void progressWarningReceived (Exception e) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressWarningReceived(e);
		}
	}

	
	/**
	 * Informs all progress listeners that quantitation was cancelled.
	 */
	protected void progressCancelled () {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressCancelled();
		}
	}
	
	/**
	 * Informs all progress listeners that quantitation is complete.
	 */
	protected void quantitatonComplete() {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressComplete("pipeline_quantitation",null);
		}
	}

}
