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
package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class TrendOverProbeDialog is a container for the probe trend
 * plot.  It can display either a progress message or the actual
 * plot.
 */
public class QuantitationTrendPlotDialog extends JDialog implements ActionListener, ChangeListener {

	/** The trend panel. */
	private JPanel trendPanel;
	private QuantitationTrendPlotPanel upstreamTrendPanel = null;
	private QuantitationTrendPlotPanel centralTrendPanel = null;
	private QuantitationTrendPlotPanel downstreamTrendPanel = null;	
	
	/** The smoothing slider */
	private JSlider smoothingSlider;

	
	private QuantitationTrendData data;
	
	/**
	 * Instantiates a new trend over probe dialog.
	 * 
	 * @param probes the probes
	 * @param stores the stores
	 * @param prefs the prefs
	 */
	public QuantitationTrendPlotDialog (QuantitationTrendData data) {
		super(SeqMonkApplication.getInstance(),"Probe Trend Plot");
		this.data = data;
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
		
		// The trend panel is now going to be made of a number of different components,
		// all arranged horizontally.
		
		/*
		 * 1) A y-axis
		 * 
		 * 2) An upstream fixed width region (if there is one)
		 * 
		 * 3) A central fixed or variable region (if there is one)
		 * 
		 * 4) A downstream fixed width region (if there is one)
		 */
		
		trendPanel = new JPanel();
		
		trendPanel.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx=0.001;
		gbc.weighty=0.5;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.fill = GridBagConstraints.VERTICAL;
		
		trendPanel.add(new AxisScalePanel(data.getMinValue(), data.getMaxValue()),gbc);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx++;
		gbc.weightx=0.5;
		
		// Upstream panel
		if (data.hasUpstream) {
			upstreamTrendPanel = new QuantitationTrendPlotPanel(data,"upstream");
			trendPanel.add(upstreamTrendPanel,gbc);
			gbc.gridx++;
		}
		
		// Central panel
		centralTrendPanel = new QuantitationTrendPlotPanel(data,"central");
		trendPanel.add(centralTrendPanel,gbc);
		gbc.gridx++;
		
		// Downstream panel
		if (data.hasDownstream) {
			downstreamTrendPanel = new QuantitationTrendPlotPanel(data,"downstream");
			trendPanel.add(downstreamTrendPanel,gbc);
			gbc.gridx++;
		}
		
		gbc.weightx=0.00001;
		gbc.fill = GridBagConstraints.VERTICAL;

		trendPanel.add(new NamePanel(data.stores(), data.lists()),gbc);
				
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(trendPanel,BorderLayout.CENTER);
		
		smoothingSlider = new JSlider(JSlider.VERTICAL,1,1000,1);

		// This call is left in to work around a bug in the Windows 7 LAF
		// which makes the slider stupidly thin in ticks are not drawn.
		smoothingSlider.setPaintTicks(true);
		smoothingSlider.addChangeListener(this);
		getContentPane().add(smoothingSlider,BorderLayout.EAST);

		
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
				
				int result = chooser.showSaveDialog(QuantitationTrendPlotDialog.this);
				if (result == JFileChooser.CANCEL_OPTION) return;

				File file = chooser.getSelectedFile();
				SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
				
				if (file.isDirectory()) return;

				if (! file.getPath().toLowerCase().endsWith(".txt")) {
					file = new File(file.getPath()+".txt");
				}
				
				// Check if we're stepping on anyone's toes...
				if (file.exists()) {
					int answer = JOptionPane.showOptionDialog(QuantitationTrendPlotDialog.this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

					if (answer > 0) {
						return;
					}
				}

				// Now write out the results
				
				try {
					PrintWriter pr = new PrintWriter(file);
					
					QuantitationTrendPlotDialog.this .data.writeData(pr);
					
					pr.close();
					
				}
				catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}
				
			
			}
		});
		
		
		buttonPanel.add(saveDataButton);

		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setSize(700,350);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}
	
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_data")){

			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new FileFilter() {
			
				@Override
				public String getDescription() {
					return "Text files";
				}
			
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) return true;
					if (f.getName().toLowerCase().endsWith(".txt")) return true;
					return false;
				}
			});
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
			if (file.isDirectory()) return;

			if (! file.getPath().toLowerCase().endsWith(".txt")) {
				file = new File(file.getPath()+".txt");
			}
			
			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}
			
//			try {
				
				// TODO: Work out how to save data...
				
//				trendPanel.saveData(file);
//			} 
//			catch (IOException e) {
//				e.printStackTrace();
//				JOptionPane.showMessageDialog(this, "Error saving data: "+e.getMessage(), "Data Save Error", JOptionPane.ERROR_MESSAGE);
//			}
			
		}
		else if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(trendPanel);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {

		// The dot size slider has been moved

		if (trendPanel != null) {
			if (upstreamTrendPanel != null) {
				upstreamTrendPanel.setSmoothing(smoothingSlider.getValue()-1);
			}
			if (centralTrendPanel != null) {
				centralTrendPanel.setSmoothing(smoothingSlider.getValue()-1);
			}
			if (downstreamTrendPanel != null) {
				downstreamTrendPanel.setSmoothing(smoothingSlider.getValue()-1);
			}
		}
	}



}
