/**
 * Copyright 2012-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.HierarchicalClusterPlot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterDataSource;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.HierarchicalClusterSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientScaleBar;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.GradientFactory;
import uk.ac.babraham.SeqMonk.Gradients.InvertedGradient;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class HierarchicalClusterDialog extends JDialog implements ProgressListener, ChangeListener, ActionListener {

	private ProbeList probes;
	private DataStore [] stores;
	private HierarchicalClusterPanel clusterPanel;
	private JPanel clusterPanelGroup;
	private GradientScaleBar scaleBar;
	private JSlider dataZoomSlider;
	private JSlider clusterSlider;
	private boolean normalise;
	private JComboBox gradients;
	private JCheckBox invertGradient;
	private ClusterDataSource clusterDataSource;
	private boolean negativeScale;

	public HierarchicalClusterDialog (ProbeList probes, DataStore [] stores, boolean normalise) {
		super(SeqMonkApplication.getInstance(),"Hierarchical Clusters for "+probes.name());
		this.probes = probes;
		
		// Work out whether we're using a pos-neg or just pos scale
		if (normalise) {
			negativeScale = true;
		}
		else if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE) {
			negativeScale = true;
		}
		
		
		// Only use stores which are actually quantitated
		Vector<DataStore> keptStores = new Vector<DataStore>();
		
		for (int d=0;d<stores.length;d++) {
			if (stores[d].isQuantitated()) {
				keptStores.add(stores[d]);
			}
		}

		this.stores = keptStores.toArray(new DataStore[0]);
		this.normalise = normalise;
		
		if (normalise) {
			clusterDataSource= new HierarchicalClusterCorrelationDataSource(probes.getAllProbes(), stores, normalise);
		}
		else {
			clusterDataSource= new HierarchicalClusterDistanceDataSource(probes.getAllProbes(), stores, normalise);
		}

		getContentPane().setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);

		buttonPanel.add(closeButton);

		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);
		
		
		JButton saveClustersButton = new JButton("Save Clusters");
		saveClustersButton.setActionCommand("save_clusters");
		saveClustersButton.addActionListener(this);
		buttonPanel.add(saveClustersButton);

		getContentPane().add(buttonPanel,BorderLayout.SOUTH);

		
		// Add data zoom slider 
		
		// The slider actually ends up as an exponential scale (to the power of 2).
		// We allow 200 increments on the slider but only go up to 2**20 hence dividing
		// by 10 to get the actual power to raise to.
		dataZoomSlider = new JSlider(0,200,20);
		dataZoomSlider.setOrientation(JSlider.VERTICAL);
		dataZoomSlider.addChangeListener(this);
		dataZoomSlider.setMajorTickSpacing(10);

		// This looks a bit pants, but we need it in to work around a bug in
		// the windows 7 LAF where the slider is tiny if labels are not drawn.
		dataZoomSlider.setPaintTicks(true);

		dataZoomSlider.setSnapToTicks(false);
		dataZoomSlider.setPaintTrack(true);
		Hashtable<Integer, Component> labelTable = new Hashtable<Integer, Component>();

		for (int i=0;i<=200;i+=20) {
			labelTable.put(new Integer(i), new JLabel(""+(i/10)));
		}
		dataZoomSlider.setLabelTable(labelTable);

		dataZoomSlider.setPaintLabels(true);
		getContentPane().add(dataZoomSlider,BorderLayout.EAST);

		
		// Add cluster level slider 
		
		clusterSlider = new JSlider(0,200,170);
		clusterSlider.setOrientation(JSlider.VERTICAL);
		clusterSlider.addChangeListener(this);
		clusterSlider.setMajorTickSpacing(20);

		// This looks a bit pants, but we need it in to work around a bug in
		// the windows 7 LAF where the slider is tiny if labels are not drawn.
		clusterSlider.setPaintTicks(true);

		clusterSlider.setSnapToTicks(false);
		clusterSlider.setPaintTrack(true);
		Hashtable<Integer, Component> clusterLabelTable = new Hashtable<Integer, Component>();

		for (int i=0;i<=200;i+=20) {
			clusterLabelTable.put(new Integer(i), new JLabel(""+((i-100)/100f)));
		}
		clusterSlider.setLabelTable(clusterLabelTable);

		clusterSlider.setPaintLabels(true);
		getContentPane().add(clusterSlider,BorderLayout.WEST);

		
		// Add the colour options to the top
		JPanel colourPanel = new JPanel();
		
		gradients = new JComboBox(GradientFactory.getGradients());
		gradients.setSelectedIndex(2);
		gradients.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				updateGradients();
			}
		});
		
		colourPanel.add(new JLabel("Colour Gradient"));
		colourPanel.add(gradients);
		
		colourPanel.add(new JLabel(" Invert"));
		invertGradient = new JCheckBox();
		invertGradient.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				updateGradients();
			}
		});
		colourPanel.add(invertGradient);

		
		getContentPane().add(colourPanel,BorderLayout.NORTH);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setSize(800,600);

		HierarchicalClusterSet heirararchy = new HierarchicalClusterSet(clusterDataSource);
		heirararchy.addListener(new ProgressDialog("Clustering...",heirararchy));
		heirararchy.addListener(this);
		heirararchy.startClustering();

	}
	
	private void updateGradients () {

		ColourGradient gradient = (ColourGradient)gradients.getSelectedItem();
		
		if (invertGradient.isSelected()) {
			gradient = new InvertedGradient(gradient);
		}
		
		if (clusterPanel != null) {
			clusterPanel.setGradient(gradient);
			scaleBar.setGradient(gradient);
		}
		
	}

	public void progressExceptionReceived(Exception e) {
		dispose();
	}

	public void progressWarningReceived(Exception e) {}

	public void progressUpdated(String message, int current, int max) {}

	public void progressCancelled() {}

	public void progressComplete(String command, Object result) {

		clusterPanelGroup = new JPanel();
		
		clusterPanelGroup.setLayout(new BorderLayout());
		
		ColourGradient gradient = (ColourGradient)gradients.getSelectedItem();
		
		if (invertGradient.isSelected()) {
			gradient = new InvertedGradient(gradient);
		}
		if (negativeScale) {
			scaleBar = new GradientScaleBar(gradient, -2, 2);
		}
		else {
			scaleBar = new GradientScaleBar(gradient, 0, 2);
		}
		JPanel topBottomSplit = new JPanel();
		topBottomSplit.setLayout(new GridLayout(2, 1));
		topBottomSplit.add(new JPanel());
		topBottomSplit.add(scaleBar);
		
		clusterPanel = new HierarchicalClusterPanel(probes,stores,(ClusterPair)result, normalise,(ColourGradient)gradients.getSelectedItem());
		
		clusterPanelGroup.add(clusterPanel,BorderLayout.CENTER);
		clusterPanelGroup.add(topBottomSplit,BorderLayout.EAST);
		
		getContentPane().add(clusterPanelGroup,BorderLayout.CENTER);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		stateChanged(new ChangeEvent(clusterSlider));

		setVisible(true);


	}

	public void stateChanged(ChangeEvent ce) {
		if (clusterPanel != null) {
			if (ce.getSource() == dataZoomSlider) {
				double value = Math.pow(2,dataZoomSlider.getValue()/10d);
				clusterPanel.setMaxValue(value);
				if (negativeScale) {
					scaleBar.setLimits(0-value, value);
				}
				else {
					scaleBar.setLimits(0, value);
				}
			}
			else if (ce.getSource() == clusterSlider) {
				int sliderValue = clusterSlider.getValue();
				
				float range = clusterDataSource.maxClusterValue()-clusterDataSource.minClusterValue();
				
				float value = clusterDataSource.minClusterValue()+((range/200)*sliderValue);
				
//				System.err.println("Min="+clusterDataSource.minClusterValue()+" range="+range+" slider="+sliderValue+" value="+value);
				
				clusterPanel.setClusterRValue(value);
			}
		}


	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_image")) {
			ImageSaver.saveImage(clusterPanelGroup);
		}
		else if (ae.getActionCommand().equals("save_clusters")) {
			
			// Get a limit for how many probes per cluster
			String howManyProbes = JOptionPane.showInputDialog(this,"Minimum number of probes per cluster","10");
			
			if (howManyProbes == null) return;
			if (howManyProbes.length() == 0) {
				howManyProbes = "0";
			}
			
			int minProbes;
			try {
				minProbes = Integer.parseInt(howManyProbes);
			}
			catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(this, howManyProbes+" was not an integer","Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			ProbeList newList = clusterPanel.saveClusters(minProbes);
			
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
	}


}
