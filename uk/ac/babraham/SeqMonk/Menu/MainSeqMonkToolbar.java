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
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;

public class MainSeqMonkToolbar extends SeqMonkToolbar {

	/** The jump to position button. */
	private JButton jumpToPositionButton;
	
	/** The change annotation button. */
	private JButton changeAnnotationButton;
	
	/** The find feature button. */
	private JButton findFeatureButton;
	
	private JButton newProjectButton;
	private JButton openProjectButton;
	
	/**
	 * Instantiates a new seq monk toolbar.
	 * 
	 * @param menu the menu
	 */
	public MainSeqMonkToolbar (SeqMonkMenu menu) {
		
		super(menu);
		
		newProjectButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/new_project.png")));
		newProjectButton.setActionCommand("new");
		newProjectButton.setToolTipText("New Project");
		newProjectButton.addActionListener(menu);
		
		add(newProjectButton);

		openProjectButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/load_project.png")));
		openProjectButton.setActionCommand("open");
		openProjectButton.setToolTipText("Open Project");
		openProjectButton.addActionListener(menu);
		
		add(openProjectButton);

		
		JButton saveProjectButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/save_project.png")));
		saveProjectButton.setActionCommand("save");
		saveProjectButton.setToolTipText("Save Project");
		saveProjectButton.addActionListener(menu);
		
		add(saveProjectButton);

		addSeparator();

		JButton showVistoryButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/launch_vistory.png")));
		showVistoryButton.setActionCommand("report_vistory");
		showVistoryButton.setToolTipText("Show Vistory");
		showVistoryButton.addActionListener(menu);

		add(showVistoryButton);
		addSeparator();

		JButton viewPrefsButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/display_prefs.png")));
		viewPrefsButton.setActionCommand("view_display_options");
		viewPrefsButton.setToolTipText("Data Track Display Options");
		viewPrefsButton.addActionListener(menu);
		
		add(viewPrefsButton);
		
		addSeparator();
		
		changeAnnotationButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/change_annotation.png")));
		changeAnnotationButton.setActionCommand("view_annotation_tracks");
		changeAnnotationButton.setToolTipText("Change Annotation Tracks");
		changeAnnotationButton.addActionListener(menu);
		
		add(changeAnnotationButton);

		JButton changeDataButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/change_data.png")));
		changeDataButton.setActionCommand("view_data_tracks");
		changeDataButton.setToolTipText("Change Data Tracks");
		changeDataButton.addActionListener(menu);
		
		add(changeDataButton);

		addSeparator();
		
		JButton changeDataZoomButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/change_data_zoom.png")));
		changeDataZoomButton.setActionCommand("view_set_zoom");
		changeDataZoomButton.setToolTipText("Change Data Zoom Level");
		changeDataZoomButton.addActionListener(menu);
		
		add(changeDataZoomButton);

		addSeparator();
		
		findFeatureButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/find_feature.png")));
		findFeatureButton.setActionCommand("find_feature");
		findFeatureButton.setToolTipText("Find Feature");
		findFeatureButton.addActionListener(menu);
		
		add(findFeatureButton);
		
		jumpToPositionButton = new JButton(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/Toolbar/jump_to_position.png")));
		jumpToPositionButton.setActionCommand("goto_position");
		jumpToPositionButton.setToolTipText("Go to Position");
		jumpToPositionButton.addActionListener(menu);
		
		add(jumpToPositionButton);

		addSeparator();
		
		reset();
	}
	
	
	
	/**
	 * Genome loaded.
	 */
	public void genomeLoaded () {
		// Enable the buttons relating only to the genome
		jumpToPositionButton.setEnabled(true);
		jumpToPositionButton.setFocusable(false);
		changeAnnotationButton.setEnabled(true);
		changeAnnotationButton.setFocusable(false);
		findFeatureButton.setEnabled(true);
		findFeatureButton.setFocusable(false);
	}
	
	public boolean showByDefault () {
		return true;
	}
	
	public void reset() {
		// We can disable everything on the toolbar.
		Component [] c = getComponents();
		for (int i=0;i<c.length;i++) {
			c[i].setEnabled(false);
			c[i].setFocusable(false);
		}
		newProjectButton.setEnabled(true);
		newProjectButton.setFocusable(true);
		openProjectButton.setEnabled(true);
		openProjectButton.setFocusable(true);
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
	
	
	public void dataSetAdded(DataSet d) {
		
		// Now we can enable everything on the toolbar.
		Component [] c = getComponents();
		for (int i=0;i<c.length;i++) {
			c[i].setEnabled(true);
			c[i].setFocusable(false);
		}

	}

	public void dataSetsRemoved(DataSet[] d) {

		// If there are no datasets loaded we need to reset everything and
		// just go with the genome loaded defaults
		if (collection().getAllDataSets().length == 0) {
			reset();
			genomeLoaded();
		}
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
		return "Main Toolbar";
	}

}
