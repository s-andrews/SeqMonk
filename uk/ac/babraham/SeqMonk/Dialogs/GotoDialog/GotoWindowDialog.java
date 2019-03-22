/**
 * Copyright Copyright 2010-19 Simon Andrews
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
import javax.swing.JPanel;
import javax.swing.JTextField;

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
public class GotoWindowDialog extends JDialog implements ActionListener, KeyListener {
	
	/** The chromosome. */
	private JComboBox chromosome;
	
	/** The centre position. */
	private JTextField centre;
	
	/** The window size. */
	private JTextField window;
	
	/** The ok button. */
	private JButton okButton;
		

	/**
	 * Instantiates a new goto window dialog.
	 * 
	 * @param application the application
	 */
	public GotoWindowDialog (SeqMonkApplication application) {
		super(application,"Jump to window...");
		setSize(300,200);
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
		
		choicePanel.add(new JLabel("Centre ",JLabel.RIGHT),gbc);
		
		gbc.gridx++;
		gbc.weightx=0.6;
		
		centre = new JTextField(""+SequenceRead.midPoint(DisplayPreferences.getInstance().getCurrentLocation()),5);
		centre.addKeyListener(new NumberKeyListener(false, false));
		centre.addKeyListener(this);
		choicePanel.add(centre,gbc);

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx=0.2;
		
		choicePanel.add(new JLabel("Window ",JLabel.RIGHT),gbc);
		
		gbc.gridx++;
		gbc.weightx=0.6;
		
		window = new JTextField(""+SequenceRead.length(DisplayPreferences.getInstance().getCurrentLocation()),5);
		window.addKeyListener(new NumberKeyListener(false, false));
		window.addKeyListener(this);
		choicePanel.add(window,gbc);
		
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
	

	/**
	 * Do goto.
	 */
	private void doGoto () {
		
		Chromosome chr = (Chromosome)chromosome.getSelectedItem();
		int centreValue = chr.length()/2;
		int windowValue = 1000;

		int startValue;
		int endValue;
		
		// Work out the window positions we want
		
		if (centre.getText().length()>0) {
			centreValue = Integer.parseInt(centre.getText());
		}
		if (window.getText().length() > 0) {
			windowValue = Integer.parseInt(window.getText());
		}
		
		if (windowValue < 1) windowValue = 1;
		
		startValue = centreValue-(windowValue/2);
		endValue = startValue+(windowValue-1);
		
		
		DisplayPreferences.getInstance().setLocation(chr, SequenceRead.packPosition(startValue, endValue, Location.UNKNOWN));

		setVisible(false);
		dispose();
		
	}
	
	
	/**
	 * Check ok.
	 */
	private void checkOK () {
		// Check to see if enough information has been added to allow us to
		// enable the OK button.
				
		// We need a start value
		
		// I've provided a default value so this is no longer needed
		
//		if (start.getText().length()==0) {
//			okButton.setEnabled(false);
//			return;
//		}

		// We need an end value

		// I've provided a default value so this is no longer needed

//		if (end.getText().length()==0) {
//			okButton.setEnabled(false);
//			return;
//		}

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
				
		checkOK();
	}


}


