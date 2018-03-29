/**
 * Copyright Copyright 2017-18 Simon Andrews
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


package uk.ac.babraham.SeqMonk.Dialogs.SeqMonkPreview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class SeqMonkPreview {
	
	private String species = "No genome found";
	private String assembly = "No assembly found";
	private String [] samples = new String [] {"No samples found"};

	public SeqMonkPreview (File file) throws IOException {
		previewFile(file);
	}
	
	private void previewFile(File file) throws IOException {
		FileInputStream fis = null;
		BufferedReader br = null;
		
		try {
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(fis)));
		}
		catch (IOException ioe) {
			
			if (fis != null) {
				fis.close();
			}
			br = new BufferedReader(new FileReader(file));
		}

			// Read the header into a separate variable in case they've clicked on
			// an empty file.  This way we can check for a null value from reading
			// the first line.
			String header = br.readLine();
			
			if (header == null || ! header.startsWith("SeqMonk Data Version")) {
				br.close();
				throw new IOException("Not a SeqMonk file");
			}

				
			// The next line should be the genome species and version
				
			String genome = br.readLine();
			if (! genome.startsWith("Genome\t")) {
				br.close();
				throw new IOException("Not a SeqMonk file");
			}

			String [] sections = genome.split("\t");
			species = sections[1];
			assembly = sections[2];
			
	
			int linesRead = 0;
			// Next we keep going until we hit the samples line, but we'll
			// give up if we haven't found the sample information after
			// 10k lines
			while (linesRead < 100000) {
				++linesRead;
				String line = br.readLine();
				if (line == null) {
					break;
				}
					
				if (line.startsWith("Samples\t")) {
					int sampleCount = Integer.parseInt((line.split("\t"))[1]);
					samples = new String[sampleCount];
					for (int i=0;i<sampleCount;i++) {
						line = br.readLine();
						samples[i] = (line.split("\t"))[0];
					}
					break;
				}
				
			}
			
			if (linesRead >= 100000) {
				throw new IOException("Couldn't find samples at top of file");
			}
								
			br.close();
	}

	public String species () {
		return species;
	}
	
	public String assembly () {
		return assembly;
	}
	
	public String [] samples () {
		return samples;
	}
	
	public String toString () {
		StringBuffer sb = new StringBuffer();
		sb.append("<html>");
		sb.append(species());
		sb.append(" ");
		sb.append(assembly());
		sb.append("<br><br>");
		
		for (int i=0;i<samples.length;i++) {
			sb.append(samples[i]);
			sb.append("<br>");
		}
		
		sb.append("</html>");
		return(sb.toString());
		
	}
	
	
}
