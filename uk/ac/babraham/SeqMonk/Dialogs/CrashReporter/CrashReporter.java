/**
 * Copyright Copyright 2010-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.CrashReporter;

import java.awt.GraphicsEnvironment;

public class CrashReporter {

	/**
	 * This is a wrapper class around our crash reporting system.  If we're in
	 * a headless environment we just spit out the error to the log and die.
	 * 
	 * If we're in a graphical environment we forward the crash to the crash
	 * reporting dialog which allows the user to submit it to us for further
	 * analysis.
	 */

	
	public CrashReporter (Throwable e) {
	
		if (GraphicsEnvironment.isHeadless()) {
			
			System.err.println("SeqMonk hit a bug - sorry about that.  The crash report is below.");
			System.err.println("For help with this issue please email the whole of the text of this");
			System.err.println("report to babraham.bioinformatics@babraham.ac.uk");
			System.err.println("### BEGIN REPORT ###");
			
			System.err.println(e.getMessage());
			e.printStackTrace();
			
			System.err.println("### END REPORT ###");
			
			System.exit(2);
			
			
		}
		else {
			// In case the crash reporter causes an issue, don't trigger 
			// recursive crashes.
			try {
				new CrashReporterDialog(e);
			}
			catch (Exception er) {
				e.printStackTrace();
				er.printStackTrace();
			}
		}
		
		
	}
		
		
}
