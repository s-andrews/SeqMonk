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
			new CrashReporterDialog(e);
		}
		
		
	}
		
		
}
