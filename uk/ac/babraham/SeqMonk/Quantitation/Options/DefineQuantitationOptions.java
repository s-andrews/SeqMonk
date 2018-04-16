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
package uk.ac.babraham.SeqMonk.Quantitation.Options;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.QuantitationTypeRenderer;
import uk.ac.babraham.SeqMonk.Filters.OptionsListener;
import uk.ac.babraham.SeqMonk.Quantitation.BasePairQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.CoverageDepthQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.CoverageQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.DifferenceQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.DistanceToFeatureQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.DuplicationQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.EnrichmentNormalisationQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.EnrichmentQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.ExactOverlapQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.FixedValueQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.FourCEnrichmentQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.HiCCisTransQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.HiCPCADomainQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.HiCPrevNextQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.HiCReadCountQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.LogTransformQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.ManualCorrectionQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.MatchDistributionsQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.PerProbeNormalisationQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.PercentileNormalisationQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.ProbeLengthQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.Quantitation;
import uk.ac.babraham.SeqMonk.Quantitation.RankQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.ReadCountQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.RelativeQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.SizeFactorNormalisationQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.SmoothingQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.SmoothingSubtractionQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.SubsetNormalisationQuantitation;
import uk.ac.babraham.SeqMonk.Quantitation.ZScoreQuantitation;

/**
 * A dialog which is used to select a quantiation method and 
 * display its options panel to allow options to be set.  Once
 * complete the quantitation method can be run.
 */
public class DefineQuantitationOptions extends JDialog implements ActionListener, ProgressListener, OptionsListener, ListSelectionListener {

	private JPanel mainPanel;
	private Quantitation [] quantitations;
	private JList quantitationList;
	private JPanel optionPanel;
	private JButton runButton;
	private SeqMonkApplication application;
	private JCheckBox onlyVisible;
	
	/**
	 * Make a new quantitation options dialog
	 * 
	 * @param application The enclosing SeqMonkApplication
	 */
	public DefineQuantitationOptions (SeqMonkApplication application) {
		super(application,"Define Quantitation...");
		this.application = application;
		getContentPane().setLayout(new BorderLayout());

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.add(new JLabel("Quantitation Options",JLabel.LEFT),BorderLayout.NORTH);

		quantitations = new Quantitation [] {
				new EnrichmentQuantitation(application),
				new ReadCountQuantitation(application),
				new HiCReadCountQuantitation(application),
				new HiCCisTransQuantitation(application),
				new HiCPrevNextQuantitation(application),
				new HiCPCADomainQuantitation(application),
				new FourCEnrichmentQuantitation(application),
				new BasePairQuantitation(application),
				new ExactOverlapQuantitation(application),
				new DifferenceQuantitation(application),
				new CoverageQuantitation(application),
				new CoverageDepthQuantitation(application),
				new DuplicationQuantitation(application),
				new DistanceToFeatureQuantitation(application),
				new ProbeLengthQuantitation(application),
				new FixedValueQuantitation(application),
				new RelativeQuantitation(application),
				new ManualCorrectionQuantitation(application),
				new LogTransformQuantitation(application),
				new PercentileNormalisationQuantitation(application),
				new SizeFactorNormalisationQuantitation(application),
				new EnrichmentNormalisationQuantitation(application),
				new SubsetNormalisationQuantitation(application),
				new PerProbeNormalisationQuantitation(application),
				new RankQuantitation(application),
				new ZScoreQuantitation(application),
				new SmoothingQuantitation(application),
				new SmoothingSubtractionQuantitation(application),
				new MatchDistributionsQuantitation(application),
		};

		
		if (! application.dataCollection().isQuantitated()) {
			// We remove any quantitation options which require an
			// existing quantitation
			
			Vector<Quantitation> keepers = new Vector<Quantitation>();
			
			for (int q=0;q<quantitations.length;q++) {
				if (! quantitations[q].requiresExistingQuantitation()) {
					keepers.add(quantitations[q]);
				}
			}
			
			quantitations = keepers.toArray(new Quantitation[0]);
			
		}
		
		// See if we have any HiC data available
		boolean haveHiC = false;
		
		DataStore [] allStores  = application.dataCollection().getAllDataStores();
		for (int d=0;d<allStores.length;d++) {
			if (allStores[d] instanceof HiCDataStore && ((HiCDataStore)allStores[d]).isValidHiC()) {
				haveHiC = true;
			}
		}
		
		if (! haveHiC) {
			// We need to remove any HiC dependent quantitation methods from the list.

			Vector<Quantitation> keepers = new Vector<Quantitation>();
			
			for (int q=0;q<quantitations.length;q++) {
				if (! quantitations[q].requiresHiC()) {
					keepers.add(quantitations[q]);
				}
			}
			
			quantitations = keepers.toArray(new Quantitation[0]);

		}
		
		
		quantitationList = new JList(quantitations);
		quantitationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		quantitationList.getSelectionModel().addListSelectionListener(this);
		quantitationList.setCellRenderer(new QuantitationTypeRenderer());
		listPanel.add(new JScrollPane(quantitationList),BorderLayout.CENTER);
		listPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

		
		JPanel otherOptionsPanel = new JPanel();
		otherOptionsPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx = 0.9;
		gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		otherOptionsPanel.add(new JLabel("Only quantitate visible stores"),gbc);
		
		gbc.weightx=0.1;
		gbc.gridx=2;
		
		onlyVisible = new JCheckBox();
		otherOptionsPanel.add(onlyVisible,gbc);
		
		listPanel.add(otherOptionsPanel,BorderLayout.SOUTH);
		
		
		mainPanel.add(listPanel,BorderLayout.WEST);
		
		getContentPane().add(mainPanel,BorderLayout.WEST);
		
		optionPanel = new JPanel();
		mainPanel.add(optionPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		
		buttonPanel.add(closeButton);

		runButton = new JButton("Quantitate");
		runButton.setActionCommand("run");
		runButton.addActionListener(this);
		runButton.setEnabled(false);
		
		buttonPanel.add(runButton);
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		optionPanel = new JPanel();
		optionPanel.add(new JLabel("Select an option"));
		getContentPane().add(optionPanel,BorderLayout.CENTER);
		
		setSize(750,500);
		setLocationRelativeTo(application);
		setVisible(true);
		
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.OptionsListener#optionsChanged()
	 */
	public void optionsChanged () {
		if (quantitations[quantitationList.getSelectedIndex()].isReady()) {
			runButton.setEnabled(true);
		}
		else {
			runButton.setEnabled(false);
		}
	}
	

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("run")) {
			
			//Get rid of the dialog once quantitation has started.
			setVisible(false);
			quantitations[quantitationList.getSelectedIndex()].addProgressListener(new ProgressDialog(application,"Quantitating...",quantitations[quantitationList.getSelectedIndex()]));
			quantitations[quantitationList.getSelectedIndex()].addProgressListener(application);

			// Some quantitations operate by modifying existing quantitations
			// In these cases we don't want to wipe the existing quantitations.
			
			if (! (quantitations[quantitationList.getSelectedIndex()].requiresExistingQuantitation()  || quantitations[quantitationList.getSelectedIndex()].canUseExistingQuantitation())) {
				// We need to tell all data stores to throw away any quantitaiton data
				// they currently have.
				DataStore [] allStores = application.dataCollection().getAllDataStores();
				for (int d=0;d<allStores.length;d++) {
					allStores[d].resetAllProbeValues();
				}
			}
			
			// We need to make up the list of stores to quantitate.  We need to exclude
			// replicate sets (since they just display other sets quantitations), but ensure
			// that their contained stores are being quantitated.
			
			DataStore [] storesToQuantitate;
			
			if (onlyVisible.isSelected()) {
				storesToQuantitate = application.drawnDataStores();
			}
			else {
				storesToQuantitate = application.dataCollection().getAllDataStores();				
			}
		
			HashSet<DataStore>filteredStores = new HashSet<DataStore>();
			
			for (int i=0;i<storesToQuantitate.length;i++) {
				if (storesToQuantitate[i] instanceof ReplicateSet) {
					DataStore [] containedStores = ((ReplicateSet)storesToQuantitate[i]).dataStores();
					for (int j=0;j<containedStores.length;j++) {
						filteredStores.add(containedStores[j]);
					}
				}
				else {
					filteredStores.add(storesToQuantitate[i]);
				}
			}
			
			// We can also go through the filtered stores and remove any data groups which
			// contain no datasets since these will just mess things up later on.

			storesToQuantitate = filteredStores.toArray(new DataStore[0]);

			filteredStores.clear();
			
			for (int i=0;i<storesToQuantitate.length;i++) {
				if (storesToQuantitate[i] instanceof DataGroup && ((DataGroup)storesToQuantitate[i]).dataSets().length == 0) {
					continue;
				}
				filteredStores.add(storesToQuantitate[i]);
			}
			
			storesToQuantitate = filteredStores.toArray(new DataStore[0]);

			filteredStores.clear();
			
						
			
			// If we're doing a quantitation which requires existing quantitation
			// then we want to remove from the set any stores which weren't originally
			// quantitated.
			
			if (quantitations[quantitationList.getSelectedIndex()].requiresExistingQuantitation()) {
				for (int i=0;i<storesToQuantitate.length;i++) {
					if (storesToQuantitate[i].isQuantitated()) {
						filteredStores.add(storesToQuantitate[i]);
					}
				}
				
				storesToQuantitate = filteredStores.toArray(new DataStore[0]);
			}
			
			// Check we've got something left
			if (storesToQuantitate.length == 0) {
				JOptionPane.showMessageDialog(this,"Can't proceeed. No stores would be quantitated.","Not going to quantitate",JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			quantitations[quantitationList.getSelectedIndex()].quantitate(storesToQuantitate);
		}
	}


	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
		// We need to add a new option panel
		getContentPane().remove(optionPanel);
		if (quantitationList.getSelectedIndex() >= 0) {
			quantitations[quantitationList.getSelectedIndex()].addOptionsListener(this);
		
			optionPanel = quantitations[quantitationList.getSelectedIndex()].getOptionsPanel();
			
			optionPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			runButton.setEnabled(quantitations[quantitationList.getSelectedIndex()].isReady());
	
			getContentPane().add(optionPanel,BorderLayout.CENTER);
			validate();
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressCancelled()
	 */
	public void progressCancelled() {
		runButton.setEnabled(quantitations[quantitationList.getSelectedIndex()].isReady());		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressComplete(java.lang.String, java.lang.Object)
	 */
	public void progressComplete(String command, Object result) {
		setVisible(false);
		dispose();		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressExceptionReceived(java.lang.Exception)
	 */
	public void progressExceptionReceived(Exception e) {
		runButton.setEnabled(quantitations[quantitationList.getSelectedIndex()].isReady());	
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressUpdated(java.lang.String, int, int)
	 */
	public void progressUpdated(String message, int current, int max) {}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressWarningReceived(java.lang.Exception)
	 */
	public void progressWarningReceived(Exception e) {}

}
