/**
 * Copyright 2011- 21 Simon Andrews
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.GradientFactory;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class HeatmapFilterOptions extends JPanel implements KeyListener, ChangeListener, ActionListener {

	private JTextField minStrengthField;
	private JSlider minStrengthSlider;

	private JTextField minDistField;
	private JSlider minDistSlider;

	private JTextField maxDistField;
	private JSlider maxDistSlider;

	private JTextField maxSignificanceField;
	private JSlider maxSignificanceSlider;

	private JTextField rValueField;
	private JSlider rValueSlider;

	private JTextField minAbsoluteField;
	private JSlider minAbsoluteSlider;

	private JComboBox colourByOptions;
	
	private JComboBox colourGradientOptions;

	private JComboBox probeLists;

	private HeatmapMatrix matrix;

	private DecimalFormat dp2 = new DecimalFormat("#.##");
	private DecimalFormat sci = new DecimalFormat("0.0E0");

	public HeatmapFilterOptions (HeatmapMatrix matrix) {

		this.matrix = matrix;

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.1;
		gbc.weighty=0.9;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill=GridBagConstraints.HORIZONTAL;

		add(new JPanel(),gbc);

		gbc.weighty=0.1;
		gbc.gridy++;


		add(new JLabel("Min Strength (Obs/Exp)"),gbc);

		gbc.gridx=2;
		minStrengthField = new JTextField(""+matrix.initialMinStrength(),8);
		minStrengthField.addKeyListener(new NumberKeyListener(true, false,matrix.maxValue()));
		minStrengthField.addKeyListener(this);
		add(minStrengthField,gbc);

		gbc.gridx=3;
		gbc.weightx=0.9;
		minStrengthSlider = new JSlider(0,1000,getSliderPosition(matrix.initialMinStrength(),matrix.initialMinStrength(),matrix.maxValue()));
		minStrengthSlider.addChangeListener(this);
		add(minStrengthSlider,gbc);

		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridy++;


		add(new JLabel("Max P-value"),gbc);

		gbc.gridx=2;
		maxSignificanceField = new JTextField(sci.format(matrix.initialMaxSignificance()),8);
		maxSignificanceField.addKeyListener(new NumberKeyListener(true, false,1));
		maxSignificanceField.addKeyListener(this);
		add(maxSignificanceField,gbc);

		gbc.gridx=3;
		gbc.weightx=0.9;
		maxSignificanceSlider = new JSlider(0,1000,getSliderPosition(Math.log10(matrix.initialMaxSignificance())*-10, 0, 50));
		maxSignificanceSlider.addChangeListener(this);
		add(maxSignificanceSlider,gbc);

		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridy++;


		add(new JLabel("Min Absolute"),gbc);

		gbc.gridx=2;
		minAbsoluteField = new JTextField(""+matrix.initialMinAbsolute(),8);
		minAbsoluteField.addKeyListener(new NumberKeyListener(false, false,100));
		minAbsoluteField.addKeyListener(this);
		add(minAbsoluteField,gbc);

		gbc.gridx=3;
		gbc.weightx=0.9;
		minAbsoluteSlider = new JSlider(0,1000,getSliderPosition(matrix.initialMinAbsolute(), 0, 100));
		minAbsoluteSlider.addChangeListener(this);
		add(minAbsoluteSlider,gbc);

		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridy++;

		add(new JLabel("Min Distance"),gbc);

		gbc.gridx=2;
		minDistField = new JTextField(""+matrix.initialMinDistance(),8);
		minDistField.addKeyListener(new NumberKeyListener(false,false,SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
		minDistField.addKeyListener(this);
		add(minDistField,gbc);

		gbc.gridx=3;
		gbc.weightx=0.9;
		minDistSlider = new JSlider(0,1000,getSliderPosition(matrix.initialMinDistance(), matrix.initialMinDistance(), SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
		minDistSlider.addChangeListener(this);
		add(minDistSlider,gbc);

		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridy++;

		add(new JLabel("Max Distance (0 for any)"),gbc);

		gbc.gridx=2;
		maxDistField = new JTextField(""+matrix.initialMaxDistance(),8);
		maxDistField.addKeyListener(new NumberKeyListener(false,false,SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
		maxDistField.addKeyListener(this);
		add(maxDistField,gbc);

		gbc.gridx=3;
		gbc.weightx=0.9;
		maxDistSlider = new JSlider(0,1000,getSliderPosition(matrix.initialMaxDistance(), 0, SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
		maxDistSlider.addChangeListener(this);
		add(maxDistSlider,gbc);


		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridy++;

		add(new JLabel("Cluster R Value"),gbc);

		gbc.gridx=2;
		rValueField = new JTextField(""+matrix.currentClusterRValue(),8);
		rValueField.addKeyListener(new NumberKeyListener(true,true,SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
		rValueField.addKeyListener(this);
		add(rValueField,gbc);

		gbc.gridx=3;
		gbc.weightx=0.9;
		rValueSlider = new JSlider(0,1000,getSliderPosition(matrix.currentClusterRValue(),0,1));
		rValueSlider.addChangeListener(this);
		add(rValueSlider,gbc);

		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridy++;

		add(new JLabel("Colour By"),gbc);

		gbc.gridx=2;
		gbc.gridwidth=2;
		colourByOptions = new JComboBox(new String [] {"Obs/Exp","P-value","Interactions","Current Quantitation"});
		colourByOptions.addActionListener(this);
		add(colourByOptions,gbc);

		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridy++;

		add(new JLabel("Colour Gradient"),gbc);

		gbc.gridx=2;
		gbc.gridwidth=2;
		colourGradientOptions = new JComboBox(GradientFactory.getGradients());
		colourGradientOptions.addActionListener(this);
		add(colourGradientOptions,gbc);
		
		gbc.gridx=1;
		gbc.weightx=0.1;
		gbc.gridwidth = 1;
		gbc.gridy++;

		add(new JLabel("Filter by list"),gbc);

		gbc.gridx=2;
		gbc.gridwidth = 2;
		ProbeList [] lists = SeqMonkApplication.getInstance().dataCollection().probeSet().getAllProbeLists();
		lists[0] = null; // This is where all probes would be
		probeLists = new JComboBox(lists);
		probeLists.addActionListener(this);
		add(probeLists,gbc);

		gbc.weighty=0.9;
		gbc.gridy++;
		add(new JPanel(),gbc);


	}



	private int getSliderPosition (double value, double min, double max) {
		// Sliders go between 0 and 1000
		int position = (int)(1000*((value-min)/(max-min)));
		if (position < 0) position = 0;
		if (position > 1000) position = 1000;

		return position;
	}

	private double getValueFromSlider (int sliderValue, double min, double max) {
		return min + ((max-min)*(sliderValue/1000d));
	}


	public void keyPressed(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void keyReleased(KeyEvent ke) {

		JTextField source = (JTextField)ke.getSource();
		Double value = null;

		try {
			value = Double.parseDouble(source.getText());
		}
		catch (NumberFormatException nfe) {}

		if (source == minStrengthField) {
			if (value == null) {
				minStrengthSlider.setValue(0);
			}
			else {
				matrix.setMinStrength(value);
				minStrengthSlider.setValue(getSliderPosition(value, matrix.initialMinStrength(), matrix.maxValue()));
			}
		}

		if (source == minAbsoluteField) {
			if (value == null) {
				minAbsoluteSlider.setValue(0);
			}
			else {
				matrix.setMinAbsolute(value.intValue());
				minAbsoluteSlider.setValue(getSliderPosition(value, matrix.initialMinAbsolute(), 100));
			}
		}

		else if (source == minDistField) {
			if (value == null) {
				minDistSlider.setValue(0);
			}
			else {
				minDistSlider.setValue(getSliderPosition(value, matrix.initialMinDistance(), SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
				matrix.setMinDistance(value.intValue());
			}
		}

		else if (source == maxDistField) {
			if (value == null) {
				maxDistSlider.setValue(0);
			}
			else {
				maxDistSlider.setValue(getSliderPosition(value, 0, SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
				matrix.setMaxDistance(value.intValue());
			}
		}

		else if (source == maxSignificanceField) {
			if (value == null) {
				maxSignificanceSlider.setValue(0);
			}
			else {
				maxSignificanceSlider.setValue(getSliderPosition(Math.log10(value)*-10, 0, 50));
				matrix.setMaxSignificance(value);
			}
		}

		else if (source == rValueField) {
			if (value == null) {
				rValueSlider.setValue(0);
			}
			else {
				rValueSlider.setValue(getSliderPosition(value, -1, 1));
				matrix.setClusterRValue(value.floatValue());
			}
		}

	}


	public void stateChanged(ChangeEvent ce) {
		JSlider source = (JSlider)ce.getSource();

		if (!source.hasFocus()) return;

		if (source == minStrengthSlider) {
			minStrengthField.setText(dp2.format(getValueFromSlider(source.getValue(), matrix.initialMinStrength(), matrix.maxValue())));
			matrix.setMinStrength(getValueFromSlider(source.getValue(), matrix.initialMinStrength(), matrix.maxValue()));
		}
		else if (source == minAbsoluteSlider) {
			minAbsoluteField.setText(""+(int)getValueFromSlider(source.getValue(), matrix.initialMinAbsolute(), 100));
			matrix.setMinAbsolute((int)getValueFromSlider(source.getValue(), matrix.initialMinAbsolute(), 100));
		}
		else if (source == minDistSlider) {
			minDistField.setText(""+(int)getValueFromSlider(source.getValue(), matrix.initialMinDistance(), SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
			matrix.setMinDistance((int)getValueFromSlider(source.getValue(), matrix.initialMinDistance(), SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
		}
		else if (source == maxDistSlider) {
			maxDistField.setText(""+(int)getValueFromSlider(source.getValue(), 0, SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
			matrix.setMaxDistance((int)getValueFromSlider(source.getValue(), 0, SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength()));
		}
		else if (source == maxSignificanceSlider) {

			double sliderValue = getValueFromSlider(source.getValue(), 0, 50);
			double pValue = Math.pow(10, sliderValue/-10);

			maxSignificanceField.setText(sci.format(pValue));
			matrix.setMaxSignificance(pValue);
		}
		else if (source == rValueSlider) {
			rValueField.setText(dp2.format(getValueFromSlider(source.getValue(), 0, 1)));
			matrix.setClusterRValue((float)getValueFromSlider(source.getValue(), 0, 1));
		}		
	}



	public void actionPerformed(ActionEvent e) {

		if (e.getSource().equals(colourByOptions)) {
			// This is the colour change options
			String colourOption = colourByOptions.getSelectedItem().toString();

			if (colourOption.equals("Obs/Exp")) {
				matrix.setColour(HeatmapMatrix.COLOUR_BY_OBS_EXP);
			}
			else if (colourOption.equals("P-value")) {
				matrix.setColour(HeatmapMatrix.COLOUR_BY_P_VALUE);
			}
			else if (colourOption.equals("Interactions")) {
				matrix.setColour(HeatmapMatrix.COLOUR_BY_INTERACTIONS);
			}
			else if (colourOption.equals("Current Quantitation")) {
				matrix.setColour(HeatmapMatrix.COLOUR_BY_QUANTITATION);
			}
			else {
				throw new IllegalArgumentException("Unknown colour option "+colourOption);
			}
		}
		else if (e.getSource().equals(colourGradientOptions)) {
			matrix.setColourGradient((ColourGradient)colourGradientOptions.getSelectedItem());
		}
		else if (e.getSource().equals(probeLists)) {
			matrix.setProbeFilterList((ProbeList)probeLists.getSelectedItem());
		}
	}

}
