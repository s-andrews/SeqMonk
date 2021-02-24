/**
 * Copyright 2012- 21 Simon Andrews
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientScaleBar;
import uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.HeatmapPanel.QuantitationHeatmapPanelGroup;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.GradientFactory;
import uk.ac.babraham.SeqMonk.Gradients.InvertedGradient;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class QuantitationTrendHeatmapDialog extends JDialog implements ChangeListener, ActionListener {

	private GradientScaleBar scaleBar;
	private JSlider dataZoomSlider;
	private JComboBox gradients;
	private JCheckBox invertGradient;
	private JButton highlightRepSetsButton;
	
	private QuantitationTrendData data;
	
	private JPanel exportPanel = new JPanel();
	private QuantitationHeatmapPanelGroup quantPanel;
	

	public QuantitationTrendHeatmapDialog (QuantitationTrendData data) {
		super(SeqMonkApplication.getInstance(),"Quantitation Trend Heatmap");

		this.data = data;

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


		JButton saveDataButton = new JButton("Save Data");
		saveDataButton.setActionCommand("save_data");
		saveDataButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {

				JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileFilter(new FileFilter() {
					
					public String getDescription() {
						return "Text Files";
					}
					
					public boolean accept(File f) {
						if (f.isDirectory() || f.getName().toLowerCase().endsWith(".txt")) {
							return true;
						}
						return false;
					}
				});
				
				int result = chooser.showSaveDialog(QuantitationTrendHeatmapDialog.this);
				if (result == JFileChooser.CANCEL_OPTION) return;

				File file = chooser.getSelectedFile();
				SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
				
				if (file.isDirectory()) return;

				if (! file.getPath().toLowerCase().endsWith(".txt")) {
					file = new File(file.getPath()+".txt");
				}
				
				// Check if we're stepping on anyone's toes...
				if (file.exists()) {
					int answer = JOptionPane.showOptionDialog(QuantitationTrendHeatmapDialog.this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

					if (answer > 0) {
						return;
					}
				}

				// Now write out the results
				
				try {
					PrintWriter pr = new PrintWriter(file);
					
					QuantitationTrendHeatmapDialog.this.data.writeData(pr);
					
					pr.close();
					
				}
				catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}
				
			
			}
		});

		buttonPanel.add(saveDataButton);

		getContentPane().add(buttonPanel,BorderLayout.SOUTH);


		// Add data zoom slider 

		// The slider actually ends up as an exponential scale (to the power of 2).
		// We allow 200 increments on the slider but only go up to 2**20 hence dividing
		// by 10 to get the actual power to raise to.
		dataZoomSlider = new JSlider(0,200,100);
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

		// TODO: Set the initial slider position to the correct place.


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

		highlightRepSetsButton = new JButton("Highlight Sets");
		highlightRepSetsButton.setActionCommand("highlight");
		highlightRepSetsButton.addActionListener(this);
		colourPanel.add(highlightRepSetsButton);


		getContentPane().add(colourPanel,BorderLayout.NORTH);
		
		double min = data.minValue;
		double max = data.maxValue;
		
		if (min < 0) {
			max = Math.max(max, 0-min);
			min = 0 - max;
		}
		
		scaleBar = new GradientScaleBar((ColourGradient)gradients.getSelectedItem(), data.minValue, data.maxValue);
		exportPanel.setLayout(new BorderLayout());
		quantPanel = new QuantitationHeatmapPanelGroup(data,(ColourGradient)gradients.getSelectedItem(),min,max);
		exportPanel.add(quantPanel, BorderLayout.CENTER);
		exportPanel.add(scaleBar, BorderLayout.EAST);
		
		add(exportPanel,BorderLayout.CENTER);

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setSize(800,600);
		setVisible(true);

	}

	private void updateGradients () {

		ColourGradient gradient = (ColourGradient)gradients.getSelectedItem();

		if (invertGradient.isSelected()) {
			gradient = new InvertedGradient(gradient);
		}

		quantPanel.setGradient(gradient);
		scaleBar.setGradient(gradient);

	}

	public void stateChanged(ChangeEvent ce) {
		if (ce.getSource() == dataZoomSlider) {
			
			double value = dataZoomSlider.getValue()/100d;
			
			double range = data.maxValue-data.minValue;
						
			range *= value;
						
			scaleBar.setLimits(data.minValue, data.minValue+range);
			quantPanel.setLimits(data.minValue, data.minValue+range);
		}

	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_image")) {
			ImageSaver.saveImage(exportPanel);
		}
		else if (ae.getActionCommand().equals("highlight")) {
//			ReplicateSet [] repSets = ReplicateSetSelector.selectReplicateSets();
			// TODO: Fix this
//			clusterPanel.setRepSets(repSets);
		}

	}


}
