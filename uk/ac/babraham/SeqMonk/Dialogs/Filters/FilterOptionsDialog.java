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
package uk.ac.babraham.SeqMonk.Dialogs.Filters;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Filters.OptionsListener;
import uk.ac.babraham.SeqMonk.Filters.ProbeFilter;

public class FilterOptionsDialog extends JDialog implements OptionsListener, ProgressListener, ActionListener {

	private ProbeFilter filter;
	private JButton filterButton;
	
	public FilterOptionsDialog (DataCollection collection, ProbeFilter filter) {
		super(SeqMonkApplication.getInstance(),filter.name());
	
		this.filter = filter;
		
		filter.addProgressListener(this);
		filter.addOptionsListener(this);
		
		getContentPane().setLayout(new BorderLayout());
		
		JLabel probeListLabel = new JLabel ("Testing probes in '"+collection.probeSet().getActiveList().name()+"' ("+collection.probeSet().getActiveList().getAllProbes().length+" probes)",JLabel.CENTER);
		probeListLabel.setFont(new Font("Default",Font.BOLD,12));
		probeListLabel.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
		getContentPane().add(probeListLabel,BorderLayout.NORTH);
		
		if (filter.hasOptionsPanel()) {
			JPanel optionsPanel = filter.getOptionsPanel();
			getContentPane().add(optionsPanel,BorderLayout.CENTER);
			setSize(optionsPanel.getPreferredSize());
		}
		else {
			getContentPane().add(new JLabel("No Options",JLabel.CENTER),BorderLayout.CENTER);
			setSize(400, 50);
		}
		
		JPanel buttonPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);
		
		filterButton = new JButton("Run Filter");
		filterButton.setActionCommand("filter");
		getRootPane().setDefaultButton(filterButton);
		filterButton.addActionListener(this);
		if (!filter.isReady()) {
			filterButton.setEnabled(false);
		}
		buttonPanel.add(filterButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}
	


	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("filter")) {
			if (filter.isReady()) {
				filterButton.setEnabled(false);
				filter.addProgressListener(new ProgressDialog(this,"Running Filter...",filter));
				try {
					filter.runFilter();
				} catch (SeqMonkException e) {
					progressExceptionReceived(e);
				}
			}
			else {
				JOptionPane.showMessageDialog(this,"Filter options have not all been set","Can't run filter",JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}


	public void optionsChanged() {
		if (filter.isReady()) {
			filterButton.setEnabled(true);
		}
		else {
			filterButton.setEnabled(false);
		}
	}


	public void progressCancelled() {
		filterButton.setEnabled(true);		
	}


	public void progressComplete(String command, Object result) {

		ProbeList newList = (ProbeList)result;
		
		filterButton.setEnabled(true);
		
		// See if any probes actually passed
		if (newList.getAllProbes().length == 0) {
			// We need to remove this empty list.
			newList.delete();
			JOptionPane.showMessageDialog(this,"No probes matched the criteria set","Info",JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		// Ask for a name for the list
		String groupName=null;
		while (true) {
			groupName = (String)JOptionPane.showInputDialog(this,"Enter list name","Found "+newList.getAllProbes().length+" probes",JOptionPane.QUESTION_MESSAGE,null,null,newList.name());
			if (groupName == null){
				// Since the list will automatically have been added to
				// the ProbeList tree we actively need to delete it if
				// they choose to cancel at this point.
				newList.delete();
				return;  // They cancelled
			}			
				
			if (groupName.length() == 0)
				continue; // Try again
			
			break;
		}
		newList.setName(groupName);		
		
	}


	public void progressExceptionReceived(Exception e) {
		filterButton.setEnabled(true);
		// We shouldn't need to do anything with the exception as it will be handled
		// by the progress dialog we kicked off.
	}


	public void progressUpdated(String message, int current, int max) {}


	public void progressWarningReceived(Exception e) {}
}
