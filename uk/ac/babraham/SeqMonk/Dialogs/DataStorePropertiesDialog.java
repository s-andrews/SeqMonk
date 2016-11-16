/**
 * Copyright 2009-15-13 Simon Andrews
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;

/**
 * The Class DataStorePropertiesDialog shows some basic stats about a data store
 */
public class DataStorePropertiesDialog extends JDialog implements ActionListener, Runnable {

	/** The forward count. */
	private JLabel forwardCount;
	
	/** The revese count. */
	private JLabel reveseCount;
	
	/** The total count. */
	private JLabel totalCount;
	
	/** The unknown count. */
	private JLabel unknownCount;
	
	/** The average length. */
	private JLabel averageLength;
	
	/** The data store. */
	private DataStore dataStore;
	
	/**
	 * Instantiates a new data store properties dialog.
	 * 
	 * @param dataStore the data store
	 */
	public DataStorePropertiesDialog(DataStore dataStore) {
	
		super(SeqMonkApplication.getInstance(),"DataStore Properties");
		this.dataStore = dataStore;
		getContentPane().setLayout(new BorderLayout());
		
		JPanel infoPanel = new JPanel();
		
		infoPanel.setLayout(new GridBagLayout());
		infoPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		infoPanel.add(new JLabel("Name"),gbc);
		gbc.gridx=2;
		infoPanel.add(new JLabel(dataStore.name()),gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		
		if (dataStore instanceof DataSet) {
			infoPanel.add(new JLabel("File Name"),gbc);
			gbc.gridx=2;
			infoPanel.add(new JLabel(((DataSet)dataStore).fileName()),gbc);
		}

		else if (dataStore instanceof DataGroup) {
			infoPanel.add(new JLabel("Data Sets"),gbc);
			gbc.gridx=2;
			infoPanel.add(new JLabel(((DataGroup)dataStore).dataSets().length+""),gbc);
			
		}
		
		gbc.gridx=1;
		gbc.gridy++;
		
		
		infoPanel.add(new JLabel("Total Reads"),gbc);
		gbc.gridx=2;
		totalCount = new JLabel(""+dataStore.getTotalReadCount());
		infoPanel.add(totalCount,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;

		infoPanel.add(new JLabel("Forward Count"),gbc);
		gbc.gridx=2;
		forwardCount = new JLabel(""+dataStore.getReadCountForStrand(Location.FORWARD));
		infoPanel.add(forwardCount,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		
		infoPanel.add(new JLabel("Reverse Count"),gbc);
		gbc.gridx=2;
		reveseCount = new JLabel(""+dataStore.getReadCountForStrand(Location.REVERSE));
		infoPanel.add(reveseCount,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		
		infoPanel.add(new JLabel("Unknown Count"),gbc);
		gbc.gridx=2;
		unknownCount = new JLabel(""+dataStore.getReadCountForStrand(Location.UNKNOWN));
		infoPanel.add(unknownCount,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;
		
		infoPanel.add(new JLabel("Average Read Length"),gbc);
		gbc.gridx=2;
		averageLength = new JLabel("Calculating...");
		infoPanel.add(averageLength,gbc);

		getContentPane().add(new JScrollPane(infoPanel),BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(300,250);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
		Thread t = new Thread(this);
		t.start();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		// The only action is to close
		setVisible(false);
		dispose();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run () {
		
		Chromosome [] chrs = dataStore.collection().genome().getAllChromosomes();
		double averageLength = 0;
		int totalCount = 0;
		int shortestLength = 0;
		int longestLength = 0;

		for (int c=0;c<chrs.length;c++) {
			long [] reads = dataStore.getReadsForChromosome(chrs[c]);

			for (int i=0;i<reads.length;i++) {
				totalCount++;
				
				if (i==0) {
					shortestLength = SequenceRead.length(reads[i]);
					longestLength = SequenceRead.length(reads[i]);
				}
	
				if (SequenceRead.length(reads[i]) < shortestLength) shortestLength = SequenceRead.length(reads[i]);
				if (SequenceRead.length(reads[i]) > longestLength) longestLength = SequenceRead.length(reads[i]);
				
				averageLength += SequenceRead.length(reads[i]);
			}
		}		
		averageLength /= totalCount;
		
		this.averageLength.setText(""+(int)averageLength+"bp ("+shortestLength+"-"+longestLength+")");

	}
	
}
