/**
 * Copyright 2009-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.HiCHeatmap;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.HierarchicalClusterSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.InteractionClusterMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Displays.Report.InteractionReport.InteractionReportOptions;
import uk.ac.babraham.SeqMonk.Reports.Interaction.AnnotatedInteractionReport;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The HeatmapProbeListDialog collects the preferences for the calculation
 * of a heatmap matrix and then displays the results once they're calculated.
 */
public class HeatmapProbeListWindow extends JFrame implements ActionListener, ProgressListener {

	/** The HiC interaction matrix */
	private HeatmapMatrix matrix;

	/** The heatmap panel. */
	private HeatmapProbeListPanelCollection heatmapPanel;

	/** The filter options panel */
	private HeatmapFilterOptions filterOptions;

	private HiCDataStore data;
	private ProbeList [] probeLists;
	private Genome genome;

	// Some static variables so we keep default values between plots
	private static int minDist = 0;
	private static int maxDist = 0;
	private static double minStrength = 1;
	private static double maxSignificance = 0.05;
	private static int minAbsolute = 3;
	private static boolean correctLinkage = true;
	
	private JButton clusterButton;
	private JButton saveClustersButton;

	/**
	 * Instantiates a new trend over probe dialog over a single specified region
	 * 
	 * @param 
	 * @param stores the stores
	 * @param prefs the prefs
	 */

	public HeatmapProbeListWindow (HiCDataStore data, ProbeList [] probeLists, Genome genome) {

		super("HiC Heatmap");
		setIconImage(SeqMonkApplication.getInstance().getIconImage());

		this.data = data;
		this.genome = genome;
		this.probeLists = probeLists;

		setTitle("HiC Heatmap ["+data.name()+"]");

		if (!data.isValidHiC()) {
			throw new IllegalArgumentException("All data stores passed to the heatmap dialog must be HiC");
		}

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		HeatmapOptionsDialog options = new HeatmapOptionsDialog();

		if (! options.exitedNormally) return;

		matrix = new HeatmapMatrix(data, probeLists, genome, minDist, maxDist, minStrength, maxSignificance,minAbsolute,correctLinkage);
		matrix.addProgressListener(this);
		matrix.addProgressListener(new ProgressDialog("Calculating interactions", matrix));
		matrix.startCalculating();
	}


	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(heatmapPanel);
		}
		else if (ae.getActionCommand().equals("cluster")){
			if (clusterButton.getText().equals("Cluster Interactions")) {
				
				// First we need to make up an interaction cluster matrix which we
				// can then feed to the generic hierarchical clustering program
				InteractionClusterMatrix clusterMatrix = new InteractionClusterMatrix(matrix.filteredInteractions(),matrix.probeCount());
				clusterMatrix.addListener(new ProgressDialog(this,"HiC Interaction Clustering", clusterMatrix));
				clusterMatrix.addListener(this); 
				clusterMatrix.startCorrelating();
				
				clusterButton.setEnabled(false);
			}
			else {
				matrix.setCluster(null);
				saveClustersButton.setEnabled(false);
				clusterButton.setText("Cluster Interactions");
			}
		}
		
		else if (ae.getActionCommand().equals("save_probes")) {
			ProbeList newList = matrix.createProbeListFromCurrentInteractions();
			
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
		
		else if (ae.getActionCommand().equals("save_clusters")){
			if (matrix.cluster() == null) return;
			
			// Get a limit for how many probes per cluster
			String howManyProbes = JOptionPane.showInputDialog(this,"Minimum number of probes per cluster","10");
			
			if (howManyProbes == null) return;
			
			int minProbes;
			try {
				minProbes = Integer.parseInt(howManyProbes);
			}
			catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(this, howManyProbes+" was not an integer","Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			ProbeList newList = matrix.createProbeListsFromClusters(minProbes,heatmapPanel.probeListPanel().currentYStartIndex(),heatmapPanel.probeListPanel().currentYEndIndex());
			
			if (newList == null) return; // This was called before clustering had completed.
			
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

		else if (ae.getActionCommand().equals("make_report")){			
			new InteractionReportOptions(SeqMonkApplication.getInstance(), new AnnotatedInteractionReport(SeqMonkApplication.getInstance().dataCollection(), matrix));
		}


	}

	public void progressCancelled() {
		
		// If the heatmap panel hasn't been made then we need to get rid of
		// the whole window.  If it has then it's a clustering which was cancelled.
		if (heatmapPanel == null || clusterButton == null) {
			dispose();
		}
		clusterButton.setEnabled(true);
	}

	public void progressComplete(String command, Object result) {

		if (command.equals("heatmap")) {
			
			if (matrix.interactions().length == 0) {
				JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "No interactions were found", "Interaction Result", JOptionPane.INFORMATION_MESSAGE);
				dispose();
				return;
			}
			
			heatmapPanel = new HeatmapProbeListPanelCollection(data, probeLists, matrix, genome);

			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(heatmapPanel,BorderLayout.CENTER);

			filterOptions = new HeatmapFilterOptions(matrix);
			getContentPane().add(filterOptions,BorderLayout.WEST);

			JPanel buttonPanel = new JPanel();
			JButton closeButton = new JButton("Close");
			closeButton.setActionCommand("close");
			closeButton.addActionListener(this);
			buttonPanel.add(closeButton);

			JButton saveImageButton = new JButton("Save Image");
			saveImageButton.setActionCommand("save_image");
			saveImageButton.addActionListener(this);
			buttonPanel.add(saveImageButton);
			
			JButton saveProbesButton = new JButton("Save Probe List");
			saveProbesButton.setActionCommand("save_probes");
			saveProbesButton.addActionListener(this);
			buttonPanel.add(saveProbesButton);

			JButton makeReportButton = new JButton("Make Report");
			makeReportButton.setActionCommand("make_report");
			makeReportButton.addActionListener(this);
			buttonPanel.add(makeReportButton);

			clusterButton = new JButton("Cluster Interactions");
			clusterButton.setActionCommand("cluster");
			clusterButton.addActionListener(this);
			buttonPanel.add(clusterButton);
			
			saveClustersButton = new JButton("Save current clusters");
			saveClustersButton.setActionCommand("save_clusters");
			saveClustersButton.addActionListener(this);
			saveClustersButton.setEnabled(false);
			buttonPanel.add(saveClustersButton);
			
			

			getContentPane().add(buttonPanel,BorderLayout.SOUTH);
			setSize(800,500);
			setLocationRelativeTo(SeqMonkApplication.getInstance());

			setVisible(true);
		}
		else if (command.equals("interaction_cluster_matrix")) {
			// We can start the actual clustering
			
			HierarchicalClusterSet clusterSet = new HierarchicalClusterSet((InteractionClusterMatrix)result);
			clusterSet.addListener(new ProgressDialog(this,"HiC Interaction Clustering", clusterSet));
			clusterSet.addListener(this);
			clusterSet.startClustering();
		}
		else if (command.equals("interaction_cluster")) {
			matrix.setCluster((ClusterPair)result);
			clusterButton.setText("Remove clustering");
			clusterButton.setEnabled(true);
			saveClustersButton.setEnabled(true);
		}
	}

	public void progressExceptionReceived(Exception e) {
		// If the heatmap panel hasn't been made then we need to get rid of
		// the whole window.  If it has then it's a clustering which failed.
		if (heatmapPanel == null) {
			dispose();
		}
		clusterButton.setEnabled(true);
	}

	public void progressUpdated(String message, int current, int max) {}

	public void progressWarningReceived(Exception e) {}


	private class HeatmapOptionsDialog extends JDialog {

		// Will only be true if they pressed the plot button
		private boolean exitedNormally = false;

		private HeatmapOptionsPanel optionsPanel;

		public HeatmapOptionsDialog () {

			super(SeqMonkApplication.getInstance());
			setTitle("Heatmap Options");
			setSize(400,300);
			setLocationRelativeTo(SeqMonkApplication.getInstance());
			setModal(true);

			getContentPane().setLayout(new BorderLayout());

			optionsPanel = new HeatmapOptionsPanel(minDist, maxDist, (float)minStrength,(float) maxSignificance, minAbsolute, correctLinkage);

			getContentPane().add(optionsPanel,BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel();
			JButton closeButton = new JButton("Close");
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					setVisible(false);
					dispose();
				}
			});

			buttonPanel.add(closeButton);

			JButton plotButton = new JButton("Plot");
			plotButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {

					minDist = optionsPanel.minDistance();
					maxDist = optionsPanel.maxDistance();
					minStrength = optionsPanel.minStrength();
					maxSignificance = optionsPanel.maxSignificance();
					minAbsolute = optionsPanel.minAbsolute();
					correctLinkage = optionsPanel.correctLinkage();
					
					exitedNormally = true;
					setVisible(false);
					dispose();
				}
			});

			buttonPanel.add(plotButton);

			getContentPane().add(buttonPanel,BorderLayout.SOUTH);

			setVisible(true);
		}

	}


}
