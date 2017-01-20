/**
 * Copyright Copyright 2010-17 Simon Andrews
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

import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

/**
 * The listener interface for receiving probeGenerator events.
 * The class that is interested in processing a probeGenerator
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addProbeGeneratorListener<code> method. When
 * the probeGenerator event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see ProbeGeneratorEvent
 */
public interface ProbeGeneratorListener {
	
	/**
	 * Update generation progress.
	 * 
	 * @param message The message to display
	 * @param current The current level of progress
	 * @param total The expected level of progress upon completion
	 */
	public void updateGenerationProgress(String message, int current, int total);
	
	/**
	 * Generation complete.
	 * 
	 * @param probes The probeset which was created
	 */
	public void generationComplete (ProbeSet probes);
	
	/**
	 * Generation exception received.
	 * 
	 * @param e The exception generated
	 */
	public void generationExceptionReceived (Exception e);
	
	/**
	 * A flag to indicate that probe generation was cancelled
	 */
	public void generationCancelled();
	
	/**
	 * A flag to indicate that options changed and are now ready
	 */
	public void optionsReady();
	
	/**
	 * A flag to indicate that options changed and are now not ready.
	 */
	public void optionsNotReady();
}
