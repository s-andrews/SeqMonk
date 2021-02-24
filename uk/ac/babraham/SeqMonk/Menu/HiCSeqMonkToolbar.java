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
package uk.ac.babraham.SeqMonk.Menu;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

public class HiCSeqMonkToolbar extends SeqMonkToolbar {

	
	/**
	 * Instantiates a new seq monk toolbar.
	 * 
	 * @param menu the menu
	 */
	public HiCSeqMonkToolbar (SeqMonkMenu menu) {
		
		super(menu);
		
		JButton make4Cvisible = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/make_4c_dataset.png")));
		make4Cvisible.setActionCommand("import_hic");
		make4Cvisible.setToolTipText("Make 4C from current visible region");
		make4Cvisible.addActionListener(menu);
		
		add(make4Cvisible);

		JButton heatmapGenome = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/hiC_genome_heatmap.png")));
		heatmapGenome.setActionCommand("view_heatmap_active");
		heatmapGenome.setToolTipText("Genome Heatmap");
		heatmapGenome.addActionListener(menu);
		
		add(heatmapGenome);
		
		JButton heatmapProbeLists = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/hiC_probe_heatmap.png")));
		heatmapProbeLists.setActionCommand("multiprobe_view_heatmap_active");
		heatmapProbeLists.setToolTipText("Probe List Heatmap");
		heatmapProbeLists.addActionListener(menu);
		
		add(heatmapProbeLists);
		
		addSeparator();
		
		reset();
	}
	
	
	
	/**
	 * Genome loaded.
	 */
	public void genomeLoaded () {		
		// Don't do anything since all functions rely on data
	}
	
	public boolean showByDefault () {
		return false;
	}
	
	public void reset() {
		// We can disable everything on the toolbar.
		Component [] c = getComponents();
		for (int i=0;i<c.length;i++) {
			c[i].setEnabled(false);
			c[i].setFocusable(false);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#setEnabled(boolean)
	 */
	public void setEnabled (boolean enable) {
		
		/*
		 * Disabling the toolbar still leaves the buttons active
		 * which isn't what we want so we'll disable everything
		 * inside it as well.
		 * 
		 * We also don't want the toolbar taking focus as it breaks
		 * the navigation using arrow keys so we explicitly disable this.
		 */
		
		super.setEnabled(enable);
		
		Component [] c = getComponents();
		for (int i=0;i<c.length;i++) {
			c[i].setEnabled(enable);
			c[i].setFocusable(false);
		}
		
	}
	
	private void checkForHiCData () {

		DataStore [] stores = collection().getAllDataStores();
		
		for (int i=0;i<stores.length;i++) {
			if (stores[i] instanceof HiCDataStore && ((HiCDataStore)stores[i]).isValidHiC()) {
				
				// Enable everything
				setEnabled(true);
				return;
			}
		}
		
		setEnabled(false);
	}
	
	public void dataSetAdded(DataSet d) {
		
		// Check to see if we have any HiC datasets
		// and enable everything if we do
		checkForHiCData();
	}

	public void dataSetsRemoved(DataSet[] d) {
		// Check to see if we have any HiC datasets
		// and enable everything if we do
		checkForHiCData();
	}

	public void dataGroupAdded(DataGroup g) {}

	public void dataGroupsRemoved(DataGroup[] g) {}

	public void dataSetRenamed(DataSet d, String oldName) {}

	public void dataGroupRenamed(DataGroup g, String oldName) {}

	public void dataGroupSamplesChanged(DataGroup g) {}

	public void probeSetReplaced(ProbeSet p) {}

	public void replicateSetAdded(ReplicateSet r) {}

	public void replicateSetsRemoved(ReplicateSet[] r) {}

	public void replicateSetRenamed(ReplicateSet r, String oldName) {}

	public void replicateSetStoresChanged(ReplicateSet r) {}

	public void activeDataStoreChanged(DataStore s) {
		
	}

	public void activeProbeListChanged(ProbeList l) {}

	public String name() {
		return "HiC Toolbar";
	}

}
