/**
 * Copyright 2009- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.PCAPlot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.Dialogs.OrderedReplicateSetSelector;
import uk.ac.babraham.SeqMonk.Displays.TsneDataStorePlot.TsneDataStoreResult;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class ScatterPlotDialog is a container for the options
 * and plot for a 2D scatterplot of quanitated data.
 */
public class PCAScatterPlotDialog extends JDialog implements ActionListener, ChangeListener {

	/** The scatter plot panel. */
	private JPanel scatterPlotPanel;
	
	/** The x stores. */
	private JComboBox xStores;
	
	/** The y stores. */
	private JComboBox yStores;
	
	/** The button to select sublists */
	private JButton repSetButton;
	
	/** The collection. */
	private PCASource data;;
	
	/** The dot size slider. */
	private JSlider dotSizeSlider;
	
	/** The common scale box. */
	private JCheckBox showLabelsBox;
		
	/** The set of sublists used for this plot */
	private ReplicateSet [] highlightedSets = null;
		
	/**
	 * Instantiates a new scatter plot dialog.
	 * 
	 * @param collection the collection
	 */
	public PCAScatterPlotDialog (PCASource data) {
		super(SeqMonkApplication.getInstance(),"PCA Plot");
		this.data = data;
		
		if (data instanceof TsneDataStoreResult) {
			setTitle("T-SNE Plot ["+data.probeListName()+"]");
		}
		else {
			setTitle("PCA Plot ["+data.probeListName()+"]");
		}
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
		getContentPane().setLayout(new BorderLayout());

		scatterPlotPanel = new JPanel();
		getContentPane().add(scatterPlotPanel,BorderLayout.CENTER);
		
		JPanel optionsPanel = new JPanel();
		String [] PCList = new String[data.getPCCount()];
		
		for (int i=0;i<data.getPCCount();i++) {
			PCList[i] = "PC"+(i+1);
		}
		
		
		xStores = new JComboBox(PCList);
		xStores.setPrototypeDisplayValue("PC000");
		xStores.addActionListener(this);
		xStores.setActionCommand("plot");

		yStores = new JComboBox(PCList);
		yStores.setPrototypeDisplayValue("PC000");
		yStores.addActionListener(this);
		yStores.setActionCommand("plot");
		
		// Don't show drop down options if there aren't actually any options.
		if (PCList.length > 2) {
			optionsPanel.add(new JLabel("Plot"));
			optionsPanel.add(xStores);
			optionsPanel.add(new JLabel("vs"));
			optionsPanel.add(yStores);
		}
		
		showLabelsBox = new JCheckBox("Labels");
		showLabelsBox.setSelected(false);
		showLabelsBox.setActionCommand("plot");
		showLabelsBox.addActionListener(this);
		
		optionsPanel.add(showLabelsBox);
		
		repSetButton = new JButton("Highlight Rep Sets");
		repSetButton.setActionCommand("sublists");
		repSetButton.addActionListener(this);
		optionsPanel.add(repSetButton);
		
		getContentPane().add(optionsPanel,BorderLayout.NORTH);
		
		dotSizeSlider = new JSlider(JSlider.VERTICAL,1,100,3);

		// This call is left in to work around a bug in the Windows 7 LAF
		// which makes the slider stupidly thin in ticks are not drawn.
		dotSizeSlider.setPaintTicks(true);
		dotSizeSlider.addChangeListener(this);
		getContentPane().add(dotSizeSlider,BorderLayout.EAST);
		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Close");
		cancelButton.setActionCommand("close");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		
		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);
		
		JButton exportDataButton = new JButton("Export Data");
		exportDataButton.setActionCommand("export");
		exportDataButton.addActionListener(this);
		buttonPanel.add(exportDataButton);

		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setSize(700,720);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
		yStores.setSelectedIndex(1);
		
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("sublists")) {
			// Select a set of sublists from the current probe list to highlight
			// in the plot
			OrderedReplicateSetSelector selector = new OrderedReplicateSetSelector(this,SeqMonkApplication.getInstance().dataCollection().getAllReplicateSets(),highlightedSets);
				
			// It's modal so by the time we get here the selection has been made
			highlightedSets = selector.getOrderedSets();
				
			// If there's nothing selected we'll make this null so the plot doesn't
			// try to highlight anything.
			if (highlightedSets != null && highlightedSets.length == 0) {
				highlightedSets = null;
			}
			selector.dispose();
			
			actionPerformed(new ActionEvent(this, 1, "plot"));
			
		}
		else if (ae.getActionCommand().equals("plot")) {
			int xIndex = xStores.getSelectedIndex();
			int yIndex = yStores.getSelectedIndex();
			
			getContentPane().remove(scatterPlotPanel);

			scatterPlotPanel = new PCAScatterPlotPanel(data,xIndex,yIndex,highlightedSets,showLabelsBox.isSelected(),dotSizeSlider.getValue());
			getContentPane().add(scatterPlotPanel,BorderLayout.CENTER);
			
			validate();
			
		}
		else if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(scatterPlotPanel);
		}
		else if (ae.getActionCommand().equals("export")) {
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new TxtFileFilter());
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
			if (file.isDirectory()) return;

			if (! file.getPath().toLowerCase().endsWith(".txt")) {
				file = new File(file.getPath()+".txt");
			}
			
			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}

			try {
				data.writeExportData(file);
			}

			catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}

		
		
		else {
			throw new IllegalArgumentException("Unknown command "+ae.getActionCommand());
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {

		// The dot size slider has been moved
		if (scatterPlotPanel instanceof PCAScatterPlotPanel) {
			((PCAScatterPlotPanel) scatterPlotPanel).setDotSize(dotSizeSlider.getValue());
		}
	}
		
}
