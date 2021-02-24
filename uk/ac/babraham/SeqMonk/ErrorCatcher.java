/**
 * Copyright 2009- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk;

import uk.ac.babraham.SeqMonk.Dialogs.CrashReporter.CrashReporter;

/**
 * The error catcher can be attached to the main JVM and is triggered
 * any time a throwable exception makes it back all the way through the
 * stack without being caught so we don't miss any errors.
 */
public class ErrorCatcher implements Thread.UncaughtExceptionHandler {

	/* (non-Javadoc)
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	public void uncaughtException(Thread thread, Throwable t) {
		try {
			new CrashReporter(t);
		}
		catch (Exception ex) {
			t.printStackTrace();
			ex.printStackTrace();
		}
	}
	
}
