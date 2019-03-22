/**
 * Copyright Copyright 2010-19 Simon Andrews
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

import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Quantitation.Quantitation;

public class QuantitationTypeRenderer extends DefaultListCellRenderer {
		
	public Component getListCellRendererComponent (JList list,Object value, int index, boolean selected, boolean hasFocus) {
		JLabel l = new JLabel(value.toString());
		if (value instanceof Quantitation) {
			if (((Quantitation)value).requiresExistingQuantitation()) {
				l.setForeground(ColourScheme.EXISTING_QUANTIATION);
				l.setBackground(ColourScheme.EXISTING_QUANTIATION);				
			}
			else if (((Quantitation)value).requiresHiC()) {
				l.setForeground(ColourScheme.HIC_QUANTITATION);
				l.setBackground(ColourScheme.HIC_QUANTITATION);				
			}
			else {
				l.setForeground(ColourScheme.NON_EXISTING_QUANTITATION);
				l.setBackground(ColourScheme.NON_EXISTING_QUANTITATION);
			}
		}

		if (selected) {
			l.setForeground(Color.WHITE);
			l.setOpaque(true);
		}
		return l;
	}


}
