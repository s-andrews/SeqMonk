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
package uk.ac.babraham.SeqMonk.Dialogs.SeqMonkPreview;

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
				
		try {
			SeqMonkPreview preview = new SeqMonkPreview(file);
			label.setText(preview.toString());
		}
		catch (IOException ioe) {
			label.setText("Failed to read file");
		}
	}
		
}
