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
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ThreadSafeIntCounter;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class TrendOverProbeDialog is a container for the probe trend
 * plot.  It can display either a progress message or the actual
 * plot.
 */
public class QuantitationTrendPlotDialog extends JDialog implements ActionListener, ChangeListener, ProgressListener {

	/** The trend panel. */
	private JPanel trendPanel;
	private QuantitationTrendPlotPanel upstreamTrendPanel = null;
	private QuantitationTrendPlotPanel centralTrendPanel = null;
	private QuantitationTrendPlotPanel downstreamTrendPanel = null;
	
	/** A counter to see how many panels we're waiting to complete */
	private ThreadSafeIntCounter waitingCounter = new ThreadSafeIntCounter();
	
	
	/** The smoothing slider */
	private JSlider smoothingSlider;

	
	/**
	 * Instantiates a new trend over probe dialog.
	 * 
	 * @param probes the probes
	 * @param stores the stores
	 * @param prefs the prefs
	 */
	public QuantitationTrendPlotDialog (ProbeList probeList, DataStore [] stores, QuantitationTrendPlotPreferencesPanel prefs) {
		super(SeqMonkApplication.getInstance(),"Probe Trend Plot ["+probeList.name()+"]");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		setupFrame(probeList, stores, prefs);
	}
	
	private void setupFrame (ProbeList probeList, DataStore [] stores, QuantitationTrendPlotPreferencesPanel prefs) {
		
		
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
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.fill = GridBagConstraints.BOTH;
						
		// Upstream panel
		Probe [] upstreamProbes = prefs.getUpstreamProbes();
		if (upstreamProbes != null) {
			waitingCounter.increment();
			upstreamTrendPanel = new QuantitationTrendPlotPanel(upstreamProbes, stores, probeList.getAllProbes(),prefs.selectedFeatureTypes()[0]);
			trendPanel.add(upstreamTrendPanel,gbc);
			gbc.gridx++;
		}
		
		// Central panel
		Probe [] centralProbes = prefs.getCoreProbes();
		if (centralProbes != null) {
			waitingCounter.increment();
			centralTrendPanel = new QuantitationTrendPlotPanel(centralProbes, stores, probeList.getAllProbes(),prefs.selectedFeatureTypes()[0]);
			trendPanel.add(centralTrendPanel,gbc);
			gbc.gridx++;
		}
		
		// Downstream panel
		Probe [] downstreamProbes = prefs.getDownstreamProbes();
		if (downstreamProbes != null) {
			waitingCounter.increment();
			downstreamTrendPanel = new QuantitationTrendPlotPanel(downstreamProbes, stores, probeList.getAllProbes(),prefs.selectedFeatureTypes()[0]);
			trendPanel.add(downstreamTrendPanel,gbc);
			gbc.gridx++;
		}
		
		// Figure out which is the leftmost panel
		if (upstreamProbes != null) {
			upstreamTrendPanel.setLeftmost(true);
		}
		else if (centralProbes != null) {
			centralTrendPanel.setLeftmost(true);
		}
		else {
			downstreamTrendPanel.setLeftmost(true);
		}
		
		// Figure out which is the rightmost panel
		if (downstreamProbes != null) {
			downstreamTrendPanel.setRightmost(true);
		}
		else if (centralProbes != null) {
			centralTrendPanel.setRightmost(true);
		}
		else {
			upstreamTrendPanel.setRightmost(true);
		}
		
		
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

		// I may not put this back, but it's broken at the moment anyway.
		
//		JButton saveDataButton = new JButton("Save Data");
//		saveDataButton.setActionCommand("save_data");
//		saveDataButton.addActionListener(this);
//		buttonPanel.add(saveDataButton);

		
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
					boolean addHeader = true;
					
					if (upstreamTrendPanel != null) {
						upstreamTrendPanel.saveData(pr, addHeader);
						addHeader = false;
					}
					
					if (centralTrendPanel != null) {
						centralTrendPanel.saveData(pr, addHeader);
						addHeader = false;
					}
					
					if (downstreamTrendPanel != null) {
						downstreamTrendPanel.saveData(pr, addHeader);
						addHeader = false;
					}
					
					pr.close();
					
				}
				catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}
				
			
			}
		});
		
		
		buttonPanel.add(saveDataButton);

		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
//		setSize(800,600);
		setSize(700,350);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		boolean addedProgress = false;
		
		if (upstreamTrendPanel != null) {
			upstreamTrendPanel.addProgressListener(this);
			if (!addedProgress) {
				upstreamTrendPanel.addProgressListener(new ProgressDialog("Quantitation Trend Plot",upstreamTrendPanel));
				addedProgress = true;
			}
			upstreamTrendPanel.startCalculating();
		}
		
		if (centralTrendPanel != null) {
			centralTrendPanel.addProgressListener(this);
			if (!addedProgress) {
				centralTrendPanel.addProgressListener(new ProgressDialog("Quantitation Trend Plot",centralTrendPanel));
				addedProgress = true;
			}
			centralTrendPanel.startCalculating();
		}

		if (downstreamTrendPanel != null) {
			downstreamTrendPanel.addProgressListener(this);
			if (!addedProgress) {
				downstreamTrendPanel.addProgressListener(new ProgressDialog("Quantitation Trend Plot",downstreamTrendPanel));
				addedProgress = true;
			}

			downstreamTrendPanel.startCalculating();
		}
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

	public void progressCancelled() {
		dispose();
	}

	public void progressComplete(String command, Object result) {
		waitingCounter.decrement();
		if (waitingCounter.value() == 0) {
			
			boolean setValue = false;
			double min = 0;
			double max = 0;
			
			if (upstreamTrendPanel != null) {
				if (!setValue) {
					min = upstreamTrendPanel.localMin();
					max = upstreamTrendPanel.localMax();
					setValue = true;
				}
				if (upstreamTrendPanel.localMin() < min) min = upstreamTrendPanel.localMin();
				if (upstreamTrendPanel.localMax() > max) max = upstreamTrendPanel.localMax();
			}

			if (centralTrendPanel != null) {
				if (!setValue) {
					min = centralTrendPanel.localMin();
					max = centralTrendPanel.localMax();
					setValue = true;
				}
				if (centralTrendPanel.localMin() < min) min = centralTrendPanel.localMin();
				if (centralTrendPanel.localMax() > max) max = centralTrendPanel.localMax();
			}

			if (downstreamTrendPanel != null) {
				if (!setValue) {
					min = downstreamTrendPanel.localMin();
					max = downstreamTrendPanel.localMax();
					setValue = true;
				}
				if (downstreamTrendPanel.localMin() < min) min = downstreamTrendPanel.localMin();
				if (downstreamTrendPanel.localMax() > max) max = downstreamTrendPanel.localMax();
			}

			// Set these values on the appropriate plots
			if (upstreamTrendPanel != null) upstreamTrendPanel.setMinMax(min, max);
			if (centralTrendPanel != null) centralTrendPanel.setMinMax(min, max);
			if (downstreamTrendPanel != null) downstreamTrendPanel.setMinMax(min, max);

			// TODO: Remove this temporary kludge to get a single scale for a set of graphs I'm making *******
//			if (upstreamTrendPanel != null) upstreamTrendPanel.setMinMax(2.5, 7);
//			if (centralTrendPanel != null) centralTrendPanel.setMinMax(2.5, 7);
//			if (downstreamTrendPanel != null) downstreamTrendPanel.setMinMax(2.5, 7);

			
			setVisible(true);
		}
	}

	public void progressExceptionReceived(Exception e) {
		dispose();
	}

	public void progressUpdated(String message, int current, int max) {}

	public void progressWarningReceived(Exception e) {}
}
