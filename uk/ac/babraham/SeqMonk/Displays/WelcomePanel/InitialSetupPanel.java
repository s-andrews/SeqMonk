/**
 * Copyright 2016-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.WelcomePanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

public class InitialSetupPanel extends JDialog implements ActionListener {

	/**
	 * This dialog is shown the first time seqmonk starts and it aims to 
	 * get people set up quickly by setting a suitable cache and genomes
	 * folder for them.
	 */
	
	private JTextField cacheFolderField;
	private JTextField genomeFolderField;
	
	
	public InitialSetupPanel () {
		
		super(SeqMonkApplication.getInstance(),"Initial SeqMonk Setup");
		setModal(true);
		
		getContentPane().setBackground(Color.WHITE);
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.01;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5); 
		
		JPanel topPanel = new JPanel();
		topPanel.setOpaque(false);
		topPanel.setLayout(new BorderLayout());
		ImageIcon logo = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/bi_logo.png"));
		ImageIcon monk = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/monk100.png"));

		topPanel.add(new JLabel("",logo,JLabel.CENTER),BorderLayout.WEST);
		topPanel.add(new JLabel("",monk,JLabel.CENTER),BorderLayout.EAST);
		
		JLabel program = new SmoothJLabel("Welcome to SeqMonk",JLabel.CENTER);
		program.setFont(new Font("Dialog",Font.BOLD,26));
		program.setForeground(ColourScheme.FORWARD_FEATURE);
		topPanel.add(program,BorderLayout.CENTER);

		add(topPanel,gbc);
		
		gbc.gridy++;
		
		JPanel explainPanel = new JPanel();
		explainPanel.setOpaque(false);
		
		JLabel explainLabel = new JLabel(
				"<html>To get started with SeqMonk we need to set up a couple of things<br><br>"
				+ " 1) A 'cache' folder where SeqMonk can temporarily write data whilst it's running<br>"
				+ " 2) A folder to store downloaded genome annotation information<br><br>"
				+ "We've assigned what we think are sensible values for these folders below but<br>"
				+ "feel free to change these if you want this data stored somewhere else.<br><br>"
				+ "Once this is done you should be on your way");
		
		explainPanel.add(explainLabel);
		
		add(explainPanel,gbc);
		
		gbc.gridy++;
		gbc.weighty = 0.999;
		
		JPanel locationPanel = new JPanel();
		locationPanel.setOpaque(false);
		
		locationPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		

		c.insets=new Insets(5, 5, 5, 5);
		c.gridx=0;
		c.gridy=0;
		c.weightx=0.1;
		c.weighty=0.5;
		c.fill = GridBagConstraints.HORIZONTAL;
		locationPanel.add(new JLabel("Cache Folder Location"),c);
		c.gridx=1;
		c.weightx=0.5;
		cacheFolderField = new JTextField();
		cacheFolderField.setText(System.getProperty("user.home")+"/seqmonk_cache");
		cacheFolderField.setEditable(true);
		locationPanel.add(cacheFolderField,c);
		c.gridx=2;
		c.weightx=0.1;
		JButton cacheButton = new JButton("Browse");
		cacheButton.setActionCommand("cache");
		cacheButton.addActionListener(this);
		locationPanel.add(cacheButton,c);
		
		c.gridx=0;
		c.gridy++;
		c.weightx=0.1;
		locationPanel.add(new JLabel("Genomes Folder Location"),c);
		c.gridx=1;
		c.weightx=0.5;
		genomeFolderField = new JTextField(System.getProperty("user.home")+"/seqmonk_genomes");
		genomeFolderField.setEditable(true);
		locationPanel.add(genomeFolderField,c);
		c.gridx=2;
		c.weightx=0.1;
		JButton dataButton = new JButton("Browse");
		dataButton.setActionCommand("genomes");
		dataButton.addActionListener(this);
		locationPanel.add(dataButton,c);
		
		add(locationPanel,gbc);
		
		gbc.gridy++;
		gbc.weighty = 0.001;
		
		JPanel buttonPanel = new JPanel();
		
		JButton goButton = new JButton("Start Using SeqMonk");
		goButton.setActionCommand("go");
		goButton.addActionListener(this);
		
		buttonPanel.add(goButton);
		
		add(buttonPanel,gbc);
		
		
		setSize(650,500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}
	
	/**
	 * A JLabel with anti-aliasing enabled.  Takes the same constructor
	 * arguments as JLabel
	 */
	private class SmoothJLabel extends JLabel {
		
		/**
		 * Creates a new label
		 * 
		 * @param text The text
		 * @param position The JLabel constant position for alignment
		 */
		public SmoothJLabel (String text, int position) {
			super(text,position);
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		public void paintComponent (Graphics g) {
			if (g instanceof Graphics2D) {
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
			super.paintComponent(g);
		}

	}

	private void getDir (JTextField f) {
		JFileChooser chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(new File(f.getText()));
	    chooser.setDialogTitle("Select Directory");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
	    	f.setText(chooser.getSelectedFile().getAbsolutePath());
	    }
	}
	
	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("cache")) {
			getDir(cacheFolderField);
		}
		else if (ae.getActionCommand().equals("genomes")) {
			getDir(genomeFolderField);
		}
		else if (ae.getActionCommand().equals("go")) {
			
			// Let's see if this is going to work.
			
			File cacheFile = new File(cacheFolderField.getText());
			if (!cacheFile.exists()) {
				if (!cacheFile.mkdirs()) {
					showError("Cache directory "+cacheFile.getPath()+" didn't exist, and we couldn't create it.  Please choose another.");
					return;
				}
			}
			if (!cacheFile.isDirectory()) {
				showError("Cache location "+cacheFile.getPath()+" appears to be a file rather than a directory.  Please choose a directory");
				return;
			}
			
			File genomesFile = new File(genomeFolderField.getText());
			if (!genomesFile.exists()) {
				if (!genomesFile.mkdirs()) {
					showError("Genomes directory "+genomesFile.getPath()+" didn't exist, and we couldn't create it.  Please choose another.");
					return;
				}
			}
			if (!genomesFile.isDirectory()) {
				showError("Genomes location "+cacheFile.getPath()+" appears to be a file rather than a directory.  Please choose a directory");
				return;
			}
			
			// If we get here then those locations are OK and must exist.  Set them and move on.
			SeqMonkPreferences p = SeqMonkPreferences.getInstance();
			
			p.setGenomeBase(genomesFile);
			p.setTempDirectory(cacheFile);

			try {
				p.savePreferences();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

			
			setVisible(false);
			dispose();
			
		}
		
	}
	
	public void showError (String message) {
		
	}
	 
	
}
