/**
 * Copyright 2006-2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;

/**
 * The Class WarningDisplayDialog.
 */
public class WarningDisplayDialog extends JDialog implements ActionListener{

	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (Exception [] exceptions) {
		super(SeqMonkApplication.getInstance(),"Request Generated Warnings...");
		constructDialog(exceptions.length,exceptions);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}
	
	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param parent the parent
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (int exceptionCount, Exception [] exceptions) {
		super(SeqMonkApplication.getInstance(),"Request Generated Warnings...");
		constructDialog(exceptionCount, exceptions);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}

	
	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param parent the parent
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (int exceptionCount, CountedException [] exceptions) {
		super(SeqMonkApplication.getInstance(),"Request Generated Warnings...");
		Arrays.sort(exceptions);
		constructDialog(exceptionCount, exceptions);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}

	
	
	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param parent the parent
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (JFrame parent, Exception [] exceptions) {
		super(parent,"Request Generated Warnings...");
		constructDialog(exceptions.length, exceptions);
		setLocationRelativeTo(parent);
		setVisible(true);
	}
	
	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param parent the parent
	 * @param exceptionCount the exception count
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (JDialog parent, int exceptionCount, Exception [] exceptions) {
		super(parent,"Request Generated Warnings...");
		constructDialog(exceptionCount, exceptions);
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	
	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param parent the parent
	 * @param exceptionCount the exception count
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (JDialog parent, int exceptionCount, CountedException [] exceptions) {
		super(parent,"Request Generated Warnings...");
		Arrays.sort(exceptions);
		constructDialog(exceptionCount, exceptions);
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	
	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param parent the parent
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (JDialog parent, Exception [] exceptions) {
		super(parent,"Request Generated Warnings...");
		constructDialog(exceptions.length, exceptions);
		setLocationRelativeTo(parent);
		setVisible(true);
	}
	
	/**
	 * Instantiates a new warning display dialog.
	 * 
	 * @param parent the parent
	 * @param exceptionCount the exception count
	 * @param exceptions the exceptions
	 */
	public WarningDisplayDialog (JFrame parent, int exceptionCount, Exception [] exceptions) {
		super(parent,"Request Generated Warnings...");
		constructDialog(exceptionCount, exceptions);
		setLocationRelativeTo(parent);
		setVisible(true);
	}
	
	private void constructDialog (int exceptionCount, Exception [] exceptions) {

		getContentPane().setLayout(new BorderLayout());
		if (exceptionCount == exceptions.length) {
			getContentPane().add(new JLabel("There were "+exceptionCount+" warnings when processing your request",UIManager.getIcon("OptionPane.warningIcon"),JLabel.LEFT),BorderLayout.NORTH);
		}
		else {
			getContentPane().add(new JLabel("There were "+exceptionCount+" warnings when processing your request - showing the first "+exceptions.length,UIManager.getIcon("OptionPane.warningIcon"),JLabel.LEFT),BorderLayout.NORTH);			
		}
		StringBuffer b = new StringBuffer();
		for (int i=0;i<exceptions.length;i++) {
			b.append(exceptions[i].getMessage());
			b.append("\n");
		}
		JTextArea text = new JTextArea(b.toString());
		text.setEditable(false);
		getContentPane().add(new JScrollPane(text),BorderLayout.CENTER);
		
		JPanel closePanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		closePanel.add(closeButton);
		getContentPane().add(closePanel,BorderLayout.SOUTH);
		
		setSize(600,300);
		setModal(true);
	}

	
	private void constructDialog (int exceptionCount,  CountedException [] exceptions) {

		getContentPane().setLayout(new BorderLayout());
		if (exceptionCount == exceptions.length) {
			getContentPane().add(new JLabel("There were "+exceptionCount+" warnings when processing your request",UIManager.getIcon("OptionPane.warningIcon"),JLabel.LEFT),BorderLayout.NORTH);
		}
		else {
			getContentPane().add(new JLabel("There were "+exceptionCount+" warnings when processing your request - showing the first "+exceptions.length,UIManager.getIcon("OptionPane.warningIcon"),JLabel.LEFT),BorderLayout.NORTH);			
		}
		StringBuffer b = new StringBuffer();
		for (int i=0;i<exceptions.length;i++) {
			b.append(exceptions[i].toString());
			b.append("\n");
		}
		JTextArea text = new JTextArea(b.toString());
		text.setEditable(false);
		getContentPane().add(new JScrollPane(text),BorderLayout.CENTER);
		
		JPanel closePanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		closePanel.add(closeButton);
		getContentPane().add(closePanel,BorderLayout.SOUTH);
		
		setSize(600,300);
		setModal(true);
	}


	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		setVisible(false);
		dispose();
	}
	
}
