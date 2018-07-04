/**
 * Copyright 2014-18 Simon Andrews
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

public class RVersionTest {

	public static String testRVersion (String pathToR) throws IOException {

		// R sucks.
		// If you're on windows then the output of R --version comes out on STDERR, if you're
		// on a Mac then it comes out on STDOUT so we have to check both of them.
		
		ProcessBuilder process = new ProcessBuilder(pathToR,"--version");

		Process p = process.start();

		BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		String versionLine = null;
		String line;
		while ((line = br.readLine()) != null) {
			if (versionLine == null) {
				versionLine = line;
				if (!versionLine.startsWith("R version")) {
					throw new IOException("Got a version but it didn't look like R: "+versionLine);
				}
				
				versionLine = versionLine.replaceFirst("R version ", "");
				versionLine = versionLine.replaceFirst(" .*", "");
			}
		}		

		br = new BufferedReader(new InputStreamReader(p.getInputStream()));

		while ((line = br.readLine()) != null) {
			if (versionLine == null) {
				versionLine = line;
				if (!versionLine.startsWith("R version")) {
					throw new IOException("Got a version but it didn't look like R: "+versionLine);
				}
				
				versionLine = versionLine.replaceFirst("R version ", "");
				versionLine = versionLine.replaceFirst(" .*", "");
			}
		}		

		
		try {
			p.waitFor();
		}
		catch (InterruptedException ie) {}

		if (p.exitValue() != 0) {
			throw new IOException("R failed to complete (exit "+p.exitValue()+")");
		}
		
		if (versionLine == null) {
			throw new IOException("R ran but we never saw anything which looked like a version line");
		}

		return versionLine;

	}

	public static boolean hasRDependencies () throws IOException {
		
		File tempDir = TempDirectory.createTempDirectory();
		
		// Get the template script
		Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/r_dependencies.r"));
				
		// Write the script file
		File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
		PrintWriter pr = new PrintWriter(scriptFile);
		pr.print(template.toString());
		pr.close();
		
		RScriptRunner runner = new RScriptRunner(tempDir);
		RProgressListener listener = new RProgressListener(runner);
		runner.runScript();

		while (true) {
			if (listener.cancelled()) {
				runner.cleanUp();
				return false;
			}
			if (listener.exceptionReceived()) {
				runner.cleanUp();
				return false;
			}
			if (listener.complete()) break;
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}			
		}

		runner.cleanUp();
		return true;

	}

	
	
	public static String autoDetectRLocation () {
		
		// See if it's in the path first
		try {
			testRVersion("R");
			return ("R");
		}
		catch (IOException e) {}
		
		// Try the same thing with a windows extension
		try {
			testRVersion("R.exe");
			return ("R.exe");
		}
		catch (IOException e) {}
		
		// Try the usual suspects
		try {
			testRVersion("/usr/local/bin/R");
			return ("/usr/local/bin/R");
		}
		catch (IOException e) {}
		try {
			testRVersion("/usr/bin/R");
			return ("/usr/bin/R");
		}
		catch (IOException e) {}
		try {
			testRVersion("/bin/R");
			return ("/bin/R");
		}
		catch (IOException e) {}
		
		// Go exploring if we're on windows
		File progFiles = new File("C:/Program Files/R/");
		String [] archs = new String [] {"x64","i386"}; 

		if (progFiles.exists() && progFiles.isDirectory()) {
			File [] installDirs = progFiles.listFiles();
						
			for (int f=installDirs.length-1;f>=0;f--) {
				for (int a=0;a<archs.length;a++) {
					File rTest = new File(installDirs[f].getAbsoluteFile()+"/bin/"+archs[a]+"/R.exe");
					if (rTest.exists()) {
						try {
							testRVersion(rTest.getAbsolutePath());
							return (rTest.getAbsolutePath());
						}
						catch (IOException e) {}					
					}
				}
			}	
		}
		
		progFiles = new File("C:/Program Files (x86)/R/");
		if (progFiles.exists() && progFiles.isDirectory()) {
			File [] installDirs = progFiles.listFiles();
			
			for (int f=installDirs.length-1;f>=0;f--) {
				for (int a=0;a<archs.length;a++) {
					File rTest = new File(installDirs[f].getAbsoluteFile()+"/bin/"+archs[a]+"/R.exe");
					if (rTest.exists()) {
						try {
							testRVersion(rTest.getAbsolutePath());
							return (rTest.getAbsolutePath());
						}
						catch (IOException e) {}					
					}
				}			
			}		
		}
		
		return null;
	}
	

//	public static void main (String [] args) {
//		System.out.println("Found R at "+autoDetectRLocation());
//	}
	
}


