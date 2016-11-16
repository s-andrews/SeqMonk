/**
 * Copyright 2014-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.SmallRNAQCPlot;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Dialogs.CrashReporter;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class SmallRNAResultsDialog extends JDialog {
	
	private JPanel resultPanel;
	private SmallRNAQCResult [] results;

	public SmallRNAResultsDialog (SmallRNAQCResult [] results) {
		
		super(SeqMonkApplication.getInstance(),"Small RNA QC Report");
		this.results = results;
		
		resultPanel = new JPanel();
		
		resultPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.fill=GridBagConstraints.BOTH;
		
		for (int i=0;i<results.length;i++) {
			resultPanel.add(new SmallRNAResultPanel(results[i]),gbc);
			gbc.gridy++;
		}

		gbc.gridx++;
		gbc.gridy=1;
		gbc.weightx=0.01;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = results.length;
		
		resultPanel.add(new FeatureNamePanel(results[0].features()),gbc);
		
		getContentPane().setLayout(new BorderLayout());
		
		getContentPane().add(resultPanel,BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {
				setVisible(false);
				dispose();
			}
		});
		
		buttonPanel.add(closeButton);
		
		JButton saveButton = new JButton("Save Image");
		saveButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {
				ImageSaver.saveImage(resultPanel);
			}
		});
		
		
		buttonPanel.add(saveButton);
		
		JButton saveDataButton = new JButton("Save Data");
		saveDataButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {

				JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileFilter(new FileFilter() {
					
					public String getDescription() {
						return "Text Files";
					}
					
					public boolean accept(File f) {
						if (f.isDirectory() || f.getName().toLowerCase().endsWith(".txt")) {
							return true;
						}
						return false;
					}
				});
				
				int result = chooser.showSaveDialog(SmallRNAResultsDialog.this);
				if (result == JFileChooser.CANCEL_OPTION) return;

				File file = chooser.getSelectedFile();
				SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
				
				if (file.isDirectory()) return;

				if (! file.getPath().toLowerCase().endsWith(".txt")) {
					file = new File(file.getPath()+".txt");
				}
				
				// Check if we're stepping on anyone's toes...
				if (file.exists()) {
					int answer = JOptionPane.showOptionDialog(SmallRNAResultsDialog.this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

					if (answer > 0) {
						return;
					}
				}

				// Now write out the results
				
				try {
					PrintWriter pr = new PrintWriter(file);
					
					// Write the header
					StringBuffer br = new StringBuffer();
					
					String [] features = SmallRNAResultsDialog.this.results[0].features();
					
					br.append("DataStore\tLength");
					for (int i=0;i<features.length;i++) {
						br.append("\t");
						br.append(features[i]);
					}
					
					pr.println(br.toString());
					
					// Now do all of the data stores
					for (int s=0;s<SmallRNAResultsDialog.this.results.length;s++) {
						
						for (int length=SmallRNAResultsDialog.this.results[s].minLength();length<=SmallRNAResultsDialog.this.results[s].maxLength();length++) {
						
							br = new StringBuffer();
							br.append(SmallRNAResultsDialog.this.results[s].store().name());
							br.append("\t");
							br.append(length);
							
							for (int i=0;i<features.length;i++) {
								br.append("\t");
								br.append(SmallRNAResultsDialog.this.results[s].getCountsForLength(length)[i]);
							}
						
							pr.println(br.toString());
						
						}
					}
					
					pr.close();
					
				}
				catch (IOException ioe) {
					new CrashReporter(ioe);
				}
				
			
			}
		});
		
		
		buttonPanel.add(saveDataButton);
		
	
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		
		setSize(800,400);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}
	
}
