/**
 * Copyright 2010-18 Simon Andrews
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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;

/**
 * The Class SeqMonkTitlePanel.
 */
public class SeqMonkTitlePanel extends JPanel {

	/**
	 * Provides a small panel which gives details of the SeqMonk version
	 * and copyright.  Used in both the welcome panel and the about dialog.
	 */
	public SeqMonkTitlePanel () {
		setLayout(new BorderLayout(5,1));

		ImageIcon logo = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/bi_logo.png"));
		ImageIcon monk = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/monk100.png"));

		add(new JLabel("",logo,JLabel.CENTER),BorderLayout.WEST);
		add(new JLabel("",monk,JLabel.CENTER),BorderLayout.EAST);
		JPanel c = new JPanel();
		c.setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx=1;
		constraints.gridy=1;
		constraints.weightx = 1;
		constraints.weighty=1;
		constraints.insets = new Insets(6, 6, 0, 0);
		constraints.fill = GridBagConstraints.NONE;

		JLabel program = new SmoothJLabel("SeqMonk Mapped Sequence Data Analyser",JLabel.CENTER);
		program.setFont(new Font("Dialog",Font.BOLD,18));
		program.setForeground(ColourScheme.FORWARD_FEATURE);
		c.add(program,constraints);

		constraints.gridy++;
		JLabel version = new SmoothJLabel("Version: "+SeqMonkApplication.VERSION, JLabel.CENTER);
		version.setFont(new Font("Dialog",Font.BOLD,15));
		version.setForeground(ColourScheme.REVERSE_FEATURE);
		c.add(version,constraints);

		constraints.gridy++;
		// Use a text field so they can copy this
		JTextField website = new JTextField(" www.bioinformatics.babraham.ac.uk/projects/ ");
		website.setFont(new Font("Dialog",Font.PLAIN,14));
		website.setEditable(false);
		website.setBorder(null);
		website.setOpaque(false);
		website.setHorizontalAlignment(JTextField.CENTER);
		c.add(website,constraints);
		constraints.gridy++;

		JLabel copyright = new JLabel("\u00a9 Simon Andrews,Laura Biggins Babraham Bioinformatics, 2006-18", JLabel.CENTER);
		copyright.setFont(new Font("Dialog",Font.PLAIN,12));
		c.add(copyright,constraints);
		constraints.gridy++;

		JLabel copyright2 = new JLabel("Picard BAM/SAM reader \u00a9The Broad Institute, 2009", JLabel.CENTER);
		copyright2.setFont(new Font("Dialog",Font.PLAIN,10));
		c.add(copyright2,constraints);

		add(c,BorderLayout.CENTER);
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
	
}
