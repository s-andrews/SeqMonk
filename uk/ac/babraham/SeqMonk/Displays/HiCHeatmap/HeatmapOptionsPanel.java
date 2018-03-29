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
package uk.ac.babraham.SeqMonk.Displays.HiCHeatmap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * This class provides a JPanel which can hold all of the initial filtering
 * options used to construct a similarity matrix.  It is complemented by
 * the HeatmapFilterOptionsPanel class which provides an interactive set
 * of filter options to use once the initial options have been set.
 * 
 * @author Simon Andrews
 *
 */

public class HeatmapOptionsPanel extends JPanel {

	private JTextField minDistField;
	private JTextField maxDistField;
	private JTextField minStrengthField;
	private JTextField maxSignificanceField;
	private JTextField minAbsoluteField;
	private JCheckBox correctLinkageBox;
	
	public HeatmapOptionsPanel (int minDist, int maxDist, float minStrength, float maxSignificance, int minAbsolute, boolean correctLinkage) {

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=1;
		gbc.weighty=1;
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);

		add(new JLabel("Min interaction distance (bp)"),gbc);

		gbc.gridx++;
		minDistField = new JTextField(""+minDist,5);
		minDistField.addKeyListener(new NumberKeyListener(false, false));
		add(minDistField,gbc);

		gbc.gridx=1;
		gbc.gridy++;
		
		add(new JLabel("Max interaction distance (bp) (0=any)"),gbc);

		gbc.gridx++;
		maxDistField = new JTextField(""+maxDist,5);
		maxDistField.addKeyListener(new NumberKeyListener(false, false));
		add(maxDistField,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Min interation strength (Obs/Exp)"),gbc);

		gbc.gridx++;
		minStrengthField = new JTextField(""+minStrength,5);
		minStrengthField.addKeyListener(new NumberKeyListener(true, false));
		add(minStrengthField,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Max P-value"),gbc);

		gbc.gridx++;
		maxSignificanceField = new JTextField(""+maxSignificance,5);
		maxSignificanceField.addKeyListener(new NumberKeyListener(true, false));
		add(maxSignificanceField,gbc);

		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Min absolute count"),gbc);

		gbc.gridx++;
		minAbsoluteField = new JTextField(""+minAbsolute,5);
		minAbsoluteField.addKeyListener(new NumberKeyListener(false, false));
		add(minAbsoluteField,gbc);
		
		gbc.gridx=1;
		gbc.gridy++;

		add(new JLabel("Correct for physical linkage"),gbc);

		gbc.gridx++;
		correctLinkageBox = new JCheckBox();
		correctLinkageBox.setSelected(correctLinkage);
		add(correctLinkageBox,gbc);
	}
	
	public int minDistance () {
		
		if (minDistField.getText().length() > 0) {
			return Integer.parseInt(minDistField.getText());
		}
		return 0;
	}
	
	public int maxDistance () {
		
		if (maxDistField.getText().length() > 0) {
			return Integer.parseInt(maxDistField.getText());
		}
		return 0;
	}
	
	public float minStrength () {
		if (minStrengthField.getText().length() > 0) {
			return Float.parseFloat(minStrengthField.getText());
		}
		return 1;
	}
	
	public float maxSignificance () {
		if (maxSignificanceField.getText().length() > 0) {
			return Float.parseFloat(maxSignificanceField.getText());
		}
		return 1;
	}
	
	public int minAbsolute () {
		if (minAbsoluteField.getText().length() > 0) {
			return Integer.parseInt(minAbsoluteField.getText());
		}
		return 1;
	}
	
	public boolean correctLinkage () {
		return correctLinkageBox.isSelected();
	}

	
}
