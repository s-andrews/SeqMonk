/**
 * Copyright 2012-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Preferences.Editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

public class DisplayPreferencesEditorPanel extends JPanel implements ActionListener {

	private JComboBox displayModeBox;
	private JComboBox readDisplayBox;
	private JComboBox graphTypeBox;
	private JComboBox readDensityBox;
	private JComboBox colourTypeBox;
	private JComboBox gradientBox;
	private JCheckBox invertGradientCheckbox;
	private JComboBox scaleTypeBox;
	private JComboBox replicateExpansionBox;
	private JComboBox variabilityRepresentationBox;
	
	
	public DisplayPreferencesEditorPanel () {

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		
		add(new JLabel("Display which data"),gbc);
		gbc.gridx=2;
		NamedValueObject [] displayModeData = getNamedValueArray(new String [] {"Reads Only","Probes Only","Reads and Probes"}, new int []{DisplayPreferences.DISPLAY_MODE_READS_ONLY,DisplayPreferences.DISPLAY_MODE_QUANTITATION_ONLY,DisplayPreferences.DISPLAY_MODE_READS_AND_QUANTITATION});
		displayModeBox = new JComboBox(displayModeData);
		displayModeBox.setRenderer(new PreferencesListCellRenderer(displayModeData));
		selectDefault(displayModeBox, DisplayPreferences.getInstance().getDisplayMode());
		displayModeBox.addActionListener(this);
		add(displayModeBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Display reads with"),gbc);
		gbc.gridx=2;
		NamedValueObject [] readDisplayData = getNamedValueArray(new String [] {"Separated Strands","Combined Strands"}, new int []{DisplayPreferences.READ_DISPLAY_SEPARATED,DisplayPreferences.READ_DISPLAY_COMBINED});
		readDisplayBox = new JComboBox(readDisplayData);
		readDisplayBox.setRenderer(new PreferencesListCellRenderer(readDisplayData));
		selectDefault(readDisplayBox, DisplayPreferences.getInstance().getReadDisplay());
		readDisplayBox.addActionListener(this);
		add(readDisplayBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Raw Read Display Density"),gbc);
		gbc.gridx=2;
		NamedValueObject [] readDensityData = getNamedValueArray(new String [] {"Low Density","Medium Density","High Density"}, new int []{DisplayPreferences.READ_DENSITY_LOW,DisplayPreferences.READ_DENSITY_MEDIUM,DisplayPreferences.READ_DENSITY_HIGH});
		readDensityBox = new JComboBox(readDensityData);
		readDensityBox.setRenderer(new PreferencesListCellRenderer(readDensityData));
		selectDefault(readDensityBox, DisplayPreferences.getInstance().getReadDensity());
		readDensityBox.addActionListener(this);
		add(readDensityBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Replicate Set Display"),gbc);
		gbc.gridx=2;
		replicateExpansionBox = new JComboBox(getNamedValueArray(new String [] {"Expanded","Compressed"}, new int []{DisplayPreferences.REPLICATE_SETS_EXPANDED,DisplayPreferences.REPLICATE_SETS_COMPRESSED}));
		selectDefault(replicateExpansionBox, DisplayPreferences.getInstance().getReplicateSetExpansion());
		replicateExpansionBox.addActionListener(this);
		add(replicateExpansionBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Replicate Set Variability"),gbc);
		gbc.gridx=2;
		variabilityRepresentationBox = new JComboBox(getNamedValueArray(new String [] {"None","StDev","SEM","MinMax","Points"}, new int []{DisplayPreferences.VARIATION_NONE,DisplayPreferences.VARIATION_STDEV,DisplayPreferences.VARIATION_SEM,DisplayPreferences.VARIATION_MAX_MIN,DisplayPreferences.VARIATION_POINTS}));
		selectDefault(variabilityRepresentationBox, DisplayPreferences.getInstance().getVariation());
		variabilityRepresentationBox.addActionListener(this);
		add(variabilityRepresentationBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		
		add(new JLabel("Display Quantitated Data as "),gbc);
		gbc.gridx=2;
		NamedValueObject [] graphTypeData = getNamedValueArray(new String [] {"Bars","Lines","Points","Blocks"}, new int []{DisplayPreferences.GRAPH_TYPE_BAR,DisplayPreferences.GRAPH_TYPE_LINE,DisplayPreferences.GRAPH_TYPE_POINT,DisplayPreferences.GRAPH_TYPE_BLOCK});
		graphTypeBox = new JComboBox(graphTypeData);
		graphTypeBox.setRenderer(new PreferencesListCellRenderer(graphTypeData));
		selectDefault(graphTypeBox, DisplayPreferences.getInstance().getGraphType());
		graphTypeBox.addActionListener(this);
		add(graphTypeBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		
		
		add(new JLabel("Quantitated Data Scale "),gbc);
		gbc.gridx=2;
		NamedValueObject [] scaleTypeData = getNamedValueArray(new String [] {"Positive Only","Positive and Negative"}, new int []{DisplayPreferences.SCALE_TYPE_POSITIVE,DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE});
		scaleTypeBox = new JComboBox(scaleTypeData);
		scaleTypeBox.setRenderer(new PreferencesListCellRenderer(scaleTypeData));
		selectDefault(scaleTypeBox, DisplayPreferences.getInstance().getScaleType());
		scaleTypeBox.addActionListener(this);
		add(scaleTypeBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;
		

		
		add(new JLabel("Quantitation Colour Scheme"),gbc);
		gbc.gridx=2;
		NamedValueObject [] colourTypeData = getNamedValueArray(new String [] {"Gradient Colours","Indexed Colours"}, new int []{DisplayPreferences.COLOUR_TYPE_GRADIENT,DisplayPreferences.COLOUR_TYPE_INDEXED});
		colourTypeBox = new JComboBox(colourTypeData);
		colourTypeBox.setRenderer(new PreferencesListCellRenderer(colourTypeData));
		selectDefault(colourTypeBox, DisplayPreferences.getInstance().getColourType());
		colourTypeBox.addActionListener(this);
		add(colourTypeBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		
		add(new JLabel("Colour Gradient Type"),gbc);
		gbc.gridx=2;
		NamedValueObject [] gradientData = getNamedValueArray(new String [] {"Cold - Hot","Red - Green","Magenta - Green","Red - White","Greyscale"}, new int []{DisplayPreferences.GRADIENT_HOT_COLD,DisplayPreferences.GRADIENT_RED_GREEN,DisplayPreferences.GRADIENT_MAGENTA_GREEN,DisplayPreferences.GRADIENT_RED_WHITE,DisplayPreferences.GRADIENT_GREYSCALE});
		gradientBox = new JComboBox(gradientData);
		gradientBox.setRenderer(new PreferencesListCellRenderer(gradientData));
		selectDefault(gradientBox, DisplayPreferences.getInstance().getGradientValue());
		gradientBox.addActionListener(this);
		add(gradientBox,gbc);
		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Invert Gradient"),gbc);
		gbc.gridx=2;
		invertGradientCheckbox = new JCheckBox();
		invertGradientCheckbox.setSelected(DisplayPreferences.getInstance().getInvertGradient());
		invertGradientCheckbox.addActionListener(this);
		add(invertGradientCheckbox,gbc);
		gbc.gridx=1;
		gbc.gridy++;
		
		
	}
	
	private void selectDefault(JComboBox box, int value) {
		for (int i=0;i<box.getModel().getSize();i++) {
			if (((NamedValueObject)box.getModel().getElementAt(i)).value() == value) {
				box.setSelectedIndex(i);
				return;
			}
		}
	}
	
	private NamedValueObject [] getNamedValueArray (String [] names, int [] values) {
		NamedValueObject [] objects = new NamedValueObject[names.length];
		for (int i=0;i<names.length;i++) {
			objects[i] = new NamedValueObject(names[i], values[i]);
		}
		
		return objects;
	}
	
	



	public void actionPerformed(ActionEvent ae) {
		Object source = ae.getSource();
		
		if (source.equals(displayModeBox)) DisplayPreferences.getInstance().setDisplayMode(((NamedValueObject)displayModeBox.getSelectedItem()).value());
		if (source.equals(readDisplayBox)) DisplayPreferences.getInstance().setReadDisplay(((NamedValueObject)readDisplayBox.getSelectedItem()).value());
		if (source.equals(graphTypeBox)) DisplayPreferences.getInstance().setGraphType(((NamedValueObject)graphTypeBox.getSelectedItem()).value());
		if (source.equals(readDensityBox)) DisplayPreferences.getInstance().setReadDensity(((NamedValueObject)readDensityBox.getSelectedItem()).value());
		if (source.equals(colourTypeBox)) DisplayPreferences.getInstance().setColourType(((NamedValueObject)colourTypeBox.getSelectedItem()).value());
		if (source.equals(gradientBox)) DisplayPreferences.getInstance().setGradient(((NamedValueObject)gradientBox.getSelectedItem()).value());
		if (source.equals(scaleTypeBox)) DisplayPreferences.getInstance().setScaleType(((NamedValueObject)scaleTypeBox.getSelectedItem()).value());
		if (source.equals(invertGradientCheckbox)) DisplayPreferences.getInstance().setInvertGradient(invertGradientCheckbox.isSelected());
		if (source.equals(replicateExpansionBox)) DisplayPreferences.getInstance().setReplicateSetExpansion(((NamedValueObject)replicateExpansionBox.getSelectedItem()).value());
		if (source.equals(variabilityRepresentationBox)) DisplayPreferences.getInstance().setVariation(((NamedValueObject)variabilityRepresentationBox.getSelectedItem()).value());
		
	}
	
}
