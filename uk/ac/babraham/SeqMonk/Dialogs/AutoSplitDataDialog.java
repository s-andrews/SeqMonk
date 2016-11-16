/**
 * Copyright 2014-15-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;

public class AutoSplitDataDialog extends JDialog implements ActionListener {

	private SeqMonkApplication application;
	private JTextArea groupNamesArea;
	private JComboBox groupsOrSetsBox;
	private JCheckBox includeGroupsBox;
	private JCheckBox caseInsensitiveBox;
	private JButton createButton;
	
	public AutoSplitDataDialog (SeqMonkApplication application)  {
		super(application,"Auto Split Data");
		this.application = application;
		
		getContentPane().setLayout(new BorderLayout());
		
		JPanel groupNamePanel = new JPanel();
		
		groupNamePanel.setLayout(new BorderLayout());
		groupNamePanel.add(new JLabel(" Group identifiers to split data ",JLabel.CENTER),BorderLayout.NORTH);
		
		groupNamesArea = new JTextArea();
		groupNamesArea.addKeyListener(new KeyListener() {
			
			public void keyTyped(KeyEvent arg0) {
				createButton.setEnabled(groupNamesArea.getText().length() > 0);
			}
			
			public void keyReleased(KeyEvent arg0) {}
			
			public void keyPressed(KeyEvent arg0) {}
		});
		
		groupNamePanel.add(new JScrollPane(groupNamesArea),BorderLayout.CENTER);
		
		
		JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		optionsPanel.add(new JLabel("Create"),gbc);
		
		gbc.gridx=2;
		
		groupsOrSetsBox = new JComboBox(new String [] {"Replicate Sets","Data Groups"});
		optionsPanel.add(groupsOrSetsBox,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		
		optionsPanel.add(new JLabel("Include Data Groups"),gbc);
		
		gbc.gridx=2;
		
		includeGroupsBox = new JCheckBox();
		includeGroupsBox.setSelected(true);
		
		optionsPanel.add(includeGroupsBox,gbc);
		
		groupsOrSetsBox.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				includeGroupsBox.setEnabled(groupsOrSetsBox.getSelectedItem().equals("Replicate Sets"));
			}
		});
		
		gbc.gridx=1;
		gbc.gridy++;
		
		optionsPanel.add(new JLabel("Case Insensitive"),gbc);
		
		gbc.gridx=2;
		
		caseInsensitiveBox = new JCheckBox();

		optionsPanel.add(caseInsensitiveBox, gbc);
		
		JPanel buttonPanel = new JPanel();
		createButton = new JButton("Create Groups");
		createButton.setEnabled(false);
		createButton.setActionCommand("create");
		createButton.addActionListener(this);
		buttonPanel.add(createButton);
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		
		buttonPanel.add(closeButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		getContentPane().add(optionsPanel,BorderLayout.CENTER);
		getContentPane().add(groupNamePanel,BorderLayout.WEST);
		
		setSize(600,300);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}
	
	
	private void createGroups() {
		
		String [] names = groupNamesArea.getText().split("\\n");
		int [] counts = new int[names.length];
		
		DataSet [] dataSets = application.dataCollection().getAllDataSets();
		DataGroup [] dataGroups = application.dataCollection().getAllDataGroups();
		
		boolean makeReplicateSets = groupsOrSetsBox.getSelectedItem().equals("Replicate Sets");
		
		boolean caseInsensitive = caseInsensitiveBox.isSelected();
		
		if (! includeGroupsBox.isSelected()) {
			dataGroups = new DataGroup[0];
		}
				
		boolean [] setsFound = new boolean[dataSets.length];
		boolean [] groupsFound = new boolean[dataGroups.length];
		
		for (int n=0;n<names.length;n++) {
			if (names[n].trim().length()==0) continue;
			
			names[n] = names[n].trim();
			
			String [] patterns = names[n].split("\\|");
			
			if (makeReplicateSets) {

				Vector<DataStore>stores = new Vector<DataStore>();
				
				DATASET: for (int i=0;i<dataSets.length;i++) {
					if (setsFound[i]) continue; // Only add to the first match we make
					for (int p=0;p<patterns.length;p++) {
						
						if (caseInsensitive) {
							if (!dataSets[i].name().toLowerCase().contains(patterns[p].toLowerCase())) {
								continue DATASET;
							}							
						}
						else {
							if (!dataSets[i].name().contains(patterns[p])) {
								continue DATASET;
							}
						}
					}	
					stores.add(dataSets[i]);
					setsFound[i] = true;
				}

				DATAGROUP: for (int i=0;i<dataGroups.length;i++) {
					if (groupsFound[i]) continue; // Only add to the first match we make
					for (int p=0;p<patterns.length;p++) {
						if (!dataGroups[i].name().contains(patterns[p])) {
							continue DATAGROUP;
						}
					}	
					stores.add(dataGroups[i]);
					groupsFound[i] = true;
				}
				
				counts[n] = stores.size();
				
				if (stores.size() > 0) {
					application.dataCollection().addReplicateSet(new ReplicateSet(names[n], stores.toArray(new DataStore[0])));
				}
				
			}
			else {
			
				Vector<DataSet> sets = new Vector<DataSet>();
				
				DATASET: for (int i=0;i<dataSets.length;i++) {
					for (int p=0;p<patterns.length;p++) {
						if (!dataSets[i].name().contains(patterns[p])) {
							continue DATASET;
						}
					}	
					sets.add(dataSets[i]);
				}

				counts[n] = sets.size();
				if (sets.size() > 0) {
					application.dataCollection().addDataGroup(new DataGroup(names[n], sets.toArray(new DataSet[0])));
				}
			}
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("<html>");
		for (int i=0;i<names.length;i++) {
			sb.append("For name ");
			sb.append(names[i]);
			sb.append(" found ");
			sb.append(counts[i]);
			sb.append(" hits<br>");
		}
		sb.append("</html>");
		
		JOptionPane.showMessageDialog(this, sb.toString(), "Group creation complete", JOptionPane.INFORMATION_MESSAGE);
		

		// It's actually better not to close after this but let them close it themselves.
//		setVisible(false);
//		dispose();
		
	}

	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("create")) {
			createGroups();
		}
		else {
			new CrashReporter(new SeqMonkException("Unknown action "+ae.getActionCommand()));
		}
	}
	
	
	
}
