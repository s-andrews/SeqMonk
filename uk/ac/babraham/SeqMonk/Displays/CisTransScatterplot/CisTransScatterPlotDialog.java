/**
 * Copyright 2009-15-13 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.CisTransScatterplot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class ScatterPlotDialog is a container for the options
 * and plot for a 2D scatterplot of quanitated data.
 */
public class CisTransScatterPlotDialog extends JDialog implements ActionListener, ChangeListener {

	/** The scatter plot panel. */
	private JPanel scatterPlotPanel;
	
	/** The x stores. */
	private JComboBox hiCStores;
	
	private JComboBox chromosomes;
		
	/** The dot size slider. */
	private JSlider dotSizeSlider;
	
	/** The common scale box. */
	private JCheckBox commonScaleBox;
	
	private DataCollection collection;
	
	/**
	 * Instantiates a new scatter plot dialog.
	 * 
	 * @param collection the collection
	 */
	public CisTransScatterPlotDialog (DataCollection collection) {
		super(SeqMonkApplication.getInstance(),"Cis-Trans ScatterPlot");
		
		this.collection = collection;
		
		DataStore [] allStores = collection.getAllDataStores();
		
		Vector<HiCDataStore> tempHiCStores = new Vector<HiCDataStore>();
	
		for (int i=0;i<allStores.length;i++) {
			if (allStores[i] instanceof HiCDataStore && ((HiCDataStore)allStores[i]).isValidHiC()) {
				tempHiCStores.add((HiCDataStore)allStores[i]);
			}
		}
		
		if (tempHiCStores.size() == 0) {
			throw new IllegalArgumentException("No HiC stores were given to the cis/trans scatterplot");
		}
	
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
		getContentPane().setLayout(new BorderLayout());

		scatterPlotPanel = new JPanel();
		getContentPane().add(scatterPlotPanel,BorderLayout.CENTER);
		
		JPanel optionsPanel = new JPanel();
		DataStore [] rawStores = collection.getAllDataStores();
		
		Vector<DataStore>quantitatedStores = new Vector<DataStore>();
		for (int i=0;i<rawStores.length;i++) {
			if (rawStores[i].isQuantitated()) {
				quantitatedStores.add(rawStores[i]);
			}
		}
		
		//Should we throw an error if there are no quantitated stores?
		DataStore [] stores = quantitatedStores.toArray(new DataStore[0]);
		
		hiCStores = new JComboBox(stores);
		hiCStores.addActionListener(this);
		hiCStores.setActionCommand("plot");
		
		Chromosome [] chrs = collection.genome().getAllChromosomes();

		Object [] chrList = new Object [chrs.length+1];
		
		chrList[0] = "All";
		for (int i=0;i<chrs.length;i++) {
			chrList[i+1] = chrs[i];
		}
		
		optionsPanel.add(new JLabel("Plot"));
		optionsPanel.add(hiCStores);

		chromosomes = new JComboBox(chrList);
		chromosomes.addActionListener(this);
		chromosomes.setActionCommand("filter_chromosome");
		optionsPanel.add(chromosomes);
		
		commonScaleBox = new JCheckBox("Common Scale");
		commonScaleBox.setSelected(true);
		commonScaleBox.setActionCommand("plot");
		commonScaleBox.addActionListener(this);
		
		optionsPanel.add(commonScaleBox);


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

		JButton saveProbeListButton = new JButton("Save Probe List");
		saveProbeListButton.setActionCommand("save_probe_list");
		saveProbeListButton.addActionListener(this);
		buttonPanel.add(saveProbeListButton);
		
		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("plot")) {
			HiCDataStore store = (HiCDataStore)hiCStores.getSelectedItem();
			
			getContentPane().remove(scatterPlotPanel);

			scatterPlotPanel = new CisTransScatterPlotPanel(store,collection.probeSet().getActiveList(),commonScaleBox.isSelected(),dotSizeSlider.getValue());
			getContentPane().add(scatterPlotPanel,BorderLayout.CENTER);
			validate();
			
		}
		else if (ae.getActionCommand().equals("filter_chromosome")) {
			Chromosome c;
			if (chromosomes.getSelectedItem() instanceof Chromosome) {
				c = (Chromosome)chromosomes.getSelectedItem();
			}
			else {
				c = null;
			}
			
			if (scatterPlotPanel instanceof CisTransScatterPlotPanel) {
				((CisTransScatterPlotPanel)scatterPlotPanel).setChromosome(c);
			}
			
		}
		else if (ae.getActionCommand().equals("save_probe_list")) {
			if (scatterPlotPanel instanceof CisTransScatterPlotPanel) {
				ProbeList list = ((CisTransScatterPlotPanel)scatterPlotPanel).getFilteredProbes(collection.probeSet());
				if (list.getAllProbes().length == 0) {
					JOptionPane.showMessageDialog(this, "No probes were selected", "No probes", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				// Ask for a name for the list
				String groupName=null;
				while (true) {
					groupName = (String)JOptionPane.showInputDialog(this,"Enter list name","Found "+list.getAllProbes().length+" probes",JOptionPane.QUESTION_MESSAGE,null,null,list.name());
					if (groupName == null) {
						list.delete();  // Remove the list which will have been created by this stage
						return;  // They cancelled
					}
						
					if (groupName.length() == 0)
						continue; // Try again
					
					break;
				}
				list.setName(groupName);
				
			}
			
		}
		else if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(scatterPlotPanel);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {

		// The dot size slider has been moved
		if (scatterPlotPanel instanceof CisTransScatterPlotPanel) {
			((CisTransScatterPlotPanel) scatterPlotPanel).setDotSize(dotSizeSlider.getValue());
		}
	}
}
