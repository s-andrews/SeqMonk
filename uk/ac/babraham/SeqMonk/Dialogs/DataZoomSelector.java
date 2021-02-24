/**
 * Copyright Copyright 2010- 21 Simon Andrews
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

package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientPanel;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientScaleAxis;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferencesListener;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * Provides a small dialog which allows the user to select a suitable
 * data zoom level.  Applies the selected level to currently visible
 * data tracks
 */
public class DataZoomSelector extends JDialog implements ActionListener, ChangeListener, DisplayPreferencesListener {

	private SeqMonkApplication application;
	private double currentMaxZoom;
	private JSlider slider;	
	
	private JPanel gradientPanel;
	private GradientPanel gradientColourPanel;
	private GradientScaleAxis gradientScaleAxis;
		
	/**
	 * Instantiates a new data zoom selector.
	 * 
	 * @param application
	 */
	public DataZoomSelector(SeqMonkApplication application) {
		super(application,"Set Data Zoom");
		this.application = application;
				
		currentMaxZoom = (int)DisplayPreferences.getInstance().getMaxDataValue();
		
		// Set some limits in case we end up with an impossible range to consider
		if (currentMaxZoom > Math.pow(2,20)) currentMaxZoom = Math.pow(2,20);
		if (currentMaxZoom < 1) currentMaxZoom = 1;
		
		getContentPane().setLayout(new BorderLayout());
				
		// The slider actually ends up as an exponential scale (to the power of 2).
		// We allow 200 increments on the slider but only go up to 2**20 hence dividing
		// by 10 to get the actual power to raise to.
		slider = new JSlider(0,200,(int)(10*(Math.log(currentMaxZoom)/Math.log(2))));
		slider.setOrientation(JSlider.VERTICAL);
		slider.addChangeListener(this);
		slider.setMajorTickSpacing(0);

		
		
		// This looks a bit pants, but we need it in to work around a bug in
		// the windows 7 LAF where the slider is tiny if labels are not drawn.
		slider.setPaintTicks(true);
		
		slider.setSnapToTicks(false);
		slider.setPaintTrack(true);
		Hashtable<Integer, Component> labelTable = new Hashtable<Integer, Component>();
		
		for (int i=0;i<=200;i+=20) {
			labelTable.put(new Integer(i), new JLabel(""));
		}
		slider.setLabelTable(labelTable);
		
		slider.setPaintLabels(true);
		getContentPane().add(slider,BorderLayout.WEST);

		gradientPanel = new JPanel();
		gradientPanel.setLayout(new BorderLayout());
		
		gradientColourPanel = new GradientPanel(DisplayPreferences.getInstance().getGradient(), GradientPanel.VERTICAL_GRADIENT);
		
		gradientPanel.add(gradientColourPanel,BorderLayout.CENTER);
		
		if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE) {
			gradientScaleAxis = new GradientScaleAxis(0,DisplayPreferences.getInstance().getMaxDataValue());			
		}
		else if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE) {
			gradientScaleAxis = new GradientScaleAxis(0-DisplayPreferences.getInstance().getMaxDataValue(),DisplayPreferences.getInstance().getMaxDataValue());			
		}
		
		gradientPanel.add(gradientScaleAxis,BorderLayout.EAST);
		
		getContentPane().add(gradientPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1,2));
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		getRootPane().setDefaultButton(closeButton);
		closeButton.addActionListener(this);
		
		buttonPanel.add(closeButton,BorderLayout.SOUTH);
		
		JButton saveButton = new JButton("Save");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		
		buttonPanel.add(saveButton,BorderLayout.SOUTH);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		DisplayPreferences.getInstance().addListener(this);
		
		setSize(150,300);
		setLocationRelativeTo(application);
		setVisible(true);
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			DisplayPreferences.getInstance().removeListener(this);
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save")) {
			ImageSaver.saveImage(gradientPanel);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {
		currentMaxZoom = Math.pow(2,slider.getValue()/10d);
		DisplayPreferences.getInstance().setMaxDataValue(currentMaxZoom);
		if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE) {
			gradientScaleAxis.setLimits(0,DisplayPreferences.getInstance().getMaxDataValue());			
		}
		else if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE) {
			gradientScaleAxis.setLimits(0-DisplayPreferences.getInstance().getMaxDataValue(),DisplayPreferences.getInstance().getMaxDataValue());			
		}
		
		validate();
		repaint();
		
		application.genomeViewer().repaint();
	}

	public void displayPreferencesUpdated(DisplayPreferences prefs) {
		gradientColourPanel.setGradient(DisplayPreferences.getInstance().getGradient());

		if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE) {
			gradientScaleAxis.setLimits(0,DisplayPreferences.getInstance().getMaxDataValue());			
		}
		else if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE) {
			gradientScaleAxis.setLimits(0-DisplayPreferences.getInstance().getMaxDataValue(),DisplayPreferences.getInstance().getMaxDataValue());			
		}
		
		validate();
		repaint();
	}
}
