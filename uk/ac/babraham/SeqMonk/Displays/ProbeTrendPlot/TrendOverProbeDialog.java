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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

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
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class TrendOverProbeDialog is a container for the probe trend
 * plot.  It can display either a progress message or the actual
 * plot.
 */
public class TrendOverProbeDialog extends JDialog implements ActionListener, ChangeListener, ProgressListener {

	/** The trend panel. */
	private TrendOverProbePanel trendPanel;
	
	/** The smoothing slider */
	private JSlider smoothingSlider;

	/**
	 * Instantiates a new trend over probe dialog over a single specified region
	 * 
	 * @param 
	 * @param stores the stores
	 * @param prefs the prefs
	 */
	
	public TrendOverProbeDialog (Chromosome c, int start, int end, DataStore [] stores, TrendOverProbePreferences prefs) {

		super(SeqMonkApplication.getInstance(),"Probe Trend Plot [Chr "+c.name()+" "+start+" - "+end+"]");

		Probe probe = new Probe(c, start, end);
		ProbeSet list = new ProbeSet("Visible Region", new Probe [] {probe});
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		setupFrame(new ProbeList [] {list}, stores, prefs);
		
	}
	
	/**
	 * Instantiates a new trend over probe dialog.
	 * 
	 * @param probes the probes
	 * @param stores the stores
	 * @param prefs the prefs
	 */
	public TrendOverProbeDialog (ProbeList [] probeLists, DataStore [] stores, TrendOverProbePreferences prefs) {
		super(SeqMonkApplication.getInstance(),"Probe Trend Plot");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		setupFrame(probeLists, stores, prefs);
	}
	
	private void setupFrame (ProbeList [] probeLists, DataStore [] stores, TrendOverProbePreferences prefs) {
		
		trendPanel = new TrendOverProbePanel(probeLists,stores,prefs);
		
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

		JButton saveDataButton = new JButton("Save Data");
		saveDataButton.setActionCommand("save_data");
		saveDataButton.addActionListener(this);
		buttonPanel.add(saveDataButton);

		
		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());

		trendPanel.addProgressListener(this);
		trendPanel.addProgressListener(new ProgressDialog("Calculating trend plot", trendPanel));
	
		trendPanel.startCalculating();
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
			
			try {
				trendPanel.saveData(file);
			} 
			catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error saving data: "+e.getMessage(), "Data Save Error", JOptionPane.ERROR_MESSAGE);
			}
			
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
			trendPanel.setSmoothing(smoothingSlider.getValue()-1);
		}
	}

	public void progressCancelled() {
		dispose();
	}

	public void progressComplete(String command, Object result) {
		setVisible(true);
	}

	public void progressExceptionReceived(Exception e) {
		dispose();
	}

	public void progressUpdated(String message, int current, int max) {}

	public void progressWarningReceived(Exception e) {}
}
