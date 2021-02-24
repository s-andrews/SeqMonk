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
package uk.ac.babraham.SeqMonk.Network;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;

/**
 * The UpdateChecker allows the program to check on the main SeqMonk
 * site to determine if a newer version of the program has been
 * released so we can prompt the user to get the update.
 */
public class UpdateChecker {

	private static String latestVersion = null;
	
	/**
	 * Checks if an is update available.
	 * 
	 * @return true, if an update is available
	 * @throws SeqMonkException if we were unable to check for an update
	 */
	public static boolean isUpdateAvailable () throws SeqMonkException {
		if (latestVersion == null) {
			getLatestVersionNumber();
		}
		return UpdateChecker.isNewer(SeqMonkApplication.VERSION, latestVersion);
	}
	
	/**
	 * Compares a local and remote version string to see if the remote
	 * version is newer.
	 * 
	 * @param thisVersion The version string from the currently running program
	 * @param remoteVersion The version string from the latest remote version
	 * @return true, if the remote version is newer
	 */
	private static boolean isNewer (String thisVersion, String remoteVersion) {
		
		String [] thisSections = thisVersion.split("[ \\.]");
		String [] remoteSections = remoteVersion.split("[ \\.]");
		
		for (int i=0;i<Math.min(thisSections.length,remoteSections.length);i++) {
						
			if (thisSections[i].toLowerCase().equals("devel")) {
				// A released version is always newer than a devel version
				return true;
			}
			
			int thisNumber = Integer.parseInt(thisSections[i]);
			int remoteNumber = Integer.parseInt(remoteSections[i]);
			
			if (remoteNumber > thisNumber) {
				// The remote version is higher
				return true;
			}
			else if (thisNumber > remoteNumber) {
				// This version is higher
				System.err.println("Local version ("+thisVersion+") is higher than the remote ("+remoteVersion+")");
				return false;
			}
		}
		
		// If we get to here then all of the common sections were the
		// same.  The remote version is therefore newer if it's longer
		// than the local version
		
		// If the local version is longer then the remote still wins if
		// the local is a devel version and the remote is a final
		
		if (remoteSections.length > thisSections.length) {
			return true;
		}
		
		if (thisSections.length > remoteSections.length && thisSections[remoteSections.length].toLowerCase().equals("devel")) {
			return true;
		}
		else if (thisSections.length > remoteSections.length) {
			System.err.println("Local version ("+thisVersion+") is higher than the remote ("+remoteVersion+")");			
		}
		
		
		return false;
		
	}

	
	/**
	 * Gets the latest version number from the main SeqMonk site
	 * 
	 * @return The version string from the remote site
	 * @throws SeqMonkException if the remote version couldn't be retrieved
	 */
	public static String getLatestVersionNumber () throws SeqMonkException  {
	
		try {
			
			URL updateURL = new URL("https","www.bioinformatics.babraham.ac.uk","/projects/seqmonk/current_version.txt");
			
			HttpsURLConnection connection = (HttpsURLConnection)updateURL.openConnection();
			connection.setUseCaches(false);
			
			DataInputStream d = new DataInputStream(new BufferedInputStream(connection.getInputStream()));
	
			byte [] data = new byte[255]; // A version number should never be more than 255 bytes
			int bytesRead = d.read(data);
			
			byte [] actualData = new byte[bytesRead];
			for (int i=0;i<bytesRead;i++) {
				actualData[i] = data[i];
			}
			
			latestVersion = new String(actualData);
			latestVersion.replaceAll("[\\r\\n]", "");
			latestVersion = latestVersion.trim();
			
			return latestVersion;
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new SeqMonkException("Couldn't contact the update server to check for updates");
		}
	}
	
	
}
