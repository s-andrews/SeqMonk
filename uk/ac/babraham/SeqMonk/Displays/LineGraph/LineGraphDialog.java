/**
 * Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.LineGraph;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ReplicateSetSelector;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class LineGraphDialog extends JDialog implements ActionListener {

	private LineGraphPanel [] lineGraphs;
	private JPanel graphPanel;
	private JCheckBox normaliseBox;
	private JCheckBox summariseBox;

	public LineGraphDialog (DataStore [] stores, ProbeList probes) throws SeqMonkException {
		this(stores,new ProbeList[] {probes});
		setTitle("Line Graph ["+probes.name()+"]");
	}
	
	public LineGraphDialog (DataStore [] stores, ProbeList [] probes) throws SeqMonkException {
		super(SeqMonkApplication.getInstance(),"Line Graph");
		
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
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
	
		JPanel optionsPanel = new JPanel();
		
		normaliseBox = new JCheckBox("Normalise probes");
		normaliseBox.addActionListener(this);
		normaliseBox.setActionCommand("normalise");
		optionsPanel.add(normaliseBox);

		summariseBox = new JCheckBox("Summarise Graph");
		summariseBox.addActionListener(this);
		summariseBox.setActionCommand("summarise");
		optionsPanel.add(summariseBox);

		
		JButton highlightButton = new JButton("Highlight Rep Sets");
		highlightButton.setActionCommand("highlight");
		highlightButton.addActionListener(this);
		optionsPanel.add(highlightButton);
		
		getContentPane().add(optionsPanel,BorderLayout.NORTH);
		
		graphPanel = new JPanel();
		int rows = (int)Math.sqrt(probes.length);
		int cols = probes.length/rows;
		
		if (probes.length % rows > 0) {
			cols++;
		}
		
		graphPanel.setLayout(new GridLayout(rows, cols));
		
		lineGraphs = new LineGraphPanel [probes.length];
		
		for (int g=0;g<probes.length;g++) {
			LineGraphPanel graph =  new LineGraphPanel(stores, probes[g]);
			lineGraphs[g] = graph;
			graphPanel.add(graph);
		}
		
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
		else if (ae.getActionCommand().contentEquals("highlight")) {
			ReplicateSet [] repSets = ReplicateSetSelector.selectReplicateSets();
			for (int g=0;g<lineGraphs.length;g++) {
				lineGraphs[g].setRepSets(repSets);
			}

		}
		else if (ae.getActionCommand().equals("save")) {
			ImageSaver.saveImage(graphPanel);
		}
		else if (ae.getActionCommand().equals("normalise")) {
			for (int g=0;g<lineGraphs.length;g++) {
				lineGraphs[g].setNormalise(normaliseBox.isSelected());
			}
		}
		else if (ae.getActionCommand().equals("summarise")) {
			for (int g=0;g<lineGraphs.length;g++) {
				lineGraphs[g].setSummarise(summariseBox.isSelected());
			}
		}
		else {
			throw new IllegalStateException("Unknown command "+ae.getActionCommand());
		}
	} 
	
}
