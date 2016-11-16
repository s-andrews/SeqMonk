/**
 * Copyright Copyright 2010-15 Simon Andrews
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.CrashReporter;

/**
 * The Class ProgressDialog is a generic progress dialog showing a progress
 * bar and a changing label.  It can also display a cancel button for 
 * progress listeners which allow it.
 */
public class ProgressRecordDialog extends JDialog implements Runnable, ProgressListener, ActionListener {

	/** The label. */
	private JTextArea textArea;
	private String currentMessage;
	
	/** The cancellable. */
	private Cancellable cancellable;
	
	private boolean hasCancelled = false;
		
	/** The warning count. */
	private int warningCount = 0;
	
	private boolean ignoreExceptions = false;
	
	/** The warnings. */
	private Vector<Exception>warnings = new Vector<Exception>();
	
	/** A record of any exception we've received */
	private Exception reportedException = null;

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param title the title
	 */
	public ProgressRecordDialog (String title) {
		this(SeqMonkApplication.getInstance(),title,null);
	}

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param title the title
	 * @param cancellable a cancellable object to end this process
	 */
	public ProgressRecordDialog (String title, Cancellable cancellable){
		this(SeqMonkApplication.getInstance(),title,cancellable);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 */
	public ProgressRecordDialog (JFrame parent, String title) {
		this(parent,title,null);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 * @param cancellable the cancellable
	 */
	public ProgressRecordDialog (JFrame parent, String title, Cancellable cancellable) {
		super(parent,title);
		setup(parent,cancellable);
	}

	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 */
	public ProgressRecordDialog (JDialog parent, String title) {
		this(parent,title,null);
	}
	
	/**
	 * Instantiates a new progress dialog.
	 * 
	 * @param parent the parent
	 * @param title the title
	 * @param cancellable the cancellable
	 */
	public ProgressRecordDialog (JDialog parent, String title, Cancellable cancellable) {
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
		
		setSize(600,400);
		setLocationRelativeTo(parent);
		
		this.cancellable = cancellable;
		textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(textArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.CENTER);
		
		if (cancellable != null) {
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(this);
			cancelButton.setActionCommand("cancel");
			getContentPane().add(cancelButton,BorderLayout.SOUTH);
		}
		
		
		Thread t = new Thread(this);
		t.start();

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
		
		if (currentMessage == null) {
			currentMessage = message;
		}
		else {
			currentMessage += "\n";
			currentMessage += message;
		}
		
		try {
			SwingUtilities.invokeAndWait(new Runnable() {			
				public void run() {
					textArea.setText(currentMessage);				
				}
			});
		} 
		catch (InterruptedException e) {} 
		catch (InvocationTargetException e) {}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressExceptionReceived(java.lang.Exception)
	 */
	public void progressExceptionReceived(Exception e) {
		
		if (reportedException != null && reportedException == e) return;
		
		reportedException = e;
		
		if (! ignoreExceptions) {
			new CrashReporter(e);
		}
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
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {}
		setVisible(false);

		if (warningCount > 0) {
			// We need to display a list of the warnings
			new WarningDisplayDialog(this,warningCount,warnings.toArray(new Exception [0]));
		}
		dispose();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressWarningReceived(java.lang.Exception)
	 */
	public void progressWarningReceived(Exception e) {
		warningCount++;
		// We just store this warning so we can display all
		// of them at the end.  We only keep the first 5000
		// so that things don't get too out of hand
		if (warningCount<=5000){
			warnings.add(e);
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


}
