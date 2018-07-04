/**
 * Copyright 2009-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.DataParser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataParsers.DataParser;

public class DataParserOptionsDialog extends JDialog implements ActionListener {

	private DataParser parser;
	
	// We need to set the default not to parse in case they use the X to close the window.
	private boolean goAheadAndParse = false;
	
	public DataParserOptionsDialog (DataParser parser) {
		super(SeqMonkApplication.getInstance());
		setModal(true);
		setTitle("Import Options");
		
		this.parser = parser;
		
		getContentPane().setLayout(new BorderLayout());
		
		getContentPane().add(new JLabel("Options for "+parser.name(),JLabel.CENTER),BorderLayout.NORTH);
		
		JPanel optionsPanel = parser.getOptionsPanel();
		getContentPane().add(optionsPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton importButton = new JButton("Import");
		importButton.setActionCommand("import");
		importButton.addActionListener(this);
		buttonPanel.add(importButton);
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		Dimension d = getPreferredSize();
		
		setSize(Math.max(d.width, 400),Math.max(d.height, 400));
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
	}
	
	public boolean view() {
		setVisible(true);
		// This will block here since the dialog is modal
		return goAheadAndParse;
	}

	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("close")) {
			goAheadAndParse = false;
			setVisible(false);
			dispose();
		}
		
		else if (ae.getActionCommand().equals("import")) {
			if (parser.readyToParse()) {
				goAheadAndParse = true;
				setVisible(false);
				dispose();
			}
			else {
				JOptionPane.showMessageDialog(this, "Some options have not been set", "Can't Import yet..", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		else {
			throw new IllegalStateException("Don't know how to handle action '"+ae.getActionCommand()+"'");
		}
	}
}
