/**
 * Copyright 2009-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.VolcanoPlot;

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
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.OrderedListSelector;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class ScatterPlotDialog is a container for the options
 * and plot for a 2D scatterplot of quanitated data.
 */
public class VolcanoPlotDialog extends JDialog implements ActionListener, ChangeListener {

	/** The scatter plot panel. */
	private JPanel volcanoPlotPanel;
	
	/** The x stores. */
	private JComboBox diffAtrributes;
	
	/** The y stores. */
	private JComboBox significanceAttributes;
	
	/** The button to select sublists */
	private JButton subListButton;
	
	/** The collection. */
	private DataCollection collection;
	
	/** The dot size slider. */
	private JSlider dotSizeSlider;
		
	/** The probe list used for this plot */
	private ProbeList probeList;
	
	/** The set of sublists used for this plot */
	private ProbeList [] subLists = null;
		
	/**
	 * Instantiates a new scatter plot dialog.
	 * 
	 * @param collection the collection
	 */
	public VolcanoPlotDialog (DataCollection collection) {
		super(SeqMonkApplication.getInstance(),"Volcano Plot");
		this.collection = collection;
		this.probeList = collection.probeSet().getActiveList();
		
		if (collection.probeSet() != null) {
			setTitle("Volcano Plot ["+collection.probeSet().getActiveList().name()+"]");
		}
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
		getContentPane().setLayout(new BorderLayout());

		volcanoPlotPanel = new JPanel();
		getContentPane().add(volcanoPlotPanel,BorderLayout.CENTER);
		
		JPanel optionsPanel = new JPanel();
		String [] rawAttributes = probeList.getValueNames();
				
		diffAtrributes = new JComboBox(rawAttributes);
		diffAtrributes.setPrototypeDisplayValue("No longer than this");
		diffAtrributes.addActionListener(this);
		diffAtrributes.setActionCommand("plot");

		significanceAttributes = new JComboBox(rawAttributes);
		significanceAttributes.setPrototypeDisplayValue("No longer than this");
		significanceAttributes.addActionListener(this);
		significanceAttributes.setActionCommand("plot");
		
		optionsPanel.add(new JLabel("Difference"));
		optionsPanel.add(diffAtrributes);
		optionsPanel.add(new JLabel(" Significance"));
		optionsPanel.add(significanceAttributes);
				
		subListButton = new JButton("Highlight Sublists");
		subListButton.setActionCommand("sublists");
		subListButton.addActionListener(this);
		optionsPanel.add(subListButton);
		
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
		setSize(700,720);
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
		else if (ae.getActionCommand().equals("sublists")) {
			// Select a set of sublists from the current probe list to highlight
			// in the plot
			OrderedListSelector selector = new OrderedListSelector(this,probeList,subLists);
				
			// It's modal so by the time we get here the selection has been made
			subLists = selector.getOrderedLists();
				
			// If there's nothing selected we'll make this null so the plot doesn't
			// try to highlight anything.
			if (subLists != null && subLists.length == 0) {
				subLists = null;
			}
			selector.dispose();
			
			actionPerformed(new ActionEvent(this, 1, "plot"));
			
		}
		else if (ae.getActionCommand().equals("plot")) {
			String diffAttribute = (String)diffAtrributes.getSelectedItem();
			String significanceAttribute = (String)significanceAttributes.getSelectedItem();
			
			getContentPane().remove(volcanoPlotPanel);

			volcanoPlotPanel = new VolcanoPlotPanel(diffAttribute,significanceAttribute,probeList,subLists,dotSizeSlider.getValue());
			getContentPane().add(volcanoPlotPanel,BorderLayout.CENTER);
			validate();
		}
		else if (ae.getActionCommand().equals("save_probe_list")) {
			if (volcanoPlotPanel instanceof VolcanoPlotPanel) {
				ProbeList list = ((VolcanoPlotPanel)volcanoPlotPanel).getFilteredProbes(collection.probeSet());
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
			ImageSaver.saveImage(volcanoPlotPanel);
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
		if (volcanoPlotPanel instanceof VolcanoPlotPanel) {
			((VolcanoPlotPanel) volcanoPlotPanel).setDotSize(dotSizeSlider.getValue());
		}
	}
		
}
