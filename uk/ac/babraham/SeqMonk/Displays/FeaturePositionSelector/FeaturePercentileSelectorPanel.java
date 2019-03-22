/**
 * Copyright 2013-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.FeaturePositionSelector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class FeaturePercentileSelectorPanel extends JPanel {

	public static final int FIXED_LENGTH = 200;
	public static final int PROPORTIONAL_LENGTH = 201;
	
	private JComboBox featureTypeBox;
	private JCheckBox useSubfeaturesCheckbox;
	private JCheckBox ignoreDirectionCheckbox;
	private JTextField probesPerFeatureField;
	private JComboBox lengthTypeBox;
	private JCheckBox includeStartEndProbes;
	private JTextField fixedLengthField;
	private JTextField lowValueField;
	private JTextField endField;

	public FeaturePercentileSelectorPanel (DataCollection collection) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=2;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2, 2, 2, 2);

		add(new JLabel("Features to design around"),gbc);
		
		gbc.gridx = 3;
		featureTypeBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
		featureTypeBox.setPrototypeDisplayValue("No longer than this please");
		add(featureTypeBox,gbc);

		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		add(new JLabel ("Split into subfeatures (exons)"),gbc);

		gbc.gridx = 3;
		useSubfeaturesCheckbox = new JCheckBox();
		useSubfeaturesCheckbox.setSelected(false);
		add(useSubfeaturesCheckbox,gbc);


		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		add(new JLabel ("Ignore feature strand information"),gbc);

		gbc.gridx = 3;
		ignoreDirectionCheckbox = new JCheckBox();
		ignoreDirectionCheckbox.setSelected(false);
		add(ignoreDirectionCheckbox,gbc);

		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		add(new JLabel ("Probes per feature"),gbc);

		gbc.gridx = 3;
		probesPerFeatureField = new JTextField(""+5);
		probesPerFeatureField.addKeyListener(new NumberKeyListener(false, false));
		add(probesPerFeatureField,gbc);
		
		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		add(new JLabel ("Probe length type"),gbc);

		gbc.gridx = 3;
		lengthTypeBox = new JComboBox(new String [] {"Proportional","Fixed"});
		add(lengthTypeBox,gbc);
		
		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		add(new JLabel ("Include start / end positions"),gbc);

		gbc.gridx = 3;
		includeStartEndProbes = new JCheckBox();
		includeStartEndProbes.setSelected(true);
		includeStartEndProbes.setEnabled(false);
		add(includeStartEndProbes,gbc);

		
		gbc.gridy++;
		gbc.gridx = 2;
		gbc.weightx = 0.1;
		add(new JLabel ("Fixed length"),gbc);

		gbc.gridx = 3;
		fixedLengthField = new JTextField(""+1000);
		fixedLengthField.addKeyListener(new NumberKeyListener(false, false));
		fixedLengthField.setEnabled(false);
		add(fixedLengthField,gbc);

		lengthTypeBox.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {
				fixedLengthField.setEnabled(lengthTypeBox.getSelectedItem().equals("Fixed"));
				includeStartEndProbes.setEnabled(lengthTypeBox.getSelectedItem().equals("Fixed"));
			}
		});
		
		
	}
	
	public String selectedFeatureType () {
		return (String)featureTypeBox.getSelectedItem();
	}
	
	public boolean useSubFeatures() {
		return useSubfeaturesCheckbox.isSelected();
	}
	
	public void setUseSubFeatures (boolean b) {
		useSubfeaturesCheckbox.setSelected(b);
	}
	
	public int lengthType () {
		if (lengthTypeBox.getSelectedItem().equals("Fixed")) {
			return FIXED_LENGTH;
		}
		return PROPORTIONAL_LENGTH;
	}
	
	public int fixedLength () {
		if (fixedLengthField.getText().length() == 0) return 1000;
		
		return Integer.parseInt(fixedLengthField.getText());
	}
	
	public int probesPerFeature () {
		if (probesPerFeatureField.getText().length() == 0) return 5;
		
		return Integer.parseInt(probesPerFeatureField.getText());
		
	}
	
	public boolean includeStartEnd () {
		return includeStartEndProbes.isSelected();
	}
	
	public boolean ignoreDirection () {
		return ignoreDirectionCheckbox.isSelected();
	}
	
	public void setIgnoreDirection (boolean b) {
		ignoreDirectionCheckbox.setSelected(b);
	}
		
	public int startOffset () {
		if (lowValueField.getText().length()>0) {
			return Integer.parseInt(lowValueField.getText());
		}
		return 0;
	}

	public int endOffset () {
		if (endField.getText().length()>0) {
			return Integer.parseInt(endField.getText());
		}
		return 0;
	}
	


}
