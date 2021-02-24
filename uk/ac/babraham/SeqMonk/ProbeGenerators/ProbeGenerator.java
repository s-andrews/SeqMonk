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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

/**
 * A probe generator is the method through which new probesets are created.
 */
public abstract class ProbeGenerator implements Cancellable {

	//TODO: We should move away from using a ProbeGeneratorListener and switch to standard OptionsListener and ProgressListener
	
	/** A list of progress listeners */	
	private Vector<ProbeGeneratorListener> listeners = new Vector<ProbeGeneratorListener>();
	
	/** The data collection. */
	protected DataCollection collection;
	
	/** A flag to say if we need to cancel */
	protected boolean cancel = false;
	
	/**
	 * Instantiates a new probe generator.
	 * 
	 * @param collection The dataCollection to which this probeset will be added
	 */
	public ProbeGenerator (DataCollection collection) {
		this.collection = collection;
	}
	
	public abstract boolean requiresExistingProbeSet ();
	
	/**
	 * Returns an option panel which is able to display and change all of
	 * the options for the generator.
	 * 
	 * @param collection the application
	 * @return the options panel
	 */
	public abstract JPanel getOptionsPanel();
	
	/**
	 * Adds the probe generator listener.
	 * 
	 * @param listener A new probe generator listener to add
	 */
	public void addProbeGeneratorListener (ProbeGeneratorListener listener) {
		if (listener != null && ! listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Update generation progress.
	 * 
	 * @param message The message to display
	 * @param current The current level of progress
	 * @param total The expected level of progress upon completion
	 */
	public void updateGenerationProgress(String message, int current, int total) {
		Enumeration<ProbeGeneratorListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().updateGenerationProgress(message, current, total);
		}
	}
	
	/**
	 * Generation complete.
	 * 
	 * @param probes The probeset which was created
	 */
	public void generationComplete (ProbeSet probes) {
		Enumeration<ProbeGeneratorListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().generationComplete(probes);
		}

	}
	
	/**
	 * Generation exception received.
	 * 
	 * @param e The exception generated
	 */
	public void generationExceptionReceived (Exception e) {
		Enumeration<ProbeGeneratorListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().generationExceptionReceived(e);
		}

	}
	
	/**
	 * A flag to indicate that probe generation was cancelled
	 */
	public void generationCancelled() {
		Enumeration<ProbeGeneratorListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().generationCancelled();
		}

	}
	
	/**
	 * A flag to indicate that options changed and are now ready
	 */
	public void optionsReady() {
		Enumeration<ProbeGeneratorListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().optionsReady();
		}

	}
	
	/**
	 * A flag to indicate that options changed and are now not ready.
	 */
	public void optionsNotReady() {
		Enumeration<ProbeGeneratorListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().optionsNotReady();
		}

	}
	
	
	/**
	 * Checks if the generator has enough options set to begin to run.
	 * 
	 * @return true, if ready
	 */
	public abstract boolean isReady();
	
	/**
	 * Start the actual probe generation.  This should be implemented in a new
	 * thread.  This method is expected to return immediately.  Further notification
	 * about progress and completion of probe generation should happen via the
	 * listener methods.
	 * 
	 * @param removeBlankProbes True if probes containing no reads should be excluded
	 */
	public abstract void generateProbes ();
	
}
