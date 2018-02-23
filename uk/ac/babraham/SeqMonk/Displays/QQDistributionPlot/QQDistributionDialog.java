/**
 * Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.QQDistributionPlot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class QQDistributionDialog extends JDialog implements ActionListener, ChangeListener {

	private QQDistributionPanel graphPanel;
	private JSlider scaleSlider;

	public QQDistributionDialog (DataStore [] stores, ProbeList list) throws SeqMonkException {
		super(SeqMonkApplication.getInstance(),"QQ Distribution Graph ["+list.name()+"]");
		graphPanel = new QQDistributionPanel(stores, list);
		setup();
	}

	public QQDistributionDialog (DataStore store, ProbeList [] lists) throws SeqMonkException {
		super(SeqMonkApplication.getInstance(),"QQ Distribution Graph ["+store.name()+"]");
		graphPanel = new QQDistributionPanel(store, lists);
		setup();
	}

	private void setup() {
		
		getContentPane().setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel();

		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);
		
		JButton saveButton = new JButton("Save");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);
		
		scaleSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
		scaleSlider.addChangeListener(this);
		scaleSlider.setMajorTickSpacing(10);
		scaleSlider.setPaintTicks(true);

		getContentPane().add(scaleSlider,BorderLayout.EAST);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);

		getContentPane().add(graphPanel,BorderLayout.CENTER);
		
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save")) {
			ImageSaver.saveImage(graphPanel);
		}
		else {
			throw new IllegalStateException("Unknown command "+ae.getActionCommand());
		}
	}

	public void stateChanged(ChangeEvent e) {

		int value = scaleSlider.getValue();
		graphPanel.setScale(value);
	} 
	
}
