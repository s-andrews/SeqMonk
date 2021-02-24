/**
 * Copyright Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.DataSetEditor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;


/**
 * The Class FindFeatureDialog shows a dialog which the user can use to
 * search for any kind of annotation.
 */
public class CollectDataSetNamesDialog extends JDialog implements ActionListener {

	/** The search. */
	private JTextArea search;
		
	/** The search button. */
	private JButton searchButton;

	private String [] names = new String [0];
	
	
	/**
	 * Instantiates a new find feature dialog.
	 * 
	 * @param application the application
	 */
	public CollectDataSetNamesDialog () {
		super(SeqMonkApplication.getInstance(),"Paste DataSet Names...");
		setSize(350,400);
		setModal(true);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
				
		getContentPane().setLayout(new BorderLayout());
		
		getContentPane().add(new JLabel("Paste DataSet Names",JLabel.CENTER), BorderLayout.NORTH);
		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Close");
		cancelButton.setActionCommand("close");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
				
		searchButton = new JButton("Search");
		searchButton.setActionCommand("search");
		searchButton.addActionListener(this);
		getRootPane().setDefaultButton(searchButton);
		buttonPanel.add(searchButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		search = new JTextArea();
		search.setWrapStyleWord(true);
		getContentPane().add(new JScrollPane(search), BorderLayout.CENTER);
		
	}

	public String [] getNames () {
		
		setVisible(true);
		return names;
	}
	
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("search")) {

			String [] inputTerms = search.getText().split("\n");

			for (int i=0;i<inputTerms.length;i++) {
				inputTerms[i] = inputTerms[i].trim();
//				System.err.println("Name is '"+inputTerms[i]+"'");
			}
			
			names = inputTerms;
		
			setVisible(false);
			dispose();
		}
		else {
			throw new IllegalStateException("No action called "+ae.getActionCommand());
		}

		
	}
	
		
}
