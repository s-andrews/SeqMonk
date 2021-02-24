/**
 * Copyright 2009- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;

/**
 * The Class TrendOverProbePreferencesDialog sets the preferences from which a
 * trend plot can be drawn.
 */
public class QuantitationTrendPlotPreferencesDialog extends JDialog implements ActionListener, ProgressListener {
	
	/** The probes. */
	private ProbeList [] probes;
	
	/** The stores. */
	private DataStore [] stores;
	
	private QuantitationTrendPlotPreferencesPanel prefPanel;
	
	/** The data collection the features will come from **/
	private DataCollection collection;
	
	private JButton plotButton;
	
	/**
	 * Instantiates a new trend over probe preferences dialog.
	 * 
	 * @param probes the probes
	 * @param stores t he stores
	 */
	public QuantitationTrendPlotPreferencesDialog (DataCollection collection, ProbeList [] probes, DataStore [] stores) {
		super(SeqMonkApplication.getInstance(),"Quantitation Trend Preferences");
		this.probes = probes;
		
		
		// Do a sanity check that all stores are quantitated, otherwise we can't use them.
		Vector<DataStore> validStores = new Vector<DataStore>();
		
		for (int s=0;s<stores.length;s++) {
			if (stores[s].isQuantitated()) {
				validStores.add(stores[s]);
			}
		}
		
		if (validStores.size() == stores.length) {
			// Everything is fine
			this.stores = stores;
		}
		else if (validStores.size() == 0) {
			// We have no data to work with.
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "Can't draw this plot. None of your visible DataStores were quantitated", "Error drawing plot", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else {
			// Some stores were bad
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "Some unquantitated stores were removed", "Warning drawing plot", JOptionPane.WARNING_MESSAGE);
			this.stores = validStores.toArray(new DataStore[0]);
		}
		
		
		this.collection = collection;
		
		setupDialog();
	}
	
	
	
	private void setupDialog () {
		
		getContentPane().setLayout(new BorderLayout());
		
		prefPanel = new QuantitationTrendPlotPreferencesPanel(collection);
		prefPanel.addChangeListener(new ChangeListener() {
			
			public void stateChanged(ChangeEvent e) {
				if (plotButton != null) {
					plotButton.setEnabled(prefPanel.selectedFeatureTypes().length>0);
				}
			}
		});
			
		getContentPane().add(prefPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel");
		buttonPanel.add(cancelButton);
		
		plotButton = new JButton("Create Plot");
		plotButton.setEnabled(false);
		plotButton.addActionListener(this);
		plotButton.setActionCommand("plot");
		buttonPanel.add(plotButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(800,400);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
			
	}
	

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("cancel")) {
			setVisible(false);
			dispose();
		}
		else if (e.getActionCommand().equals("plot")) {
			if (prefPanel.selectedFeatureTypes().length == 0) {
				return;
			}
			if (prefPanel.getProbes().length == 0) {
				JOptionPane.showMessageDialog(this, "These feature options don't generate any probes","No Probes made",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			QuantitationTrendData trendData = new QuantitationTrendData(stores, probes, prefPanel);
			
			trendData.addProgressListener(new ProgressDialog(this, "Calculating trend", trendData));
			
			trendData.addProgressListener(this);
			
			trendData.startCalculating();
						
			setVisible(false);
			//dispose(); // Can we do this?
		}
	}



	public void progressExceptionReceived(Exception e) {
		setVisible(false);
		dispose();
	}
	public void progressWarningReceived(Exception e) {}
	public void progressUpdated(String message, int current, int max) {}


	public void progressCancelled() {
		setVisible(false);
		dispose();
	}


	public void progressComplete(String command, Object result) {
		if (this instanceof QuantitationTrendHeatmapPreferencesDialog) {
			new QuantitationTrendHeatmapDialog((QuantitationTrendData)result);
		}
		else {
			new QuantitationTrendPlotDialog((QuantitationTrendData)result);
		}
	}	
	
}
