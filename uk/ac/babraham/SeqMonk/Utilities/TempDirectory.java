/**
 * Copyright 2014-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities;

import java.io.File;
import java.io.IOException;

import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

public class TempDirectory {

	public static File createTempDirectory () throws IOException {
		// This makes a temp directory inside the current cache directory
		
		// Java 6 doesn't have a mechanism to make a temp directory, so we make a temp
		// file and then convert it to being a directory.
		
		File tempFile = File.createTempFile("seqmonk_", "_temp", SeqMonkPreferences.getInstance().tempDirectory());
	
		int count = 0;
		
		while (true) {
			
			File tempDir = new File(tempFile+"d"+count);
			if (tempDir.mkdir()) {
				tempFile.delete();
				return tempDir;
			}
			else {
				++count;
			}
			
			if (count > 100) {
				throw new IOException("Failed to make a temp directory from "+tempFile.getAbsolutePath());
			}
			
		}
		
		
	}
	
	
}
