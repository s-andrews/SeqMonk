/**
 * Copyright 2014- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.R;

import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;

public class RProgressListener implements ProgressListener {

	private boolean exceptionReceived = false;
	private Exception receivedException = null;
	private boolean complete = false;
	private boolean warningReceived = false;
	private boolean cancelled = false;
	
	public RProgressListener (RScriptRunner scriptRunner) {
		scriptRunner.addProgressListener(this);
	}

	public void progressExceptionReceived(Exception e) {
		exceptionReceived = true;
		receivedException = e;
	}
	
	public Exception exception () {
		return receivedException;
	}
	
	public boolean exceptionReceived () {
		return exceptionReceived;
	}

	public void progressWarningReceived(Exception e) {
		warningReceived = true;
	}

	public boolean warningReceived () {
		return warningReceived;
	}
	
	public void progressUpdated(String message, int current, int max) {
//		System.err.println(message);
	}

	public void progressCancelled() {
		cancelled = true;
	}
	
	public boolean cancelled () {
		return cancelled;
	}

	public void progressComplete(String command, Object result) {
		complete = true;
	}
	
	public boolean complete () {
		return complete;
	}
	
	
}
