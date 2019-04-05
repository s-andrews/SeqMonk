/**
 * Copyright 2012-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.HTMLFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.VistoryFileFilter;
import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryProjectSummary;
import uk.ac.babraham.SeqMonk.Vistory.VistoryText;
import uk.ac.babraham.SeqMonk.Vistory.VistoryTitle;

public class VistoryToolbar extends JToolBar implements ActionListener {

	private VistoryDialog dialog;
	
	public VistoryToolbar (VistoryDialog dialog) {
		this.dialog = dialog;
		JButton loadButton = new JButton("Load",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/load_project.png")));
		loadButton.setActionCommand("load_vistory");
		loadButton.addActionListener(this);
		add(loadButton);

		JButton saveButton = new JButton("Save",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/save_project.png")));
		saveButton.setActionCommand("save_vistory");
		saveButton.addActionListener(this);
		add(saveButton);

		JButton saveAsButton = new JButton("Save As",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/save_project.png")));
		saveAsButton.setActionCommand("saveas_vistory");
		saveAsButton.addActionListener(this);
		add(saveAsButton);

		JButton addTitleButton = new JButton("Add Title",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/add_title.png")));
		addTitleButton.setActionCommand("add_title");
		addTitleButton.addActionListener(this);
		add(addTitleButton);

		JButton addTextButton = new JButton("Add text",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/add_text.png")));
		addTextButton.setActionCommand("add_text");
		addTextButton.addActionListener(this);
		add(addTextButton);

		JButton clearVistoryButton = new JButton("Clear Vistory",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/clear_vistory.png")));
		clearVistoryButton.setActionCommand("clear");
		clearVistoryButton.addActionListener(this);
		add(clearVistoryButton);

		JButton projectSummaryButton = new JButton("Project Summary",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/project_status.png")));
		projectSummaryButton.setActionCommand("summary");
		projectSummaryButton.addActionListener(this);
		add(projectSummaryButton);

		
		JButton exportVistoryButton = new JButton("Export to HTML",new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/export_to_web.png")));
		exportVistoryButton.setActionCommand("export");
		exportVistoryButton.addActionListener(this);
		add(exportVistoryButton);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		
		if (command.equals("add_text")) {
			Vistory.getInstance().addBlock(new VistoryText());
		}
		else if (command.equals("add_title")) {
			Vistory.getInstance().addBlock(new VistoryTitle());
		}
		else if (command.equals("clear")) {
			
			if (JOptionPane.showConfirmDialog(dialog, "Are you sure you want to wipe your vistory?","Clear vistory",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
			
			Vistory.getInstance().clear();
		}
		else if (command.equals("summary")) {
			VistoryProjectSummary.addProjectSummary();
		}
		
		else if (command.equals("export")) {
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new HTMLFileFilter());

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
				Vistory.getInstance().writeReport(file);
				
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
		else if (command.equals("save_vistory") || command.equals("saveas_vistory")) {
			
			File file = null;
			
			if (command.equals("saveas_vistory") || Vistory.getInstance().saveFile() == null) {
				JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileFilter(new VistoryFileFilter());
				
				if (SeqMonkApplication.getInstance().currentFile() != null) {
					chooser.setSelectedFile(new File(SeqMonkApplication.getInstance().currentFile().getName().replaceAll("\\.smk$", "")+".smv"));
				}

				int result = chooser.showSaveDialog(SeqMonkApplication.getInstance());
				if (result == JFileChooser.CANCEL_OPTION) return;

				file = chooser.getSelectedFile();
				SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);

				if (file.isDirectory()) return;

				if (! (file.getPath().toLowerCase().endsWith(".smv") )) {
					file = new File(file.getPath()+".smv");
				}

				// Check if we're stepping on anyone's toes...
				if (file.exists()) {
					int answer = JOptionPane.showOptionDialog(SeqMonkApplication.getInstance(),file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

					if (answer > 0) {
						return;
					}
				}
			}
			else {
				file = Vistory.getInstance().saveFile();
			}
			
			try {					
				Vistory.getInstance().saveToFile(file);	
				dialog.setTitle("Vistory ["+file.getName()+"]");
			}
			
			catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}
		else if (command.equals("load_vistory")) {
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new VistoryFileFilter());

			int result = chooser.showOpenDialog(SeqMonkApplication.getInstance());
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);

			if (file.isDirectory()) return;
			
			if (JOptionPane.showConfirmDialog(dialog, "Do you want to wipe your existing vistory before loading?","Clear before loading",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {			
				Vistory.getInstance().clear();
			}

			try {					
				Vistory.getInstance().loadFile(file);
				dialog.setTitle("Vistory ["+file.getName()+"]");
			}
			
			catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}

		else {
			throw new IllegalArgumentException("Unknown command "+command);
		}
	}
	
		
	
}
