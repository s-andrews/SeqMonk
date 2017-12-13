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
package uk.ac.babraham.SeqMonk.Displays.Report.InteractionReport;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Filters.OptionsListener;
import uk.ac.babraham.SeqMonk.Reports.Interaction.InteractionReport;

/**
 * The Class ReportOptions displays a dialog containing the options panel
 * for a report.
 */
public class InteractionReportOptions extends JDialog implements ActionListener, ProgressListener, OptionsListener {
	
	/** The ok button. */
	private JButton okButton;
	
	/** The application. */
	private SeqMonkApplication application;
	
	/** The report. */
	private InteractionReport report;
	
	/**
	 * Instantiates a new report options.
	 * 
	 * @param parent the parent
	 * @param report the report
	 */
	public InteractionReportOptions (SeqMonkApplication parent, InteractionReport report) {
		super(parent,report.name()+" Options");
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		this.report = report;
		this.application = parent;
		
		report.addOptionsListener(this);
		
		setSize(500,250);
		setLocationRelativeTo(parent);

		getContentPane().setLayout(new BorderLayout());
				
		JPanel topPanel = new JPanel();
		
		JLabel probeListLabel;
		
		if (report.dataCollection().probeSet() != null) {
			probeListLabel = new JLabel ("Reporting on probes in '"+report.dataCollection().probeSet().getActiveList().name()+"' ("+report.dataCollection().probeSet().getActiveList().getAllProbes().length+" probes)",JLabel.CENTER);
		}
		else {
			probeListLabel = new JLabel ("Reporting on all data",JLabel.CENTER);			
		}
		probeListLabel.setFont(new Font("Default",Font.BOLD,12));
		topPanel.add(probeListLabel);

		getContentPane().add(topPanel,BorderLayout.NORTH);

		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		
		okButton = new JButton("OK");
		okButton.setActionCommand("ok");
		okButton.addActionListener(this);
		buttonPanel.add(okButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		JPanel optionsPanel = report.getOptionsPanel();
		
		if (optionsPanel != null) {
			
			getContentPane().add(report.getOptionsPanel());			
			setVisible(true);
		}
		else {
			actionPerformed(new ActionEvent(this,0,"ok"));
		}
		
		// Some reports need to have set up their options panel
		// before the isReady option works, so we make this call
		// only once the whole dialog is laid out.
		okButton.setEnabled(report.isReady());

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
			okButton.setEnabled(false);
			report.addProgressListener(this);
			report.addProgressListener(new ProgressDialog(this,"Creating report...",report));
			report.generateReport();
		}		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressCancelled()
	 */
	public void progressCancelled() {
		// Reenable the OK button as long as they haven't been messing around.
		optionsChanged();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressComplete(java.lang.String, java.lang.Object)
	 */
	public void progressComplete(String command, Object result) {
		new InteractionReportTableDialog(application,report,(TableModel)result);
		setVisible(false);
		dispose();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressExceptionReceived(java.lang.Exception)
	 */
	public void progressExceptionReceived(Exception e) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressUpdated(java.lang.String, int, int)
	 */
	public void progressUpdated(String message, int current, int max) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressWarningReceived(java.lang.Exception)
	 */
	public void progressWarningReceived(Exception e) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.OptionsListener#optionsChanged()
	 */
	public void optionsChanged() {

		if (report.isReady()) {
			okButton.setEnabled(true);
		}
		else {
			okButton.setEnabled(false);
		}
	}

}
