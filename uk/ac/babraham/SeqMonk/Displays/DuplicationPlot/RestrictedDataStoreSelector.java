/**
 * Copyright 2016- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.DuplicationPlot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class RestrictedDataStoreSelector extends JDialog implements ListSelectionListener {

	private int limit;
	private DataStore [] selectedStores;
	
	private JList storesList;
	private JButton selectButton;
	
	
	
	public RestrictedDataStoreSelector (DataStore [] stores, int limit) {
		super(SeqMonkApplication.getInstance());
		this.limit = limit;
		setTitle("Please select stores");
		setModal(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		
		JLabel topLabel = new JLabel("Please select up to "+limit+" stores",JLabel.CENTER);
		getContentPane().add(topLabel, BorderLayout.NORTH);
		
		storesList = new JList(stores);
		storesList.addListSelectionListener(this);
		
		getContentPane().add(new JScrollPane(storesList), BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		selectButton = new JButton("Select");
		selectButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		
		buttonPanel.add(selectButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
	}
	
	
	public DataStore [] selectStores () {

		setSize(400,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		return(selectedStores);
	}


	public void valueChanged(ListSelectionEvent e) {

		Object [] sel = storesList.getSelectedValues();
		
		if (sel.length > limit) {
			selectedStores = new DataStore[0];
			selectButton.setEnabled(false);
			return;
		}
		else {
			selectedStores = new DataStore[sel.length];
			for (int i=0;i<sel.length;i++) {
				selectedStores[i] = (DataStore)sel[i];
			}
			selectButton.setEnabled(true);
		}
	}
	
}
