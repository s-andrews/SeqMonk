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
package uk.ac.babraham.SeqMonk.Dialogs.CrashReporter;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Displays.HTMLDisplay.HTMLDisplayDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.R.RException;

/**
 * The Class CrashReporter is the dialog which appears when an
 * unexpected exception is encountered.  It can generate a stack
 * trace and submit it back to the authors to fix.
 * 
 * This class shouldn't be called directly - you should use the CrashReporter
 * class, which can in turn call this if we're in an environment which would
 * allow this to be displayed.
 */
public class CrashReporterDialog extends JDialog implements ActionListener {

	/** The Constant reportURLString. */
	public static final String reportURLString = "http://www.bioinformatics.babraham.ac.uk/cgi-bin/public/crashreporter.cgi";
//	public static final String reportURLString = "http://bilin1/cgi-bin/public/crashreporter.cgi";
	
	/** The e. */
	private final Throwable e;	
		
	/**
	 * Instantiates a new crash reporter.
	 * 
	 * @param e the e
	 * @param c the c
	 */
	public CrashReporterDialog (Throwable e) {
		super(SeqMonkApplication.getInstance(),"Oops - Crash Reporter");
		
		this.e = e;
		e.printStackTrace();

		if (e instanceof OutOfMemoryError) {
			// Don't issue a normal crash report but tell them that they
			// ran out of memory
			
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "<html>You ran out of memory!<br><br>Please look at Help &gt; Contents &gt; Configuration to see how to fix this", "Out of memory", JOptionPane.ERROR_MESSAGE);				
			return;
		}
		
		
		setModal(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setSize(600,200);
		setLocationRelativeTo(SeqMonkApplication.getInstance());

		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx=2;
		gbc.gridy=1;
		gbc.weightx = 0.2;
		gbc.gridheight = 4;
		gbc.weighty = 0.5;
		gbc.fill=GridBagConstraints.HORIZONTAL;
		
		ImageIcon sadMonk = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/sad_monk100.png"));
	
		getContentPane().add(new JLabel(sadMonk),gbc);
		gbc.weightx = 0.2;
		gbc.weighty = 0.5;

		gbc.weightx = 0.8;
		gbc.gridheight = 1;
		gbc.gridx=1;

		JLabel genericMessage = new JLabel("SeqMonk encountered a problem.",JLabel.CENTER);

		getContentPane().add(genericMessage,gbc);
		gbc.gridy++;

		JLabel sorryMessage = new JLabel("Sorry about that.  The error was: ",JLabel.CENTER);
		
		getContentPane().add(sorryMessage,gbc);
		gbc.gridy++;

		JLabel errorClass = new JLabel(e.getClass().getName(),JLabel.CENTER);
		errorClass.setFont(new Font("default",Font.BOLD,12));
		errorClass.setForeground(Color.RED);
		getContentPane().add(errorClass,gbc);

		
		gbc.gridy++;
		JLabel errorMessage = new JLabel(e.getLocalizedMessage(),JLabel.CENTER);
		errorMessage.setFont(new Font("default",Font.BOLD,12));
		errorMessage.setForeground(Color.RED);
		
		getContentPane().add(errorMessage,gbc);
		
		JPanel buttonPanel = new JPanel();
		JButton sendButton = new JButton("Report Error And Get Help");
		sendButton.setActionCommand("send_report");
		sendButton.addActionListener(this);
		buttonPanel.add(sendButton);
		
		JButton closeButton = new JButton("Ignore");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		
		buttonPanel.add(closeButton);
		
		gbc.gridy++;
		gbc.gridwidth=2;
		getContentPane().add(buttonPanel,gbc);
		
		setVisible(true);
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("send_report")) {
			new ReportSender(this);
		}
	}
	
	/**
	 * The Class ReportSender.
	 */
	private class ReportSender extends JDialog implements ActionListener, Runnable {
		
		/** The report text. */
		private String reportText;
		
		/** The email. */
		private JTextField email;
		
		/** Do we remember the email */
		private JCheckBox rememberEmail;
		
		/** The send button. */
		private JButton sendButton;
		
		/** The cancel button. */
		private JButton cancelButton;
		
		/** A flag to say if we found any SeqMonk specific classes **/
		private boolean foundSeqMonkClass = false;
		
		/** The cr. */
		private CrashReporterDialog cr;

		
		/**
		 * Instantiates a new report sender.
		 * 
		 * @param cr the cr
		 */
		public ReportSender (CrashReporterDialog cr) {
			super(cr,"Send Error Report");
			this.cr = cr;
			setSize(500,500);
			setLocationRelativeTo(cr);
			
			reportText = makeReportText();
			
			getContentPane().setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.gridx=1;
			gbc.gridy=1;
			gbc.weightx = 0.5;
			gbc.weighty = 0.1;
			gbc.fill=GridBagConstraints.HORIZONTAL;
			
			JLabel introLabel = new JLabel("The contents of the report are shown below:",JLabel.CENTER);
			getContentPane().add(introLabel,gbc);
			
			JTextArea reportTextArea = new JTextArea(reportText);
			reportTextArea.setEditable(false);
			
			gbc.gridy++;
			gbc.weighty = 0.9;
			gbc.fill=GridBagConstraints.BOTH;
			getContentPane().add(new JScrollPane(reportTextArea),gbc);
			
			gbc.gridy++;
			gbc.weighty = 0.1;
			gbc.fill=GridBagConstraints.HORIZONTAL;

			
			JLabel notifyLabel = new JLabel("Enter your email below to allow us to help you with this problem",JLabel.CENTER);

			getContentPane().add(notifyLabel,gbc);
			
			JPanel emailPanel = new JPanel();
			emailPanel.add(new JLabel("Your Email:"));
			email = new JTextField(30);
			email.setText(SeqMonkPreferences.getInstance().getCrashEmail());
			emailPanel.add(email);
			
			gbc.gridy++;
			
			getContentPane().add(emailPanel,gbc);
			
			JPanel rememberPanel = new JPanel();
			rememberPanel.add(new JLabel("Remember this address for future reports "));
			rememberEmail = new JCheckBox();
			
			// We don't initially remember their email - but we will 
			// once they have supplied one.
			if (SeqMonkPreferences.getInstance().getCrashEmail().length() > 0) {
				rememberEmail.setSelected(true);
			}
			rememberPanel.add(rememberEmail);
			
			gbc.gridy++;
			
			getContentPane().add(rememberPanel,gbc);
			
			JPanel buttonPanel = new JPanel();
			sendButton = new JButton("Send");
			sendButton.setActionCommand("send_report");
			sendButton.addActionListener(this);
			buttonPanel.add(sendButton);
			
			cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("cancel");
			cancelButton.addActionListener(this);
			
			buttonPanel.add(cancelButton);
			
			gbc.gridy++;
			getContentPane().add(buttonPanel,gbc);
			
			setVisible(true);
		}
		
		/**
		 * Make report text.
		 * 
		 * @return the string
		 */
		private String makeReportText () {
			StringBuffer sb = new StringBuffer();
			
			sb.append("SeqMonk Version:");
			sb.append(SeqMonkApplication.VERSION);
			sb.append("\n\n");
			
			sb.append("Operating System:");
			sb.append(System.getProperty("os.name"));
			sb.append(" - ");
			sb.append(System.getProperty("os.version"));
			sb.append("\n\n");

			sb.append("Java Version:");
			sb.append(System.getProperty("java.version"));
			sb.append(" - ");
			sb.append(System.getProperty("java.vm.version"));
			sb.append("\n\n");
			
			sb.append(e.getClass().getName());
			if (e.getClass().getName().indexOf("SeqMonk") >=0) {
				foundSeqMonkClass = true;
			}
			sb.append("\n");
			sb.append(e.getMessage());
			if (e.getMessage() != null && e.getMessage().indexOf("SeqMonk") >=0) {
				foundSeqMonkClass = true;
			}
			sb.append("\n\n");

			StackTraceElement [] elements = e.getStackTrace();
			for (int i=0;i<elements.length;i++) {
				sb.append(elements[i].toString());
				sb.append("\n");
				if (elements[i].toString().indexOf("SeqMonk") >=0) {
					foundSeqMonkClass = true;
				}
			}
			
			// R errors have an extra block of text with the R log file in them.
			if (e instanceof RException) {
				sb.append(((RException)e).logText());
			}
			
			return sb.toString();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent ae) {
			if (ae.getActionCommand().equals("cancel")) {
				setVisible(false);
				dispose();
			}
			else if (ae.getActionCommand().equals("send_report")) {
		
				if (! foundSeqMonkClass) {
					int reply = JOptionPane.showConfirmDialog(this, "<html>This error doesn't appear to come from within SeqMonk but is a bug in the core Java classes.<br>You can still submit this report, but the SeqMonk authors may not be able to fix it!<br><br>Do you still want to send the report?</html>", "Not our fault!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (reply == JOptionPane.NO_OPTION) return;
				}
				
				if (email.getText().length() == 0) {
					int reply = JOptionPane.showConfirmDialog(this, "<html>You have not provided an email address.<br>Your report is still useful to us, but we can't send you any feedback about this bug.<br><br>Do you want to send anyway?</html>", "Send anonymous report?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (reply == JOptionPane.NO_OPTION) return;
				}
				
				if (email.getText().toLowerCase().equals("simon.andrews@babraham.ac.uk") | email.getText().toLowerCase().equals("babraham.bioinformatics@babraham.ac.uk")) {
					JOptionPane.showMessageDialog(this, "<html>That isn't your email - it's mine!<br><br>We need your email so we know who to reply to.</html>", "Can't reply to that...", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				
				// Check if we need to store their email
				if (rememberEmail.isSelected()) {
					SeqMonkPreferences.getInstance().setCrashEmail(email.getText());
					try {
						SeqMonkPreferences.getInstance().savePreferences();
					} 
					catch (IOException e) {
						// We're going to ignore this in the UI since we're already
						// inside a crash dialog.
						e.printStackTrace();
					}
				}
				
				Thread t = new Thread(this);
				t.start();
			}		
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {

			sendButton.setEnabled(false);
			sendButton.setText("Sending...");
			cancelButton.setEnabled(false);
			
			// Send the actual report.
			
			// We don't need to worry about proxy settings as these
			// will have been put into the System properties by the
			// SeqMonkPreferences class and should be picked up
			// automatically.
			
			try {
				URL url = new URL(reportURLString);
				String data = URLEncoder.encode("email","ISO-8859-1")+"="+URLEncoder.encode(email.getText(),"ISO-8859-1")+"&"+URLEncoder.encode("stacktrace","ISO-8859-1")+"="+URLEncoder.encode(reportText,"ISO-8859-1");

				URLConnection conn = url.openConnection();
				conn.setDoOutput(true);
				
				OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
				writer.write(data);
				writer.flush();
				
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				
				StringBuffer htmlResponse = new StringBuffer();
				String line;
				while ((line=reader.readLine()) != null) {
					htmlResponse.append(line);
				}
				writer.close();
				reader.close();
				
				if (!htmlResponse.toString().startsWith("Report Sent")) {
					
					JOptionPane.showMessageDialog(cr, "We found some information which might help solve the probelm you hit", "Help found", JOptionPane.INFORMATION_MESSAGE);
					
					// We've been returned a possible solution
					new HTMLDisplayDialog(htmlResponse.toString());
				}
				setVisible(false);
				cr.setVisible(false);
				cr.dispose();
				

			}
			catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error Sending Report: "+e.getLocalizedMessage(), "Error sending crash Report", JOptionPane.ERROR_MESSAGE);

				sendButton.setText("Sending Failed...");
				cancelButton.setEnabled(true);
				
			}
			
		}
	}
}
