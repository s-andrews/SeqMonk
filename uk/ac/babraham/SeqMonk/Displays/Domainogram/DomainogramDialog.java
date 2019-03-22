/**
 * Copyright 2012-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Domainogram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.GenomeViewer.ChromosomeScale;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientScaleBar;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Gradients.GradientFactory;
import uk.ac.babraham.SeqMonk.Gradients.InvertedGradient;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class DomainogramDialog extends JDialog implements ChangeListener, ActionListener {

	private DomainogramChromosomePanel [] chromosomeDomainPanels;
	private GradientScaleBar scaleBar;
	private JSlider dataZoomSlider;
	private JComboBox gradients;
	private JCheckBox invertGradient;
	private boolean positiveOnly = true;
	private JPanel domainogramPanelGroup;

	public DomainogramDialog (ProbeList probes, DataStore [] data, int [] levels, boolean ignoreBlankChromosomes, boolean trimEnds) {
		super(SeqMonkApplication.getInstance(),"Domainogram for "+probes.name());
		setBackground(Color.WHITE);

		getContentPane().setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);

		buttonPanel.add(closeButton);

		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);
				
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);

		
		// Add data zoom slider 
		
		// The slider actually ends up as an exponential scale (to the power of 2).
		// We allow 200 increments on the slider but only go up to 2**20 hence dividing
		// by 10 to get the actual power to raise to.
		dataZoomSlider = new JSlider(0,200,20);
		dataZoomSlider.setOrientation(JSlider.VERTICAL);
		dataZoomSlider.addChangeListener(this);
		dataZoomSlider.setMajorTickSpacing(10);

		// This looks a bit pants, but we need it in to work around a bug in
		// the windows 7 LAF where the slider is tiny if labels are not drawn.
		dataZoomSlider.setPaintTicks(true);

		dataZoomSlider.setSnapToTicks(false);
		dataZoomSlider.setPaintTrack(true);
		Hashtable<Integer, Component> labelTable = new Hashtable<Integer, Component>();

		for (int i=0;i<=200;i+=20) {
			labelTable.put(new Integer(i), new JLabel(""+(i/10)));
		}
		dataZoomSlider.setLabelTable(labelTable);

		dataZoomSlider.setPaintLabels(true);
		getContentPane().add(dataZoomSlider,BorderLayout.EAST);

		
		// Add the colour options to the top
		JPanel colourPanel = new JPanel();
		
		gradients = new JComboBox(GradientFactory.getGradients());
		gradients.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				updateGradients();
			}
		});
		
		colourPanel.add(new JLabel("Colour Gradient"));
		colourPanel.add(gradients);
		
		colourPanel.add(new JLabel(" Invert"));
		invertGradient = new JCheckBox();
		invertGradient.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				updateGradients();
			}
		});
		colourPanel.add(invertGradient);
		
		getContentPane().add(colourPanel,BorderLayout.NORTH);
		
		// Make up the panel of chromosome views
		domainogramPanelGroup = new JPanel();
		domainogramPanelGroup.setLayout(new BorderLayout());

		float max = (float)Math.pow(2,dataZoomSlider.getValue()/10d);
		float min = 0;
		if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE) {
			min = 0 - max;
			positiveOnly = false;
		}

		scaleBar = new GradientScaleBar(GradientFactory.getGradients()[0], min, max);
		JPanel topBottomSplit = new JPanel();
		topBottomSplit.setLayout(new GridLayout(2, 1));
		topBottomSplit.add(new JPanel());
		topBottomSplit.add(scaleBar);

		domainogramPanelGroup.add(topBottomSplit,BorderLayout.EAST);
		
		JPanel domainogramPanel = new JPanel();
		
		domainogramPanel.setLayout(new GridBagLayout());
		domainogramPanel.setBackground(Color.WHITE);
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.weightx=0.01;
		gbc.weighty=0.99;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.insets = new Insets(2, 5, 2, 5);
		gbc.fill = GridBagConstraints.BOTH;
		
		Chromosome [] chrs = SeqMonkApplication.getInstance().dataCollection().genome().getAllChromosomes();
		chromosomeDomainPanels = new DomainogramChromosomePanel [chrs.length * data.length];
		
		// If we're ignoring blanks then find the longest chromosome we're actually going
		// to display, and find the last index we're going to use.
		int lastUsableIndex = chrs.length-1;
		int longestChromosome = SeqMonkApplication.getInstance().dataCollection().genome().getLongestChromosomeLength();
		
		if (ignoreBlankChromosomes) {
			longestChromosome = 0;
			
			for (int c=0;c<chrs.length;c++) {
				Probe [] p = probes.getProbesForChromosome(chrs[c]);
				if (p.length > 0) {
					lastUsableIndex = c;
					if (chrs[c].length() > longestChromosome) longestChromosome = chrs[c].length();
				}
				
			}
		}
		
		int validChromosomes = 0;
		
		for (int c=0;c<chrs.length;c++) {

			Probe [] chrProbes = probes.getProbesForChromosome(chrs[c]);
			
			if (chrProbes.length == 0 && ignoreBlankChromosomes) continue;
			
			++validChromosomes;
			
			for (int d=0;d<data.length;d++) {
			
				JLabel nameLabel = new JLabel(data[d].name());
				nameLabel.setForeground(ColourIndexSet.getColour(d));
				domainogramPanel.add(nameLabel,gbc);
				gbc.gridx=2;
				
				JLabel chrLabel = new JLabel(chrs[c].name());
				domainogramPanel.add(chrLabel,gbc);
				gbc.weightx=0.99;
				gbc.gridx=3;
			
				chromosomeDomainPanels[c*data.length+d] = new DomainogramChromosomePanel(chrProbes, data[d], levels, (ColourGradient)gradients.getSelectedItem(), min, max,longestChromosome,trimEnds);
				domainogramPanel.add(chromosomeDomainPanels[c*data.length+d],gbc);
			
				gbc.weightx=0.01;
				gbc.gridx=1;
				gbc.gridy++;
			}
			if (data.length>1  && c != lastUsableIndex) {
				gbc.gridwidth = 3;
				gbc.weighty=0.5;
				JPanel panel = new JPanel();
				panel.setBackground(Color.WHITE);
				domainogramPanel.add(panel,gbc);
				gbc.gridwidth = 1;
				gbc.weighty=0.99;
				gbc.gridy++;
			}
		}
		
		// Add a scale bar
		gbc.weighty=0.01;
		gbc.weightx=0.99;
		gbc.gridx=3;
		
		int longestVisibleLength = longestChromosome;
		
		if (trimEnds) {
			longestVisibleLength = 0;
			for (int i=0;i<chromosomeDomainPanels.length;i++) {
				if (chromosomeDomainPanels[i] == null) continue;
				if (chromosomeDomainPanels[i].getVisibleLength() > longestVisibleLength) {
					longestVisibleLength = chromosomeDomainPanels[i].getVisibleLength();
				}
			}
		}
		
		// If we're trimming ends then don't show a scale bar if there's more than one
		// chromosome since there isn't a common scale any more
		if (!(trimEnds  && validChromosomes != 1)) {
			domainogramPanel.add(new ChromosomeScale(longestVisibleLength),gbc);			
		}
		
		
		domainogramPanelGroup.add(domainogramPanel,BorderLayout.CENTER);
		getContentPane().add(new JScrollPane(domainogramPanelGroup),BorderLayout.CENTER);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setSize(800,600);
		
		setLocationRelativeTo(SeqMonkApplication.getInstance());

		setVisible(true);
		
		setVisible(true);

	}
	
	private void updateGradients () {

		ColourGradient gradient = (ColourGradient)gradients.getSelectedItem();
		
		if (invertGradient.isSelected()) {
			gradient = new InvertedGradient(gradient);
		}
		
		for (int i=0;i<chromosomeDomainPanels.length;i++) {
			if (chromosomeDomainPanels[i] == null) continue; // We skipped it
			chromosomeDomainPanels[i].setGradient(gradient);
		}
		scaleBar.setGradient(gradient);
		
	}


	public void stateChanged(ChangeEvent ce) {
		if (ce.getSource() == dataZoomSlider) {
			float max = (float)Math.pow(2,dataZoomSlider.getValue()/10d);
			float min = 0;
			if (! positiveOnly) {
				min = 0-max;
			}
			
			for (int i=0;i<chromosomeDomainPanels.length;i++) {
				if (chromosomeDomainPanels[i] == null) continue; // We skipped it
				chromosomeDomainPanels[i].setLimits(min, max);
			}
			
			scaleBar.setLimits(min, max);
		}
	}


	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_image")) {
			ImageSaver.saveImage(domainogramPanelGroup);
		}
	}


}
