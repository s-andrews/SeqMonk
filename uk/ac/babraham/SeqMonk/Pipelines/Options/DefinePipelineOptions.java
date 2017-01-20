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
package uk.ac.babraham.SeqMonk.Pipelines.Options;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

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
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Filters.OptionsListener;
import uk.ac.babraham.SeqMonk.Pipelines.AntisenseTranscriptionPipeline;
import uk.ac.babraham.SeqMonk.Pipelines.BisulphiteFeaturePipeline;
import uk.ac.babraham.SeqMonk.Pipelines.CodonBiasPipeline;
import uk.ac.babraham.SeqMonk.Pipelines.GeneTrapQuantitationPipeline;
import uk.ac.babraham.SeqMonk.Pipelines.IntronRegressionPipeline;
import uk.ac.babraham.SeqMonk.Pipelines.Pipeline;
import uk.ac.babraham.SeqMonk.Pipelines.SplicingEfficiencyPipeline;
import uk.ac.babraham.SeqMonk.Pipelines.TranscriptionTerminationPipeline;
import uk.ac.babraham.SeqMonk.Pipelines.WigglePipeline;
import uk.ac.babraham.SeqMonk.Pipelines.Transcription.ActiveTranscriptionPipeline;
import uk.ac.babraham.SeqMonk.Pipelines.Transcription.RNASeqPipeline;

/**
 * A dialog which is used to select a pipeline and 
 * display its options panel to allow options to be set.  Once
 * complete the pipeline can be run.
 */
public class DefinePipelineOptions extends JDialog implements ActionListener, ProgressListener, OptionsListener, ListSelectionListener {

	private JPanel mainPanel;
	private Pipeline [] pipelines;
	private JList pipelineList;
	private JPanel optionPanel;
	private JButton runButton;
	private SeqMonkApplication application;
	private JCheckBox onlyVisible;
	
	/**
	 * Make a new quantitation options dialog
	 * 
	 * @param application The enclosing SeqMonkApplication
	 */
	public DefinePipelineOptions (SeqMonkApplication application) {
		super(application,"Define Quantitation...");
		this.application = application;
		getContentPane().setLayout(new BorderLayout());

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.add(new JLabel("Quantitation Options",JLabel.LEFT),BorderLayout.NORTH);

		pipelines = new Pipeline [] {
				new RNASeqPipeline(application.dataCollection()),
				new ActiveTranscriptionPipeline(application.dataCollection()),
				new IntronRegressionPipeline(application.dataCollection()),
				new GeneTrapQuantitationPipeline(application.dataCollection()),
				new WigglePipeline(application),
				new BisulphiteFeaturePipeline(application.dataCollection()),
				new SplicingEfficiencyPipeline(application.dataCollection()),
				new AntisenseTranscriptionPipeline(application.dataCollection()),
				new CodonBiasPipeline(application.dataCollection()),
				new TranscriptionTerminationPipeline(application.dataCollection()),
		};


		pipelineList = new JList(pipelines);
		pipelineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		pipelineList.getSelectionModel().addListSelectionListener(this);
		listPanel.add(new JScrollPane(pipelineList),BorderLayout.CENTER);
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

		runButton = new JButton("Run Pipeline");
		runButton.setActionCommand("run");
		runButton.addActionListener(this);
		runButton.setEnabled(false);
		
		buttonPanel.add(runButton);
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		optionPanel = new JPanel();
		optionPanel.add(new JLabel("Select an option"));
		getContentPane().add(optionPanel,BorderLayout.CENTER);
		
		setSize(800,500);
		setLocationRelativeTo(application);
		setVisible(true);
		
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.OptionsListener#optionsChanged()
	 */
	public void optionsChanged () {
		if (pipelines[pipelineList.getSelectedIndex()].isReady()) {
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
			
			// Let's make sure they really want to do this
			if (application.dataCollection().probeSet() != null && pipelines[pipelineList.getSelectedIndex()].createsNewProbes()) {
				int answer = JOptionPane.showConfirmDialog(this, "This will wipe out your existing probes and quantitations.  Are you sure you want to continue?","Are you sure?",JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION) {
					return;
				}
			}

			
			//Get rid of the dialog once quantitation has started.
			setVisible(false);
			pipelines[pipelineList.getSelectedIndex()].addProgressListener(new ProgressDialog(application,"Quantitating...",pipelines[pipelineList.getSelectedIndex()]));
			pipelines[pipelineList.getSelectedIndex()].addProgressListener(application);

			
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
			
			storesToQuantitate = filteredStores.toArray(new DataStore[0]);
			
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
			
			pipelines[pipelineList.getSelectedIndex()].runPipeline(storesToQuantitate);
		}
	}


	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) {
		// We need to add a new option panel
		getContentPane().remove(optionPanel);
		if (pipelineList.getSelectedIndex() >= 0) {
			pipelines[pipelineList.getSelectedIndex()].addOptionsListener(this);
		
			optionPanel = pipelines[pipelineList.getSelectedIndex()].getOptionsPanel(application);
			
			optionPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			runButton.setEnabled(pipelines[pipelineList.getSelectedIndex()].isReady());
	
			getContentPane().add(optionPanel,BorderLayout.CENTER);
			validate();
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressCancelled()
	 */
	public void progressCancelled() {
		runButton.setEnabled(pipelines[pipelineList.getSelectedIndex()].isReady());		
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
		runButton.setEnabled(pipelines[pipelineList.getSelectedIndex()].isReady());	
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
