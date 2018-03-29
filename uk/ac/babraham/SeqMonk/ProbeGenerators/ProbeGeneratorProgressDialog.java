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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

// TODO: Move over to using a standard progress dialog
/**
 * The Class ProbeGeneratorProgressDialog.
 */
public class ProbeGeneratorProgressDialog extends JDialog implements Runnable, ProbeGeneratorListener {

	private JLabel label;	
	private int current = 0;
	private int total = 1;
	private ProgressBar progressBar = new ProgressBar();
	private Cancellable cancellable;


	/**
	 * Instantiates a new probe generator progress dialog.
	 * 
	 * @param parent 
	 * @param cancellable A cancellable object which can be used to stop this action
	 */
	public ProbeGeneratorProgressDialog (JFrame parent, Cancellable cancellable) {
		super(parent,"Creating Probes...");
		setSize(450,75);
		setLocationRelativeTo(parent);
		
		this.cancellable = cancellable;
		
		label = new JLabel("",JLabel.CENTER);
		getContentPane().setLayout(new BorderLayout());
		JPanel messagePanel = new JPanel();
		messagePanel.setLayout(new BorderLayout());
		messagePanel.add(label,BorderLayout.CENTER);

		if (cancellable != null) {
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					cancel();
				}
			});
			
			messagePanel.add(cancelButton,BorderLayout.EAST);
		}

		getContentPane().add(messagePanel,BorderLayout.CENTER);
		
		getContentPane().add(progressBar,BorderLayout.SOUTH);
		
		
		Thread t = new Thread(this);
		t.start();
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setVisible(true);
	}
	
	/**
	 * Sets a cancellation flag indicating that generation should stop
	 */
	private void cancel() {
		cancellable.cancel();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#updateGenerationProgress(java.lang.String, int, int)
	 */
	public void updateGenerationProgress(String message, int current, int total) {
		this.current = current;
		this.total = total;
		progressBar.repaint();
		label.setText(message);		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#generationExceptionReceived(java.lang.Exception)
	 */
	public void generationExceptionReceived(Exception e) {
		setVisible(false);
		dispose();
		throw new IllegalStateException(e);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#generationCancelled()
	 */
	public void generationCancelled () {
		setVisible(false);
		dispose();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#optionsNotReady()
	 */
	public void optionsNotReady() {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#optionsReady()
	 */
	public void optionsReady() {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGeneratorListener#generationComplete(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet)
	 */
	public void generationComplete(ProbeSet probes) {
		setVisible(false);
		dispose();
	}
	
	/**
	 * The Class ProgressBar.
	 */
	private class ProgressBar extends JPanel {
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paint(java.awt.Graphics)
		 */
		public void paint (Graphics g) {
			super.paint(g);
			g.setColor(Color.RED);
			g.fillRect(0,0,(int)(getWidth()*((float)current/total)),getHeight());
		}
		
	}
	
}
