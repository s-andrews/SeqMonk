/**
 * Copyright Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Quantitation;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Filters.OptionsListener;

/**
 * An abstract representation of a quantitation method
 */
public abstract class Quantitation implements Cancellable, Runnable {

	/** The set of listeners listening to the progress of the quantitation. */
	private ArrayList<ProgressListener> listeners = new ArrayList<ProgressListener>();
	
	/** The set of listeners listening to the change of options for the quantitation */
	private ArrayList<OptionsListener> optionsListeners = new ArrayList<OptionsListener>();
	
	/** A flag to say whether we need to cancel */
	protected boolean cancel = false;

	/** A fixed log2 value to make log2 calculation quicker */
	protected final float log2 = (float)Math.log(2);

	protected SeqMonkApplication application;
	
	public Quantitation (SeqMonkApplication application) {
		this.application = application;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
	}
	
	/**
	 * Gets an options panel which can display and set the options for the quantitation
	 * 
	 * @return A panel to display and set the options for the quantitation.
	 */
	public abstract JPanel getOptionsPanel();
	
	/**
	 * Says whether the current options allow the quantitation to be run.
	 * 
	 * @return true, if it is ready to run.
	 */
	public abstract boolean isReady();
	
	/**
	 * Says whether this quantitation method relies on the presence of
	 * an existing quantitation for the data, or whether it can start
	 * from unquantitated data.
	 * 
	 * @return true if existing quantitation is required.
	 */
	public abstract boolean requiresExistingQuantitation();

	
	/**
	 * Says whether this quantitation method can use an existing 
	 * quantitation if present, but doesn't require it.  Setting
	 * this will prevent the datastores from wiping their existing
	 * quantitations before new quantitations are added.
	 * 
	 * @return true if existing quantitation is required.
	 */
	public boolean canUseExistingQuantitation() {
		return false;
	}

	
	/**
	 * Provides a text description of the options used in this
	 * quantitation.  If the quantitation extends a previous 
	 * one then it should keep the previous information in tact
	 * 
	 * @return Description of the options used in quantitation.
	 */
	public abstract String description();
	
	
	/**
	 * Says whether this quantitation method relies on the presence of
	 * HiC read pair data.
	 * 
	 * @return true if HiC data is required.
	 */
	public boolean requiresHiC() {
		return false;
	}
	
	/**
	 * Start quantitating the data
	 * 
	 * @param data The sets of data to quantitate
	 */
	public abstract void quantitate (DataStore [] data);

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
	 * where the update is solely the number of probes processed.
	 * This call will work out an appropriate interval at which to
	 * inform the listeners and can therefore be safely called with
	 * every probe processed
	 * 
	 * @param current The number of probes processed
	 * @param total The total number of probes
	 */
	protected void progressUpdated(int current, int total) {
		if (current % ((total/100)+1) == 0) {
			progressUpdated("Quantitated "+current+" out of "+total+" probes", current, total);
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
	 * Informs all progress listeners that an exception was received
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
		
		//Add the new quantitation description to the probe set.
		application.dataCollection().probeSet().setCurrentQuantitation(description());
		
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressComplete("data_quantitation",null);
		}
	}

}
