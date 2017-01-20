/**
 * Copyright Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.FeatureViewer;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

/**
 * The Class FeatureViewer shows the key/value annotations for
 * a selected feature.
 */
public class FeatureViewer extends JDialog implements MouseListener {
		
	/**
	 * Instantiates a new feature viewer.
	 * 
	 * @param feature the feature
	 * @param application the application
	 */
	
	private JTable table;
	private FeatureAnnotationTableModel model;
	private Feature feature;
	
	public FeatureViewer (Feature feature) {
		super(SeqMonkApplication.getInstance(),feature.type()+" feature "+feature.name());
	
		this.feature = feature;
		model = new FeatureAnnotationTableModel(feature);
		table = new JTable(model);
		table.setColumnSelectionAllowed(true);
		table.setRowSelectionAllowed(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(100);
		table.getColumnModel().getColumn(1).setPreferredWidth(350);
		table.addMouseListener(this);
		
		setContentPane(new JScrollPane(table));

		setSize(550,300);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}

	public void mouseClicked(MouseEvent me) {
		if (me.getClickCount() == 2) {
			
			System.err.println("Selected row is "+table.getSelectedRow()+" value is "+model.getValueAt(table.getSelectedRow(),0));
			
			if (model.getValueAt(table.getSelectedRow(),0).equals("Location")) {
				DisplayPreferences.getInstance().setLocation(SeqMonkApplication.getInstance().dataCollection().genome().getChromosome(feature.chromosomeName()).chromosome(),SequenceRead.packPosition(feature.location().start(), feature.location().end(), Location.UNKNOWN));
			}
		}
		
	}

	public void mouseEntered(MouseEvent arg0) {}

	public void mouseExited(MouseEvent arg0) {}

	public void mousePressed(MouseEvent arg0) {}

	public void mouseReleased(MouseEvent arg0) {}

}
