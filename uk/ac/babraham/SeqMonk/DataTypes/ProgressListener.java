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
package uk.ac.babraham.SeqMonk.DataTypes;

/**
 * The listener interface for receiving progress events.
 * The class that is interested in processing a progress
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addProgressListener<code> method. When
 * the progress event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see ProgressEvent
 */
public interface ProgressListener {

	/**
	 * This interface is used generically for any operation which is spawned
	 * in a new thread and which needs to be followed and acted upon on
	 * completion.  We used to have many different similar classes for different
	 * types of events, but these have been consolidated into this single class
	 * to make maintenance easier - this explains the somewhat generic way of
	 * transferring the data we get at the end of the process
	 * 
	 * @param e the e
	 */
	
	/**
	 * Sending an exception via this method indicates that the process has
	 * terminated due to this error and will never return it's intended
	 * result.
	 */
	public void progressExceptionReceived (Exception e);
	
	/**
	 * Sending an exception via the warning method indicates a non-fatal
	 * error and that the process will continue and a result will still be
	 * produced.  Classes implementing this method might wish to store these
	 * warnings to let the user know what went wrong when the result is
	 * finally collected.
	 * 
	 * @param e the e
	 */
	public void progressWarningReceived (Exception e);
	
	/**
	 * The progress update will update any interactive displays to show the
	 * current level of progress.  It is intended that each process goes through
	 * only one full cycle of 0-100% complete, but this is not enforced.
	 * 
	 * @param message the message
	 * @param current the current
	 * @param max the max
	 */
	public void progressUpdated (String message, int current, int max);

	/**
	 * This method will be called if the progress is cancelled.  In this case
	 * no result will be returned, but there was no error.  This ususally happens
	 * as the result of a user initiated cancellation.
	 */
	public void progressCancelled ();
	
	/**
	 * Once the watched process completes the progress finished method will be
	 * called.  This method can return an object of results and a string indicating
	 * what needs to be done with the attached result.  Methods can choose to
	 * not return an object in the results if that makes sense, but they should
	 * put a sensible command name in the string.
	 * 
	 * @param command the command
	 * @param result the result
	 */
	public void progressComplete (String command, Object result);
}
