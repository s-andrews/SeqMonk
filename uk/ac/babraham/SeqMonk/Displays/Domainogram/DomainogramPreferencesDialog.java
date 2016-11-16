/**
 * Copyright 2013-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Domainogram;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class DomainogramPreferencesDialog extends JDialog implements ActionListener {

	private JTextField numberOfClassesField;
	private JCheckBox logScaleBox;
	private JCheckBox ignoreBlankBox;
	private JCheckBox trimEndsBox;
	private JTextField minCountField;
	private JTextField maxCountField;
	private DataStore [] data;
	private ProbeList list;
	
	private static int numberOfClasses = 10;
	private static boolean logScale = false;
	private static boolean ignoreBlank = false;
	private static boolean trimEnds = false;
	private static int minCount = 1;
	private static int maxCount = 100;
	
	public DomainogramPreferencesDialog (DataStore [] data, ProbeList list) {
		
		super(SeqMonkApplication.getInstance(),"Domainogram Prefs");
		
		// We only want data stores which have been quantitated
		Vector<DataStore> keepers = new Vector<DataStore>();
		for (int d=0;d<data.length;d++) {
			if (data[d].isQuantitated()) {
				keepers.add(data[d]);
			}
		}
		
		if (keepers.size() == 0) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "No quatitated visible data stores to plot", "Can't plot domainogram", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		
		this.data = keepers.toArray(new DataStore[0]);
		this.list = list;
		
		JPanel prefsPanel = new JPanel();
		
		prefsPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.gridx=1;
		gbc.gridy=1;
		
		prefsPanel.add(new JLabel("Number of classes"),gbc);
		
		gbc.gridx=2;
		
		numberOfClassesField = new JTextField(""+numberOfClasses,7);
		numberOfClassesField.addKeyListener(new NumberKeyListener(false, false, 1000));
		
		prefsPanel.add(numberOfClassesField,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;

		prefsPanel.add(new JLabel("Min Probe Count"),gbc);
		
		gbc.gridx=2;
		
		minCountField = new JTextField(""+minCount,7);
		minCountField.addKeyListener(new NumberKeyListener(false, false, 10000));

		prefsPanel.add(minCountField,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		prefsPanel.add(new JLabel("Max Probe Count"),gbc);
		
		gbc.gridx=2;
		
		maxCountField = new JTextField(""+maxCount,7);
		maxCountField.addKeyListener(new NumberKeyListener(false, false, 10000));
				
		prefsPanel.add(maxCountField,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		
		prefsPanel.add(new JLabel("Log Scale"),gbc);
		
		gbc.gridx=2;
		
		logScaleBox = new JCheckBox();
		logScaleBox.setSelected(logScale);
		
		prefsPanel.add(logScaleBox,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		
		prefsPanel.add(new JLabel("Ignore Blank Chromosomes"),gbc);
		
		gbc.gridx=2;
		
		ignoreBlankBox = new JCheckBox();
		ignoreBlankBox.setSelected(ignoreBlank);
		
		prefsPanel.add(ignoreBlankBox,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		
		prefsPanel.add(new JLabel("Trim Chromosome Ends"),gbc);
		
		gbc.gridx=2;
		
		trimEndsBox = new JCheckBox();
		trimEndsBox.setSelected(trimEnds);
		
		prefsPanel.add(trimEndsBox,gbc);

		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(prefsPanel,BorderLayout.CENTER);
		
		
		JPanel buttonPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		closeButton.setActionCommand("close");
		buttonPanel.add(closeButton);
		
		JButton plotButton = new JButton("Plot");
		plotButton.setActionCommand("plot");
		plotButton.addActionListener(this);
		buttonPanel.add(plotButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(300,250);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (e.getActionCommand().equals("plot")) {
			
			if (numberOfClassesField.getText().length()>0) {
				numberOfClasses = Integer.parseInt(numberOfClassesField.getText());
			}
			else {
				numberOfClasses = 10;
			}
			
			if (numberOfClasses < 5) numberOfClasses = 5;
			
			int [] levels = new int[numberOfClasses];
			
			if (minCountField.getText().length()>0) {
				minCount = Integer.parseInt(minCountField.getText());
			}
			else {
				minCount = 1;
			}
			
			if (minCount < 1) minCount=1;

			if (maxCountField.getText().length()>0) {
				maxCount = Integer.parseInt(maxCountField.getText());
			}
			
			if (maxCount<minCount+levels.length) maxCount=minCount+levels.length;
			
			logScale = logScaleBox.isSelected();
			
			for (int i=0;i<levels.length;i++) {
				if (logScale) {
					levels[i] = (int)(minCount+Math.pow(10,((Math.log10(maxCount-minCount)/(numberOfClasses-1))*i)));
				}
				else {
					levels[i] = (int)(minCount+(((maxCount-minCount)/(numberOfClasses-1))*i));
				}
								
			}
			
			trimEnds = trimEndsBox.isSelected();
			ignoreBlank = ignoreBlankBox.isSelected();
						
			new DomainogramDialog(list, data, levels, ignoreBlank,trimEnds);
			setVisible(false);
			dispose();
		}
	}
	
	
	
	
}
