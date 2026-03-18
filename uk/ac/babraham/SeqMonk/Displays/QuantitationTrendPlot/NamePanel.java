/**
 * Copyright Copyright 2018- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;

public class NamePanel extends JPanel {

	DataStore [] stores;
	ProbeList [] lists;
	ReplicateSet [] repSets = new ReplicateSet[0];
	private int maxWidth = 0;

	
	public NamePanel (DataStore [] stores, ProbeList [] lists) {
		
		this.stores = stores;
		this.lists = lists;
		calculateMaxWidth();
		
	}
	
	private void calculateMaxWidth() {		
		
		FontMetrics fm = this.getFontMetrics(this.getFont());
		
		if (repSets.length == 0) {
		
			for (int d=0;d<stores.length;d++) {
				for (int l=0;l<lists.length;l++) {
					String text = stores[d].name()+" - "+lists[l].name();
					int yWidth = fm.stringWidth(text)+6;
					if (yWidth > maxWidth) {
						maxWidth = yWidth;
					}
				}
			}	
		}
		else {
			for (int d=0;d<repSets.length;d++) {
				for (int l=0;l<lists.length;l++) {
					String text = repSets[d].name()+" - "+lists[l].name();
					int yWidth = fm.stringWidth(text)+6;
					if (yWidth > maxWidth) {
						maxWidth = yWidth;
					}
				}
			}	
		}
	}
	
	public void setReplicateSets(ReplicateSet[] repSets) {
		this.repSets = repSets;
		calculateMaxWidth();
		repaint();
	}
	
	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (repSets.length == 0) {
			for (int d=0;d<stores.length;d++) {
				for (int l=0;l<lists.length;l++) {
					g.setColor(ColourIndexSet.getColour((lists.length*d)+l));
	
					String text = stores[d].name()+" - "+lists[l].name();
					
					g.drawString(text, 3, ((lists.length*d)+l+1)*g.getFontMetrics().getHeight());
	
				}
			}
		}
		else {
			for (int d=0;d<repSets.length;d++) {
				for (int l=0;l<lists.length;l++) {
					g.setColor(ColourIndexSet.getColour((lists.length*d)+l));
	
					String text = repSets[d].name()+" - "+lists[l].name();
					
					g.drawString(text, 3, ((lists.length*d)+l+1)*g.getFontMetrics().getHeight());
	
				}
			}
			
			
		}
		
	}
	
	
	public Dimension getPreferredSize () {
		return new Dimension(maxWidth+3, 1000);
	}
	
	
	public Dimension getMinimumSize() {
		return new Dimension(maxWidth+3, 1);
	}	
	
	
}
