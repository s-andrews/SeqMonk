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
package uk.ac.babraham.SeqMonk.Displays.GradientScaleBar;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.RedGreenColourGradient;

public class GradientScaleBar extends JPanel {
	
	private GradientScaleAxis axis;
	private GradientPanel gradientPanel;
	
	public GradientScaleBar (ColourGradient gradient, double min, double max) {
		
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.9;
		gbc.weighty=0.5;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.BOTH;
		
		gradientPanel = new GradientPanel(gradient, GradientPanel.VERTICAL_GRADIENT);
		add(gradientPanel,gbc);
		
		gbc.weightx=0.1;
		gbc.gridx++;
		
		axis = new GradientScaleAxis(min, max);
		
		add(axis,gbc);
	}
	
	public void setLimits (double min,double max) {
		axis.setLimits(min, max);
		
		validate();	
	}
	
	public void setGradient (ColourGradient gradient) {
		gradientPanel.setGradient(gradient);
	}
	
	public static void main (String [] args) {
		JFrame frame = new JFrame("Gradient test");
		
		frame.setContentPane(new GradientScaleBar(new RedGreenColourGradient(), -5, 5));
		
		frame.setSize(50,300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
}
