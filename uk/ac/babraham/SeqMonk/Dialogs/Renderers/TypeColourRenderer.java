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
package uk.ac.babraham.SeqMonk.Dialogs.Renderers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;

public class TypeColourRenderer extends DefaultListCellRenderer {
		
	public Component getListCellRendererComponent (JList list,Object value, int index, boolean selected, boolean hasFocus) {

		// This shouldn't ever happen but we've seen it in crash reporters so hopefully this
		// will make it do something more sensible
		if (value == null) {
			return new JLabel("null");
		}
		
		JLabel l = new JLabel(value.toString());
		
		if (value instanceof HiCDataStore) {
			if (((HiCDataStore)value).isValidHiC()) {
				l.setText("[HiC] "+l.getText());
			}
		}
		
		if (value instanceof DataSet) {
			l.setForeground(ColourScheme.DATASET_LIST);
			l.setBackground(ColourScheme.DATASET_LIST);
		}
		else if (value instanceof DataGroup){
			l.setForeground(ColourScheme.DATAGROUP_LIST);
			l.setBackground(ColourScheme.DATAGROUP_LIST);
		}
		else {
			// Should only be replicate sets
			l.setForeground(ColourScheme.REPLICATE_SET_LIST);
			l.setBackground(ColourScheme.REPLICATE_SET_LIST);
			
		}

		if (selected) {
			l.setForeground(Color.WHITE);
			l.setOpaque(true);
		}
		return l;
	}


}
