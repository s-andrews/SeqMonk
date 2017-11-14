/**
 * Copyright 2014-17 Simon Andrews
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JOptionPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

public class RScriptRunner implements Runnable, Cancellable {
	
	private File directory;
	private File scriptFile;
	private boolean cancel = false;
	
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	
	public RScriptRunner (File directory) {
		this.directory = directory;
		// There should be a file called script.r in the directory
		scriptFile = new File(directory.getAbsolutePath()+"/script.r");
				
		if (!scriptFile.exists()) {	
			throw new IllegalArgumentException("Couldn't find script.r in "+directory.getAbsolutePath());
		}
	}
	
	public File directory () {
		return directory;
	}
	
	public void addProgressListener (ProgressListener l) {
		if (l != null && ! listeners.contains(l)) {
			listeners.add(l);
		}
	}
	
	public void removeListeren (ProgressListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}
	
	public void runScript () {
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		try {
			
			// For some reason if we call scriptFile.getAbsolutePath when we launch our R
			// process then on OSX we get wierd problems with line endings being stuck where
			// we have spaces in our cache path.  Since we've already set the working directory
			// for the script we don't need this anyway and we can just use script.r as the
			// path to the script.
			
//			System.err.println("Creating process");
			ProcessBuilder process = new ProcessBuilder(SeqMonkPreferences.getInstance().RLocation(),"CMD","BATCH","script.r");
			
//			System.err.println("Setting process directory to "+directory.getAbsolutePath());
			process.directory(directory);
	
//			System.err.println("Starting process");
			Process p = process.start();

			int lastLinesOfOutput = 0;
			long lastFileSize = 0;
			
			boolean finishNextTime = false;
			
			while (true) {
				
				// We go around checking to see if the script has finished
				try {
//					int exit = p.exitValue();
					p.exitValue();
					// We don't exit immediately because we want to show the final
					// output in the output file.
					
//					System.err.println("Got valid exit state of "+exit);
					Thread.sleep(500);
					finishNextTime = true;
				}
				catch (IllegalThreadStateException itse) {
					// It's not finished yet.
//					System.err.println("Illegal thread state "+itse.getLocalizedMessage());
				}
		
				if (cancel) {
//					System.err.println("Cancelling");
					p.destroy();
					Enumeration<ProgressListener> en = listeners.elements();
					while (en.hasMoreElements()) {
						en.nextElement().progressCancelled();
					}
					
					// Remove the files we'd made to this point.
					cleanUp();

					return;
				}
				
				// Read the output file and see if there's any new data
				File outputFile = new File(scriptFile.getAbsolutePath()+".Rout");
//				System.err.println("Checking for output file "+outputFile.getAbsolutePath());
				if (outputFile.exists()) {
//					System.err.println("Outfile exists");
					if (outputFile.length() > lastFileSize) {
//						System.err.println("Outfile is bigger than last time");
						lastFileSize = outputFile.length();

//						System.err.println("Reading outfile");
						BufferedReader br = new BufferedReader(new FileReader(outputFile));

						// Skip over the lines we've already sent
//						System.err.println("Skipping first "+lastLinesOfOutput+" lines");
						for (int i=0;i<=lastLinesOfOutput;i++) {
							br.readLine();
						}
						String line;
						while ((line = br.readLine())!=null) {
							if (cancel) break; // Don't keep streaming output if we're cancelling anyway
							++lastLinesOfOutput;
							Enumeration<ProgressListener> en = listeners.elements();
//							System.err.println("Sent new output line "+line);
							while (en.hasMoreElements()) {
								en.nextElement().progressUpdated(line, 0, 1);
								Thread.sleep(50); // Make the output scroll by at a sensible rate
							}
							
						}
						br.close(); 
					}
					Thread.sleep(1000); // Don't poll too often
				}
				
				if (finishNextTime) break;
			}			
			
			if (p.exitValue() != 0) {
				StringBuffer sb = new StringBuffer();
				
				File outputFile = new File(scriptFile.getAbsolutePath()+".Rout");
				if (outputFile.exists()) {
						BufferedReader br = new BufferedReader(new FileReader(outputFile));
						String line;
						while ((line = br.readLine())!=null) {
							sb.append(line);
							sb.append("\n");
						}
						br.close();
				}
				else {
					sb.append("Found no Rout file at "+scriptFile.getAbsolutePath()+".Rout");
				}
				
				throw new RException("R Script failed with exit "+p.exitValue(), sb.toString());
			}
		
			Enumeration<ProgressListener> en = listeners.elements();
			while (en.hasMoreElements()) {
				en.nextElement().progressComplete("r", directory);;
			}
			
			
		}
		catch (Exception e) {
			Enumeration<ProgressListener> en = listeners.elements();
			while (en.hasMoreElements()) {
				en.nextElement().progressExceptionReceived(e);
			}
		}
		
	}
	
	public void cleanUp () throws IOException {
		
		// We simply list everything under the directory and delete it,
		// then delete the directory itself.
		
		// We need to check if we're running in R debug mode.  If we are then
		// we'll not do the cleanup and will give them a warning about this.
		
		if (SeqMonkPreferences.getInstance().suspendRCleanup()) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "R Script directory was not removed as we are in R debug mode", "R Debug Mode", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		
		File [] files = directory.listFiles();
		
		for (int f=0;f<files.length;f++) {
			if (!files[f].delete()) {
				throw new IOException("Failed to delete "+files[f].getAbsolutePath());
			}
		}
		
		// Now remove the directory
		directory.delete();
	
	}

	public void cancel() {
		cancel = true;
	}
	
	
//	public static void main (String [] args) {
//		RScriptRunner r = new RScriptRunner(new File("/Volumes/Data/RTest"));
//		
//		r.addProgressListener(new ProgressListener() {
//			
//			public void progressWarningReceived(Exception e) {
//				e.printStackTrace();
//				
//			}
//			
//			public void progressUpdated(String message, int current, int max) {
//				System.out.println(message);
//			}
//			
//			public void progressExceptionReceived(Exception e) {
//				e.printStackTrace();
//			}
//			
//			public void progressComplete(String command, Object result) {
//				System.out.println("Complete");
//			}
//			
//			public void progressCancelled() {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//		
//		r.runScript();
//		
//		
//	}
	
	
	
}
