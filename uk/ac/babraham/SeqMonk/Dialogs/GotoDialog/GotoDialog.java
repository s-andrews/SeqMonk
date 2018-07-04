/**
 * Copyright Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.GotoDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;


/**
 * The Class GotoDialog provides a quick way to jump to a known position
 * in the genome.
 */
public class GotoDialog extends JDialog implements ActionListener, KeyListener, ListSelectionListener {

	/** The chromosome. */
	private JComboBox chromosome;
	
	/** The start. */
	private JTextField start;
	
	/** The end. */
	private JTextField end;
	
	private JTextField location;
	
	/** The ok button. */
	private JButton okButton;
	
	private JList recentList;
	
	private static RecentLocation [] recentLocations = new RecentLocation[10];
	
	private static final Color DARK_GREEN = new Color(0,180,0);
	private static final Color DARK_RED = new Color(200,0,0);
	

	/**
	 * Instantiates a new goto dialog.
	 * 
	 * @param application the application
	 */
	public GotoDialog (SeqMonkApplication application) {
		super(application,"Goto Position...");
		setSize(350,400);
		setLocationRelativeTo(application);
		setModal(true);
		
		getContentPane().setLayout(new BorderLayout());
		
		JPanel choicePanel = new JPanel();

		choicePanel.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=0;
		gbc.gridy=0;
		gbc.weightx=0.2;
		gbc.weighty=0.5;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill=GridBagConstraints.HORIZONTAL;
		
		choicePanel.add(new JLabel("Chromosome",JLabel.RIGHT),gbc);
	
		gbc.gridx++;
		gbc.weightx=0.6;
		
		Chromosome [] chrs = application.dataCollection().genome().getAllChromosomes();
		
		chromosome = new JComboBox(chrs);
		
		choicePanel.add(chromosome,gbc);
		
		Chromosome currentChromosome = DisplayPreferences.getInstance().getCurrentChromosome();
		for (int i=0;i<chrs.length;i++) {
			if (chrs[i] == currentChromosome) {
				chromosome.setSelectedIndex(i);
				break;
			}
		}
		

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx=0.2;
		
		choicePanel.add(new JLabel("From ",JLabel.RIGHT),gbc);
		
		gbc.gridx++;
		gbc.weightx=0.6;
		
		start = new JTextField(""+SequenceRead.start(DisplayPreferences.getInstance().getCurrentLocation()),5);
		start.addKeyListener(new NumberKeyListener(false, false));
		start.addKeyListener(this);
		choicePanel.add(start,gbc);

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx=0.2;
		
		choicePanel.add(new JLabel("To ",JLabel.RIGHT),gbc);
		
		gbc.gridx++;
		gbc.weightx=0.6;
		
		end = new JTextField(""+SequenceRead.end(DisplayPreferences.getInstance().getCurrentLocation()),5);
		end.addKeyListener(new NumberKeyListener(false, false));
		end.addKeyListener(this);
		choicePanel.add(end,gbc);

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx=0.2;
		
		choicePanel.add(new JLabel("Location (chr:start-end) ",JLabel.RIGHT),gbc);
		
		gbc.gridx++;
		gbc.weightx=0.6;
		
		location = new JTextField("",5);
		location.addKeyListener(this);
		choicePanel.add(location,gbc);
		
		gbc.gridx=0;
		gbc.gridy++;
		gbc.gridwidth = 2;
		
		choicePanel.add(new JLabel("Recent Locations",JLabel.CENTER),gbc);
		
		recentList = new JList(recentLocations);
		recentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recentList.getSelectionModel().addListSelectionListener(this);
		gbc.gridy++;
		gbc.weighty = 0.9;
		choicePanel.add(new JScrollPane(recentList),gbc);
		
		getContentPane().add(choicePanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		
		okButton = new JButton("OK");
		okButton.setActionCommand("ok");
		okButton.addActionListener(this);
		okButton.setEnabled(true);
		getRootPane().setDefaultButton(okButton);
		buttonPanel.add(okButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setVisible(true);
	}
	
	public static void clearRecentLocations () {
		for (int i=0;i<recentLocations.length;i++) {
			recentLocations[i] = null;
		}
	}
	
	public static void addRecentLocation (Chromosome c, int start, int end) {
		RecentLocation l = new RecentLocation(c, start, end);
		
		// We now need to go through the existing set of locations.
		// If we find this location already there we remove it and shuffle
		// everything after it up.
		
		for (int i=0;i<recentLocations.length;i++) {
			if (recentLocations[i] == null) break;
			
			if (recentLocations[i].compareTo(l) == 0) {
				for (int j=i+1;j<recentLocations.length;j++) {
					recentLocations[j-1] = recentLocations[j];
				}
				break;
			}
		}
		
		// We now move all of the locations down one, and put the new
		// one at the top
		for (int i=recentLocations.length-2;i>=0;i--) {
			recentLocations[i+1] = recentLocations[i];
		}
		
		recentLocations[0] = l;
	}

	/**
	 * Do goto.
	 */
	private void doGoto () {
		
		Chromosome chr = (Chromosome)chromosome.getSelectedItem();
		int startValue = 1;
		int endValue = chr.length();

		if (start.getText().length()>0) {
			startValue = Integer.parseInt(start.getText());
		}
		if (end.getText().length() > 0) {
			endValue = Integer.parseInt(end.getText());
			if (endValue > chr.length()) endValue = chr.length();
		}

		if (startValue > endValue) {
			int temp = startValue;
			startValue = endValue;
			endValue = temp;
		}
		
		if (endValue - startValue < 5) {
			// This is too small, don't do it
			return;
		}
		
		DisplayPreferences.getInstance().setLocation(chr,SequenceRead.packPosition(startValue, endValue, Location.UNKNOWN));

		setVisible(false);
		dispose();
		
	}
	
	
	/**
	 * Check ok.
	 */
	private void checkOK () {
		// Check to see if enough information has been added to allow us to
		// enable the OK button.
					
		// If we get here then we're good to go
		okButton.setEnabled(true);
		
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("cancel")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("ok")) {
			doGoto();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent ke) {

	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent ke) {
		
		if (ke.getSource() == location) {
			// Try to parse the data out of the location
			String text = location.getText();
			
			if (text.toLowerCase().startsWith("chr")) {
				text = text.substring(3);
			}
			
			String [] sections = text.split("[-:]");
			
			if (sections.length != 3) {
				System.err.println("Found "+sections.length+" sections in "+text);
				location.setForeground(DARK_RED);
				return;
			}
			
			boolean foundChr = false;
			for (int c=0;c<chromosome.getModel().getSize();c++) {
				if (((Chromosome)chromosome.getModel().getElementAt(c)).name().equals(sections[0])) {
					chromosome.setSelectedIndex(c);
					foundChr = true;
					break;
				}
			}
			
			if (!foundChr) {
				System.err.println("Couldn't find chr "+sections[0]);
				location.setForeground(DARK_RED);
				return;				
			}
			
			try {
				int start = Integer.parseInt(sections[1]);
				int end = Integer.parseInt(sections[2]);
				this.start.setText(""+start);
				this.end.setText(""+end);
			}
			catch (NumberFormatException nfe) {
				location.setForeground(DARK_RED);
				return;
			}
			
			location.setForeground(DARK_GREEN);
			
		}
		
		checkOK();
	}

	public void valueChanged(ListSelectionEvent lse) {

		RecentLocation l = (RecentLocation)recentList.getSelectedValue();
		if (l != null) {
			for (int c=0;c<chromosome.getModel().getSize();c++) {
				if (chromosome.getModel().getElementAt(c) == l.chromsome()) {
					chromosome.setSelectedIndex(c);
					break;
				}
			}
			
			start.setText(""+l.start());
			end.setText(""+l.end());
			
		}
	}



}


