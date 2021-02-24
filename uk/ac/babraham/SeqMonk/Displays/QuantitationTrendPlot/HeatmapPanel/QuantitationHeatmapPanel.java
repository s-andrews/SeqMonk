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
package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.HeatmapPanel;

import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.QuantitationTrendData;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Utilities.DoubleVector;

public class QuantitationHeatmapPanel extends JPanel {

	private double [] data;
	private ColourGradient gradient;
	private double min;
	private double max;
	
	public QuantitationHeatmapPanel(QuantitationTrendData data, DataStore store, ProbeList list, ColourGradient gradient, double min, double max) {
		this.gradient = gradient;
		this.min = min;
		this.max = max;
		
		DoubleVector v = new DoubleVector();
		
		if (data.hasUpstream()) {
			double [] upstream = data.getUpstreamData(store, list);
			for (int d=0;d<upstream.length;d++) {
				v.add(upstream[d]);
			}
		}
		
		if (true) {
			double [] central = data.getCentralData(store, list);
			for (int d=0;d<central.length;d++) {
				v.add(central[d]);
			}
		}
		
		if (data.hasDownstream()) {
			double [] downstream = data.getDownstreamData(store, list);
			for (int d=0;d<downstream.length;d++) {
				v.add(downstream[d]);
			}
		}

		this.data = v.toArray();
		
	}

	public void setGradient (ColourGradient gradient) {
		this.gradient = gradient;
		repaint();
	}
	
	public void setLimits(double min, double max) {
		this.min = min;
		this.max = max;
		repaint();
	}
	
	public void paint (Graphics g) {
		super.paint(g);
		
		int lastX = -1;
		
		for (int d=0;d<data.length;d++) {
			int thisX = getX(d);
			if (thisX > lastX) {
				g.setColor(gradient.getColor(data[d], min, max));
				g.fillRect(lastX, 0, thisX-lastX, getHeight());
				lastX = thisX;
			}
		}
	}

	
	private int getX (int index) {
		double proportion = index/(double)(data.length-1);
		return (int)(getWidth()*proportion);
	}
	
	
}
