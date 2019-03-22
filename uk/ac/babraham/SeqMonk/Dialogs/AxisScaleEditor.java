/**
 * Copyright 2014-19 Simon Andrews
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

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class AxisScaleEditor extends JDialog implements KeyListener {

	private axisPanel xPanel;
	private axisPanel yPanel;
	private boolean editX;
	private boolean editY;
	
	private boolean cancelled = true;
	
	private JButton setAxisButton;
	
	public AxisScaleEditor (Frame parent, boolean editX, boolean editY, double minX, double maxX, double minY, double maxY) {
		super(parent,"Edit axes");
		
		this.editX = editX;
		this.editY = editY;
		
		getContentPane().setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=0;
		gbc.gridy=0;
		gbc.weightx=0.5;
		gbc.weighty=0.99;
		gbc.fill = GridBagConstraints.BOTH;
		
		
		if (editX) {
			xPanel = new axisPanel("X-axis", minX, maxX);
			getContentPane().add(xPanel,gbc);
			gbc.gridy++;
		}
		else {
			xPanel = null;
		}

		
		if (editY) {
			yPanel = new axisPanel("Y-axis", minY, maxY);
			getContentPane().add(yPanel,gbc);
			gbc.gridy++;
		}
		else {
			yPanel = null;
		}

		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {
				setVisible(false);
				dispose();
			}
		});
		
		
		buttonPanel.add(cancelButton);
		
		setAxisButton = new JButton("Set Axes");
		setAxisButton.setEnabled(checkValid());
		setAxisButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ae) {
				cancelled = false;
				setVisible(false);
				dispose();
			}
		});
		
		
		int height = 100;
		if (editX) height +=50;
		if (editY) height +=50;
		
		
		setSize(500,height);
		setModal(true);
		setLocationRelativeTo(parent);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		
		
	}
	
	private boolean checkValid () {
		if (editX) {
			if (xPanel.maxValue()<=xPanel.minValue()) {
				return false;
			}
		}

		if (editY) {
			if (yPanel.maxValue()<=yPanel.minValue()) {
				return false;
			}
		}
		
		return true;

	}

	
	public double minX () {
		if (editX) {
			return xPanel.minValue();
		}
		else {
			throw new IllegalArgumentException("X axis was not editable");
		}
	}

	public double maxX () {
		if (editX) {
			return xPanel.maxValue();
		}
		else {
			throw new IllegalArgumentException("X axis was not editable");
		}
	}

	public double minY () {
		if (editY) {
			return yPanel.minValue();
		}
		else {
			throw new IllegalArgumentException("Y axis was not editable");
		}
	}

	public double maxY () {
		if (editY) {
			return yPanel.maxValue();
		}
		else {
			throw new IllegalArgumentException("Y axis was not editable");
		}
	}

	public boolean wasCancelled () {
		return cancelled;
	}


	private class axisPanel extends JPanel {
				
		private JTextField minField;
		private JTextField maxField;
		
		public axisPanel (String name, double min, double max) {
			
			setLayout(new GridBagLayout());
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx=0.5;
			gbc.weighty=0.2;
			gbc.gridx=0;
			gbc.gridy=0;
			
			add(new JLabel(name),gbc);
			gbc.gridy++;
			add(new JLabel("Min value"),gbc);
			
			gbc.gridx++;
			gbc.weightx=0.8;
			gbc.fill = GridBagConstraints.BOTH;
			
			minField = new JTextField(""+min,5);
			minField.addKeyListener(new NumberKeyListener(true, true));
			add(minField,gbc);
			
			gbc.gridx++;
			gbc.weightx=0.2;
			gbc.fill = GridBagConstraints.NONE;

			add(new JLabel("Max value"),gbc);
			
			gbc.gridx++;
			gbc.weightx=0.8;
			gbc.fill = GridBagConstraints.BOTH;
			
			maxField = new JTextField(""+max,5);
			maxField.addKeyListener(new NumberKeyListener(true, true));
			add(maxField,gbc);

		}
		
		public double minValue () {
			if (minField.getText().length()>0) {
				return Double.parseDouble(minField.getText());
			}
			else {
				return 0;
			}
		}
		
		public double maxValue () {
			if (maxField.getText().length()>0) {
				return Double.parseDouble(maxField.getText());
			}
			else {
				return 0;
			}
		}
		
	}


	public void keyPressed(KeyEvent ae) {
		setAxisButton.setEnabled(checkValid());
	}

	public void keyReleased(KeyEvent arg0) {}

	public void keyTyped(KeyEvent arg0) {}
	
}
