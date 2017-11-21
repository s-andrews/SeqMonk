/**
 * Copyright 2009-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.AlignedProbePlot;


import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;

/**
 * The Class AlignedSummaryPreferencesDialog sets the preferences from which a
 * trend plot can be drawn.
 */
public class AlignedSummaryPreferencesDialog extends JDialog implements ActionListener {
	
	/** The prefs. */
	private AlignedSummaryPreferences prefs = new AlignedSummaryPreferences();
	
	/** The strand box. */
	private JComboBox strandBox;
	
	private JComboBox orderBox;
		
	/** The remove duplicates box. */
	private JCheckBox removeDuplicatesBox;
	
	/** The force relative box. */
	private JCheckBox forceRelativeBox;

	/** The use log scale box. */
	private JCheckBox logTransformBox;

	
	/** The probes. */
	private ProbeList [] lists;
	
	/** The store. */
	private DataStore [] stores;
	
	/**
	 * Instantiates a new trend over probe preferences dialog.
	 * 
	 * @param probes the probes
	 * @param stores the stores
	 */
	public AlignedSummaryPreferencesDialog (ProbeList [] lists, DataStore [] stores) {
		super(SeqMonkApplication.getInstance(),"Aligned Probe Preferences");
		this.lists = lists;
		this.stores = stores;
		
		getContentPane().setLayout(new BorderLayout());
		
		JPanel prefPanel = new JPanel();
	
		prefPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.NONE;
		
		prefPanel.add(new JLabel("Use reads on strand"),gbc);
	
		gbc.gridx = 2;
		strandBox = new JComboBox(QuantitationStrandType.getTypeOptions());
		strandBox.addActionListener(this);
		strandBox.setActionCommand("change_strand");
		prefPanel.add(strandBox,gbc);
				
		gbc.gridy++;
		gbc.gridx=1;
		
		
		prefPanel.add(new JLabel("Order probes"),gbc);
		
		gbc.gridx = 2;
		String [] orderOptions = new String [stores.length+1];
		orderOptions[0] = "Independently";
		for (int i=0;i<stores.length;i++) {
			orderOptions[i+1] = stores[i].name();
		}
		orderBox = new JComboBox(orderOptions);
		orderBox.setPrototypeDisplayValue("No longer than this please");
		orderBox.addActionListener(this);
		orderBox.setActionCommand("change_order");
		prefPanel.add(orderBox,gbc);
				
		gbc.gridy++;
		gbc.gridx=1;
		
		
		prefPanel.add(new JLabel("Remove duplicate reads"),gbc);
		gbc.gridx = 2;
		removeDuplicatesBox = new JCheckBox();
		removeDuplicatesBox.setSelected(prefs.removeDuplicates);
		prefPanel.add(removeDuplicatesBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		
		prefPanel.add(new JLabel("Log transform counts"),gbc);
		gbc.gridx = 2;
		logTransformBox = new JCheckBox();
		logTransformBox.setSelected(prefs.useLogScale);
		prefPanel.add(logTransformBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		
		prefPanel.add(new JLabel("Force plot to be relative"),gbc);
		gbc.gridx = 2;
		forceRelativeBox = new JCheckBox();
		forceRelativeBox.setSelected(false);
		prefPanel.add(forceRelativeBox,gbc);
		
		getContentPane().add(prefPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel");
		buttonPanel.add(cancelButton);
		
		JButton plotButton = new JButton("Create Plot");
		plotButton.addActionListener(this);
		plotButton.setActionCommand("plot");
		buttonPanel.add(plotButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(400,300);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
			
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("change_strand")) {			
			prefs.quantitationType = (QuantitationStrandType)strandBox.getSelectedItem();
		}
		
		if (e.getActionCommand().equals("change_order")) {
			if (orderBox.getSelectedIndex() == 0) {
				prefs.orderBy = null;
			}
			else {
				prefs.orderBy = stores[orderBox.getSelectedIndex()-1];
			}
		}
		
		else if (e.getActionCommand().equals("cancel")) {
			setVisible(false);
			dispose();
		}
		else if (e.getActionCommand().equals("plot")) {
			prefs.forceRelative = forceRelativeBox.isSelected();
			prefs.removeDuplicates = removeDuplicatesBox.isSelected();
			prefs.useLogScale = logTransformBox.isSelected();
			setVisible(false);
			new AlignedSummaryDialog(lists,stores,prefs);
		}
	}

}
