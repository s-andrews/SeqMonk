/**
 * Copyright 2013-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.ProbeListReport;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.HTMLFileFilter;

public class ProbeListReportCreator {

	private ProbeListReport report;

	public ProbeListReportCreator (ProbeList list) {

		report = new ProbeListReport(list);

		JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
		chooser.setMultiSelectionEnabled(false);
		chooser.addChoosableFileFilter(new HTMLFileFilter());

		int result = chooser.showSaveDialog(SeqMonkApplication.getInstance());
		if (result == JFileChooser.CANCEL_OPTION) return;

		File file = chooser.getSelectedFile();
		SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);

		if (file.isDirectory()) return;

		if (! (file.getPath().toLowerCase().endsWith(".html") || file.getPath().toLowerCase().endsWith(".html")) ) {
			file = new File(file.getPath()+".html");
		}

		// Check if we're stepping on anyone's toes...
		if (file.exists()) {
			int answer = JOptionPane.showOptionDialog(SeqMonkApplication.getInstance(),file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

			if (answer > 0) {
				return;
			}
		}

		try {					
			PrintWriter pr = new PrintWriter(file);
			pr.print(report.getHTML());
			pr.close();
			
			// Try to launch the default browser to display the report
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(file.toURI());
			}
			
		}

		catch (UnsupportedOperationException uoe) {
			// This platform doesn't allow us to open a browser 
			// automatically.  Print a message instead.
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "<html>Your report has been saved at<br><br>"+file.getAbsolutePath()+"<br><br>Open this file in a browser to view the report.", "Report created", JOptionPane.INFORMATION_MESSAGE);
		}
		
		catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}



}
