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
package uk.ac.babraham.SeqMonk.Displays.ProbeTrendPlot;


import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;

/**
 * The Class TrendOverProbePreferencesDialog sets the preferences from which a
 * trend plot can be drawn.
 */
public class TrendOverProbePreferencesDialog extends JDialog implements ActionListener {
	
	/** The prefs. */
	private TrendOverProbePreferences prefs = new TrendOverProbePreferences();
	
	/** Whether we're doing a visible region plot **/
	private boolean visibleRegionPlot;
	
	private Chromosome chromosome;
	private int start;
	private int end;
	
	/** The plot type box */
	private JComboBox plotTypeBox;
	
	/** The strand box. */
	private JComboBox strandBox;
	
	/** The correct total box. */
	private JCheckBox correctTotalBox;
	private JLabel correctTotalBoxLabel;
	
	/** The correct each store box. */
	private JCheckBox correctEachStoreBox;
	
	/** The remove duplicates box. */
	private JCheckBox removeDuplicatesBox;
	
	/** The force relative box. */
	private JCheckBox forceRelativeBox;
	
	/** The probes. */
	private ProbeList [] probeLists;
	
	/** The stores. */
	private DataStore [] stores;
	
	/**
	 * Instantiates a new trend over probe preferences dialog.
	 * 
	 * @param probes the probes
	 * @param stores the stores
	 */
	public TrendOverProbePreferencesDialog (ProbeList [] probeLists, DataStore [] stores) {
		super(SeqMonkApplication.getInstance(),"Trend Over Probe Preferences");
		this.probeLists = probeLists;
		this.stores = stores;
		visibleRegionPlot = false;
		
		setupDialog();
	}
	
	/**
	 * Instantiates a new trend over probe preferences dialog.
	 * 
	 * @param c the chromosome
	 * @param start the start
	 * @param end the end
	 * @param stores the stores
	 */
	public TrendOverProbePreferencesDialog (Chromosome c, int start, int end, DataStore [] stores) {
		super(SeqMonkApplication.getInstance(),"Trend Over Probe Preferences");
		this.chromosome = c;
		this.start = start;
		this.end = end;
		this.stores = stores;
		visibleRegionPlot = true;
		
		setupDialog();
	}

	
	
	private void setupDialog () {
		
		getContentPane().setLayout(new BorderLayout());
		
		JPanel prefPanel = new JPanel();
	
		prefPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.NONE;

		prefPanel.add(new JLabel("Plot type"),gbc);
		
		gbc.gridx = 2;
		plotTypeBox = new JComboBox(new String [] {"Relative Distribution Plot","Cumulative Count Plot"});
		plotTypeBox.addActionListener(this);
		plotTypeBox.setActionCommand("change_plot");
		prefPanel.add(plotTypeBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;

		
		prefPanel.add(new JLabel("Use reads on strand"),gbc);
	
		gbc.gridx = 2;
		strandBox = new JComboBox(QuantitationStrandType.getTypeOptions());
		strandBox.addActionListener(this);
		strandBox.setActionCommand("change_strand");
		prefPanel.add(strandBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		
		correctTotalBoxLabel = new JLabel("Correct for total read count");
		if (prefs.plotType == TrendOverProbePreferences.RELATIVE_DISTRIBUTION_PLOT) correctTotalBoxLabel.setEnabled(false);
		prefPanel.add(correctTotalBoxLabel,gbc);
		gbc.gridx = 2;
		correctTotalBox = new JCheckBox();
		correctTotalBox.setSelected(prefs.correctForTotalCount);
		if (prefs.plotType == TrendOverProbePreferences.RELATIVE_DISTRIBUTION_PLOT) correctTotalBox.setEnabled(false);
		prefPanel.add(correctTotalBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		
		prefPanel.add(new JLabel("Scale within each data store/list"),gbc);
		gbc.gridx = 2;
		correctEachStoreBox = new JCheckBox();
		correctEachStoreBox.setSelected(prefs.correctWithinEachStore);
		prefPanel.add(correctEachStoreBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		
		prefPanel.add(new JLabel("Remove duplicate reads"),gbc);
		gbc.gridx = 2;
		removeDuplicatesBox = new JCheckBox();
		removeDuplicatesBox.setSelected(prefs.removeDuplicates);
		prefPanel.add(removeDuplicatesBox,gbc);
		
		gbc.gridy++;
		gbc.gridx=1;
		
		prefPanel.add(new JLabel("Force plot to be relative"),gbc);
		gbc.gridx = 2;
		forceRelativeBox = new JCheckBox();
		forceRelativeBox.setSelected(false);
		prefPanel.add(forceRelativeBox,gbc);
		
		getContentPane().add(prefPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel");
		buttonPanel.add(cancelButton);
		
		JButton plotButton = new JButton("Create Plot");
		plotButton.addActionListener(this);
		plotButton.setActionCommand("plot");
		buttonPanel.add(plotButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(400,300);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
			
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("change_strand")) {			
			prefs.quantitationType = (QuantitationStrandType)strandBox.getSelectedItem();
		}
		
		else if (e.getActionCommand().equals("change_plot")) {
			if (plotTypeBox.getSelectedItem().equals("Relative Distribution Plot")) {
				prefs.plotType = TrendOverProbePreferences.RELATIVE_DISTRIBUTION_PLOT;
				correctTotalBox.setEnabled(false);
				correctTotalBoxLabel.setEnabled(false);
			}
			else {
				prefs.plotType = TrendOverProbePreferences.CUMULATIVE_COUNT_PLOT;				
				correctTotalBox.setEnabled(true);
				correctTotalBoxLabel.setEnabled(true);
			}
		}
		
		else if (e.getActionCommand().equals("cancel")) {
			setVisible(false);
			dispose();
		}
		else if (e.getActionCommand().equals("plot")) {
			prefs.correctForTotalCount = correctTotalBox.isSelected();
			prefs.correctWithinEachStore = correctEachStoreBox.isSelected();
			prefs.forceRelative = forceRelativeBox.isSelected();
			prefs.removeDuplicates = removeDuplicatesBox.isSelected();
			setVisible(false);
			if (visibleRegionPlot) {
				new TrendOverProbeDialog(chromosome,start,end, stores, prefs);
			}
			else {
				new TrendOverProbeDialog(probeLists,stores,prefs);
			}
		}
	}

}
