/**
 * Copyright 2014-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.SmallRNAQCPlot;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;

public class FeatureNamePanel extends JPanel {
		
	public FeatureNamePanel (String [] names) {
		setBackground(Color.WHITE);
		setOpaque(true);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		for (int i=0;i<names.length;i++) {
			JLabel l = new JLabel(names[i]);
			l.setForeground(ColourIndexSet.getColour(i));
			add(l);
		}

	}
	
	
}
