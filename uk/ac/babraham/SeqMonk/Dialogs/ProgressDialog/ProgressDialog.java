/**
 * Copyright Copyright 2010-17 Simon Andrews
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Dialogs.CrashReporter.CrashReporter;


/**
 * The Class ProgressDialog is a generic progress dialog showing a progress
 * bar and a changing label.  It can also display a cancel button for 
 * progress listeners which allow it.
 */
public class ProgressDialog extends JDialog implements Runnable, ProgressListener, ActionListener {

	/** The label. */
	private JLabel label;
	
	/** The cancellable. */
	private Cancellable cancellable;
	
	private boolean hasCancelled = false;
	
	/** The current. */
	private int current = 0;
	
	/** The total. */
	private int total = 1;
	
	/** The progress bar. */
	private ProgressBar progressBar = new ProgressBar();
	
	/** The warning count. */
	private int warningCount = 0;
	
	private boolean ignoreExceptions = false;
	
	/** The warnings. */
	private Hashtable<String,CountedException>warnings = new Hashtable<String, CountedException>();
	
	/** A record of any exception we've recevied */
	private Exception reportedException = null;

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param title the title
	 */
	public ProgressDialog (String title) {
		this(SeqMonkApplication.getInstance(),title,null);
	}

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param title the title
	 * @param cancellable a cancellable object to end this process
	 */
	public ProgressDialog (String title, Cancellable cancellable){
		this(SeqMonkApplication.getInstance(),title,cancellable);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 */
	public ProgressDialog (JFrame parent, String title) {
		this(parent,title,null);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 * @param cancellable the cancellable
	 */
	public ProgressDialog (JFrame parent, String title, Cancellable cancellable) {
		super(parent,title);
		setup(parent,cancellable);
	}

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 */
	public ProgressDialog (JDialog parent, String title) {
		this(parent,title,null);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 * @param cancellable the cancellable
	 */
	public ProgressDialog (JDialog parent, String title, Cancellable cancellable) {
		super(parent,title);
		setup(parent,cancellable);
	}	
	
	
	/**
	 * Setup.
	 * 
	 * @param parent the parent
	 * @param cancellable the cancellable
	 */
	private void setup (Component parent, Cancellable cancellable) {
		setSize(400,75);
		setLocationRelativeTo(parent);
		
		this.cancellable = cancellable;
		label = new JLabel("",JLabel.CENTER);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(label,BorderLayout.CENTER);
		
		if (cancellable != null) {
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(this);
			cancelButton.setActionCommand("cancel");
			getContentPane().add(cancelButton,BorderLayout.EAST);
		}
		
		getContentPane().add(progressBar,BorderLayout.SOUTH);
		Thread t = new Thread(this);
		t.start();
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setVisible(true);
		
	}
	
	public void setIgnoreExceptions (boolean ignore) {
		this.ignoreExceptions = ignore;
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
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressUpdated(java.lang.String, int, int)
	 */
	public void progressUpdated(String message, int currentPos, int totalPos) {
		label.setText(message);
		current = currentPos;
		total = totalPos;
		progressBar.repaint();
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressExceptionReceived(java.lang.Exception)
	 */
	public void progressExceptionReceived(Exception e) {
		
		if (reportedException != null && reportedException == e) return;
		
		reportedException = e;
		
		setVisible(false);
		dispose();
		if (! ignoreExceptions) {
			// We need to actually invoke a crash reporter here.  
			new CrashReporter(e);		}
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressCancelled()
	 */
	public void progressCancelled() {
		setVisible(false);
		dispose();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressComplete(java.lang.String, java.lang.Object)
	 */
	public void progressComplete(String command, Object result) {
		setVisible(false);

		if (warningCount > 0) {
			// We need to display a list of the warnings
			new WarningDisplayDialog(this,warningCount,warnings.values().toArray(new CountedException [0]));
		}
		dispose();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressWarningReceived(java.lang.Exception)
	 */
	public void progressWarningReceived(Exception e) {
		warningCount++;
		
		// If this warning already exists we'll just count it
		if (warnings.containsKey(e.getMessage())) {
			warnings.get(e.getMessage()).increment();
		}

		else {
			// As long as we're not already storing over 5000 messages
			// we'll keep hold of this one as well.
			if (warnings.size()<=5000){
			
				warnings.put(e.getMessage(),new CountedException(e));
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		// This can only come from the cancel button
		
		if (hasCancelled) {
			// They've already pressed cancel and it's been ignored so we'll let the box go
			// if they press it again.  This could leave stuff running in the background but
			// having errant boxes around is really annoying.
			
			setVisible(false);
			dispose();
			return;
			
			
		}
		cancellable.cancel();
		hasCancelled = true;
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
