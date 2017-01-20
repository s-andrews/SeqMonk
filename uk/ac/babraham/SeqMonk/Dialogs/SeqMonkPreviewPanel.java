/**
 * Copyright 2011-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class SeqMonkPreviewPanel extends JPanel implements PropertyChangeListener {

	private JLabel label;
	
	public SeqMonkPreviewPanel () {
		
		setPreferredSize(new Dimension(300,300));
		
		setLayout(new BorderLayout());
		
		label = new JLabel("No file selected",JLabel.CENTER);
		
		add(label,BorderLayout.CENTER);
		
	}
	
	public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
                
        // Make sure we are responding to the right event.
        if (propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File selection = (File)e.getNewValue();
                        
            if (selection == null || selection.isDirectory()) {
            	clearText();
            }
            else {
            	previewFile(selection);
            }
            
        }
    }
	
	private void clearText() {
		label.setText("No file selected");
	}
	
	private void previewFile (File file) {
		
		FileInputStream fis = null;
		BufferedReader br = null;
		
		try {
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(fis)));
		}
		catch (IOException ioe) {
			
			try {
				if (fis != null) {
					fis.close();
				}
				br = new BufferedReader(new FileReader(file));
			}
			catch (IOException ioex) {
				label.setText("Failed to read file");
				return;
			}
		}

		try {
			
			// Read the header into a separate variable in case they've clicked on
			// an empty file.  This way we can check for a null value from reading
			// the first line.
			String header = br.readLine();
			
			if (header == null || ! header.startsWith("SeqMonk Data Version")) {
				label.setText("Not a SeqMonk file");
				br.close();
				return;
			}

			StringBuffer sb = new StringBuffer();
			sb.append("<html>");
				
			// The next line should be the genome species and version
				
			String genome = br.readLine();
			if (! genome.startsWith("Genome\t")) {
				label.setText("Not a SeqMonk file");
				br.close();
				return;					
			}

			genome = genome.replaceAll("\t", " ");
				
			sb.append(genome);
			sb.append("<br><br>");
				
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
					sb.append("Samples:<br>");
					int sampleCount = Integer.parseInt((line.split("\t"))[1]);
					for (int i=0;i<sampleCount;i++) {
						line = br.readLine();
						sb.append((line.split("\t"))[0]);
						sb.append("<br>");
					}
					sb.append("<br>");
					break;
				}
				
			}
			
			if (linesRead >= 100000) {
				sb.append("Couldn't find samples at top of file");
			}
								
			br.close();
			sb.append("</html>");
			label.setText(sb.toString());
			
		} 
		catch (IOException ex) {
			ex.printStackTrace();
			label.setText("Failed to read file");
		}		
	}
		
}
